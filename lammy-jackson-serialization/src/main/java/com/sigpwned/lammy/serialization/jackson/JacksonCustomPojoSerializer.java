package com.sigpwned.lammy.serialization.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
      return fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  @Override
  public <T> T fromJson(String input, Type type) {
    try {
      return fromJson(new StringReader(input), type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  private <T> T fromJson(Reader input, Type type) throws IOException {
    return getMapper().readValue(input, getMapper().getTypeFactory().constructType(type));
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
