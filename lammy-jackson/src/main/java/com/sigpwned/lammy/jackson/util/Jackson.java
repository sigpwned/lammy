package com.sigpwned.lammy.jackson.util;

import java.lang.reflect.Type;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * <p>
 * Default {@link ObjectMapper} has the following configuration:
 * </p>
 *
 * <ul>
 * <li>Jdk8Module</li>
 * <li>JavaTimeModule</li>
 * </ul>
 */
public final class Jackson {
  private Jackson() {}

  private static ObjectMapper defaultObjectMapper =
      JsonMapper.builder().addModule(new Jdk8Module()).addModule(new JavaTimeModule()).build();

  public static void setDefaultObjectMapper(ObjectMapper defaultObjectMapper) {
    if (defaultObjectMapper == null)
      throw new NullPointerException();
    Jackson.defaultObjectMapper = defaultObjectMapper;
  }

  public static ObjectMapper getDefaultObjectMapper() {
    return defaultObjectMapper;
  }

  /**
   * Recommended method for configuring (as opposed to replacing) default {@link ObjectMapper}
   * instance.
   */
  public static void withDefaultObjectMapper(Consumer<ObjectMapper> configurator) {
    configurator.accept(getDefaultObjectMapper());
  }

  public static TypeFactory defaultTypeFactory() {
    return defaultObjectMapper.getTypeFactory();
  }

  public static ObjectReader defaultObjectReader() {
    return defaultObjectMapper.reader();
  }

  public static ObjectWriter defaultObjectWriter() {
    return defaultObjectMapper.writer();
  }

  public static JavaType toJavaType(Type type) {
    return defaultTypeFactory().constructType(type);
  }
}
