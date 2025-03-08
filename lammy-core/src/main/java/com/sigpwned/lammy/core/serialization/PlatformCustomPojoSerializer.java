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
package com.sigpwned.lammy.core.serialization;

import static java.util.Objects.requireNonNull;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.amazonaws.services.lambda.runtime.serialization.factories.GsonFactory;
import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;
import com.sigpwned.lammy.core.base.bean.BeanLambdaProcessorBase;
import com.sigpwned.lammy.core.base.streamedbean.StreamedBeanLambdaProcessorBase;

/**
 * The default {@link PojoSerializer serializer} used by the AWS Lambda platform. Note that it is
 * {@link ContextAwareCustomPojoSerializer context-aware}, which means it can only be used in
 * application-serialized Lambda functions (e.g., {@link StreamedBeanLambdaProcessorBase}) and not in
 * platform-serialized Lambda functions (e.g., {@link BeanLambdaProcessorBase}).
 */
public class PlatformCustomPojoSerializer implements ContextAwareCustomPojoSerializer {
  private static enum Platform {
    ANDROID, IOS, UNKNOWN;

    public static Platform fromContext(Context context) {
      ClientContext cc = context.getClientContext();
      if (cc == null) {
        return Platform.UNKNOWN;
      }

      Map<String, String> env = cc.getEnvironment();
      if (env == null) {
        return Platform.UNKNOWN;
      }

      String platform = env.get("platform");
      if (platform == null) {
        return Platform.UNKNOWN;
      }

      if ("Android".equalsIgnoreCase(platform)) {
        return Platform.ANDROID;
      } else if ("iPhoneOS".equalsIgnoreCase(platform)) {
        return Platform.IOS;
      } else {
        return Platform.UNKNOWN;
      }
    }
  }

  private static class SerializerKey {
    private final Type type;
    private final Platform platform;

    public SerializerKey(Type type, Platform platform) {
      this.type = requireNonNull(type);
      this.platform = requireNonNull(platform);
    }

    @Override
    public int hashCode() {
      return Objects.hash(platform, type);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SerializerKey other = (SerializerKey) obj;
      return platform == other.platform && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
      return "SerializerKey [type=" + type + ", platform=" + platform + "]";
    }
  }

  private static final int CACHE_SIZE = 16;

  private final Map<SerializerKey, PojoSerializer> cache;

  public PlatformCustomPojoSerializer() {
    this.cache = new LinkedHashMap<SerializerKey, PojoSerializer>(CACHE_SIZE) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<SerializerKey, PojoSerializer> e) {
        return size() >= CACHE_SIZE;
      }
    };
  }

  @Override
  public <T> T fromJson(InputStream input, Type type, Context context) {
    return (T) getSerializer(type, context).fromJson(input);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type, Context context) {
    getSerializer(type, context).toJson(value, output);
  }


  private PojoSerializer getSerializer(Type type, Context context) {
    final Platform platform = Platform.fromContext(context);
    final SerializerKey key = new SerializerKey(type, platform);

    // TODO Do we need to be concerned about thread safety here? Probably not, but just in case...
    PojoSerializer serializer;
    synchronized (cache) {
      serializer = cache.get(key);
    }

    if (serializer == null) {
      PojoSerializer newSerializer = newSerializer(type, context);

      PojoSerializer existing;
      synchronized (cache) {
        existing = cache.putIfAbsent(key, newSerializer);
      }

      serializer = existing != null ? existing : newSerializer;
    }

    return serializer;
  }

  /**
   * test hook
   */
  /* default */ PojoSerializer newSerializer(Type type, Context context) {
    // if serializing a Class that is a Lambda supported event, use Jackson with customizations
    if (type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      if (LambdaEventSerializers.isLambdaSupportedEvent(clazz.getName())) {
        return LambdaEventSerializers.serializerFor(clazz,
            Thread.currentThread().getContextClassLoader());
      }
    }

    final Platform platform = Platform.fromContext(context);

    // else platform dependent (Android uses GSON but all other platforms use Jackson)
    if (Objects.requireNonNull(platform) == Platform.ANDROID) {
      return GsonFactory.getInstance().getSerializer(type);
    }

    return JacksonFactory.getInstance().getSerializer(type);
  }
}
