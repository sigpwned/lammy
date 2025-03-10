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
import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import io.aleph0.lammy.test.BeanFunctionTesting;
import io.aleph0.lammy.test.LammyTestBase;
import io.aleph0.lammy.test.StreamFunctionTesting;

@Testcontainers
public class StreamedBeanLambdaProcessorFullSmokeIT extends LammyTestBase
    implements StreamFunctionTesting, BeanFunctionTesting {
  static {
    // Enable this when needed for debugging
    // localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  public static final String GREETING_PROCESSOR_REQUEST_TYPE = "Map<String, Object>";

  public static final String GREETING_PROCESSOR_RESPONSE_TYPE = "String";

  public String greetingProcessorRequest(String name) {
    return "{\"name\":\"" + name + "\"}";
  }

  public String greetingProcessorResponse(String name) {
    return "\"Hello, " + name + "!\"";
  }

  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadInputInterceptors,
      Boolean autoloadOutputInterceptors, Boolean autoloadRequestFilters,
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
      + "public class LambdaFunction extends StreamedBeanLambdaProcessorBase<" + GREETING_PROCESSOR_REQUEST_TYPE + ", " + GREETING_PROCESSOR_RESPONSE_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new StreamedBeanLambdaProcessorConfiguration()\n"
      + "      .withAutoloadInputInterceptors(" + autoloadInputInterceptors + ")\n"
      + "      .withAutoloadOutputInterceptors(" + autoloadOutputInterceptors + ")\n"
      + "      .withAutoloadRequestFilters(" + autoloadRequestFilters + ")\n"
      + "      .withAutoloadResponseFilters(" + autoloadResponseFilters + ")\n"
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

  @Test
  public void givenExample_whenBuildAndInvoke_thenGetExpectedResult() throws IOException {
    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadOutputInterceptors = null;
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(
        greetingProcessorSource(autoloadAll, autoloadInputInterceptors, autoloadOutputInterceptors,
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
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadOutputInterceptors = null;
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(
        greetingProcessorSource(autoloadAll, autoloadInputInterceptors, autoloadOutputInterceptors,
            autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject inputInterceptor = prepareSourceFile(inputInterceptorSource(nonce, "A"));

    final JavaFileObject outputInterceptor = prepareSourceFile(outputInterceptorSource(nonce, "A"));

    final JavaFileObject requestFilter =
        prepareSourceFile(requestFilterSource(nonce, "A", GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject responseFilter = prepareSourceFile(responseFilterSource(nonce, "A",
        GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject exceptionMapper = prepareSourceFile(exceptionMapperSource(nonce, "A",
        "IllegalArgumentException", GREETING_PROCESSOR_RESPONSE_TYPE, "e.getMessage()"));

    final Compilation compilation = doCompile(handler, inputInterceptor, outputInterceptor,
        requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            inputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            outputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
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

    assertThat(localstack.getLogs()).contains(inputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs().contains(requestFilterInitMessage(nonce, "A")));
    assertThat(localstack.getLogs().contains(requestFilterFilterMessage(nonce, "A")));
    assertThat(localstack.getLogs().contains(responseFilterInitMessage(nonce, "A")));
    assertThat(localstack.getLogs().contains(responseFilterFilterMessage(nonce, "A")));
    assertThat(localstack.getLogs()).contains(outputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(exceptionMapperInitMessage(nonce, "A"));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenAllEnabled_thenAllServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = true;
    final Boolean autoloadOutputInterceptors = true;
    final Boolean autoloadRequestFilters = true;
    final Boolean autoloadResponseFilters = true;
    final Boolean autoloadExceptionMappers = true;

    final JavaFileObject handler = prepareSourceFile(
        greetingProcessorSource(autoloadAll, autoloadInputInterceptors, autoloadOutputInterceptors,
            autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject inputInterceptor = prepareSourceFile(inputInterceptorSource(nonce, "A"));

    final JavaFileObject outputInterceptor = prepareSourceFile(outputInterceptorSource(nonce, "A"));

    final JavaFileObject requestFilter =
        prepareSourceFile(requestFilterSource(nonce, "A", GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject responseFilter = prepareSourceFile(responseFilterSource(nonce, "A",
        GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject exceptionMapper = prepareSourceFile(exceptionMapperSource(nonce, "A",
        "IllegalArgumentException", GREETING_PROCESSOR_RESPONSE_TYPE, "e.getMessage()"));

    final Compilation compilation = doCompile(handler, inputInterceptor, outputInterceptor,
        requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            inputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            outputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
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

    assertThat(localstack.getLogs()).contains(inputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs().contains(requestFilterInitMessage(nonce, "A")));
    assertThat(localstack.getLogs().contains(requestFilterFilterMessage(nonce, "A")));
    assertThat(localstack.getLogs().contains(responseFilterInitMessage(nonce, "A")));
    assertThat(localstack.getLogs().contains(responseFilterFilterMessage(nonce, "A")));
    assertThat(localstack.getLogs()).contains(outputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(exceptionMapperInitMessage(nonce, "A"));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenNotEnabled_thenNoServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadOutputInterceptors = null;
    final Boolean autoloadRequestFilters = null;
    final Boolean autoloadResponseFilters = null;
    final Boolean autoloadExceptionMappers = null;

    final JavaFileObject handler = prepareSourceFile(
        greetingProcessorSource(autoloadAll, autoloadInputInterceptors, autoloadOutputInterceptors,
            autoloadRequestFilters, autoloadResponseFilters, autoloadExceptionMappers));

    final JavaFileObject inputInterceptor = prepareSourceFile(inputInterceptorSource(nonce, "A"));

    final JavaFileObject outputInterceptor = prepareSourceFile(outputInterceptorSource(nonce, "A"));

    final JavaFileObject requestFilter =
        prepareSourceFile(requestFilterSource(nonce, "A", GREETING_PROCESSOR_REQUEST_TYPE));

    final JavaFileObject responseFilter = prepareSourceFile(responseFilterSource(nonce, "A",
        GREETING_PROCESSOR_REQUEST_TYPE, GREETING_PROCESSOR_RESPONSE_TYPE));

    final JavaFileObject exceptionMapper = prepareSourceFile(exceptionMapperSource(nonce, "A",
        "IllegalArgumentException", GREETING_PROCESSOR_RESPONSE_TYPE, "e.getMessage()"));

    final Compilation compilation = doCompile(handler, inputInterceptor, outputInterceptor,
        requestFilter, responseFilter, exceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            inputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            outputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
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

    assertThat(localstack.getLogs()).doesNotContain(inputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(requestFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(requestFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(responseFilterInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(responseFilterFilterMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(outputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(exceptionMapperInitMessage(nonce, "A"));
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
