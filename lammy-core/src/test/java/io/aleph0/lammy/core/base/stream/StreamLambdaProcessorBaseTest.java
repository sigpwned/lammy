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
package io.aleph0.lammy.core.base.stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;
import io.aleph0.lammy.core.io.BrokenOutputStream;
import io.aleph0.lammy.core.io.NullInputStream;
import io.aleph0.lammy.core.model.stream.ExceptionWriter;
import io.aleph0.lammy.core.model.stream.InputContext;
import io.aleph0.lammy.core.model.stream.InputInterceptor;
import io.aleph0.lammy.core.model.stream.OutputContext;
import io.aleph0.lammy.core.model.stream.OutputInterceptor;

public class StreamLambdaProcessorBaseTest {
  public static class TestInputInterceptor implements InputInterceptor {
    public final String id;

    public TestInputInterceptor(String id) {
      this.id = id;
    }

    @Override
    public void interceptRequest(InputContext requestContext, Context lambdaContext)
        throws IOException {
      final String originalInput =
          StreamLambdaProcessorBaseTest.toString(requestContext.getInputStream());
      final String updatedInput = originalInput + id;
      requestContext
          .setInputStream(new ByteArrayInputStream(updatedInput.getBytes(StandardCharsets.UTF_8)));
    }
  }

  public static class TestOutputInterceptor implements OutputInterceptor {
    public final String id;

    public TestOutputInterceptor(String id) {
      this.id = id;
    }

    @Override
    public void interceptResponse(InputContext requestContext, OutputContext responseContext,
        Context lambdaContext) throws IOException {
      final OutputStream originalOutput = responseContext.getOutputStream();
      final OutputStream decoratedOutput = new FilterOutputStream(originalOutput) {
        @Override
        public void close() throws IOException {
          write(id.getBytes(StandardCharsets.UTF_8));
          super.close();
        }
      };
      responseContext.setOutputStream(decoratedOutput);
    }
  }

  public static class InterceptorTestStreamLambdaProcessor extends StreamLambdaProcessorBase {
    public InterceptorTestStreamLambdaProcessor() {
      registerInputInterceptor(new TestInputInterceptor("A"));
      registerInputInterceptor(new TestInputInterceptor("B"));
      registerOutputInterceptor(new TestOutputInterceptor("X"));
      registerOutputInterceptor(new TestOutputInterceptor("Y"));
    }

    @Override
    public void handleStreamingRequest(InputStream inputStream, OutputStream outputStream,
        Context context) throws IOException {
      transferTo(inputStream, outputStream);
      outputStream.write('K');
    }
  }

  @Test
  public void givenProcessorWithInterceptors_whenInvoke_thenInterceptorsRunAsExpected()
      throws IOException {
    final InterceptorTestStreamLambdaProcessor unit = new InterceptorTestStreamLambdaProcessor();

    final String output;
    try (
        ByteArrayInputStream in =
            new ByteArrayInputStream("Gandalf".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      unit.handleRequest(in, out, null);

      output = new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    assertThat(output).isEqualTo("GandalfABKYX");
  }

  public static class TestExceptionWriter implements ExceptionWriter<IllegalArgumentException> {
    @Override
    public void writeExceptionTo(IllegalArgumentException e, OutputStream out, Context context)
        throws IOException {
      out.write(e.getMessage().getBytes(StandardCharsets.UTF_8));
    }
  }

  public static class ExceptionTestStreamLambdaProcessor extends StreamLambdaProcessorBase {
    private final Supplier<? extends RuntimeException> exceptionFactory;

    public ExceptionTestStreamLambdaProcessor(
        Supplier<? extends RuntimeException> exceptionFactory) {
      registerExceptionWriter(new TestExceptionWriter());
      this.exceptionFactory = requireNonNull(exceptionFactory);
    }

    @Override
    public void handleStreamingRequest(InputStream inputStream, OutputStream outputStream,
        Context context) throws IOException {
      throw exceptionFactory.get();
    }
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowSubclassOfHandledExceptionType_thenExceptionMapped()
      throws IOException {
    // We use NumberFormatException here because it's a convenient child of IllegalArgumentException
    final ExceptionTestStreamLambdaProcessor unit =
        new ExceptionTestStreamLambdaProcessor(() -> new NumberFormatException("message"));

    final String output;
    try (InputStream in = new NullInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      unit.handleRequest(in, out, null);
      output = new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    assertThat(output).isEqualTo("message");
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowExactHandledExceptionType_thenExceptionMapped()
      throws IOException {
    final ExceptionTestStreamLambdaProcessor unit =
        new ExceptionTestStreamLambdaProcessor(() -> new NumberFormatException("message"));

    final String output;
    try (InputStream in = new NullInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      unit.handleRequest(in, out, null);
      output = new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    assertThat(output).isEqualTo("message");
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowUnhandledExceptionType_thenExceptionMapped() {
    final ExceptionTestStreamLambdaProcessor unit =
        new ExceptionTestStreamLambdaProcessor(() -> new RuntimeException("message"));
    assertThatThrownBy(
        () -> unit.handleRequest(new NullInputStream(), new BrokenOutputStream(), null))
            .isExactlyInstanceOf(RuntimeException.class);
  }

  public static String toString(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    transferTo(in, out);
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  public static void transferTo(InputStream in, OutputStream out) throws IOException {
    final byte[] buf = new byte[8192];
    for (int nread = in.read(buf); nread > 0; nread = in.read(buf)) {
      out.write(buf, 0, nread);
    }
  }
}
