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
import org.junit.jupiter.api.Test;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.sigpwned.just.json.JustJson;

public abstract class ExceptionWriterTestBase extends LammyTestBase
    implements StreamFunctionTesting {

  // THROWING PROCESSOR ////////////////////////////////////////////////////////////////////////////

  public String throwingProcessorRequest(String name) {
    return name;
  }

  public String throwingProcessorResponseValue(String exceptionWriterId, String name) {
    return "Exception Mapper " + exceptionWriterId + ": Invalid name: " + name;
  }

  public String throwingProcessorResponseExpr(String exceptionWriterId) {
    return "\"Exception Mapper " + exceptionWriterId + ": Invalid name: \" + e.getMessage()";
  }

  public abstract String throwingProcessorSource(Boolean autoloadAll,
      Boolean autoloadInputInterceptors, Boolean autoloadOutputInterceptors,
      Boolean autoloadExceptionMappers);

  // EXCEPTION MAPPER TESTS ////////////////////////////////////////////////////////////////////////

  protected String buildAndInvokeThrowingProcessorWithTwoExceptionMappers(String nonce,
      Boolean autoloadAll, Boolean autoloadExceptionMappers, String firstExceptionMapperId,
      String firstExceptionType, String secondExceptionMapperId, String secondExceptionType,
      String name) throws IOException {
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadOutputInterceptors = null;

    final JavaFileObject handler = prepareSourceFile(throwingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionMappers));

    final JavaFileObject firstExceptionMapper =
        prepareSourceFile(exceptionWriterSource(nonce, firstExceptionMapperId, firstExceptionType,
            throwingProcessorResponseExpr(firstExceptionMapperId)));

    final JavaFileObject secondExceptionMapper =
        prepareSourceFile(exceptionWriterSource(nonce, secondExceptionMapperId, secondExceptionType,
            throwingProcessorResponseExpr(secondExceptionMapperId)));

    final Compilation compilation = doCompile(handler, firstExceptionMapper, secondExceptionMapper);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call first then second.
    final ExtraJarEntry exceptionWriterServices =
        new ExtraJarEntry(new JarEntry(EXCEPTION_WRITER_SERVICE_LOADER_JAR_ENTRY_NAME),
            String
                .join("\n", exceptionWriterQualifiedClassName(firstExceptionMapperId),
                    exceptionWriterQualifiedClassName(secondExceptionMapperId))
                .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, exceptionWriterServices);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);
      output = doInvokeLambdaFunction(functionName, throwingProcessorRequest(name));
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isEqualTo(-1);

    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterAFilterMessageIndex);
    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterBInitMessageIndex);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isEqualTo(-1);

    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterAFilterMessageIndex);
    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterBInitMessageIndex);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isNotEqualTo(-1);

    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterBInitMessageIndex);
    assertThat(exceptionWriterBInitMessageIndex).isLessThan(exceptionWriterBFilterMessageIndex);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isNotEqualTo(-1);

    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterBInitMessageIndex);
    assertThat(exceptionWriterBInitMessageIndex).isLessThan(exceptionWriterBFilterMessageIndex);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isEqualTo(-1);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isEqualTo(-1);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isEqualTo(-1);
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

    final int exceptionWriterAInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "A"));
    assertThat(exceptionWriterAInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterAFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "A"));
    assertThat(exceptionWriterAFilterMessageIndex).isEqualTo(-1);

    final int exceptionWriterBInitMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterInitMessage(nonce, "B"));
    assertThat(exceptionWriterBInitMessageIndex).isNotEqualTo(-1);

    final int exceptionWriterBFilterMessageIndex =
        localstack.getLogs().indexOf(exceptionWriterFilterMessage(nonce, "B"));
    assertThat(exceptionWriterBFilterMessageIndex).isEqualTo(-1);

    assertThat(exceptionWriterAInitMessageIndex).isLessThan(exceptionWriterBInitMessageIndex);
  }
}
