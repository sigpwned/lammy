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
package com.sigwned.lammy.core.base.stream;



import static java.util.Collections.unmodifiableList;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sigwned.lammy.core.base.LambdaFunctionBase;
import com.sigwned.lammy.core.model.stream.InputInterceptor;
import com.sigwned.lammy.core.util.MoreObjects;

public abstract class StreamLambdaBase extends LambdaFunctionBase implements RequestStreamHandler {
  private final List<InputInterceptor> inputInterceptors;

  public StreamLambdaBase(StreamLambdaConfiguration configuration) {
    this.inputInterceptors = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadInputInterceptors(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(InputInterceptor.class).iterator()
          .forEachRemaining(this::registerInputInterceptor);
    }
  }

  protected void registerInputInterceptor(InputInterceptor inputInterceptor) {
    if (inputInterceptor == null)
      throw new NullPointerException();
    if (isInitialized() == true)
      throw new IllegalStateException("initialized");
    inputInterceptors.add(inputInterceptor);
  }

  protected List<InputInterceptor> getInputInterceptors() {
    return unmodifiableList(inputInterceptors);
  }

  protected abstract boolean isInitialized();
}
