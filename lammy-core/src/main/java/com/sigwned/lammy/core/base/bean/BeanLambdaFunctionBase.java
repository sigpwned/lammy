/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 Andy Boothe
 * ====================================SECTION=====================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================LICENSE_END===================================
 */
package com.sigwned.lammy.core.base.bean;

import static java.util.Objects.requireNonNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sigwned.lammy.core.base.LambdaFunctionBase;
import com.sigwned.lammy.core.model.bean.ExceptionMapper;
import com.sigwned.lammy.core.model.bean.RequestContext;
import com.sigwned.lammy.core.model.bean.RequestFilter;
import com.sigwned.lammy.core.model.bean.ResponseContext;
import com.sigwned.lammy.core.model.bean.ResponseFilter;
import com.sigwned.lammy.core.util.ExceptionMappers;
import com.sigwned.lammy.core.util.GenericTypes;
import com.sigwned.lammy.core.util.MoreObjects;

public abstract class BeanLambdaFunctionBase<RequestT, ResponseT> extends LambdaFunctionBase
    implements RequestHandler<RequestT, ResponseT> {
  private final Type requestType;
  private final Type responseType;
  private final List<RequestFilter<RequestT>> requestFilters;
  private final List<ResponseFilter<RequestT, ResponseT>> responseFilters;
  private final List<ExceptionMapper<?, ResponseT>> exceptionMappers;
  private boolean initialized;

  protected BeanLambdaFunctionBase() {
    this(new BeanLambdaConfiguration());
  }

  protected BeanLambdaFunctionBase(BeanLambdaConfiguration configuration) {
    this(null, null, configuration);
  }

  protected BeanLambdaFunctionBase(Type requestType, Type responseType) {
    this(requestType, responseType, new BeanLambdaConfiguration());
  }

  protected BeanLambdaFunctionBase(Type requestType, Type responseType,
      BeanLambdaConfiguration configuration) {
    if (requestType == null)
      requestType = GenericTypes.findGenericParameter(getClass(), BeanLambdaFunctionBase.class, 0)
          .orElseThrow(() -> new IllegalArgumentException("Could not determine request type"));
    this.requestType = requireNonNull(requestType);

    if (responseType == null)
      responseType = GenericTypes.findGenericParameter(getClass(), BeanLambdaFunctionBase.class, 1)
          .orElseThrow(() -> new IllegalArgumentException("Could not determine response type"));
    this.responseType = requireNonNull(responseType);

    requestFilters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadRequestFilters(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(RequestFilter.class).iterator().forEachRemaining(requestFilters::add);
    }

    responseFilters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadResponseFilters(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(ResponseFilter.class).iterator().forEachRemaining(responseFilters::add);
    }

    exceptionMappers = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadExceptionMappers(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(ExceptionMapper.class).iterator().forEachRemaining(exceptionMappers::add);
    }
  }

  /**
   * hook
   */
  protected void completeInitialization(Context context) {}

  @Override
  public final ResponseT handleRequest(RequestT originalRequest, Context context) {
    if (initialized == false) {
      try {
        completeInitialization(context);
      } finally {
        initialized = true;
      }
    }

    final RequestContext<RequestT> requestContext = new DefaultRequestContext<>(originalRequest);

    ResponseT preparedResponse;
    try {
      // Prepare the input bean for the function
      final RequestT preparedRequest = prepareRequest(requestContext, context);

      // Run the function to get the output
      final ResponseT originalResponse = handleBeanRequest(preparedRequest, context);

      // Prepare the output bean for writing
      final ResponseContext<ResponseT> responseContext =
          new DefaultResponseContext<>(originalResponse);
      preparedResponse = prepareResponse(requestContext, responseContext, context);
    } catch (Exception e) {
      final ExceptionMapper<? extends Exception, ResponseT> exceptionMapper =
          ExceptionMappers.findExceptionMapperForException(exceptionMappers, e).orElse(null);
      if (exceptionMapper == null)
        throw e;

      try {
        final ResponseT originalError = exceptionMapper.mapExceptionTo(e, responseType, context);
        final ResponseContext<ResponseT> errorContext = new DefaultResponseContext<>(originalError);
        preparedResponse = prepareResponse(requestContext, errorContext, context);
      } catch (Exception e2) {
        // Propagate the original exception if the mapper fails
        throw e;
      }
    }

    return preparedResponse;
  }

  public abstract ResponseT handleBeanRequest(RequestT input, Context context);

  public Type getRequestType() {
    return requestType;
  }

  public Type getResponseType() {
    return responseType;
  }

  private RequestT prepareRequest(RequestContext<RequestT> requestContext, Context lambdaContext) {
    for (RequestFilter<RequestT> requestFilter : requestFilters)
      requestFilter.filterRequest(requestContext, lambdaContext);
    return requestContext.getInputValue();
  }

  protected void registerRequestFilter(RequestFilter<RequestT> requestFilter) {
    if (requestFilter == null)
      throw new NullPointerException();
    if (initialized)
      throw new IllegalStateException();
    requestFilters.add(requestFilter);
  }

  private ResponseT prepareResponse(RequestContext<RequestT> requestContext,
      ResponseContext<ResponseT> responseContext, Context lambdaContext) {
    for (ResponseFilter<RequestT, ResponseT> responseFilter : responseFilters)
      responseFilter.filterResponse(requestContext, responseContext, lambdaContext);
    return responseContext.getOutputValue();
  }

  protected void registerResponseFilter(ResponseFilter<RequestT, ResponseT> responseFilter) {
    if (responseFilter == null)
      throw new NullPointerException();
    if (initialized)
      throw new IllegalStateException();
    responseFilters.add(responseFilter);
  }

  protected <E extends Exception> void registerExceptionMapper(
      ExceptionMapper<E, ResponseT> exceptionMapper) {
    if (exceptionMapper == null)
      throw new NullPointerException();
    if (initialized)
      throw new IllegalStateException();

    // Just test that it's possible.
    ExceptionMappers.findExceptionMapperExceptionType(exceptionMapper).orElseThrow(() -> {
      return new IllegalArgumentException("Cannot find exception type for " + exceptionMapper);
    });

    // TODO Should we test for response type equivalence? Should we extract response type at all?

    exceptionMappers.add(exceptionMapper);
  }
}
