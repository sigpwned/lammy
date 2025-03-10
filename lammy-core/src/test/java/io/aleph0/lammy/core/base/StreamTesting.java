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
package io.aleph0.lammy.core.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.amazonaws.services.lambda.runtime.Context;
import io.aleph0.lammy.core.model.stream.ExceptionWriter;
import io.aleph0.lammy.core.model.stream.InputContext;
import io.aleph0.lammy.core.model.stream.InputInterceptor;
import io.aleph0.lammy.core.model.stream.OutputContext;
import io.aleph0.lammy.core.model.stream.OutputInterceptor;

public interface StreamTesting {
  public static class TestInputInterceptor implements InputInterceptor {
    public final String id;

    public TestInputInterceptor(String id) {
      this.id = id;
    }

    @Override
    public void interceptRequest(InputContext requestContext, Context lambdaContext)
        throws IOException {
      final String originalInput = StreamTesting.toString(requestContext.getInputStream());
      final String updatedInput = originalInput.replace("Gandalf", "Gandalf" + id);
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

  public static class TestExceptionWriter implements ExceptionWriter<IllegalArgumentException> {
    @Override
    public void writeExceptionTo(IllegalArgumentException e, OutputStream out, Context context)
        throws IOException {
      out.write(e.getMessage().getBytes(StandardCharsets.UTF_8));
    }
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
