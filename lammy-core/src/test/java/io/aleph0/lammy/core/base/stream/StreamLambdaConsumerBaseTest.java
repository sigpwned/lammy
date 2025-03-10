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

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;
import io.aleph0.lammy.core.io.BrokenOutputStream;
import io.aleph0.lammy.core.model.stream.InputContext;
import io.aleph0.lammy.core.model.stream.InputInterceptor;

public class StreamLambdaConsumerBaseTest {
  public static class TestInputInterceptor implements InputInterceptor {
    public final String id;

    public TestInputInterceptor(String id) {
      this.id = id;
    }

    @Override
    public void interceptRequest(InputContext requestContext, Context lambdaContext)
        throws IOException {
      final String originalInput =
          StreamLambdaConsumerBaseTest.toString(requestContext.getInputStream());
      final String updatedInput = originalInput + id;
      requestContext
          .setInputStream(new ByteArrayInputStream(updatedInput.getBytes(StandardCharsets.UTF_8)));
    }
  }

  public String output;

  public class InterceptorTestStreamLambdaConsumer extends StreamLambdaConsumerBase {
    public InterceptorTestStreamLambdaConsumer() {
      registerInputInterceptor(new TestInputInterceptor("A"));
      registerInputInterceptor(new TestInputInterceptor("B"));
    }

    @Override
    public void consumeStreamingRequest(InputStream inputStream, Context context)
        throws IOException {
      output = StreamLambdaConsumerBaseTest.toString(inputStream);
    }
  }

  @Test
  public void givenProcessorWithInterceptors_whenInvoke_thenInterceptorsRunAsExpected()
      throws IOException {
    final InterceptorTestStreamLambdaConsumer unit = new InterceptorTestStreamLambdaConsumer();

    try (
        ByteArrayInputStream in =
            new ByteArrayInputStream("Gandalf".getBytes(StandardCharsets.UTF_8));
        OutputStream out = new BrokenOutputStream()) {
      unit.handleRequest(in, out, null);
    }

    assertThat(output).isEqualTo("GandalfAB");
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
