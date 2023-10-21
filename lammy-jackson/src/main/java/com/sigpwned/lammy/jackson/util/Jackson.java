package com.sigpwned.lammy.jackson.util;

import java.lang.reflect.Type;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;

public final class Jackson {
  private Jackson() {}

  private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

  public static JavaType toJavaType(Type type) {
    return TypeFactory.defaultInstance().constructType(type);
  }

  public static TypeFactory defaultTypeFactory() {
    return DEFAULT_OBJECT_MAPPER.getTypeFactory();
  }

  public static ObjectReader defaultObjectReader() {
    return DEFAULT_OBJECT_MAPPER.reader();
  }

  public static ObjectWriter defaultObjectWriter() {
    return DEFAULT_OBJECT_MAPPER.writer();
  }
}
