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
