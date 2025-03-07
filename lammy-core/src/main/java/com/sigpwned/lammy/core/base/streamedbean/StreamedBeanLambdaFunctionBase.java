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
package com.sigpwned.lammy.core.base.streamedbean;

import static java.util.Collections.unmodifiableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sigpwned.lammy.core.io.CountingOutputStream;
import com.sigpwned.lammy.core.io.NonClosingInputStream;
import com.sigpwned.lammy.core.io.NonClosingOutputStream;
import com.sigpwned.lammy.core.model.bean.ExceptionMapper;
import com.sigpwned.lammy.core.model.bean.RequestContext;
import com.sigpwned.lammy.core.model.bean.RequestFilter;
import com.sigpwned.lammy.core.model.bean.ResponseContext;
import com.sigpwned.lammy.core.model.bean.ResponseFilter;
import com.sigpwned.lammy.core.model.stream.InputContext;
import com.sigpwned.lammy.core.model.stream.InputInterceptor;
import com.sigpwned.lammy.core.model.stream.OutputContext;
import com.sigpwned.lammy.core.model.stream.OutputInterceptor;
import com.sigpwned.lammy.core.serialization.ContextAwareCustomPojoSerializer;
import com.sigpwned.lammy.core.util.ExceptionMappers;
import com.sigpwned.lammy.core.util.GenericTypes;
import com.sigpwned.lammy.core.util.MoreObjects;

/**
 * A basic implementation of a Lambda function that handles streaming input and output.
 */
