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
package com.sigpwned.lammy.test.bean;

import org.testcontainers.junit.jupiter.Testcontainers;
import com.sigpwned.lammy.test.BeanSmokeTestBase;

@Testcontainers
public class BeanLambdaProcessorSmokeIT extends BeanSmokeTestBase {
  static {
    // Enable this when needed for debugging
    // localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  @Override
  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadRequestFilters,
      Boolean autoloadResponseFilters, Boolean autoloadExceptionMappers) {
    // @formatter:off
    return ""
      + "package com.example;\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
      + "import com.sigpwned.lammy.core.base.bean.BeanLambdaProcessorBase;\n"
      + "import com.sigpwned.lammy.core.base.bean.BeanLambdaProcessorConfiguration;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class LambdaFunction extends BeanLambdaProcessorBase<" + GREETING_PROCESSOR_REQUEST_TYPE + ", " + GREETING_PROCESSOR_RESPONSE_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new BeanLambdaProcessorConfiguration()\n"
      + "      .withAutoloadRequestFilters(" + autoloadRequestFilters + ")\n"
      + "      .withAutoloadResponseFilters(" + autoloadResponseFilters + ")\n"
      + "      .withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public " + GREETING_PROCESSOR_RESPONSE_TYPE +  " handleBeanRequest(" + GREETING_PROCESSOR_REQUEST_TYPE + " input, Context context) {\n"
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
}
