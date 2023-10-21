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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.crac.Resource;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sigwned.lammy.core.ExceptionMapper;
import com.sigwned.lammy.core.LambdaFunctionBase;
import com.sigwned.lammy.core.util.ByteStreams;

public abstract class StreamLambdaFunctionBase extends LambdaFunctionBase
    implements RequestStreamHandler {
  private final List<StreamLambdaRequestInterceptor> requestInterceptors;
  private final List<StreamLambdaResponseInterceptor> responseInterceptors;

  public StreamLambdaFunctionBase() {
    this.requestInterceptors = new ArrayList<>();
    this.responseInterceptors = new ArrayList<>();
  }

  private void prepareStreamInput(StreamLambdaRequestContext streamingRequestContext,
      Context lambdaContext) throws IOException {
    for (StreamLambdaRequestInterceptor requestInterceptor : getRequestInterceptors()) {
      requestInterceptor.interceptRequest(streamingRequestContext, lambdaContext);
    }
  }

  private List<StreamLambdaRequestInterceptor> getRequestInterceptors() {
    return requestInterceptors;
  }

  public void registerRequestInterceptor(StreamLambdaRequestInterceptor requestInterceptor) {
    if (requestInterceptor == null)
      throw new NullPointerException();
    getRequestInterceptors().add(requestInterceptor);
  }

  private void prepareStreamOutput(StreamLambdaRequestContext streamingRequestContext,
      StreamLambdaResponseContext streamingResponseContext, Context lambdaContext)
      throws IOException {
    for (StreamLambdaResponseInterceptor responseInterceptor : getResponseInterceptors()) {
      responseInterceptor.interceptResponse(streamingRequestContext, streamingResponseContext,
          lambdaContext);
    }
  }

  private List<StreamLambdaResponseInterceptor> getResponseInterceptors() {
    return responseInterceptors;
  }

  public void registerResponseInterceptor(StreamLambdaResponseInterceptor responseInterceptor) {
    if (responseInterceptor == null)
      throw new NullPointerException();
    getResponseInterceptors().add(responseInterceptor);
  }

  @Override
  public void handleRequest(InputStream lambdaInputStream, OutputStream lambdaOutputStream,
      Context context) throws IOException {
    byte[] requestBytes = ByteStreams.toByteArray(lambdaInputStream);

    final StreamLambdaRequestContext requestContext =
        new DefaultStreamLambdaRequestContext(new ByteArrayInputStream(requestBytes));

    final StreamLambdaResponseContext responseContext =
        new DefaultStreamLambdaResponseContext(lambdaOutputStream);

    byte[] responseBytes;
    try {
      prepareStreamInput(requestContext, context);

      ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
      handleRequest(requestContext.getInputStream(), bufferOutputStream, context);
      responseBytes = bufferOutputStream.toByteArray();

      prepareStreamOutput(requestContext, responseContext, context);
    } catch (Exception e) {
      Optional<ExceptionMapper<? super Exception>> maybeExceptionMapper =
          findExceptionMapperForException(e);
      if (!maybeExceptionMapper.isPresent())
        throw e;

      ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
      maybeExceptionMapper.get().writeExceptionTo(e, bufferOutputStream);
      responseBytes = bufferOutputStream.toByteArray();

      prepareStreamOutput(requestContext, responseContext, context);
    }

    responseContext.getOutputStream().write(responseBytes);
  }

  public abstract void handleStreamingRequest(InputStream inputStream, OutputStream outputStream,
      Context context) throws IOException;

  @Override
  public void beforeCheckpoint(org.crac.Context<? extends Resource> context) throws Exception {}

  @Override
  public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {}
}
