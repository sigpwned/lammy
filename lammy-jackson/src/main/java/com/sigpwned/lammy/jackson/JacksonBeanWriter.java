package com.sigpwned.lammy.jackson;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanWriter;

public class JacksonBeanWriter<T> implements BeanWriter<T> {
  private final ObjectWriter writer;
  @SuppressWarnings("unused")
  private final JavaType type;

  public JacksonBeanWriter(Class<T> type) {
    this(Jackson.defaultObjectWriter(), (Type) type);
  }

  public JacksonBeanWriter(ObjectWriter writer, Class<T> type) {
    this(writer, (Type) type);
  }

  public JacksonBeanWriter(TypeReference<T> typeReference) {
    this(Jackson.defaultObjectWriter(), typeReference.getType());
  }

  public JacksonBeanWriter(ObjectWriter writer, TypeReference<T> typeReference) {
    this(writer, typeReference.getType());
  }

  public JacksonBeanWriter(Type type) {
    this(Jackson.defaultObjectWriter(), type);
  }

  public JacksonBeanWriter(ObjectWriter writer, Type type) {
    this(writer, TypeFactory.defaultInstance().constructType(type));
  }

  public JacksonBeanWriter(JavaType type) {
    this(Jackson.defaultObjectWriter(), type);
  }

  public JacksonBeanWriter(ObjectWriter writer, JavaType type) {
    if (writer == null)
      throw new NullPointerException();
    if (type == null)
      throw new NullPointerException();
    this.writer = writer;
    this.type = type;
  }

  @Override
  public void writeBeanTo(T value, OutputStream output) throws IOException {
    try (JsonGenerator g = writer.createGenerator(output)) {
      writer.writeValue(g, value);
    }
  }
}
