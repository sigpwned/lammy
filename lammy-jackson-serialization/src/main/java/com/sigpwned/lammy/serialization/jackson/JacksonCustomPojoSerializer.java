package com.sigpwned.lammy.serialization.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson-based implementation of the {@link CustomPojoSerializer} interface.
 */
public class JacksonCustomPojoSerializer implements CustomPojoSerializer {
  private static AtomicReference<ObjectMapper> MAPPER = new AtomicReference<>(new ObjectMapper());

  public static void setMapper(ObjectMapper mapper) {
    if (mapper == null)
      throw new NullPointerException();
    MAPPER.getAndSet(mapper);
  }

  @Override
  public <T> T fromJson(InputStream input, Type type) {
    try {
      return getMapper().readValue(input, getMapper().getTypeFactory().constructType(type));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  @Override
  public <T> T fromJson(String input, Type type) {
    try {
      return getMapper().readValue(input, getMapper().getTypeFactory().constructType(type));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    try {
      getMapper().writeValue(output, value);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write output", e);
    }
  }

  /**
   * test hook
   */
  protected ObjectMapper getMapper() {
    return MAPPER.get();
  }
}
