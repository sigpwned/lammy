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
package io.aleph0.lammy.core.serialization;

import static java.util.Objects.requireNonNull;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;

/**
 * A {@link ContextAwareCustomPojoSerializer} implementation that delegates JSON serialization and
 * deserialization to two separate serializers, each either a
 * {@link ContextAwareCustomPojoSerializer} or a {@link CustomPojoSerializer}.
 *
 * <p>
 * This design enables different implementations or configurations for serializing and deserializing
 * JSON, providing flexibility for cases where these behaviors need to differ.
 * </p>
 *
 * @see CustomPojoSerializer
 * @see ContextAwareCustomPojoSerializer
 */
public class CompositeContextAwareCustomPojoSerializer implements ContextAwareCustomPojoSerializer {
  private final ContextAwareCustomPojoSerializer serializer;
  private final ContextAwareCustomPojoSerializer deserializer;

  /**
   * Constructs a new CompositeCustomPojoSerializer with the specified serializers.
   *
   * @param serializer the {@link CustomPojoSerializer} used for deserialization (from JSON)
   * @param deserializer the {@link CustomPojoSerializer} used for serialization (to JSON)
   * @throws NullPointerException if either {@code serializer} or {@code deserializer} is
   *         {@code null}
   */
  public CompositeContextAwareCustomPojoSerializer(CustomPojoSerializer serializer,
      CustomPojoSerializer deserializer) {
    this(ContextAwareCustomPojoSerializer.fromCustomPojoSerializer(serializer),
        ContextAwareCustomPojoSerializer.fromCustomPojoSerializer(deserializer));
  }

  /**
   * Constructs a new CompositeCustomPojoSerializer with the specified serializers.
   *
   * @param serializer the {@link CustomPojoSerializer} used for deserialization (from JSON)
   * @param deserializer the {@link CustomPojoSerializer} used for serialization (to JSON)
   * @throws NullPointerException if either {@code serializer} or {@code deserializer} is
   *         {@code null}
   */
  public CompositeContextAwareCustomPojoSerializer(ContextAwareCustomPojoSerializer serializer,
      CustomPojoSerializer deserializer) {
    this(serializer, ContextAwareCustomPojoSerializer.fromCustomPojoSerializer(deserializer));
  }

  /**
   * Constructs a new CompositeCustomPojoSerializer with the specified serializers.
   *
   * @param serializer the {@link CustomPojoSerializer} used for deserialization (from JSON)
   * @param deserializer the {@link CustomPojoSerializer} used for serialization (to JSON)
   * @throws NullPointerException if either {@code serializer} or {@code deserializer} is
   *         {@code null}
   */
  public CompositeContextAwareCustomPojoSerializer(CustomPojoSerializer serializer,
      ContextAwareCustomPojoSerializer deserializer) {
    this(ContextAwareCustomPojoSerializer.fromCustomPojoSerializer(serializer), deserializer);
  }

  /**
   * Constructs a new CompositeCustomPojoSerializer with the specified serializers.
   *
   * @param serializer the {@link CustomPojoSerializer} used for deserialization (from JSON)
   * @param deserializer the {@link CustomPojoSerializer} used for serialization (to JSON)
   * @throws NullPointerException if either {@code serializer} or {@code deserializer} is
   *         {@code null}
   */
  public CompositeContextAwareCustomPojoSerializer(ContextAwareCustomPojoSerializer serializer,
      ContextAwareCustomPojoSerializer deserializer) {
    this.serializer = requireNonNull(serializer);
    this.deserializer = requireNonNull(deserializer);
  }

  /**
   * Deserializes JSON from the given InputStream into an object of the specified type.
   *
   * @param input the InputStream to read JSON from
   * @param type the target type for deserialization
   * @param context the Lambda function context
   * @param <T> the type of the deserialized object
   * @return the deserialized object
   */
  @Override
  public <T> T fromJson(InputStream input, Type type, Context context) {
    return getSerializer().fromJson(input, type, context);
  }

  /**
   * Serializes the given object to JSON and writes it to the provided OutputStream.
   *
   * @param value the object to serialize
   * @param output the OutputStream to write JSON to
   * @param type the type of the object being serialized
   * @param context the Lambda function context
   * @param <T> the type of the object
   */
  @Override
  public <T> void toJson(T value, OutputStream output, Type type, Context context) {
    getDeserializer().toJson(value, output, type, context);
  }


  private ContextAwareCustomPojoSerializer getSerializer() {
    return serializer;
  }

  private ContextAwareCustomPojoSerializer getDeserializer() {
    return deserializer;
  }
}
