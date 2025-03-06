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
package com.sigwned.lammy.core.bean;

import static java.util.Objects.requireNonNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sigwned.lammy.core.LambdaFunctionBase;
import com.sigwned.lammy.core.util.GenericTypes;
import com.sigwned.lammy.core.util.MoreObjects;

public abstract class BeanLambdaFunctionBase<RequestT, ResponseT> extends LambdaFunctionBase
    implements RequestHandler<RequestT, ResponseT> {
  private final Type requestType;
  private final Type responseType;
  private List<RequestFilter<RequestT>> requestFilters;
  private List<ResponseFilter<RequestT, ResponseT>> responseFilters;
  private List<ExceptionMapper> exceptionMappers;
  private boolean initialized;

  protected BeanLambdaFunctionBase() {
    this(new BeanLambdaConfiguration());
  }

  protected BeanLambdaFunctionBase(BeanLambdaConfiguration configuration) {
    this.requestType =
        GenericTypes.findGenericParameter(getClass(), BeanLambdaFunctionBase.class, 0).orElseThrow(
            () -> new AssertionError("Failed to compute requestType for " + getClass()));
    this.responseType =
        GenericTypes.findGenericParameter(getClass(), BeanLambdaFunctionBase.class, 1).orElseThrow(
            () -> new AssertionError("Failed to compute responseType for " + getClass()));
    initialize(configuration);
  }

  protected BeanLambdaFunctionBase(Type requestType, Type responseType) {
    this(requestType, responseType, new BeanLambdaConfiguration());
  }

  protected BeanLambdaFunctionBase(Type requestType, Type responseType,
      BeanLambdaConfiguration configuration) {
    this.requestType = requireNonNull(requestType);
    this.responseType = requireNonNull(responseType);
    initialize(configuration);
  }

  private void initialize(BeanLambdaConfiguration configuration) {
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

  @Override
  @SuppressWarnings("unchecked")
  public final ResponseT handleRequest(RequestT originalInput, Context context) {
    initialized = true;
    try {
      // Prepare the input bean for the function
      final RequestContext<RequestT> requestContext = new DefaultRequestContext<>(originalInput);
      final RequestT preparedInput = prepareRequest(requestContext, context);

      // Run the function to get the output
      final ResponseT originalOutput = handleBeanRequest(preparedInput, context);

      // Prepare the output bean for writing
      final ResponseContext<ResponseT> responseContext =
          new DefaultResponseContext<>(originalOutput);
      final ResponseT preparedOutput = prepareResponse(requestContext, responseContext, context);

      // Return the prepared output
      return preparedOutput;
    } catch (Exception e) {
      final ExceptionMapper exceptionMapper = exceptionMappers.stream()
          .filter(em -> em.canMapException(e, responseType)).findFirst().orElse(null);
      if (exceptionMapper == null)
        throw e;

      final ResponseT mappedException;
      try {
        mappedException = (ResponseT) exceptionMapper.mapExceptionTo(e, responseType, context);
      } catch (Exception e2) {
        // Propagate the original exception if the mapper fails
        throw e;
      }

      return mappedException;
    }
  }

  public abstract ResponseT handleBeanRequest(RequestT input, Context context);

  public Type getRequestType() {
    return requestType;
  }

  public Type getResponseType() {
    return responseType;
  }

  private RequestT prepareRequest(RequestContext<RequestT> requestContext,
      Context lambdaContext) {
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

  protected void registerExceptionMapper(ExceptionMapper exceptionMapper) {
    if (exceptionMapper == null)
      throw new NullPointerException();
    if (initialized)
      throw new IllegalStateException();
    exceptionMappers.add(exceptionMapper);
  }
}
