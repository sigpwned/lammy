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

public abstract class RequestFilterTestBase extends LammyTestBase implements BeanFunctionTesting {

  // CODE GENERATION ///////////////////////////////////////////////////////////////////////////////

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

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

    assertThat(output).isEqualTo(getExpectedResponseForName(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(requestFilterFilterMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }

  protected String getExpectedResponseForName(String name) {
    return greetingProcessorResponse(name);
  }
}
