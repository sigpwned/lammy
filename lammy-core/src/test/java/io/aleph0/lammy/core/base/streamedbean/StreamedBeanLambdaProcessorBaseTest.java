/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 - 2025 Andy Boothe
 * ====================================SECTION=====================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================LICENSE_END===================================
 */
package io.aleph0.lammy.core.base.streamedbean;


import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.Lists;
import org.testcontainers.shaded.org.apache.commons.io.output.ByteArrayOutputStream;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigpwned.just.json.JustJson;
import io.aleph0.lammy.core.base.BeanTesting;
import io.aleph0.lammy.core.base.StreamTesting;
import io.aleph0.lammy.core.io.BrokenOutputStream;
import io.aleph0.lammy.core.model.bean.ExceptionMapper;

public class StreamedBeanLambdaProcessorBaseTest implements BeanTesting, StreamTesting {
  public static class FilterInterceptorTestStreamedBeanLambdaProcessor
      extends StreamedBeanLambdaProcessorBase<Map<String, Object>, Map<String, Object>> {
    public FilterInterceptorTestStreamedBeanLambdaProcessor() {
      registerInputInterceptor(new TestInputInterceptor("1"));
      registerInputInterceptor(new TestInputInterceptor("2"));
      registerRequestFilter(new TestRequestFilter("A"));
      registerRequestFilter(new TestRequestFilter("B"));
      registerResponseFilter(new TestResponseFilter("X"));
      registerResponseFilter(new TestResponseFilter("Y"));
      registerOutputInterceptor(new TestOutputInterceptor("8"));
      registerOutputInterceptor(new TestOutputInterceptor("9"));
    }

    @Override
    public Map<String, Object> handleStreamedBeanRequest(Map<String, Object> input,
        Context context) {
      final String name = (String) input.get("name");

      final String greeting = "Hello, " + name + "!";

      final Map<String, Object> output = new HashMap<>(input);
      output.put("greeting", greeting);

      return output;
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void givenProcessorWithFiltersAndInterceptors_whenInvoke_thenFiltersAndInterceptorsRunAsExpected()
      throws IOException {
    final FilterInterceptorTestStreamedBeanLambdaProcessor unit =
        new FilterInterceptorTestStreamedBeanLambdaProcessor();

    // We can't use M
    final Context context = mock(Context.class);

    String outputText;
    final String inputText = "{\"name\":\"Gandalf\"}";
    try (
        ByteArrayInputStream in =
            new ByteArrayInputStream(inputText.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      unit.handleRequest(in, out, context);
      outputText = new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    assertThat(outputText).endsWith("98");

    outputText = outputText.substring(0, outputText.length() - 2);

    final Map<String, Object> output = (Map<String, Object>) JustJson.parseDocument(outputText);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("name", "Gandalf21");
    expected.put("greeting", "Hello, Gandalf21!");
    expected.put("requestFilters", Lists.newArrayList("A", "B"));
    expected.put("responseFilters", Lists.newArrayList("X", "Y"));

    assertThat(output).isEqualTo(expected);
  }

  public static class TestExceptionMapper
      implements ExceptionMapper<IllegalArgumentException, Map<String, Object>> {
    @Override
    public Map<String, Object> mapExceptionTo(IllegalArgumentException e, Type responseType,
        Context context) {
      final Map<String, Object> output = new HashMap<>();
      output.put("error", e.getMessage());
      return output;
    }
  }

  public static class ExceptionTestBeanLambdaProcessor
      extends StreamedBeanLambdaProcessorBase<Map<String, Object>, Map<String, Object>> {
    private final Supplier<? extends RuntimeException> exceptionFactory;

    public ExceptionTestBeanLambdaProcessor(Supplier<? extends RuntimeException> exceptionFactory) {
      registerExceptionMapper(new TestExceptionMapper());
      this.exceptionFactory = requireNonNull(exceptionFactory);
    }

    @Override
    public Map<String, Object> handleStreamedBeanRequest(Map<String, Object> input,
        Context context) {
      throw exceptionFactory.get();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void givenProcessorWithExceptionMapper_whenThrowSubclassOfHandledExceptionType_thenExceptionMapped()
      throws IOException {
    // We use NumberFormatException here because it's a convenient child of IllegalArgumentException
    final ExceptionTestBeanLambdaProcessor unit =
        new ExceptionTestBeanLambdaProcessor(() -> new NumberFormatException("message"));

    final Context context = mock(Context.class);

    String outputText;
    try (ByteArrayInputStream in = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      unit.handleRequest(in, out, context);
      outputText = new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    final Map<String, Object> output = (Map<String, Object>) JustJson.parseDocument(outputText);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("error", "message");

    assertThat(output).isEqualTo(expected);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void givenProcessorWithExceptionMapper_whenThrowExactHandledExceptionType_thenExceptionMapped()
      throws IOException {
    final ExceptionTestBeanLambdaProcessor unit =
        new ExceptionTestBeanLambdaProcessor(() -> new IllegalArgumentException("message"));

    final Context context = mock(Context.class);

    String outputText;
    try (ByteArrayInputStream in = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      unit.handleRequest(in, out, context);
      outputText = new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    final Map<String, Object> output = (Map<String, Object>) JustJson.parseDocument(outputText);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("error", "message");

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowUnhandledExceptionType_thenExceptionMapped() {
    final ExceptionTestBeanLambdaProcessor unit =
        new ExceptionTestBeanLambdaProcessor(() -> new RuntimeException("message"));

    final Context context = mock(Context.class);

    assertThatThrownBy(() -> {
      try (InputStream in = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
          OutputStream out = new BrokenOutputStream()) {
        unit.handleRequest(in, out, context);
      }
    }).isExactlyInstanceOf(RuntimeException.class);
  }
}
