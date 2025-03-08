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
package com.sigpwned.lammy.test.stream;

import org.testcontainers.junit.jupiter.Testcontainers;
import com.sigpwned.lammy.test.ExceptionWriterTestBase;

@Testcontainers
public class StreamLambdaProcessorExceptionWriterTest extends ExceptionWriterTestBase {
  static {
    // Enable this when needed for debugging
    // localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  @Override
  public String throwingProcessorSource(Boolean autoloadAll, Boolean autoloadInputInterceptors,
      Boolean autoloadOutputInterceptors, Boolean autoloadExceptionWriters) {
    // @formatter:off
    return ""
      + "package com.example;\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.sigpwned.lammy.core.base.stream.StreamLambdaProcessorBase;\n"
      + "import com.sigpwned.lammy.core.base.stream.StreamLambdaProcessorConfiguration;\n"
      + "import java.io.IOException;\n"
      + "import java.io.UncheckedIOException;\n"
      + "import java.io.InputStream;\n"
      + "import java.io.OutputStream;\n"
      + "import java.io.ByteArrayOutputStream;\n"
      + "import java.nio.charset.StandardCharsets;\n"
      + "\n"
      + "public class LambdaFunction extends StreamLambdaProcessorBase {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new StreamLambdaProcessorConfiguration()\n"
      + "      .withAutoloadInputInterceptors(" + autoloadInputInterceptors + ")\n"
      + "      .withAutoloadOutputInterceptors(" + autoloadOutputInterceptors + ")\n"
      + "      .withAutoloadExceptionWriters(" + autoloadExceptionWriters + "));\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public void handleStreamingRequest(InputStream input, OutputStream output, Context context) {\n"
      + "    try {\n"
      + "      final byte[] requestBytes = toByteArray(input);\n"
      + "      final String requestText = new String(requestBytes, StandardCharsets.UTF_8);\n"
      + "      throw new IllegalArgumentException(requestText);\n"
      + "    } catch (IOException e) {\n"
      + "      throw new UncheckedIOException(e);\n"
      + "    }\n"
      + "  }\n"
      + "\n"
      + "  public byte[] toByteArray(InputStream input) throws IOException {\n"
      + "    try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {\n"
      + "      final byte[] buffer = new byte[8192];\n"
      + "      for(int nread=input.read(buffer); nread != -1; nread=input.read(buffer)) {\n"
      + "        output.write(buffer, 0, nread);\n"
      + "      }\n"
      + "      return output.toByteArray();\n"
      + "    }\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  protected Boolean getAutoloadAll() {\n"
      + "    return " + autoloadAll + ";\n"
      + "  }\n"
      + "}\n";
    // @formatter:on
  }
}
