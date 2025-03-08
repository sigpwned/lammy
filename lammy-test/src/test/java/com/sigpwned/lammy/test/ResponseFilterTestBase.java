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
import java.util.jar.JarEntry;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;

public abstract class ResponseFilterTestBase extends LammyTestBase implements BeanFunctionTesting {

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
}
