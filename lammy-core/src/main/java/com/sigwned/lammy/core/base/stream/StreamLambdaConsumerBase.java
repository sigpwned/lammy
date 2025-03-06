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
package com.sigwned.lammy.core.base.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigwned.lammy.core.io.NonClosingInputStream;
import com.sigwned.lammy.core.model.stream.InputContext;
import com.sigwned.lammy.core.model.stream.InputInterceptor;

public abstract class StreamLambdaConsumerBase extends StreamLambdaBase {
  private boolean initialized;

  public StreamLambdaConsumerBase(StreamLambdaConsumerConfiguration configuration) {
    super(StreamLambdaConfiguration.fromConsumerConfiguration(configuration));
  }

  /**
   * hook
   */
  protected void completeInitialization(Context context) {}

  @Override
  public void handleRequest(InputStream originalInputStream, OutputStream originalOutputStream,
      Context context) throws IOException {
    if (isInitialized() == false) {
      try {
        completeInitialization(context);
      } finally {
        setInitialized(true);
      }
    }

    final InputStream preparingInputStream = new NonClosingInputStream(originalInputStream);
    final InputContext inputContext = new DefaultInputContext(preparingInputStream);
    try (final InputStream preparedInputStream = prepareInput(inputContext, context)) {
      consumeStreamingRequest(preparedInputStream, context);
    }
  }

  public abstract void consumeStreamingRequest(InputStream inputStream, Context context);

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

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  private void setInitialized(boolean initialized) {
    if (initialized == false && this.initialized == true)
      throw new IllegalStateException("already initialized");
    this.initialized = initialized;
  }
}
