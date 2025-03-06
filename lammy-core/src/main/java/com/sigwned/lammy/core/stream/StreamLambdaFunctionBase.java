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
package com.sigwned.lammy.core.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sigwned.lammy.core.LambdaFunctionBase;
import com.sigwned.lammy.core.io.CountingOutputStream;
import com.sigwned.lammy.core.io.NonClosingInputStream;
import com.sigwned.lammy.core.io.NonClosingOutputStream;
import com.sigwned.lammy.core.util.MoreObjects;

/**
 * A basic implementation of a Lambda function that handles streaming input and output.
 */
public abstract class StreamLambdaFunctionBase extends LambdaFunctionBase
    implements RequestStreamHandler {
  private final List<InputInterceptor> inputInterceptors;
  private final List<OutputInterceptor> outputInterceptors;
  private final List<ExceptionWriter> exceptionWriters;
  private boolean initialized;

  protected StreamLambdaFunctionBase() {
    this(new StreamLambdaConfiguration());
  }

  protected StreamLambdaFunctionBase(StreamLambdaConfiguration configuration) {
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

    exceptionWriters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadExceptionWriters(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(ExceptionWriter.class).iterator().forEachRemaining(exceptionWriters::add);
    }
  }

  @Override
  public final void handleRequest(InputStream originalInputStream,
      OutputStream originalOutputStream, Context context) throws IOException {
    initialized = true;

    CountingOutputStream countingOutputStream = null;
    try {
      final InputContext requestContext =
          new DefaultInputContext(new NonClosingInputStream(originalInputStream));
      final OutputContext responseContext =
          new DefaultOutputContext(new NonClosingOutputStream(originalOutputStream));
      try (final InputStream preparedInputStream = prepareInput(requestContext, context);
          final OutputStream preparedOutputStream =
              prepareOutput(requestContext, responseContext, context)) {
        countingOutputStream = new CountingOutputStream(preparedOutputStream);
        handleStreamingRequest(preparedInputStream, countingOutputStream, context);
      }
    } catch (Exception e) {
      // If we're already committed, then just propagate. We can't come back from that.
      if (countingOutputStream != null && countingOutputStream.getCount() > 0)
        throw e;

      // Otherwise, try to find an exception writer that can handle this exception. If we don't
      // find one, then propagate the exception.
      final ExceptionWriter exceptionWriter = exceptionWriters.stream()
          .filter(writer -> writer.canWriteException(e)).findFirst().orElse(null);
      if (exceptionWriter == null)
        throw e;

      try {
        exceptionWriter.writeExceptionTo(e, countingOutputStream, context);
      } catch (Exception e2) {
        // Well, we're just having a bad time, aren't we? Propagate the original exception.
        throw e;
      }
    }
  }

  public abstract void handleStreamingRequest(InputStream inputStream, OutputStream outputStream,
      Context context) throws IOException;

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

  protected void registerExceptionWriter(ExceptionWriter exceptionWriter) {
    if (exceptionWriter == null)
      throw new NullPointerException();
    if (initialized == true)
      throw new IllegalStateException("initialized");
    exceptionWriters.add(exceptionWriter);
  }
}
