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

public abstract class BeanLambdaConsumerBase<InputT> extends BeanLambdaFunctionBase<InputT, Void> {
  protected BeanLambdaConsumerBase() {
    this(new BeanLambdaConfiguration());
  }

  protected BeanLambdaConsumerBase(BeanLambdaConfiguration configuration) {
    super(configuration);
  }

  protected BeanLambdaConsumerBase(Type requestType) {
    this(requestType, new BeanLambdaConfiguration());
  }

  protected BeanLambdaConsumerBase(Type requestType, BeanLambdaConfiguration configuration) {
    super(requestType, Void.class, configuration);
  }

  @Override
  public final Void handleBeanRequest(InputT input, Context context) {
    consumeBeanRequest(input, context);
    return null;
  }

  public abstract void consumeBeanRequest(InputT input, Context context);
}
