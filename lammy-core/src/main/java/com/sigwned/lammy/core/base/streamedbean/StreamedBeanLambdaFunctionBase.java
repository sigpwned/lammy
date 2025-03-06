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
package com.sigwned.lammy.core.base.streamedbean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sigwned.lammy.core.base.LambdaFunctionBase;
import com.sigwned.lammy.core.io.CountingOutputStream;
import com.sigwned.lammy.core.io.NonClosingInputStream;
import com.sigwned.lammy.core.io.NonClosingOutputStream;
import com.sigwned.lammy.core.model.bean.ExceptionMapper;
import com.sigwned.lammy.core.model.bean.RequestContext;
import com.sigwned.lammy.core.model.bean.RequestFilter;
import com.sigwned.lammy.core.model.bean.ResponseContext;
import com.sigwned.lammy.core.model.bean.ResponseFilter;
import com.sigwned.lammy.core.model.stream.InputContext;
import com.sigwned.lammy.core.model.stream.InputInterceptor;
import com.sigwned.lammy.core.model.stream.OutputContext;
import com.sigwned.lammy.core.model.stream.OutputInterceptor;
import com.sigwned.lammy.core.serialization.PlatformCustomPojoSerializer;
import com.sigwned.lammy.core.util.CustomPojoSerializers;
import com.sigwned.lammy.core.util.ExceptionMappers;
import com.sigwned.lammy.core.util.GenericTypes;
import com.sigwned.lammy.core.util.MoreObjects;

/**
 * A basic implementation of a Lambda function that handles streaming input and output.
 */
public abstract class StreamedBeanLambdaFunctionBase<RequestT, ResponseT> extends LambdaFunctionBase
    implements RequestStreamHandler {
  private CustomPojoSerializer serializer;
  private final Type requestType;
  private final Type responseType;
  private final List<InputInterceptor> inputInterceptors;
  private final List<RequestFilter<RequestT>> requestFilters;
  private final List<OutputInterceptor> outputInterceptors;
  private final List<ResponseFilter<RequestT, ResponseT>> responseFilters;
  private final List<ExceptionMapper<?, ResponseT>> exceptionMappers;
  private boolean initialized;

  protected StreamedBeanLambdaFunctionBase() {
    this(new StreamedBeanLambdaConfiguration());
  }

  protected StreamedBeanLambdaFunctionBase(StreamedBeanLambdaConfiguration configuration) {
    this(null, null, null, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(CustomPojoSerializer serializer) {
    this(serializer, new StreamedBeanLambdaConfiguration());
  }

  protected StreamedBeanLambdaFunctionBase(CustomPojoSerializer serializer,
      StreamedBeanLambdaConfiguration configuration) {
    this(serializer, null, null, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(Type requestType, Type responseType) {
    this(requestType, responseType, new StreamedBeanLambdaConfiguration());
  }


  protected StreamedBeanLambdaFunctionBase(Type requestType, Type responseType,
      StreamedBeanLambdaConfiguration configuration) {
    this(null, requestType, responseType, configuration);
  }

  protected StreamedBeanLambdaFunctionBase(CustomPojoSerializer serializer, Type requestType,
      Type responseType, StreamedBeanLambdaConfiguration configuration) {
    if (serializer == null)
      serializer = CustomPojoSerializers.loadSerializer();
    this.serializer = serializer;

    if (requestType == null)
      requestType = GenericTypes
          .findGenericParameter(getClass(), StreamedBeanLambdaFunctionBase.class, 0)
          .orElseThrow(() -> new AssertionError("Failed to compute requestType for " + getClass()));
    this.requestType = requestType;

    if (responseType == null)
      responseType = GenericTypes
          .findGenericParameter(getClass(), StreamedBeanLambdaFunctionBase.class, 1).orElseThrow(
              () -> new AssertionError("Failed to compute responseType for " + getClass()));
    this.responseType = responseType;

    inputInterceptors = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadInputInterceptors(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(InputInterceptor.class).iterator()
          .forEachRemaining(inputInterceptors::add);
    }

    outputInterceptors = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadOutputInterceptors(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(OutputInterceptor.class).iterator()
          .forEachRemaining(outputInterceptors::add);
    }

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
  protected void completeInitialization(Context context) {
    if (serializer == null)
      serializer = PlatformCustomPojoSerializer.forContext(context, requestType);
  }

  @Override
  public final void handleRequest(InputStream originalInputStream,
      OutputStream originalOutputStream, Context context) throws IOException {
    if (initialized == false) {
      try {
        completeInitialization(context);
      } finally {
        initialized = true;
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
      final RequestT originalRequest = serializer.fromJson(preparedInputStream, requestType);
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
            ExceptionMappers.findExceptionMapperForException(exceptionMappers, e).orElse(null);
        if (exceptionMapper == null)
          throw e;

        try {
          final ResponseT originalError = exceptionMapper.mapExceptionTo(e, responseType, context);
          final ResponseContext<ResponseT> errorContext =
              new DefaultResponseContext<>(originalError);
          preparedResponse = prepareResponse(requestContext, errorContext, context);
        } catch (Exception e2) {
          throw e;
        }
      }

      serializer.toJson(preparedResponse, preparedOutputStream, responseType);
    }
  }

  public abstract ResponseT handleStreamedBeanRequest(RequestT request, Context context);

  public Type getRequestType() {
    return requestType;
  }

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
      for (InputInterceptor requestInterceptor : inputInterceptors) {
        requestInterceptor.interceptRequest(streamingRequestContext, lambdaContext);
      }
      result = streamingRequestContext.getInputStream();
    } finally {
      if (result == null)
        streamingRequestContext.getInputStream().close();
    }

    return result;
  }

  protected void registerInputInterceptor(InputInterceptor inputInterceptor) {
    if (inputInterceptor == null)
      throw new NullPointerException();
    if (initialized == true)
      throw new IllegalStateException("initialized");
    inputInterceptors.add(inputInterceptor);
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
