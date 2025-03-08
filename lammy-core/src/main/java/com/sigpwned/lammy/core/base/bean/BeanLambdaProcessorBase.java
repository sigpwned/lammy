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
package com.sigpwned.lammy.core.base.bean;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sigpwned.lammy.core.model.bean.ExceptionMapper;
import com.sigpwned.lammy.core.model.bean.RequestContext;
import com.sigpwned.lammy.core.model.bean.RequestFilter;
import com.sigpwned.lammy.core.model.bean.ResponseContext;
import com.sigpwned.lammy.core.model.bean.ResponseFilter;
import com.sigpwned.lammy.core.util.ExceptionMappers;
import com.sigpwned.lammy.core.util.GenericTypes;
import com.sigpwned.lammy.core.util.MoreObjects;

/**
 * A base class for a Lambda function that accepts a bean as input and returns a bean as output. It
 * uses platform serialization, possibly using a custom serializer as loaded by the platform. It
 * also supports {@link RequestFilter}s and {@link ResponseFilter}s for preprocessing the input and
 * output, respsectively. It also supports {@link ExceptionMapper}s for mapping exceptions to
 * responses, if desired.
 *
 * @param <RequestT> The type of the input bean
 * @param <ResponseT> The type of the output bean
 *
 * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html">
 *      https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html</a>
 */
public abstract class BeanLambdaProcessorBase<RequestT, ResponseT>
    extends BeanLambdaBase<RequestT, ResponseT> implements RequestHandler<RequestT, ResponseT> {
  private final Type responseType;
  private final List<ResponseFilter<RequestT, ResponseT>> responseFilters;
  private final List<ExceptionMapper<?, ResponseT>> exceptionMappers;
  private boolean initialized;

  protected BeanLambdaProcessorBase() {
    this(new BeanLambdaProcessorConfiguration());
  }

  protected BeanLambdaProcessorBase(BeanLambdaProcessorConfiguration configuration) {
    this(null, null, configuration);
  }

  protected BeanLambdaProcessorBase(Type requestType, Type responseType) {
    this(requestType, responseType, new BeanLambdaProcessorConfiguration());
  }

  protected BeanLambdaProcessorBase(Type requestType, Type responseType,
      BeanLambdaProcessorConfiguration configuration) {
    super(requestType, BeanLambdaConfiguration.fromProcessorConfiguration(configuration));

    if (responseType == null)
      responseType = GenericTypes.findGenericParameter(getClass(), BeanLambdaProcessorBase.class, 1)
          .orElseThrow(() -> new IllegalArgumentException("Could not determine response type"));
    this.responseType = requireNonNull(responseType);

    responseFilters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadResponseFilters(), getAutoloadAll())
        .orElse(false)) {
      ServiceLoader.load(ResponseFilter.class).iterator().forEachRemaining(responseFilters::add);
    }

    exceptionMappers = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadExceptionMappers(), getAutoloadAll())
        .orElse(false)) {
      ServiceLoader.load(ExceptionMapper.class).iterator().forEachRemaining(exceptionMappers::add);
    }
  }

  @Override
  public final ResponseT handleRequest(RequestT originalRequest, Context context) {
    if (isInitialized() == false) {
      try {
        completeInitialization(context);
      } finally {
        setInitialized(true);
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
      final ExceptionMapper exceptionMapper =
          ExceptionMappers.findExceptionMapperForException(getExceptionMappers(), e).orElse(null);
      if (exceptionMapper == null)
        throw e;

      try {
        final ResponseT originalError =
            (ResponseT) exceptionMapper.mapExceptionTo(e, responseType, context);
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

  public Type getResponseType() {
    return responseType;
  }

  private RequestT prepareRequest(RequestContext<RequestT> requestContext, Context lambdaContext) {
    for (RequestFilter<RequestT> requestFilter : getRequestFilters())
      requestFilter.filterRequest(requestContext, lambdaContext);
    return requestContext.getInputValue();
  }

  private ResponseT prepareResponse(RequestContext<RequestT> requestContext,
      ResponseContext<ResponseT> responseContext, Context lambdaContext) {
    for (ResponseFilter<RequestT, ResponseT> responseFilter : getResponseFilters())
      responseFilter.filterResponse(requestContext, responseContext, lambdaContext);
    return responseContext.getOutputValue();
  }

  protected void registerResponseFilter(ResponseFilter<RequestT, ResponseT> responseFilter) {
    if (responseFilter == null)
      throw new NullPointerException();
    if (isInitialized())
      throw new IllegalStateException();
    responseFilters.add(responseFilter);
  }

  protected List<ResponseFilter<RequestT, ResponseT>> getResponseFilters() {
    return unmodifiableList(responseFilters);
  }

  protected <E extends Exception> void registerExceptionMapper(
      ExceptionMapper<E, ResponseT> exceptionMapper) {
    if (exceptionMapper == null)
      throw new NullPointerException();
    if (isInitialized())
      throw new IllegalStateException();

    // Just test that it's possible.
    ExceptionMappers.findExceptionMapperExceptionType(exceptionMapper).orElseThrow(() -> {
      return new IllegalArgumentException("Cannot find exception type for " + exceptionMapper);
    });

    // TODO Should we test for response type equivalence? Should we extract response type at all?

    exceptionMappers.add(exceptionMapper);
  }

  protected List<ExceptionMapper<?, ResponseT>> getExceptionMappers() {
    return unmodifiableList(exceptionMappers);
  }

  @Override
  protected boolean isInitialized() {
    return initialized;
  }

  private void setInitialized(boolean initialized) {
    if (initialized == false && this.initialized == true)
      throw new IllegalStateException("already initialized");
    this.initialized = initialized;
  }
}
