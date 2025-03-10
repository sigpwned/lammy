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
package io.aleph0.lammy.test.streamedbean;

import static java.util.Collections.unmodifiableList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import io.aleph0.lammy.test.OutputInterceptorTestBase;

@Testcontainers
public class StreamedBeanLambdaProcessorOutputInterceptorIT extends OutputInterceptorTestBase {
  static {
    // Enable this when needed for debugging
    // localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  public static final String GREETING_PROCESSOR_REQUEST_TYPE = "Map<String, Object>";

  public static final String GREETING_PROCESSOR_RESPONSE_TYPE = "String";

  @Override
  public String greetingProcessorRequest(String name) {
    return "{\"name\":\"" + name + "\"}";
  }

  @Override
  public String greetingProcessorResponse(String name) {
    return "\"Hello, " + name + "!\"";
  }

  @Override
  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadInputInterceptors,
      Boolean autoloadOutputInterceptors, Boolean autoloadExceptionMappers) {
    // @formatter:off
    return ""
      + "package " + PACKAGE_NAME + ";\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import io.aleph0.lammy.core.base.streamedbean.StreamedBeanLambdaProcessorBase;\n"
      + "import io.aleph0.lammy.core.base.streamedbean.StreamedBeanLambdaProcessorConfiguration;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class LambdaFunction extends StreamedBeanLambdaProcessorBase<" + GREETING_PROCESSOR_REQUEST_TYPE + ", " + GREETING_PROCESSOR_RESPONSE_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new StreamedBeanLambdaProcessorConfiguration()\n"
      + "      .withAutoloadInputInterceptors(" + autoloadInputInterceptors + ")\n"
      + "      .withAutoloadOutputInterceptors(" + autoloadOutputInterceptors + ")\n"
      + "      .withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public " + GREETING_PROCESSOR_RESPONSE_TYPE +  " handleStreamedBeanRequest(" + GREETING_PROCESSOR_REQUEST_TYPE + " input, Context context) {\n"
      + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
      + "    return \"Hello, \" + name + \"!\";\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  protected Boolean getAutoloadAll() {\n"
      + "    return " + autoloadAll + ";\n"
      + "  }\n"
      + "}\n";
    // @formatter:on
  }

  /**
   * We don't seem to have access to the runtime client when running in LocalStack, so we can't use
   * the default serializer. Use Just JSON for testing.
   */
  @Override
  protected List<File> getRunClasspath(Compilation compilation) throws IOException {
    final List<File> result = new ArrayList<>(super.getRunClasspath(compilation));
    result.add(findJarInBuild("lammy-just-json-serialization"));
    result.add(findJarInLocalMavenRepository("com.sigpwned", "just-json", JUST_JSON_VERSION));
    return unmodifiableList(result);
  }
}
