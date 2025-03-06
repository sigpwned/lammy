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
package com.sigwned.lammy.core.base.streamedbean;

import java.io.OutputStream;
import com.sigwned.lammy.core.model.stream.OutputContext;

/* default */ class DefaultOutputContext
    implements OutputContext {
  private OutputStream outputStream;

  public DefaultOutputContext(OutputStream outputStream) {
    if (outputStream == null)
      throw new NullPointerException();
    this.outputStream = outputStream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public void setOutputStream(OutputStream outputStream) {
    if (outputStream == null)
      throw new NullPointerException();
    this.outputStream = outputStream;
  }
}
