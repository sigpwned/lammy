package com.sigpwned.lammy.serialization.platform.and.gson;

import static java.util.Objects.requireNonNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigwned.lammy.core.serialization.ContextAwareCustomPojoSerializer;
import com.sigwned.lammy.core.serialization.PlatformCustomPojoSerializer;

public class PlatformAndGsonCustomPojoSerializer implements ContextAwareCustomPojoSerializer {
  private final PlatformCustomPojoSerializer platformSerializer;
  private final GsonCustomPojoSerializer gsonSerializer;

  public PlatformAndGsonCustomPojoSerializer() {
    this(new PlatformCustomPojoSerializer(), new GsonCustomPojoSerializer());
  }

  /* default */ PlatformAndGsonCustomPojoSerializer(PlatformCustomPojoSerializer platformSerializer,
      GsonCustomPojoSerializer gsonSerializer) {
    this.platformSerializer = requireNonNull(platformSerializer);
    this.gsonSerializer = requireNonNull(gsonSerializer);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    getGsonSerializer().toJson(value, output, type);
  }

  @Override
  public <T> T fromJson(InputStream input, Type type) {
    return getPlatformSerializer().fromJson(input, type);
  }

  @Override
  public <T> T fromJson(String input, Type type) {
    try (InputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
      return fromJson(in, type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  @Override
  public void setContext(Context context) {
    getPlatformSerializer().setContext(context);
  }

  private PlatformCustomPojoSerializer getPlatformSerializer() {
    return platformSerializer;
  }

  private GsonCustomPojoSerializer getGsonSerializer() {
    return gsonSerializer;
  }
}
