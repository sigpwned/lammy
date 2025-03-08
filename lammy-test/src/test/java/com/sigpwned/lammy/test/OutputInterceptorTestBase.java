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

public abstract class OutputInterceptorTestBase extends LammyTestBase
    implements StreamFunctionTesting {

  // GREETING PROCESSOR ////////////////////////////////////////////////////////////////////////////

  public String greetingProcessorRequest(String name) {
    return name;
  }

  public String greetingProcessorResponse(String name) {
    return "Hello, " + name + "!";
  }

  public abstract String greetingProcessorSource(Boolean autoloadAll,
      Boolean autoloadInputInterceptors, Boolean autoloadOutputInterceptors,
      Boolean autoloadExceptionWriters);

  // RESPONSE FILTER TESTS /////////////////////////////////////////////////////////////////////////

  protected String buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(String nonce,
      Boolean autoloadAll, Boolean autoloadOutputInterceptors, String firstOutputInterceptorId,
      String secondOutputInterceptorId, String name) throws IOException {
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadExceptionWriters = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionWriters));

    final JavaFileObject responseFilterA =
        prepareSourceFile(outputInterceptorSource(nonce, firstOutputInterceptorId));

    final JavaFileObject responseFilterB =
        prepareSourceFile(outputInterceptorSource(nonce, secondOutputInterceptorId));

    final Compilation compilation = doCompile(handler, responseFilterA, responseFilterB);

    CompilationSubject.assertThat(compilation).succeeded();

    // Order matters here. Call first then second.
    final ExtraJarEntry outputInterceptorServices =
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            String
                .join("\n", outputInterceptorQualifiedClassName(firstOutputInterceptorId),
                    outputInterceptorQualifiedClassName(secondOutputInterceptorId))
                .getBytes(StandardCharsets.UTF_8));

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation, outputInterceptorServices);
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);
      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    return output;
  }

  @Test
  public void givenOutputInterceptorServicesAB_whenAutoloadExplicitlyEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadOutputInterceptors = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int outputInterceptorAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(outputInterceptorAIndex).isNotEqualTo(-1);

    final int outputInterceptorBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(outputInterceptorBIndex).isNotEqualTo(-1);

    assertThat(outputInterceptorAIndex).isLessThan(outputInterceptorBIndex);
  }

  @Test
  public void givenOutputInterceptorServicesAB_whenAutoloadBlanketEnabled_thenCallAB()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadOutputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    assertThat(responseFilterAIndex).isLessThan(responseFilterBIndex);
  }

  @Test
  public void givenOutputInterceptorServicesBA_whenAutoloadExplicitlyEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadOutputInterceptors = true;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    assertThat(responseFilterBIndex).isLessThan(responseFilterAIndex);
  }

  @Test
  public void givenOutputInterceptorServicesBA_whenAutoloadBlanketEnabled_thenCallBA()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadOutputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "B", "A", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isNotEqualTo(-1);

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isNotEqualTo(-1);

    assertThat(responseFilterBIndex).isLessThan(responseFilterAIndex);
  }

  @Test
  public void givenOutputInterceptorServices_whenAutoloadNotEnabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadOutputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenOutputInterceptorServices_whenAutoloadBlanketEnabledExplicitlyDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = true;
    final Boolean autoloadOutputInterceptors = false;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isEqualTo(-1);
  }

  @Test
  public void givenOutputInterceptorServices_whenAutoloadBlanketDisabled_thenDontCall()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = false;
    final Boolean autoloadOutputInterceptors = null;

    final String name = "Test";

    final String output = buildAndInvokeGreetingProcessorWithTwoOutputInterceptors(nonce,
        autoloadAll, autoloadOutputInterceptors, "A", "B", name);

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    final int responseFilterAIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(responseFilterAIndex).isEqualTo(-1);

    final int responseFilterBIndex =
        localstack.getLogs().indexOf(outputInterceptorInterceptMessage(nonce, "B"));
    assertThat(responseFilterBIndex).isEqualTo(-1);
  }
}
