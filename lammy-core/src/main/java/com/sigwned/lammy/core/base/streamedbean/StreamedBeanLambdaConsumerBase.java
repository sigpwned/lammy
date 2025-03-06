/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 - 2025 Andy Boothe
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
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.sigwned.lammy.core.io.NonClosingInputStream;
import com.sigwned.lammy.core.model.bean.RequestContext;
import com.sigwned.lammy.core.model.bean.RequestFilter;
import com.sigwned.lammy.core.model.stream.InputContext;
import com.sigwned.lammy.core.model.stream.InputInterceptor;

public abstract class StreamedBeanLambdaConsumerBase<RequestT>
    extends StreamedBeanLambdaBase<RequestT, Void> {
  private boolean initialized;

  protected StreamedBeanLambdaConsumerBase() {
    this(new StreamedBeanLambdaConsumerConfiguration());
  }

  protected StreamedBeanLambdaConsumerBase(StreamedBeanLambdaConsumerConfiguration configuration) {
    this(null, null, configuration);
  }

  protected StreamedBeanLambdaConsumerBase(CustomPojoSerializer serializer) {
    this(serializer, new StreamedBeanLambdaConsumerConfiguration());
  }

  protected StreamedBeanLambdaConsumerBase(CustomPojoSerializer serializer,
      StreamedBeanLambdaConsumerConfiguration configuration) {
    this(serializer, null, configuration);
  }

  protected StreamedBeanLambdaConsumerBase(Type requestType) {
    this(requestType, new StreamedBeanLambdaConsumerConfiguration());
  }


  protected StreamedBeanLambdaConsumerBase(Type requestType,
      StreamedBeanLambdaConsumerConfiguration configuration) {
    this(null, requestType, configuration);
  }

  protected StreamedBeanLambdaConsumerBase(CustomPojoSerializer serializer, Type requestType,
      StreamedBeanLambdaConsumerConfiguration configuration) {
    super(serializer, requestType,
        StreamedBeanLambdaConfiguration.fromConsumerConfiguration(configuration));
  }

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
      final RequestT originalRequest =
          getSerializer().fromJson(preparedInputStream, getRequestType());
      final RequestContext<RequestT> requestContext = new DefaultRequestContext<>(originalRequest);
      final RequestT preparedRequest = prepareRequest(requestContext, context);
      consumeStreamedBeanRequest(preparedRequest, context);
    }
  }

  public abstract void consumeStreamedBeanRequest(RequestT request, Context context);

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

  private RequestT prepareRequest(RequestContext<RequestT> requestContext, Context lambdaContext) {
    for (RequestFilter<RequestT> requestFilter : getRequestFilters())
      requestFilter.filterRequest(requestContext, lambdaContext);
    return requestContext.getInputValue();
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
