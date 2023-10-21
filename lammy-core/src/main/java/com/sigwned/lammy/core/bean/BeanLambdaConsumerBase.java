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
package com.sigwned.lammy.core.bean;

import java.io.IOException;
import java.io.OutputStream;
import com.amazonaws.services.lambda.runtime.Context;

public abstract class BeanLambdaConsumerBase<InputT> extends BeanLambdaFunctionBase<InputT, Void> {
  private static final BeanWriter<Void> VOID_BEAN_WRITER = new BeanWriter<Void>() {
    @Override
    public void writeBeanTo(Void value, OutputStream output) throws IOException {
      if (value != null)
        throw new AssertionError("Void value unexpectedly not null");
      output.write('{');
      output.write('}');
    }
  };

  protected BeanLambdaConsumerBase() {
    setBeanWriter(VOID_BEAN_WRITER);
  }

  public BeanLambdaConsumerBase(BeanReader<InputT> beanReader) {
    super(beanReader, VOID_BEAN_WRITER);
  }

  @Override
  public Void handleBeanRequest(InputT input, Context context) throws IOException {
    consumeBeanRequest(input, context);
    return null;
  }

  public abstract void consumeBeanRequest(InputT input, Context context) throws IOException;
}
