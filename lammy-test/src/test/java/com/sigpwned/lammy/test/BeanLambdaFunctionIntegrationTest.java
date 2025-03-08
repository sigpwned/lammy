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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.JarEntry;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.sigpwned.just.json.JustJson;

@Testcontainers
public class BeanLambdaFunctionIntegrationTest extends LammyTestBase {
  @Test
  public void givenExample_whenBuildAndInvoke_thenGetExpectedResult() throws IOException {
    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler);

    CompilationSubject.assertThat(compilation).succeeded();

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo("\"Hello, Test!\"");
  }

  @Test
  @Disabled("Does LocalStack support custom serializers?")
  public void givenCustomSerializerWithServiceLoad_whenBuildAndInvoke_thenCustomSerializerIsUsed()
      throws IOException {
    final String nonce = nonce();

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject customSerializer = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;\n"
        + "import java.io.IOException;\n"
        + "import java.io.InputStream;\n"
        + "import java.io.OutputStream;\n"
        + "import java.io.ByteArrayOutputStream;\n"
        + "import java.io.UncheckedIOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.nio.charset.StandardCharsets;\n"
        + "\n"
        + "public class ExampleCustomSerializer implements CustomPojoSerializer {\n"
        + "  @Override\n"
        + "  public <T> T fromJson(InputStream input, Type type) {\n"
        + "    System.out.println(\"" + nonce + ": CustomSerializer.fromJson\");\n"
        + "    try {\n"
        + "      return (T) new String(readAllBytes(input), StandardCharsets.UTF_8);\n"
        + "    } catch (IOException e) {\n"
        + "      throw new UncheckedIOException(\"Failed to read input\", e);\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public <T> T fromJson(String input, Type type) {\n"
        + "    System.out.println(\"" + nonce + ": CustomSerializer.fromJson\");\n"
        + "    return (T) input;\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public <T> void toJson(T value, OutputStream output, Type type) {\n"
        + "    System.out.println(\"" + nonce + ": CustomSerializer.toJson\");\n"
        + "    try {\n"
        + "      output.write(value.toString().getBytes(StandardCharsets.UTF_8));\n"
        + "    } catch (IOException e) {\n"
        + "      throw new UncheckedIOException(\"Failed to write output\", e);\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  private byte[] readAllBytes(InputStream input) throws IOException {\n"
        + "    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {\n"
        + "      byte[] buf = new byte[4096];\n"
        + "      for(int n=input.read(buf);n != -1;n=input.read(buf)) {\n"
        + "        output.write(buf, 0, n);\n"
        + "      }\n"
        + "      return output.toByteArray();\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, customSerializer);

    CompilationSubject.assertThat(compilation).succeeded();

    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry(
            "META-INF/services/com.amazonaws.services.lambda.runtime.CustomPojoSerializer"),
        "com.example.ExampleCustomSerializer".getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).contains(nonce + ": CustomSerializer.fromJson");
    assertThat(localstack.getLogs()).contains(nonce + ": CustomSerializer.toJson");
  }

  @Test
  public void givenRequestFiltersWithServiceLoadAndExplicitlyEnabledOrderAB_whenBuildAndInvoke_thenFiltersAreCalledInCorrectOrder()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadRequestFilters = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadRequestFilters(" + autoloadRequestFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilterA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilterA implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilterA.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilterB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilterB implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilterB.filterRequest\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, requestFilterA, requestFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.RequestFilter"),
        new StringBuilder().append("com.example.ExampleRequestFilterA\n")
            .append("com.example.ExampleRequestFilterB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleRequestFilterA.filterRequest";
    final String requestFilterBLog = nonce + ": ExampleRequestFilterB.filterRequest";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).contains(requestFilterALog);
    assertThat(localstack.getLogs()).contains(requestFilterBLog);

    final int requestFilterAIndex = localstack.getLogs().indexOf(requestFilterALog);
    final int requestFilterBIndex = localstack.getLogs().indexOf(requestFilterBLog);

    assertThat(requestFilterAIndex).isLessThan(requestFilterBIndex);
  }

  @Test
  public void givenRequestFiltersWithServiceLoadAndExplicitlyEnabledOrderBA_whenBuildAndInvoke_thenFiltersAreCalledInCorrectOrder()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadRequestFilters = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadRequestFilters(" + autoloadRequestFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilterA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilterA implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilterA.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilterB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilterB implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilterB.filterRequest\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, requestFilterA, requestFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call B then A.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.RequestFilter"),
        new StringBuilder().append("com.example.ExampleRequestFilterB\n")
            .append("com.example.ExampleRequestFilterA\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleRequestFilterA.filterRequest";
    final String requestFilterBLog = nonce + ": ExampleRequestFilterB.filterRequest";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).contains(requestFilterALog);
    assertThat(localstack.getLogs()).contains(requestFilterBLog);

    final int requestFilterAIndex = localstack.getLogs().indexOf(requestFilterALog);
    final int requestFilterBIndex = localstack.getLogs().indexOf(requestFilterBLog);

    assertThat(requestFilterBIndex).isLessThan(requestFilterAIndex);
  }

  @Test
  public void givenRequestFiltersWithServiceLoadAndExplicitlyDisabled_whenBuildAndInvoke_thenFiltersAreNotCalled()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadRequestFilters = false;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadRequestFilters(" + autoloadRequestFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilterA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilterA implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilterA.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilterB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilterB implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilterB.filterRequest\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, requestFilterA, requestFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.RequestFilter"),
        new StringBuilder().append("com.example.ExampleRequestFilterA\n")
            .append("com.example.ExampleRequestFilterB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleRequestFilterA.filterRequest";
    final String requestFilterBLog = nonce + ": ExampleRequestFilterB.filterRequest";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).doesNotContain(requestFilterALog);
    assertThat(localstack.getLogs()).doesNotContain(requestFilterBLog);
  }

  @Test
  public void givenResponseFiltersWithServiceLoadAndExplicitlyEnabledOrderAB_whenBuildAndInvoke_thenFiltersAreCalledInCorrectOrder()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadResponseFilters = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadResponseFilters(" + autoloadResponseFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilterA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilterA implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilterA.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilterB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilterB implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilterB.filterRequest\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, responseFilterA, responseFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ResponseFilter"),
        new StringBuilder().append("com.example.ExampleResponseFilterA\n")
            .append("com.example.ExampleResponseFilterB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String responseFilterALog = nonce + ": ExampleResponseFilterA.filterRequest";
    final String responseFilterBLog = nonce + ": ExampleResponseFilterB.filterRequest";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).contains(responseFilterALog);
    assertThat(localstack.getLogs()).contains(responseFilterBLog);

    final int requestFilterAIndex = localstack.getLogs().indexOf(responseFilterALog);
    final int requestFilterBIndex = localstack.getLogs().indexOf(responseFilterBLog);

    assertThat(requestFilterAIndex).isLessThan(requestFilterBIndex);
  }

  @Test
  public void givenResponseFiltersWithServiceLoadAndExplicitlyEnabledOrderBA_whenBuildAndInvoke_thenFiltersAreCalledInCorrectOrder()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadResponseFilters = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadResponseFilters(" + autoloadResponseFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilterA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilterA implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilterA.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilterB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilterB implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilterB.filterRequest\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, responseFilterA, responseFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ResponseFilter"),
        new StringBuilder().append("com.example.ExampleResponseFilterB\n")
            .append("com.example.ExampleResponseFilterA\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String responseFilterALog = nonce + ": ExampleResponseFilterA.filterRequest";
    final String responseFilterBLog = nonce + ": ExampleResponseFilterB.filterRequest";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).contains(responseFilterALog);
    assertThat(localstack.getLogs()).contains(responseFilterBLog);

    final int requestFilterAIndex = localstack.getLogs().indexOf(responseFilterALog);
    final int requestFilterBIndex = localstack.getLogs().indexOf(responseFilterBLog);

    assertThat(requestFilterBIndex).isLessThan(requestFilterAIndex);
  }

  @Test
  public void givenResponseFiltersWithServiceLoadAndExplicitlyDisabled_whenBuildAndInvoke_thenFiltersAreNotCalled()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadResponseFilters = false;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadResponseFilters(" + autoloadResponseFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilterA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilterA implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilterA.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilterB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilterB implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilterB.filterRequest\");\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, responseFilterA, responseFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ResponseFilter"),
        new StringBuilder().append("com.example.ExampleResponseFilterA\n")
            .append("com.example.ExampleResponseFilterB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String responseFilterALog = nonce + ": ExampleResponseFilterA.filterRequest";
    final String responseFilterBLog = nonce + ": ExampleResponseFilterB.filterRequest";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).doesNotContain(responseFilterALog);
    assertThat(localstack.getLogs()).doesNotContain(responseFilterBLog);
  }

  @Test
  public void givenExceptionMappersWithServiceLoadAndExplicitlyEnabledOrderAB_whenBuildAndInvokeAndThrowMatchingAB_thenAUsed()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadExceptionMappers = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    throw new IllegalArgumentException(\"Invalid name: \" + name);\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperA implements ExceptionMapper<IllegalArgumentException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(IllegalArgumentException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperA.mapExceptionTo\");\n"
        + "    return \"Exception Mapper A: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperB implements ExceptionMapper<RuntimeException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(RuntimeException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperB.mapExceptionTo\");\n"
        + "    return \"Exception Mapper B: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, exceptionMapperA, exceptionMapperB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper"),
        new StringBuilder().append("com.example.ExampleExceptionMapperA\n")
            .append("com.example.ExampleExceptionMapperB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleExceptionMapperA.mapExceptionTo";
    final String requestFilterBLog = nonce + ": ExampleExceptionMapperB.mapExceptionTo";

    assertThat(output).isEqualTo("\"Exception Mapper A: Invalid name: Test\"");
    assertThat(localstack.getLogs()).contains(requestFilterALog);
    assertThat(localstack.getLogs()).doesNotContain(requestFilterBLog);
  }

  @Test
  public void givenExceptionMappersWithServiceLoadAndExplicitlyEnabledOrderAB_whenBuildAndInvokeAndThrowMatchingB_thenBUsed()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadExceptionMappers = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    throw new RuntimeException(\"Invalid name: \" + name);\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperA implements ExceptionMapper<IllegalArgumentException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(IllegalArgumentException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperA.mapExceptionTo\");\n"
        + "    return \"Exception Mapper A: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperB implements ExceptionMapper<RuntimeException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(RuntimeException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperB.mapExceptionTo\");\n"
        + "    return \"Exception Mapper B: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, exceptionMapperA, exceptionMapperB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper"),
        new StringBuilder().append("com.example.ExampleExceptionMapperA\n")
            .append("com.example.ExampleExceptionMapperB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleExceptionMapperA.mapExceptionTo";
    final String requestFilterBLog = nonce + ": ExampleExceptionMapperB.mapExceptionTo";

    assertThat(output).isEqualTo("\"Exception Mapper B: Invalid name: Test\"");
    assertThat(localstack.getLogs()).doesNotContain(requestFilterALog);
    assertThat(localstack.getLogs()).contains(requestFilterBLog);
  }

  @Test
  public void givenExceptionMappersWithServiceLoadAndExplicitlyEnabledOrderBA_whenBuildAndInvokeAndThrowMatchingAB_thenBUsed()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadExceptionMappers = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    throw new IllegalArgumentException(\"Invalid name: \" + name);\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperA implements ExceptionMapper<IllegalArgumentException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(IllegalArgumentException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperA.mapExceptionTo\");\n"
        + "    return \"Exception Mapper A: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperB implements ExceptionMapper<RuntimeException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(RuntimeException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperB.mapExceptionTo\");\n"
        + "    return \"Exception Mapper B: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, exceptionMapperA, exceptionMapperB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper"),
        new StringBuilder().append("com.example.ExampleExceptionMapperB\n")
            .append("com.example.ExampleExceptionMapperA\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleExceptionMapperA.mapExceptionTo";
    final String requestFilterBLog = nonce + ": ExampleExceptionMapperB.mapExceptionTo";

    assertThat(output).isEqualTo("\"Exception Mapper B: Invalid name: Test\"");
    assertThat(localstack.getLogs()).doesNotContain(requestFilterALog);
    assertThat(localstack.getLogs()).contains(requestFilterBLog);
  }

  @Test
  public void givenExceptionMappersWithServiceLoadAndExplicitlyEnabledOrderAB_whenBuildAndInvokeAndThrowMatchingNone_thenPropagated()
      throws IOException {
    final String nonce = nonce();

    final boolean autoloadExceptionMappers = true;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    throw new RuntimeException(\"Invalid name: \" + name);\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperA = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperA implements ExceptionMapper<IllegalArgumentException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(IllegalArgumentException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperA.mapExceptionTo\");\n"
        + "    return \"Exception Mapper A: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapperB = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapperB implements ExceptionMapper<UnsupportedOperationException, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(UnsupportedOperationException e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapperB.mapExceptionTo\");\n"
        + "    return \"Exception Mapper B: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation = doCompile(handler, exceptionMapperA, exceptionMapperB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry customSerializerService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper"),
        new StringBuilder().append("com.example.ExampleExceptionMapperA\n")
            .append("com.example.ExampleExceptionMapperB\n").toString()
            .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, customSerializerService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterALog = nonce + ": ExampleExceptionMapperA.mapExceptionTo";
    final String requestFilterBLog = nonce + ": ExampleExceptionMapperB.mapExceptionTo";

    @SuppressWarnings("unchecked")
    final Map<String, Object> error = (Map<String, Object>) JustJson.parseDocument(output);
    assertThat(error).containsEntry("errorMessage", "Invalid name: Test");
    assertThat(error).containsEntry("errorType", "java.lang.RuntimeException");

    assertThat(localstack.getLogs()).doesNotContain(requestFilterALog);
    assertThat(localstack.getLogs()).doesNotContain(requestFilterBLog);
  }

  @Test
  public void givenRequestFiltersAndResponseFiltersAndExceptionMappersWithServiceLoadAndBlanketEnabled_whenBuildAndInvoke_thenFiltersAreCalled()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadRequestFilters = null;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadRequestFilters(" + autoloadRequestFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    return \"Hello, \" + name + \"!\";\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  protected Boolean getAutoloadAll() {\n"
        + "    return " + autoloadAll + ";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilter = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilter implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilter.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilter = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilter implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilter.filterResponse\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapper = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapper implements ExceptionMapper<Exception, String> {\n"
        + "  public ExampleExceptionMapper() {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapper.new\");\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(Exception e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapper.mapExceptionTo\");\n"
        + "    return \"Exception Mapper: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation =
        doCompile(handler, requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry requestFilterService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.RequestFilter"),
        "com.example.ExampleRequestFilter".getBytes(StandardCharsets.UTF_8));
    final ExtraJarEntry responseFilterService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ResponseFilter"),
        "com.example.ExampleResponseFilter".getBytes(StandardCharsets.UTF_8));
    final ExtraJarEntry errorMapperService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper"),
        "com.example.ExampleExceptionMapper".getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, requestFilterService,
        responseFilterService, errorMapperService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String exceptionMapperNewLog = nonce + ": ExampleExceptionMapper.new";
    final String requestFilterLog = nonce + ": ExampleRequestFilter.filterRequest";
    final String responseFilterLog = nonce + ": ExampleResponseFilter.filterResponse";
    final String exceptionMapperLog = nonce + ": ExampleExceptionMapper.mapExceptionTo";

    assertThat(output).isEqualTo("\"Hello, Test!\"");
    assertThat(localstack.getLogs()).contains(exceptionMapperNewLog);
    assertThat(localstack.getLogs()).contains(requestFilterLog);
    assertThat(localstack.getLogs()).contains(responseFilterLog);
    assertThat(localstack.getLogs()).doesNotContain(exceptionMapperLog);

    final int requestFilterIndex = localstack.getLogs().indexOf(requestFilterLog);
    final int responseFilterIndex = localstack.getLogs().indexOf(responseFilterLog);

    assertThat(requestFilterIndex).isLessThan(responseFilterIndex);
  }

  @Test
  public void givenRequestFiltersAndResponseFiltersAndExceptionMappersWithServiceLoadAndBlanketEnabled_whenBuildAndInvokeAndThrow_thenExceptionMapperIsCalled()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadRequestFilters = null;

    // @formatter:off
    final JavaFileObject handler = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
        + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class LambdaFunction extends BeanLambdaFunctionBase<Map<String, Object>, String> {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new BeanLambdaFunctionConfiguration().withAutoloadRequestFilters(" + autoloadRequestFilters + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String handleBeanRequest(Map<String, Object> input, Context context) {\n"
        + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
        + "    throw new IllegalArgumentException(\"Invalid name: \" + name);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  protected Boolean getAutoloadAll() {\n"
        + "    return " + autoloadAll + ";\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject requestFilter = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleRequestFilter implements RequestFilter<Map<String, Object>> {\n"
        + "  @Override\n"
        + "  public void filterRequest(RequestContext<Map<String, Object>> requestContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleRequestFilter.filterRequest\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject responseFilter = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
        + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class ExampleResponseFilter implements ResponseFilter<Map<String, Object>, String> {\n"
        + "  @Override\n"
        + "  public void filterResponse(RequestContext<Map<String, Object>> requestContext, \n"
        + "      ResponseContext<String> responseContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleResponseFilter.filterResponse\");\n"
        + "  }\n"
        + "}\n");

    final JavaFileObject exceptionMapper = prepareSourceFile(""
        + "package com.example;\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class ExampleExceptionMapper implements ExceptionMapper<Exception, String> {\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(Exception e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + nonce + ": ExampleExceptionMapper.mapExceptionTo\");\n"
        + "    return \"Exception Mapper: \" + e.getMessage();\n"
        + "  }\n"
        + "}\n");
    // @formatter:on

    final Compilation compilation =
        doCompile(handler, requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call A then B.
    final ExtraJarEntry requestFilterService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.RequestFilter"),
        "com.example.ExampleRequestFilter".getBytes(StandardCharsets.UTF_8));
    final ExtraJarEntry responseFilterService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ResponseFilter"),
        "com.example.ExampleResponseFilter".getBytes(StandardCharsets.UTF_8));
    final ExtraJarEntry errorMapperService = new ExtraJarEntry(
        new JarEntry("META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper"),
        "com.example.ExampleExceptionMapper".getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, requestFilterService,
        responseFilterService, errorMapperService);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, "{\"name\":\"Test\"}");
    } finally {
      deploymentPackage.delete();
    }

    final String requestFilterLog = nonce + ": ExampleRequestFilter.filterRequest";
    final String responseFilterLog = nonce + ": ExampleResponseFilter.filterResponse";
    final String exceptionMapperLog = nonce + ": ExampleExceptionMapper.mapExceptionTo";

    assertThat(output).isEqualTo("\"Exception Mapper: Invalid name: Test\"");
    assertThat(localstack.getLogs()).contains(requestFilterLog);
    assertThat(localstack.getLogs()).contains(responseFilterLog);
    assertThat(localstack.getLogs()).contains(exceptionMapperLog);

    final int requestFilterIndex = localstack.getLogs().indexOf(requestFilterLog);
    final int responseFilterIndex = localstack.getLogs().indexOf(responseFilterLog);

    assertThat(requestFilterIndex).isLessThan(responseFilterIndex);
  }
}
