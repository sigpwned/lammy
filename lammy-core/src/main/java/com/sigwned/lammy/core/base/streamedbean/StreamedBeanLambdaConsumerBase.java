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

import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;

public abstract class StreamedBeanLambdaConsumerBase<RequestT>
    extends StreamedBeanLambdaFunctionBase<RequestT, Void> {

  protected StreamedBeanLambdaConsumerBase() {
    this(new StreamedBeanLambdaConfiguration());
  }

  protected StreamedBeanLambdaConsumerBase(StreamedBeanLambdaConfiguration configuration) {
    this(null, null, configuration);
  }

  protected StreamedBeanLambdaConsumerBase(CustomPojoSerializer serializer) {
    this(serializer, new StreamedBeanLambdaConfiguration());
  }

  protected StreamedBeanLambdaConsumerBase(CustomPojoSerializer serializer,
      StreamedBeanLambdaConfiguration configuration) {
    this(serializer, null, configuration);
  }

  protected StreamedBeanLambdaConsumerBase(Type requestType) {
    this(requestType, new StreamedBeanLambdaConfiguration());
  }


  protected StreamedBeanLambdaConsumerBase(Type requestType,
      StreamedBeanLambdaConfiguration configuration) {
    this(null, requestType, configuration);
  }

  protected StreamedBeanLambdaConsumerBase(CustomPojoSerializer serializer, Type requestType,
      StreamedBeanLambdaConfiguration configuration) {
    super(serializer, requestType, Void.class, configuration);
  }

  @Override
  public final Void handleStreamedBeanRequest(RequestT request, Context context) {
    consumeStreamedBeanRequest(request, context);
    return null;
  }

  public abstract void consumeStreamedBeanRequest(RequestT request, Context context);
}
