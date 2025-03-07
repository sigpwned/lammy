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
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.amazonaws.services.lambda.runtime.serialization.factories.GsonFactory;
import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;
import com.sigwned.lammy.core.base.bean.BeanLambdaFunctionBase;
import com.sigwned.lammy.core.base.streamedbean.StreamedBeanLambdaFunctionBase;

/**
 * The default {@link PojoSerializer serializer} used by the AWS Lambda platform. Note that it is
 * {@link ContextAwareCustomPojoSerializer context-aware}, which means it can only be used in
 * application-serialized Lambda functions (e.g., {@link StreamedBeanLambdaFunctionBase}) and not in
 * platform-serialized Lambda functions (e.g., {@link BeanLambdaFunctionBase}).
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

  private Context context;
  private PojoSerializer delegate;

  public PlatformCustomPojoSerializer() {}

  /**
   * For testing only
   */
  /* default */ PlatformCustomPojoSerializer(PojoSerializer<?> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public <T> T fromJson(InputStream input, Type type) {
    return (T) getDelegate(type).fromJson(input);
  }

  @Override
  public <T> T fromJson(String input, Type type) {
    return (T) getDelegate(type).fromJson(input);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    getDelegate(type).toJson(value, output);
  }

  private PojoSerializer getDelegate(Type type) {
    // if serializing a Class that is a Lambda supported event, use Jackson with customizations
    if (type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      if (LambdaEventSerializers.isLambdaSupportedEvent(clazz.getName())) {
        return delegate = LambdaEventSerializers.serializerFor(clazz,
            Thread.currentThread().getContextClassLoader());
      }
    }

    final Platform platform = Platform.fromContext(getContext());

    // else platform dependent (Android uses GSON but all other platforms use Jackson)
    if (Objects.requireNonNull(platform) == Platform.ANDROID) {
      return delegate = GsonFactory.getInstance().getSerializer(type);
    }

    return delegate = JacksonFactory.getInstance().getSerializer(type);
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  private Context getContext() {
    if (context == null) {
      // This is called when a context-aware serializer is used without being informed of its
      // context. This happens when the serializer is used INSIDE of the AWS Lambda runtime (e.g.,
      // in a BeanLambdaFunctionBase) because the context is not available until the function is
      // called, but the serializer is invoked before the function is called to deserialize the
      // input for the call. Thus, context-aware serializers should only be used in the context of
      // an application-serialized Lambda function (e.g., in a StreamedBeanLambdaFunctionBase).
      throw new IllegalStateException("context not set");
    }
    return context;
  }
}
