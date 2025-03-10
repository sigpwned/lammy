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

public abstract class StreamSmokeTestBase extends LammyTestBase implements StreamFunctionTesting {

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

  // SMOKE TESTS ///////////////////////////////////////////////////////////////////////////////////

  @Test
  public void givenExample_whenBuildAndInvoke_thenGetExpectedResult() throws IOException {
    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadOutputInterceptors = null;
    final Boolean autoloadExceptionWriters = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionWriters));

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
    final Boolean autoloadExceptionWriters = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionWriters));

    final JavaFileObject inputInterceptor = prepareSourceFile(inputInterceptorSource(nonce, "A"));

    final JavaFileObject outputInterceptor = prepareSourceFile(outputInterceptorSource(nonce, "A"));

    final JavaFileObject exceptionWriter = prepareSourceFile(
        exceptionWriterSource(nonce, "A", "IllegalArgumentException", "e.getMessage()"));

    final Compilation compilation =
        doCompile(handler, inputInterceptor, outputInterceptor, exceptionWriter);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            inputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            outputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(EXCEPTION_WRITER_SERVICE_LOADER_JAR_ENTRY_NAME),
            exceptionWriterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)));
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    assertThat(localstack.getLogs()).contains(inputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(outputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(exceptionWriterInitMessage(nonce, "A"));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenAllEnabled_thenAllServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = true;
    final Boolean autoloadOutputInterceptors = true;
    final Boolean autoloadExceptionWriters = true;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionWriters));

    final JavaFileObject inputInterceptor = prepareSourceFile(inputInterceptorSource(nonce, "A"));

    final JavaFileObject outputInterceptor = prepareSourceFile(outputInterceptorSource(nonce, "A"));

    final JavaFileObject exceptionWriter = prepareSourceFile(
        exceptionWriterSource(nonce, "A", "IllegalArgumentException", "e.getMessage()"));

    final Compilation compilation =
        doCompile(handler, inputInterceptor, outputInterceptor, exceptionWriter);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            inputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            outputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(EXCEPTION_WRITER_SERVICE_LOADER_JAR_ENTRY_NAME),
            exceptionWriterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)));
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    assertThat(localstack.getLogs()).contains(inputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(outputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).contains(exceptionWriterInitMessage(nonce, "A"));
  }

  @Test
  public void givenExampleWithServicesAvailable_whenNotEnabled_thenNoServicesLoaded()
      throws IOException {
    final String nonce = nonce();

    final Boolean autoloadAll = null;
    final Boolean autoloadInputInterceptors = null;
    final Boolean autoloadOutputInterceptors = null;
    final Boolean autoloadExceptionWriters = null;

    final JavaFileObject handler = prepareSourceFile(greetingProcessorSource(autoloadAll,
        autoloadInputInterceptors, autoloadOutputInterceptors, autoloadExceptionWriters));

    final JavaFileObject inputInterceptor = prepareSourceFile(inputInterceptorSource(nonce, "A"));

    final JavaFileObject outputInterceptor = prepareSourceFile(outputInterceptorSource(nonce, "A"));

    final JavaFileObject exceptionWriter = prepareSourceFile(
        exceptionWriterSource(nonce, "A", "IllegalArgumentException", "e.getMessage()"));

    final Compilation compilation =
        doCompile(handler, inputInterceptor, outputInterceptor, exceptionWriter);

    CompilationSubject.assertThat(compilation).succeeded();

    final String name = "Gandalf";

    final String output;
    final File deploymentPackage = createDeploymentPackage(compilation,
        new ExtraJarEntry(new JarEntry(INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            inputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME),
            outputInterceptorQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)),
        new ExtraJarEntry(new JarEntry(EXCEPTION_WRITER_SERVICE_LOADER_JAR_ENTRY_NAME),
            exceptionWriterQualifiedClassName("A").getBytes(StandardCharsets.UTF_8)));
    try {
      final String functionName = doDeployLambdaFunction(deploymentPackage);

      output = doInvokeLambdaFunction(functionName, greetingProcessorRequest(name));
    } finally {
      deploymentPackage.delete();
    }

    assertThat(output).isEqualTo(greetingProcessorResponse(name));

    assertThat(localstack.getLogs()).doesNotContain(inputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(inputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(outputInterceptorInitMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(outputInterceptorInterceptMessage(nonce, "A"));
    assertThat(localstack.getLogs()).doesNotContain(exceptionWriterInitMessage(nonce, "A"));
  }
}
