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

import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigwned.lammy.core.model.bean.RequestContext;
import com.sigwned.lammy.core.model.bean.RequestFilter;

/**
 * A base class for a Lambda function that accepts a bean as input and produces no output. It uses
 * platform serialization, possibly using a custom serializer as loaded by the platform. It also
 * supports {@link RequestFilter}s for preprocessing the input.
 *
 * @param <RequestT> The type of the input bean
 *
 * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html">
 *      https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html</a>
 */
public abstract class BeanLambdaConsumerBase<RequestT> extends BeanLambdaBase<RequestT, Void> {
  private boolean initialized;

  protected BeanLambdaConsumerBase() {
    this(new BeanLambdaConsumerConfiguration());
  }

  protected BeanLambdaConsumerBase(BeanLambdaConsumerConfiguration configuration) {
    this(null, configuration);
  }

  protected BeanLambdaConsumerBase(Type requestType) {
    this(requestType, new BeanLambdaConsumerConfiguration());
  }

  protected BeanLambdaConsumerBase(Type requestType,
      BeanLambdaConsumerConfiguration configuration) {
    super(requestType, BeanLambdaConfiguration.fromConsumerConfiguration(configuration));
  }

  @Override
  public final Void handleRequest(RequestT originalRequest, Context context) {
    if (isInitialized() == false) {
      try {
        completeInitialization(context);
      } finally {
        setInitialized(true);
      }
    }

    final RequestContext<RequestT> requestContext = new DefaultRequestContext<>(originalRequest);
    final RequestT preparedRequest = prepareRequest(requestContext, context);
    consumeBeanRequest(preparedRequest, context);

    return null;
  }

  public abstract void consumeBeanRequest(RequestT input, Context context);

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
