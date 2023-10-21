package com.sigpwned.lammy.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanReader;

public class JacksonBeanReader<T> implements BeanReader<T> {
  private final ObjectReader reader;
  private final JavaType type;

  public JacksonBeanReader(Class<T> type) {
    this((Type) type);
  }

  public JacksonBeanReader(Type type) {
    this(TypeFactory.defaultInstance().constructType(type));
  }

  public JacksonBeanReader(JavaType type) {
    this(Jackson.defaultObjectReader(), type);
  }

  public JacksonBeanReader(ObjectReader reader, JavaType type) {
    if (reader == null)
      throw new NullPointerException();
    if (type == null)
      throw new NullPointerException();
    this.reader = reader;
    this.type = type;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T readBeanFrom(InputStream input) throws IOException {
    T result;
    try (JsonParser p = reader.createParser(input)) {
      result = (T) reader.readValue(p, type);
    }
    return result;
  }
}
