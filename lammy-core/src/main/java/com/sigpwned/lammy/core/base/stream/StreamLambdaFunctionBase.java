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
package com.sigpwned.lammy.core.base.stream;

import static java.util.Collections.unmodifiableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigpwned.lammy.core.io.CountingOutputStream;
import com.sigpwned.lammy.core.io.NonClosingInputStream;
import com.sigpwned.lammy.core.io.NonClosingOutputStream;
import com.sigpwned.lammy.core.model.stream.ExceptionWriter;
import com.sigpwned.lammy.core.model.stream.InputContext;
import com.sigpwned.lammy.core.model.stream.InputInterceptor;
import com.sigpwned.lammy.core.model.stream.OutputContext;
import com.sigpwned.lammy.core.model.stream.OutputInterceptor;
import com.sigpwned.lammy.core.util.ExceptionWriters;
import com.sigpwned.lammy.core.util.MoreObjects;

/**
 * A basic implementation of a Lambda function that handles streaming input and output.
 */
public abstract class StreamLambdaFunctionBase extends StreamLambdaBase {
  private final List<OutputInterceptor> outputInterceptors;
  private final List<ExceptionWriter<?>> exceptionWriters;
  boolean initialized;

  protected StreamLambdaFunctionBase() {
    this(new StreamLambdaFunctionConfiguration());
  }

  protected StreamLambdaFunctionBase(StreamLambdaFunctionConfiguration configuration) {
    super(StreamLambdaConfiguration.fromFunctionConfiguration(configuration));
    outputInterceptors = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadOutputInterceptors(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(OutputInterceptor.class).iterator()
          .forEachRemaining(this::registerOutputInterceptor);
    }

    exceptionWriters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadExceptionWriters(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(ExceptionWriter.class).iterator()
          .forEachRemaining(this::registerExceptionWriter);
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
      try {
        handleStreamingRequest(preparedInputStream, preparedOutputStream, context);
      } catch (Exception e) {
        // If we're already committed, then just propagate. We can't come back from that.
        if (preparingOutputStream.getCount() > 0)
          throw e;

        // Otherwise, try to find an exception writer that can handle this exception. If we don't
        // find one, then propagate the exception.
        final ExceptionWriter<? extends Exception> exceptionWriter =
            ExceptionWriters.findExceptionWriterForException(getExceptionWriters(), e).orElse(null);
        if (exceptionWriter == null)
          throw e;

        try {
          exceptionWriter.writeExceptionTo(e, preparingOutputStream, context);
        } catch (Exception e2) {
          // Well, we're just having a bad time, aren't we? Propagate the original exception.
          throw e;
        }
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
      for (OutputInterceptor responseInterceptor : getOutputInterceptors()) {
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
    if (isInitialized() == true)
      throw new IllegalStateException("initialized");
    outputInterceptors.add(outputInterceptor);
  }

  protected List<OutputInterceptor> getOutputInterceptors() {
    return unmodifiableList(outputInterceptors);
  }

  protected <E extends Exception> void registerExceptionWriter(ExceptionWriter<E> exceptionWriter) {
    if (exceptionWriter == null)
      throw new NullPointerException();
    if (isInitialized() == true)
      throw new IllegalStateException("initialized");
    exceptionWriters.add(exceptionWriter);
  }

  protected List<ExceptionWriter<?>> getExceptionWriters() {
    return unmodifiableList(exceptionWriters);
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
