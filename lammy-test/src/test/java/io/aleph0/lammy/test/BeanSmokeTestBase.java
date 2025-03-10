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
package io.aleph0.lammy.test;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;

public abstract class BeanSmokeTestBase extends LammyTestBase implements BeanFunctionTesting {

  // GREETING PROCESSOR ////////////////////////////////////////////////////////////////////////////

  public String greetingProcessorRequest(String name) {
    return "{\"name\": \"" + name + "\"}";
  }

  public String greetingProcessorResponse(String name) {
    return "\"Hello, " + name + "!\"";
  }

  public static final String GREETING_PROCESSOR_REQUEST_TYPE = "Map<String, Object>";

  public static final String GREETING_PROCESSOR_RESPONSE_TYPE = "String";

  public abstract String greetingProcessorSource(Boolean autoloadAll,
      Boolean autoloadRequestFilters, Boolean autoloadResponseFilters,
      Boolean autoloadExceptionMappers);

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
}