public abstract class StreamedBeanLambdaFunctionBase<RequestT, ResponseT>
    extends StreamedBeanLambdaBase<RequestT, ResponseT> implements RequestStreamHandler {
  private final Type responseType;
  private final List<OutputInterceptor> outputInterceptors;
  private final List<ResponseFilter<RequestT, ResponseT>> responseFilters;
  private final List<ExceptionMapper<?, ResponseT>> exceptionMappers;
  boolean initialized;

  protected StreamedBeanLambdaFunctionBase() {
    this(new StreamedBeanLambdaFunctionConfiguration());
  }

  protected StreamedBeanLambdaFunctionBase(StreamedBeanLambdaFunctionConfiguration configuration) {
    this((ContextAwareCustomPojoSerializer) null, null, null, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(CustomPojoSerializer serializer) {
    this(serializer, new StreamedBeanLambdaFunctionConfiguration());
  }

  protected StreamedBeanLambdaFunctionBase(CustomPojoSerializer serializer,
      StreamedBeanLambdaFunctionConfiguration configuration) {
    this(serializer, null, null, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(Type requestType, Type responseType) {
    this(requestType, responseType, new StreamedBeanLambdaFunctionConfiguration());
  }

  protected StreamedBeanLambdaFunctionBase(Type requestType, Type responseType,
      StreamedBeanLambdaFunctionConfiguration configuration) {
    this((ContextAwareCustomPojoSerializer) null, requestType, responseType, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(CustomPojoSerializer serializer, Type requestType,
      Type responseType, StreamedBeanLambdaFunctionConfiguration configuration) {
    this(
        Optional.ofNullable(serializer)
            .map(ContextAwareCustomPojoSerializer::fromCustomPojoSerializer).orElse(null),
        requestType, responseType, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(ContextAwareCustomPojoSerializer serializer,
      Type requestType, Type responseType, StreamedBeanLambdaFunctionConfiguration configuration) {
    super(serializer, requestType,
        StreamedBeanLambdaConfiguration.fromFunctionConfiguration(configuration));

    if (responseType == null)
      responseType = GenericTypes
          .findGenericParameter(getClass(), StreamedBeanLambdaFunctionBase.class, 1).orElseThrow(
              () -> new AssertionError("Failed to compute responseType for " + getClass()));
    this.responseType = responseType;

    outputInterceptors = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadOutputInterceptors(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(OutputInterceptor.class).iterator()
          .forEachRemaining(this::registerOutputInterceptor);
    }

    responseFilters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadResponseFilters(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(ResponseFilter.class).iterator()
          .forEachRemaining(this::registerResponseFilter);
    }

    exceptionMappers = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadExceptionMappers(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(ExceptionMapper.class).iterator()
          .forEachRemaining(this::registerExceptionMapper);
    }
  }

  @Override
  public final void handleRequest(InputStream originalInputStream,
      OutputStream originalOutputStream, Context context) throws IOException {
    if (isInitialized() == false) {
      try {
        completeInitialization(context);
      } finally {
        setInitialized(true);
      }
    }

    final InputStream preparingInputStream = new NonClosingInputStream(originalInputStream);
    final InputContext inputContext = new DefaultInputContext(preparingInputStream);
    final CountingOutputStream preparingOutputStream =
        new CountingOutputStream(new NonClosingOutputStream(originalOutputStream));
    final OutputContext outputContext = new DefaultOutputContext(preparingOutputStream);
    try (final InputStream preparedInputStream = prepareInput(inputContext, context);
        final OutputStream preparedOutputStream =
            prepareOutput(inputContext, outputContext, context)) {
      final RequestT originalRequest =
          getSerializer().fromJson(preparedInputStream, getRequestType(), context);
      final RequestContext<RequestT> requestContext = new DefaultRequestContext<>(originalRequest);

      ResponseT preparedResponse;
      try {
        final RequestT preparedRequest = prepareRequest(requestContext, context);

        final ResponseT originalResponse = handleStreamedBeanRequest(preparedRequest, context);

        final ResponseContext<ResponseT> responseContext =
            new DefaultResponseContext<>(originalResponse);
        preparedResponse = prepareResponse(requestContext, responseContext, context);
      } catch (Exception e) {
        // If we have already committed, then just propagate the exception. This is much less likely
        // to happen in a StreamedBean, since in theory we're just writing a response bean to the
        // output, but the user gets access to the output stream in output interceptors, so it's
        // still possible the user could have written something at this point.
        if (preparingOutputStream.getCount() > 0)
          throw e;

        // Otherwise, try to find an exception writer that can handle this exception. If we don't
        // find one, then propagate the exception.
        final ExceptionMapper<? extends Exception, ResponseT> exceptionMapper =
            ExceptionMappers.findExceptionMapperForException(getExceptionMappers(), e).orElse(null);
        if (exceptionMapper == null)
          throw e;

        try {
          final ResponseT originalError =
              exceptionMapper.mapExceptionTo(e, getResponseType(), context);
          final ResponseContext<ResponseT> errorContext =
              new DefaultResponseContext<>(originalError);
          preparedResponse = prepareResponse(requestContext, errorContext, context);
        } catch (Exception e2) {
          throw e;
        }
      }

      getSerializer().toJson(preparedResponse, preparedOutputStream, getResponseType(), context);
    }
  }

  public abstract ResponseT handleStreamedBeanRequest(RequestT request, Context context);

  public Type getResponseType() {
    return responseType;
  }

  /**
   * Prepare the input stream for the lambda function using the registered request interceptors.
   * Returns the prepared input stream. On exception, ensures that the input stream is closed before
   * propagating the exception.
   */
  private InputStream prepareInput(InputContext streamingRequestContext, Context lambdaContext)
      throws IOException {
    InputStream result = null;

    try {
      for (InputInterceptor requestInterceptor : getInputInterceptors()) {
        requestInterceptor.interceptRequest(streamingRequestContext, lambdaContext);
      }
      result = streamingRequestContext.getInputStream();
    } finally {
      if (result == null)
        streamingRequestContext.getInputStream().close();
    }

    return result;
  }

  /**
   * Prepare the output stream for the lambda function using the registered response interceptors.
   * Returns the prepared output stream. On exception, ensures that the output stream is closed
   * before propagating the exception.
   */
  private OutputStream prepareOutput(InputContext streamingRequestContext,
      OutputContext streamingResponseContext, Context lambdaContext) throws IOException {
    OutputStream result = null;

    try {
      for (OutputInterceptor responseInterceptor : outputInterceptors) {
        responseInterceptor.interceptResponse(streamingRequestContext, streamingResponseContext,
            lambdaContext);
      }
      result = streamingResponseContext.getOutputStream();
    } finally {
      if (result == null)
        streamingResponseContext.getOutputStream().close();
    }

    return result;
  }

  protected void registerOutputInterceptor(OutputInterceptor outputInterceptor) {
    if (outputInterceptor == null)
      throw new NullPointerException();
    if (initialized == true)
      throw new IllegalStateException("initialized");
    outputInterceptors.add(outputInterceptor);
  }

  protected List<OutputInterceptor> getOutputInterceptors() {
    return unmodifiableList(outputInterceptors);
  }

  private RequestT prepareRequest(RequestContext<RequestT> requestContext, Context lambdaContext) {
    for (RequestFilter<RequestT> requestFilter : getRequestFilters())
      requestFilter.filterRequest(requestContext, lambdaContext);
    return requestContext.getInputValue();
  }

  private ResponseT prepareResponse(RequestContext<RequestT> requestContext,
      ResponseContext<ResponseT> responseContext, Context lambdaContext) {
    for (ResponseFilter<RequestT, ResponseT> responseFilter : responseFilters)
      responseFilter.filterResponse(requestContext, responseContext, lambdaContext);
    return responseContext.getOutputValue();
  }

  protected List<ResponseFilter<RequestT, ResponseT>> getResponseFilters() {
    return unmodifiableList(responseFilters);
  }

  protected void registerResponseFilter(ResponseFilter<RequestT, ResponseT> responseFilter) {
    if (responseFilter == null)
      throw new NullPointerException();
    if (isInitialized())
      throw new IllegalStateException();
    responseFilters.add(responseFilter);
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
