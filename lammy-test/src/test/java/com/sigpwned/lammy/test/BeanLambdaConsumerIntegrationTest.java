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
package com.sigpwned.lammy.test;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;

@Testcontainers
@Disabled("Is this the cause of errors?")
public class BeanLambdaConsumerIntegrationTest extends LammyTestBase {
  @Test
  public void givenBeanLambdaConsumerBaseExample_whenBuildAndInvoke_thenGetExpectedResult()
      throws IOException {
    final String nonce = nonce();

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaConsumerBase;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaConsumerBase<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void consumeBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    System.out.println(\"" + nonce+ ": Hello, \" + name + \"!\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler);

    CompilationSubject.assertThat(compilation).succeeded();

    final File deploymentPackage = createDeploymentPackage(compilation);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    assertThat(localstack.getLogs()).contains(nonce + ": Hello, Test!");
  }
}
