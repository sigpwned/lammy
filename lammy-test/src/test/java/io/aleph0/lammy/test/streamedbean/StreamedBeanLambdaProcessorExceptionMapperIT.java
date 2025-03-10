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
import io.aleph0.lammy.test.ExceptionMapperTestBase;

@Testcontainers
public class StreamedBeanLambdaProcessorExceptionMapperIT extends ExceptionMapperTestBase {
  static {
    // Enable this when needed for debugging
    // localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  @Override
  public String throwingProcessorSource(Boolean autoloadAll, Boolean autoloadRequestFilters,
      Boolean autoloadResponseFilters, Boolean autoloadExceptionMappers) {
    // @formatter:off
    return ""
      + "package com.example;\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
      + "import io.aleph0.lammy.core.base.streamedbean.StreamedBeanLambdaProcessorBase;\n"
      + "import io.aleph0.lammy.core.base.streamedbean.StreamedBeanLambdaProcessorConfiguration;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class LambdaFunction extends StreamedBeanLambdaProcessorBase<" + THROWING_PROCESSOR_REQUEST_TYPE + ", " + THROWING_PROCESSOR_RESPONSE_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new StreamedBeanLambdaProcessorConfiguration()\n"
      + "      .withAutoloadRequestFilters(" + autoloadRequestFilters + ")\n"
      + "      .withAutoloadResponseFilters(" + autoloadResponseFilters + ")\n"
      + "      .withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public " + THROWING_PROCESSOR_RESPONSE_TYPE +  " handleStreamedBeanRequest(" + THROWING_PROCESSOR_REQUEST_TYPE + " input, Context context) {\n"
      + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
      + "    throw new IllegalArgumentException(name);\n"
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
