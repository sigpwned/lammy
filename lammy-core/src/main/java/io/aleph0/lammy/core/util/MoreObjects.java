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
package io.aleph0.lammy.core.util;

import java.util.Optional;

public final class MoreObjects {
  private MoreObjects() {}

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> coalesce(T first, T second, T... more) {
    if (first != null) {
      return Optional.of(first);
    }
    if (second != null) {
      return Optional.of(second);
    }
    for (T t : more) {
      if (t != null) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }
}
