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
package com.sigwned.lammy.core.util;

import java.util.Iterator;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.amazonaws.services.lambda.runtime.api.client.TooManyServiceProvidersFoundException;

public final class CustomPojoSerializers {
  private CustomPojoSerializers() {}

  /**
   * Loads a {@link CustomPojoSerializer} service using the same logic used by the AWS SDK to load
   * the custom serializer.
   *
   * @return the custom serializer or null if none
   *
   * @throws TooManyServiceProvidersFoundException if more than one serializer is found
   *
   * @see <a href=
   *      "https://github.com/aws/aws-lambda-java-libs/blob/ff5d9e0c58c97be9e9451a5669ecc9049fe51743/aws-lambda-java-runtime-interface-client/src/main/java/com/amazonaws/services/lambda/runtime/api/client/PojoSerializerLoader.java#L24">
   *      https://github.com/aws/aws-lambda-java-libs/blob/ff5d9e0c58c97be9e9451a5669ecc9049fe51743/aws-lambda-java-runtime-interface-client/src/main/java/com/amazonaws/services/lambda/runtime/api/client/PojoSerializerLoader.java#L24</a>
   */
  public static CustomPojoSerializer loadSerializer() {
    final ServiceLoader<CustomPojoSerializer> loader = ServiceLoader
        .load(CustomPojoSerializer.class, Thread.currentThread().getContextClassLoader());
    final Iterator<CustomPojoSerializer> serializers = loader.iterator();
    if (!serializers.hasNext()) {
      return null;
    }

    CustomPojoSerializer result = serializers.next();

    if (serializers.hasNext()) {
      throw new TooManyServiceProvidersFoundException(
          "Too many serializers provided inside the META-INF/services folder, only one is allowed");
    }

    return result;
  }
}
