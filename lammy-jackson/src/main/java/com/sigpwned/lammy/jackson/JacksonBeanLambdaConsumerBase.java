package com.sigpwned.lammy.jackson;

import static java.lang.String.format;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanLambdaConsumerBase;
import com.sigwned.lammy.core.util.GenericTypes;

public abstract class JacksonBeanLambdaConsumerBase<InputT> extends BeanLambdaConsumerBase<InputT> {

  public JacksonBeanLambdaConsumerBase() {
    final Class<?> clazz = getClass();

    final Type inputType =
        GenericTypes.findGenericParameter(clazz, JacksonBeanLambdaFunctionBase.class, 0)
            .orElseThrow(() -> new AssertionError(format(
                "Class %s must extend JacksonBeanLambdaFunctionBase with concrete input type",
                clazz.getName())));

    setBeanReader(new JacksonBeanReader<>(inputType));
  }

  public JacksonBeanLambdaConsumerBase(Class<InputT> inputType) {
    this((Type) inputType);
  }

  public JacksonBeanLambdaConsumerBase(TypeReference<InputT> inputTypeReference) {
    this(inputTypeReference.getType());
  }

  public JacksonBeanLambdaConsumerBase(Type inputType) {
    this(Jackson.toJavaType(inputType));
  }

  public JacksonBeanLambdaConsumerBase(JavaType inputType) {
    this(new JacksonBeanReader<>(inputType));
  }

  public JacksonBeanLambdaConsumerBase(JacksonBeanReader<InputT> beanReader) {
    super(beanReader);
  }
}
