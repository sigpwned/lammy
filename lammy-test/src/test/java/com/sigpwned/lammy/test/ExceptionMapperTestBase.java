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

public abstract class ExceptionMapperTestBase extends LammyTestBase implements BeanFunctionTesting {

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

  public abstract String throwingProcessorSource(Boolean autoloadAll,
      Boolean autoloadRequestFilters, Boolean autoloadResponseFilters,
      Boolean autoloadExceptionMappers);

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
        firstExceptionMapperId, firstExceptionType, THROWING_PROCESSOR_RESPONSE_TYPE,
        throwingProcessorResponseExpr(firstExceptionMapperId)));

    final JavaFileObject secondExceptionMapper = prepareSourceFile(exceptionMapperSource(nonce,
        secondExceptionMapperId, secondExceptionType, THROWING_PROCESSOR_RESPONSE_TYPE,
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
