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

public abstract class InputInterceptorTestBase extends LammyTestBase
    implements StreamFunctionTesting {

  // CODE GENERATION ///////////////////////////////////////////////////////////////////////////////

  public String greetingProcessorRequest(String name) {
    return name;
  }

  public String greetingProcessorResponse(String name) {
    return "Hello, " + name + "!";
  }

  public abstract String greetingProcessorSource(Boolean autoloadAll,
      Boolean autoloadInputInterceptors, Boolean autoloadResponseFilters,
      Boolean autoloadExceptionMappers);

  // INPUT INTERCEPTOR TESTS ///////////////////////////////////////////////////////////////////////

  protected String buildAndInvokeGreetingProcessorWithTwoInputInterceptors(String nonce,
      Boolean autoloadAll, Boolean autoloadInputInterceptors, String firstInputInterceptorId,
      String secondInputInterceptorId, String name) throws IOException {
    final Boolean autoloadOutputInterceptors = null;
    final Boolean autoloadExceptionWriters = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionWriters));

    final JavaFileObject requestFilterA =
        prepareSourceFile(inputInterceptorSource(nonce, firstInputInterceptorId));

    final JavaFileObject requestFilterB =
        prepareSourceFile(inputInterceptorSource(nonce, secondInputInterceptorId));

    final Compilation compilation = doCompile(handler, requestFilterA, requestFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call first then second.
    final ExtraJarEntry inputInterceptorServices =
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            String
                .join("\n", inputInterceptorQualifiedClassName(firstInputInterceptorId),
                    inputInterceptorQualifiedClassName(secondInputInterceptorId))
                .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, inputInterceptorServices);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);
      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    return output;
  }

  @Test
  public void givenInputInterceptorServicesAB_whenAutoloadExplicitlyEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    assertThat(requestFilterAIndex).isLessThan(requestFilterBIndex);
  }

  @Test
  public void givenInputInterceptorServicesAB_whenAutoloadBlanketEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadInputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    assertThat(requestFilterAIndex).isLessThan(requestFilterBIndex);
  }

  @Test
  public void givenInputInterceptorServicesBA_whenAutoloadExplicitlyEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    assertThat(requestFilterBIndex).isLessThan(requestFilterAIndex);
  }

  @Test
  public void givenInputInterceptorServicesBA_whenAutoloadBlanketEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadInputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isNotEqualTo(-1);

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isNotEqualTo(-1);

    assertThat(requestFilterBIndex).isLessThan(requestFilterAIndex);
  }

  @Test
  public void givenInputInterceptorServices_whenAutoloadNotEnabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenInputInterceptorServices_whenAutoloadBlanketEnabledExplicitlyDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadInputInterceptors = false;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenInputInterceptorServices_whenAutoloadBlanketDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = false;
    final Boolean autoloadInputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoInputInterceptors(nonce,
        autoloadAll, autoloadInputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int requestFilterAIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(requestFilterAIndex).isEqualTo(-1);

    final int requestFilterBIndex =
        localstack.getLogs().indexOf(inputInterceptorInterceptMessage(nonce, "B"));
    assertThat(requestFilterBIndex).isEqualTo(-1);
  }
}
