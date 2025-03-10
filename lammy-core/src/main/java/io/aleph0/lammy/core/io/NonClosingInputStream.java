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
package io.aleph0.lammy.core.io;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * An {@link InputStream} that delegates all calls to another {@link InputStream} except
 * {@link #close()}, which it ignores. This is useful when you want to prevent a wrapped
 * {@link InputStream} from being closed, even when used in a context that would normally close it.
 */
public class NonClosingInputStream extends FilterInputStream {
  public NonClosingInputStream(InputStream delegate) {
    super(delegate);
  }

  @Override
  public void close() {
    // Do nothing. That's the point of this class!
  }
}
