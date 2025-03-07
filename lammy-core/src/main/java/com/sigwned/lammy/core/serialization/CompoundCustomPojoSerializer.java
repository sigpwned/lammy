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
package com.sigwned.lammy.core.serialization;

import static java.util.Objects.requireNonNull;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;

public class CompoundCustomPojoSerializer implements ContextAwareCustomPojoSerializer {
  private final CustomPojoSerializer serializer;
  private final CustomPojoSerializer deserializer;

  public CompoundCustomPojoSerializer(CustomPojoSerializer serializer,
      CustomPojoSerializer deserializer) {
    this.serializer = requireNonNull(serializer);
    this.deserializer = requireNonNull(deserializer);
  }

  @Override
  public <T> T fromJson(InputStream input, Type type) {
    return getSerializer().fromJson(input, type);
  }

  @Override
  public <T> T fromJson(String input, Type type) {
    return getSerializer().fromJson(input, type);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    getDeserializer().toJson(value, output, type);
  }

  private CustomPojoSerializer getSerializer() {
    return serializer;
  }

  private CustomPojoSerializer getDeserializer() {
    return deserializer;
  }

  @Override
  public void setContext(Context context) {
    if (getSerializer() instanceof ContextAwareCustomPojoSerializer)
      ((ContextAwareCustomPojoSerializer) getSerializer()).setContext(context);
    if (getDeserializer() instanceof ContextAwareCustomPojoSerializer)
      ((ContextAwareCustomPojoSerializer) getDeserializer()).setContext(context);
  }
}
