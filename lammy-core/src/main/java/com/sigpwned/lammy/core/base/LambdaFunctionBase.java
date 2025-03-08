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
package com.sigpwned.lammy.core.base;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import com.sigpwned.lammy.core.model.OptionalEnvironmentVariable;

public abstract class LambdaFunctionBase implements Resource {
  protected LambdaFunctionBase() {
    // Register ourselves as a CRaC handler for SnapStart
    Core.getGlobalContext().register(this);
  }

  @Override
  public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {}

  @Override
  public void afterRestore(Context<? extends Resource> context) throws Exception {}

  /**
   * test hook
   */
  protected Boolean getAutoloadAll() {
    return OptionalEnvironmentVariable.getenv("LAMMY_AUTOLOAD_ALL").map(Boolean::parseBoolean)
        .orElse(null);
  }
}
