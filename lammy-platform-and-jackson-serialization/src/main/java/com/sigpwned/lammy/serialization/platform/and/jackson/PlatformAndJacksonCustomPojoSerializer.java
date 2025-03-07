package com.sigpwned.lammy.serialization.platform.and.jackson;

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

public class PlatformAndJacksonCustomPojoSerializer implements ContextAwareCustomPojoSerializer {
  private final PlatformCustomPojoSerializer platformSerializer;
  private final JacksonCustomPojoSerializer jacksonSerializer;

  public PlatformAndJacksonCustomPojoSerializer() {
    this(new PlatformCustomPojoSerializer(), new JacksonCustomPojoSerializer());
  }

  /* default */ PlatformAndJacksonCustomPojoSerializer(
      PlatformCustomPojoSerializer platformSerializer,
      JacksonCustomPojoSerializer jacksonSerializer) {
    this.platformSerializer = requireNonNull(platformSerializer);
    this.jacksonSerializer = requireNonNull(jacksonSerializer);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    getJacksonSerializer().toJson(value, output, type);
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

  private JacksonCustomPojoSerializer getJacksonSerializer() {
    return jacksonSerializer;
  }
}
