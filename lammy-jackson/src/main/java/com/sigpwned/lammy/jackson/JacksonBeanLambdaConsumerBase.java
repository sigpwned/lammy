package com.sigpwned.lammy.jackson;

import java.lang.reflect.Type;
import com.fasterxml.jackson.databind.JavaType;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanLambdaConsumerBase;
import com.sigwned.lammy.core.util.GenericTypes;

public abstract class JacksonBeanLambdaConsumerBase<InputT> extends BeanLambdaConsumerBase<InputT> {

  public JacksonBeanLambdaConsumerBase() {
    Type inputType =
        GenericTypes.findGenericParameter(getClass(), JacksonBeanLambdaConsumerBase.class, 0)
            .orElseThrow(() -> new RuntimeException());
    setBeanReader(new JacksonBeanReader<>(inputType));
  }

  public JacksonBeanLambdaConsumerBase(Class<InputT> inputType) {
    this((Type) inputType);
  }

  public JacksonBeanLambdaConsumerBase(Type inputType) {
    this(Jackson.toJavaType(inputType));
  }

  public JacksonBeanLambdaConsumerBase(JavaType inputType) {
    super(new JacksonBeanReader<>(inputType));
  }
}
