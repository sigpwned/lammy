package com.sigpwned.lammy.jackson;

import static java.lang.String.format;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanExceptionMapper;
import com.sigwned.lammy.core.util.GenericTypes;

public abstract class JacksonBeanExceptionMapper<E extends Exception, T>
    extends BeanExceptionMapper<E, T> {

  public JacksonBeanExceptionMapper() {
    final Class<?> clazz = getClass();

    final Type beanType =
        GenericTypes.findGenericParameter(clazz, BeanExceptionMapper.class, 1)
            .orElseThrow(() -> new AssertionError(
                format("Class %s must extend BeanExceptionMapper with concrete bean type",
                    clazz.getName())));

    setBeanWriter(new JacksonBeanWriter<>(beanType));
  }

  public JacksonBeanExceptionMapper(Class<T> beanType) {
    this((Type) beanType);
  }

  public JacksonBeanExceptionMapper(TypeReference<T> beanTypeReference) {
    this(beanTypeReference.getType());
  }

  public JacksonBeanExceptionMapper(Type beanType) {
    this(Jackson.toJavaType(beanType));
  }

  public JacksonBeanExceptionMapper(JavaType beanType) {
    this(new JacksonBeanWriter<>(beanType));
  }

  public JacksonBeanExceptionMapper(JacksonBeanWriter<T> beanWriter) {
    super(beanWriter);
  }
}
