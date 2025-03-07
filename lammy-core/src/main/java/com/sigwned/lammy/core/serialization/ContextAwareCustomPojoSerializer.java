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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;

/**
 * Dear Amazon, please add the {@link Context} to the {@link CustomPojoSerializer} interface
 * {@link #fromJson(InputStream, Type)} and {@link #toJson(Object, OutputStream, Type)} methods so
 * we can use it in our serializers. Just in case there was some serializer that needed it. Like
 * maybe the built-in {@link PlatformCustomPojoSerializer}. Or something, Hypothetically.
 */
public interface ContextAwareCustomPojoSerializer extends CustomPojoSerializer {
  /**
   * Sets the {@link Context lambda context} for this serializer.
   */
  public void setContext(Context context);
}
