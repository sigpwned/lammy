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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;

@Testcontainers
public class LambdaIntegrationTest extends LammyTestBase {
  @Test
  public void testDeployAndInvokeLambda() throws Exception {
    final JavaFileObject handler = prepareSourceFile("package com.example;\n" + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import java.util.Map;\n" + "\n"
        + "public class HelloLambda extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "    @Override\n"
        + "    public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "        String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "        return \"Hello, \" + name + \"!\";\n" + "    }\n" + "}\n");

    final Compilation compilation = doCompile(handler);

    CompilationSubject.assertThat(compilation).succeeded();

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation);
    try {
      // Make sure our JAR file doesn't contain any amazon classes. All that stuff is provided by
      // the Lambda environment. We don't want it in our JARs.
      try (JarFile deploymentPackageJar = new JarFile(deploymentPackage)) {
        // Here, we're looking for a few specific entries in the JAR file:
        // - META-INF/ -- various and sundry metadata, which is all fine
        // - com/sigpwned/lammy/ -- the lammy prefix
        // - com/example/ -- this exmaple lambda function
        // - org/crac/ -- the Coordinated Restore at Checkpoint library
        assertThat(deploymentPackageJar.stream()).map(JarEntry::getName).allMatch(
            name -> name.startsWith("com/sigpwned/lammy/") || name.startsWith("com/example/")
                || name.startsWith("org/crac/") || name.startsWith("META-INF/"));
      }

      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo("\"Hello, Test!\"");
  }
}
