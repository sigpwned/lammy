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
import java.util.Map;
import java.util.Objects;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.amazonaws.services.lambda.runtime.serialization.factories.GsonFactory;
import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;

public class DefaultPojoSerializer implements CustomPojoSerializer {
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

  public static DefaultPojoSerializer forContext(Context context, Type type) {
    final Platform platform = Platform.fromContext(context);

    final PojoSerializer<?> serializer = getSerializer(platform, type);

    return new DefaultPojoSerializer(serializer);
  }

  private static <T> PojoSerializer<T> getSerializer(Platform platform, Type type) {
    // if serializing a Class that is a Lambda supported event, use Jackson with customizations
    if (type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      if (LambdaEventSerializers.isLambdaSupportedEvent(clazz.getName())) {
        return (PojoSerializer<T>) LambdaEventSerializers.serializerFor(clazz,
            Thread.currentThread().getContextClassLoader());
      }
    }

    // else platform dependent (Android uses GSON but all other platforms use Jackson)
    if (Objects.requireNonNull(platform) == Platform.ANDROID) {
      return (PojoSerializer<T>) GsonFactory.getInstance().getSerializer(type);
    }

    return (PojoSerializer<T>) JacksonFactory.getInstance().getSerializer(type);
  }

  private final PojoSerializer delegate;

  public DefaultPojoSerializer(PojoSerializer<?> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public <T> T fromJson(InputStream input, Type type) {
    return (T) delegate.fromJson(input);
  }

  @Override
  public <T> T fromJson(String input, Type type) {
    return (T) delegate.fromJson(input);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    delegate.toJson(value, output);
  }
}
