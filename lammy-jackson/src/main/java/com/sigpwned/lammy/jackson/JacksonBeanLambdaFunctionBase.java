package com.sigpwned.lammy.jackson;

import static java.lang.String.format;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.sigpwned.lammy.jackson.util.Jackson;
import com.sigwned.lammy.core.bean.BeanLambdaFunctionBase;
import com.sigwned.lammy.core.util.GenericTypes;

public abstract class JacksonBeanLambdaFunctionBase<InputT, OutputT>
    extends BeanLambdaFunctionBase<InputT, OutputT> {

  public JacksonBeanLambdaFunctionBase() {
    final Class<?> clazz = getClass();

    final Type inputType =
        GenericTypes.findGenericParameter(clazz, JacksonBeanLambdaFunctionBase.class, 0)
            .orElseThrow(() -> new AssertionError(format(
                "Class %s must extend JacksonBeanLambdaFunctionBase with concrete input type",
                clazz.getName())));
    setBeanReader(new JacksonBeanReader<>(inputType));

    final Type outputType =
        GenericTypes.findGenericParameter(getClass(), JacksonBeanLambdaFunctionBase.class, 1)
            .orElseThrow(() -> new AssertionError(format(
                "Class %s must extend JacksonBeanLambdaFunctionBase with concrete output type",
                clazz.getName())));
    setBeanWriter(new JacksonBeanWriter<>(outputType));
  }

  public JacksonBeanLambdaFunctionBase(Class<InputT> inputType, Class<OutputT> outputType) {
    this((Type) inputType, (Type) outputType);
  }

  public JacksonBeanLambdaFunctionBase(TypeReference<InputT> inputTypeReference,
      TypeReference<OutputT> outputTypeReference) {
    this(inputTypeReference.getType(), outputTypeReference.getType());
  }

  public JacksonBeanLambdaFunctionBase(Type inputType, Type outputType) {
    this(Jackson.toJavaType(inputType), Jackson.toJavaType(outputType));
  }

  public JacksonBeanLambdaFunctionBase(JavaType inputType, JavaType outputType) {
    super(new JacksonBeanReader<>(inputType), new JacksonBeanWriter<>(outputType));
  }

  public JacksonBeanLambdaFunctionBase(JacksonBeanReader<InputT> beanReader,
      JacksonBeanWriter<OutputT> beanWriter) {
    super(beanReader, beanWriter);
  }
}
