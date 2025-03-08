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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.sigpwned.just.json.JustJson;

@Testcontainers
public class BeanLambdaFunctionIntegrationTest extends LammyTestBase
    implements BeanFunctionTesting {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BeanLambdaFunctionIntegrationTest.class);

  static {
    localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  // GREETING PROCESSOR ////////////////////////////////////////////////////////////////////////////

  public String greetingProcessorRequest(String name) {
    return "{\"name\": \"" + name + "\"}";
  }

  public String greetingProcessorResponse(String name) {
    return "\"Hello, " + name + "!\"";
  }

  public static final String GREETING_PROCESSOR_REQUEST_TYPE = "Map<String, Object>";

  public static final String GREETING_PROCESSOR_RESPONSE_TYPE = "String";

  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadRequestFilters,
      Boolean autoloadResponseFilters, Boolean autoloadExceptionMappers) {
    // @formatter:off
    return ""
      + "package com.example;\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
      + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
      + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class LambdaFunction extends BeanLambdaFunctionBase<" + GREETING_PROCESSOR_REQUEST_TYPE + ", " + GREETING_PROCESSOR_RESPONSE_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new BeanLambdaFunctionConfiguration()\n"
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

  // SMOKE TESTS ///////////////////////////////////////////////////////////////////////////////////

  @Test
  public void givenExample_whenBuildAndInvoke_thenGetExpectedResult() throws IOException {
    final Boolean autoloadAll = null;
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final Compilation compilation = doCompile(handler);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenBlanketEnabled_thenAllServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject requestFilter =
        prepareSourceFile(requestFilterSource(nonce, "A", GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject responseFilter = prepareSourceFile(responseFilterSource(nonce, "A",
        GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject exceptionMapper = prepareSourceFile(exceptionMapperSource(nonce, "A",
        "IllegalArgumentException", GREETING_PROCESSOR_RESPONSE_TYPE, "e.getMessage()"));

    final Compilation compilation =
        doCompile(handler, requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(REQUEST_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            requestFilterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(RESPONSE_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            responseFilterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(EXCEPTION_MAPPER_SERVICE_LOADER_JAR_ENTRY_NAME),
            exceptionMapperQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)));
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    assertThat(localstack.getLogs()).contains(requestFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(requestFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(responseFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(responseFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(exceptionMapperInitMessage(nonce, "A"));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenAllEnabled_thenAllServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadRequestFilters = true;
    final Boolean autoloadResponseFilters = true;
    final Boolean autoloadExceptionMappers = true;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject requestFilter =
        prepareSourceFile(requestFilterSource(nonce, "A", GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject responseFilter = prepareSourceFile(responseFilterSource(nonce, "A",
        GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject exceptionMapper = prepareSourceFile(exceptionMapperSource(nonce, "A",
        "IllegalArgumentException", GREETING_PROCESSOR_RESPONSE_TYPE, "e.getMessage()"));

    final Compilation compilation =
        doCompile(handler, requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(REQUEST_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            requestFilterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(RESPONSE_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            responseFilterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(EXCEPTION_MAPPER_SERVICE_LOADER_JAR_ENTRY_NAME),
            exceptionMapperQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)));
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    assertThat(localstack.getLogs()).contains(requestFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(requestFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(responseFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(responseFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(exceptionMapperInitMessage(nonce, "A"));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenNotEnabled_thenNoServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject requestFilter =
        prepareSourceFile(requestFilterSource(nonce, "A", GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject responseFilter = prepareSourceFile(responseFilterSource(nonce, "A",
        GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject exceptionMapper = prepareSourceFile(exceptionMapperSource(nonce, "A",
        "IllegalArgumentException", GREETING_PROCESSOR_RESPONSE_TYPE, "e.getMessage()"));

    final Compilation compilation =
        doCompile(handler, requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(REQUEST_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            requestFilterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(RESPONSE_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            responseFilterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(EXCEPTION_MAPPER_SERVICE_LOADER_JAR_ENTRY_NAME),
            exceptionMapperQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)));
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    assertThat(localstack.getLogs()).doesNotContain(requestFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(requestFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(responseFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(responseFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(exceptionMapperInitMessage(nonce, "A"));
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

  // REQUEST FILTERS TESTS /////////////////////////////////////////////////////////////////////////

  protected String buildAndInvokeGreetingProcessorWithTwoRequestFilters(String nonce,
      Boolean autoloadAll, Boolean autoloadRequestFilters, String firstRequestFilterId,
      String secondRequestFilterId, String name) throws IOException {
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject requestFilterA = prepareSourceFile(
        requestFilterSource(nonce, firstRequestFilterId, GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject requestFilterB = prepareSourceFile(
        requestFilterSource(nonce, secondRequestFilterId, GREETING_PROCESSOR_REQUEST_TYPE));

    final Compilation compilation = doCompile(handler, requestFilterA, requestFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call first then second.
    final ExtraJarEntry requestFilterServices =
        new ExtraJarEntry(new JarEntry(REQUEST_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            String
                .join("\n", requestFilterQualifiedClassName(firstRequestFilterId),
                    requestFilterQualifiedClassName(secondRequestFilterId))
                .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, requestFilterServices);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);
      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    return output;
  }

  @Test
  public void givenRequestFilterServicesAB_whenAutoloadExplicitlyEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadRequestFilters = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    assertThat(requestFilterAIndex).isLessThan(requestFilterBIndex);
  }

  @Test
  public void givenRequestFilterServicesAB_whenAutoloadBlanketEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadRequestFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    assertThat(requestFilterAIndex).isLessThan(requestFilterBIndex);
  }

  @Test
  public void givenRequestFilterServicesBA_whenAutoloadExplicitlyEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadRequestFilters = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    assertThat(requestFilterBIndex).isLessThan(requestFilterAIndex);
  }

  @Test
  public void givenRequestFilterServicesBA_whenAutoloadBlanketEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadRequestFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    assertThat(requestFilterBIndex).isLessThan(requestFilterAIndex);
  }

  @Test
  public void givenRequestFilterServices_whenAutoloadNotEnabled_thenDontCall() throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadRequestFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenRequestFilterServices_whenAutoloadBlanketEnabledExplicitlyDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadRequestFilters = false;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenRequestFilterServices_whenAutoloadBlanketDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = false;
    final Boolean autoloadRequestFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoRequestFilters(nonce, autoloadAll,
        autoloadRequestFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }

  // RESPONSE FILTER TESTS /////////////////////////////////////////////////////////////////////////

  protected String buildAndInvokeGreetingProcessorWithTwoResponseFilters(String nonce,
      Boolean autoloadAll, Boolean autoloadResponseFilters, String firstResponseFilterId,
      String secondResponseFilterId, String name) throws IOException {
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject responseFilterA = prepareSourceFile(responseFilterSource(nonce,
        firstResponseFilterId, GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject responseFilterB = prepareSourceFile(responseFilterSource(nonce,
        secondResponseFilterId, GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final Compilation compilation = doCompile(handler, responseFilterA, responseFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call first then second.
    final ExtraJarEntry responseFilterServices =
        new ExtraJarEntry(new JarEntry(RESPONSE_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME),
            String
                .join("\n", responseFilterQualifiedClassName(firstResponseFilterId),
                    responseFilterQualifiedClassName(secondResponseFilterId))
                .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, responseFilterServices);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);
      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    return output;
  }

  @Test
  public void givenResponseFilterServicesAB_whenAutoloadExplicitlyEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadResponseFilters = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    assertThat(responseFilterAIndex).isLessThan(responseFilterBIndex);
  }

  @Test
  public void givenResponseFilterServicesAB_whenAutoloadBlanketEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadResponseFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    assertThat(responseFilterAIndex).isLessThan(responseFilterBIndex);
  }

  @Test
  public void givenResponseFilterServicesBA_whenAutoloadExplicitlyEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadResponseFilters = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    assertThat(responseFilterBIndex).isLessThan(responseFilterAIndex);
  }

  @Test
  public void givenResponseFilterServicesBA_whenAutoloadBlanketEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadResponseFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    assertThat(responseFilterBIndex).isLessThan(responseFilterAIndex);
  }

  @Test
  public void givenResponseFilterServices_whenAutoloadNotEnabled_thenDontCall() throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadResponseFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenResponseFilterServices_whenAutoloadBlanketEnabledExplicitlyDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadResponseFilters = false;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenResponseFilterServices_whenAutoloadBlanketDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = false;
    final Boolean autoloadResponseFilters = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoResponseFilters(nonce, autoloadAll,
        autoloadResponseFilters, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(responseFilterFilterMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isEqualTo(-1);
  }

  // THROWING PROCESSOR ////////////////////////////////////////////////////////////////////////////

  public String throwingProcessorRequest(String name) {
    return "{\"name\": \"" + name + "\"}";
  }

  public String throwingProcessorResponseValue(String exceptionMapperId, String name) {
    return "\"Exception Mapper " + exceptionMapperId + ": Invalid name: " + name + "\"";
  }

  public String throwingProcessorResponseExpr(String exceptionMapperId) {
    return "\"Exception Mapper " + exceptionMapperId + ": Invalid name: \" + e.getMessage()";
  }

  public static final String THROWING_PROCESSOR_REQUEST_TYPE = "Map<String, Object>";

  public static final String THROWING_PROCESSOR_RESPONSE_TYPE = "String";

  public String throwingProcessorSource(Boolean autoloadAll, Boolean autoloadRequestFilters,
      Boolean autoloadResponseFilters, Boolean autoloadExceptionMappers) {
    // @formatter:off
    return ""
      + "package com.example;\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.amazonaws.services.lambda.runtime.RequestHandler;\n"
      + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionBase;\n"
      + "import com.sigpwned.lammy.core.base.bean.BeanLambdaFunctionConfiguration;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class LambdaFunction extends BeanLambdaFunctionBase<" + THROWING_PROCESSOR_REQUEST_TYPE + ", " + THROWING_PROCESSOR_RESPONSE_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new BeanLambdaFunctionConfiguration()\n"
      + "      .withAutoloadRequestFilters(" + autoloadRequestFilters + ")\n"
      + "      .withAutoloadResponseFilters(" + autoloadResponseFilters + ")\n"
      + "      .withAutoloadExceptionMappers(" + autoloadExceptionMappers + "));\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public " + THROWING_PROCESSOR_RESPONSE_TYPE +  " handleBeanRequest(" + THROWING_PROCESSOR_REQUEST_TYPE + " input, Context context) {\n"
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

  // EXCEPTION MAPPER TESTS ////////////////////////////////////////////////////////////////////////

  protected String buildAndInvokeThrowingProcessorWithTwoExceptionMappers(String nonce,
      Boolean autoloadAll, Boolean autoloadExceptionMappers, String firstExceptionMapperId,
      String firstExceptionType, String secondExceptionMapperId, String secondExceptionType,
      String name) throws IOException {
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;

    final JavaFileObject handler = prepareSourceFile(throwingProcessorSource(autoloadAll,
        autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject firstExceptionMapper = prepareSourceFile(exceptionMapperSource(nonce,
        firstExceptionMapperId, firstExceptionType, GREETING_PROCESSOR_RESPONSE_TYPE,
        throwingProcessorResponseExpr(firstExceptionMapperId)));

    final JavaFileObject secondExceptionMapper = prepareSourceFile(exceptionMapperSource(nonce,
        secondExceptionMapperId, secondExceptionType, GREETING_PROCESSOR_RESPONSE_TYPE,
        throwingProcessorResponseExpr(secondExceptionMapperId)));

    final Compilation compilation = doCompile(handler, firstExceptionMapper, secondExceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call first then second.
    final ExtraJarEntry responseFilterServices =
        new ExtraJarEntry(new JarEntry(EXCEPTION_MAPPER_SERVICE_LOADER_JAR_ENTRY_NAME),
            String
                .join("\n", exceptionMapperQualifiedClassName(firstExceptionMapperId),
                    exceptionMapperQualifiedClassName(secondExceptionMapperId))
                .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, responseFilterServices);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);
      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    return output;
  }

  @Test
  public void givenExceptionMapperServicesAB_whenAutoloadExplicitlyEnabledAndThrowMatchingAB_thenUseA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadExceptionMappers = true;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "IllegalArgumentException", "B", "RuntimeException", name);

    assertThat(output).isEqualTo(throwingProcessorResponseValue("A", name));

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isEqualTo(-1);

    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperAFilterMessageIndex);
    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperBInitMessageIndex);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenAutoloadBlanketEnabledAndThrowMatchingAB_thenUseA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadExceptionMappers = null;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "IllegalArgumentException", "B", "RuntimeException", name);

    assertThat(output).isEqualTo(throwingProcessorResponseValue("A", name));

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isEqualTo(-1);

    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperAFilterMessageIndex);
    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperBInitMessageIndex);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenAutoloadExplicitlyEnabledAndThrowMatchingB_thenUseB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadExceptionMappers = true;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "UnsupportedOperationException", "B", "RuntimeException",
        name);

    assertThat(output).isEqualTo(throwingProcessorResponseValue("B", name));

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isNotEqualTo(-1);

    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperBInitMessageIndex);
    assertThat(exceptionMapperBInitMessageIndex).isLessThan(exceptionMapperBFilterMessageIndex);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenAutoloadBlanketEnabledAndThrowMatchingB_thenUseB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadExceptionMappers = null;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "UnsupportedOperationException", "B", "RuntimeException",
        name);

    assertThat(output).isEqualTo(throwingProcessorResponseValue("B", name));

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isNotEqualTo(-1);

    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperBInitMessageIndex);
    assertThat(exceptionMapperBInitMessageIndex).isLessThan(exceptionMapperBFilterMessageIndex);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenNotEnabledAndThrowMatchingAB_thenPropagate()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadExceptionMappers = null;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "IllegalArgumentException", "B", "RuntimeException", name);

    @SuppressWarnings("unchecked")
    final Map<String, Object> response = (Map<String, Object>) JustJson.parseDocument(output);
    assertThat(response).containsEntry("errorType", "java.lang.IllegalArgumentException");
    assertThat(response).containsEntry("errorMessage", "Test");

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isEqualTo(-1);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenBlanketEnabledExplicitlyDisabledAndThrowMatchingAB_thenPropagate()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadExceptionMappers = false;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "IllegalArgumentException", "B", "RuntimeException", name);

    @SuppressWarnings("unchecked")
    final Map<String, Object> response = (Map<String, Object>) JustJson.parseDocument(output);
    assertThat(response).containsEntry("errorType", "java.lang.IllegalArgumentException");
    assertThat(response).containsEntry("errorMessage", "Test");

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isEqualTo(-1);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenBlanketDisabledAndThrowMatchingAB_thenPropagate()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = false;
    final Boolean autoloadExceptionMappers = null;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "IllegalArgumentException", "B", "RuntimeException", name);

    @SuppressWarnings("unchecked")
    final Map<String, Object> response = (Map<String, Object>) JustJson.parseDocument(output);
    assertThat(response).containsEntry("errorType", "java.lang.IllegalArgumentException");
    assertThat(response).containsEntry("errorMessage", "Test");

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isEqualTo(-1);
  }

  @Test
  public void givenExceptionMapperServicesAB_whenAutoloadExplicitlyEnabledAndThrowNonMatching_thenPropagate()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadExceptionMappers = true;

    final String name = "Test";

    final String output = buildAndInvokeThrowingProcessorWithTwoExceptionMappers(nonce, autoloadAll,
        autoloadExceptionMappers, "A", "UnsupportedOperationException", "B",
        "IllegalStateException", name);

    @SuppressWarnings("unchecked")
    final Map<String, Object> response = (Map<String, Object>) JustJson.parseDocument(output);
    assertThat(response).containsEntry("errorType", "java.lang.IllegalArgumentException");
    assertThat(response).containsEntry("errorMessage", "Test");

    final int exceptionMapperAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "A"));
    assertThat(exceptionMapperAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "A"));
    assertThat(exceptionMapperAFilterMessageIndex).isEqualTo(-1);

    final int exceptionMapperBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperInitMessage(nonce, "B"));
    assertThat(exceptionMapperBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionMapperBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionMapperFilterMessage(nonce, "B"));
    assertThat(exceptionMapperBFilterMessageIndex).isEqualTo(-1);

    assertThat(exceptionMapperAInitMessageIndex).isLessThan(exceptionMapperBInitMessageIndex);
  }
}
