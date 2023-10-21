package com.sigpwned.lammy.jackson;

import java.lang.reflect.Type;
import com.fasterxml.jackson.databind.JavaType;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanLambdaFunctionBase;
import com.sigwned.lammy.core.util.GenericTypes;

public abstract class JacksonBeanLambdaFunctionBase<InputT, OutputT>
    extends BeanLambdaFunctionBase<InputT, OutputT> {

  public JacksonBeanLambdaFunctionBase() {
    Type inputType =
        GenericTypes.findGenericParameter(getClass(), JacksonBeanLambdaFunctionBase.class, 0)
            .orElseThrow(() -> new RuntimeException());
    setBeanReader(new JacksonBeanReader<>(inputType));

    Type outputType =
        GenericTypes.findGenericParameter(getClass(), JacksonBeanLambdaFunctionBase.class, 1)
            .orElseThrow(() -> new RuntimeException());
    setBeanWriter(new JacksonBeanWriter<>(outputType));
  }

  public JacksonBeanLambdaFunctionBase(Class<InputT> inputType, Class<OutputT> outputType) {
    this((Type) inputType, (Type) outputType);
  }

  public JacksonBeanLambdaFunctionBase(Type inputType, Type outputType) {
    this(Jackson.toJavaType(inputType), Jackson.toJavaType(outputType));
  }

  public JacksonBeanLambdaFunctionBase(JavaType inputType, JavaType outputType) {
    super(new JacksonBeanReader<>(inputType), new JacksonBeanWriter<>(outputType));
  }
}
