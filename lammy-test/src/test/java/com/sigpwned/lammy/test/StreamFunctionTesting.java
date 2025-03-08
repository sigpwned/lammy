package com.sigpwned.lammy.test;

import com.sigpwned.lammy.core.model.stream.ExceptionWriter;
import com.sigpwned.lammy.core.model.stream.InputContext;
import com.sigpwned.lammy.core.model.stream.InputInterceptor;
import com.sigpwned.lammy.core.model.stream.OutputContext;
import com.sigpwned.lammy.core.model.stream.OutputInterceptor;
import com.sigpwned.lammy.test.util.CodeGenerating;

public interface StreamFunctionTesting extends CodeGenerating {
  // REQUEST FILTER ////////////////////////////////////////////////////////////////////////////////

  public static final String INPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME =
      "META-INF/services/com.sigpwned.lammy.core.model.stream.InputInterceptor";

  /**
   * Equivalent to {@code inputInterceptorSource(nonce, id, "")}.
   *
   * @see #inputInterceptorSource(String, String, String)
   */
  public default String inputInterceptorSource(String nonce, String id) {
    return inputInterceptorSource(nonce, id, "");
  }

  /**
   * Generates source code for a {@link InputInterceptor} based on the parameters. The generated
   * class has the following properties:
   *
   * <ul>
   * <li>It is in package {@code com.example}</li>
   * <li>Its is {@link #inputInterceptorSimpleClassName(String) simple name}
   * {@code ExampleInputInterceptor} followed by the given {@code id} (e.g., for id {@code "A"}, the
   * name would be {@code ExampleInputInterceptorA})</li>
   * <li>It implements {@link InputInterceptor}</li>
   * <li>Its default constructor prints {@link #inputInterceptorInitMessage(String, String) a
   * message} to {@link System#out}</li>
   * <li>Its
   * {@link InputInterceptor#interceptRequest(InputContext, com.amazonaws.services.lambda.runtime.Context)}
   * implementation prints {@link #inputInterceptorInterceptMessage(String, String) a message} to
   * {@link System#out}</li>
   * <li>The body of the {@code interceptRequest} method is the given {@code body}</li>
   * </ul>
   *
   * @param nonce a unique identifier for the generated source code
   * @param id the identifier for the generated class (e.g., {@code "A"})
   * @param body the body of the {@code interceptRequest} method
   * @return the generated source code
   */
  public default String inputInterceptorSource(String nonce, String id, String body) {
    // @formatter:off
    return ""
      + "package " + PACKAGE_NAME + ";\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.sigpwned.lammy.core.model.stream.InputInterceptor;\n"
      + "import com.sigpwned.lammy.core.model.stream.InputContext;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class " + inputInterceptorSimpleClassName(id) + " implements InputInterceptor {\n"
      + "  public " + inputInterceptorSimpleClassName(id) + "() {\n"
      + "    System.out.println(\"" + inputInterceptorInitMessage(nonce, id) + "\");\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public void interceptRequest(InputContext inputContext, Context lambdaContext) {\n"
      + "    System.out.println(\"" + inputInterceptorInterceptMessage(nonce, id) + "\");\n"
      + "    " + body
      + "  }\n"
      + "}\n";
    // @formatter:on
  }

  public default String inputInterceptorQualifiedClassName(String id) {
    return PACKAGE_NAME + "." + inputInterceptorSimpleClassName(id);
  }

  public default String inputInterceptorSimpleClassName(String id) {
    return "ExampleInputInterceptor" + id;
  }

  public default String inputInterceptorInitMessage(String nonce, String id) {
    return nonce + ": " + inputInterceptorSimpleClassName(id) + ".<init>";
  }

  public default String inputInterceptorInterceptMessage(String nonce, String id) {
    return nonce + ": " + inputInterceptorSimpleClassName(id) + ".interceptRequest";
  }

  // RESPONSE FILTER ///////////////////////////////////////////////////////////////////////////////

  public static final String OUTPUT_INTERCEPTOR_SERVICE_LOADER_JAR_ENTRY_NAME =
      "META-INF/services/com.sigpwned.lammy.core.model.stream.OutputInterceptor";

  /**
   * Equivalent to {@code outputInterceptorSource(nonce, id, "")}.
   */
  public default String outputInterceptorSource(String nonce, String id) {
    return outputInterceptorSource(nonce, id, "");
  }

  /**
   * Generates source code for a {@link OutputInterceptor} based on the parameters. The generated
   * class has the following properties:
   *
   * <ul>
   * <li>It is in package {@code com.example}</li>
   * <li>Its is {@link #outputInterceptorSimpleClassName(String) simple name}
   * {@code ExampleOutputInterceptor} followed by the given {@code id} (e.g., for id {@code "A"},
   * the name would be {@code ExampleOutputInterceptorA})</li>
   * <li>It implements {@link OutputInterceptor}</li>
   * <li>Its default constructor prints {@link #outputInterceptorInitMessage(String, String) a
   * message} to {@link System#out}</li>
   * <li>Its
   * {@link OutputInterceptor#interceptResponse(OutputContext, com.amazonaws.services.lambda.runtime.Context)}
   * implementation prints {@link #outputInterceptorInterceptMessage(String, String) a message} to
   * {@link System#out}</li>
   * <li>The body of the {@code interceptResponse} method is the given {@code body}</li>
   * </ul>
   *
   * @param nonce a unique identifier for the generated source code
   * @param id the identifier for the generated class (e.g., {@code "A"})
   * @param body the body of the {@code interceptResponse} method
   * @return the generated source code
   */
  public default String outputInterceptorSource(String nonce, String id, String body) {
    // @formatter:off
    return ""
        + "package " + PACKAGE_NAME + ";\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import com.sigpwned.lammy.core.model.stream.OutputInterceptor;\n"
        + "import com.sigpwned.lammy.core.model.stream.OutputContext;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public class " + outputInterceptorSimpleClassName(id) + " implements OutputInterceptor {\n"
        + "  public " + outputInterceptorSimpleClassName(id) + "() {\n"
        + "    System.out.println(\"" + outputInterceptorInitMessage(nonce, id) + "\");\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void interceptResponse(OutputContext outputContext, Context lambdaContext) {\n"
        + "    System.out.println(\"" + outputInterceptorInterceptMessage(nonce, id) + "\");\n"
        + "    " + body
        + "  }\n"
        + "}\n";
    // @formatter:on
  }

  public default String outputInterceptorQualifiedClassName(String id) {
    return PACKAGE_NAME + "." + outputInterceptorSimpleClassName(id);
  }

  public default String outputInterceptorSimpleClassName(String id) {
    return "ExampleResponseFilter" + id;
  }

  public default String outputInterceptorInitMessage(String nonce, String id) {
    return nonce + ": " + outputInterceptorSimpleClassName(id) + ".<init>";
  }

  public default String outputInterceptorInterceptMessage(String nonce, String id) {
    return nonce + ": " + outputInterceptorSimpleClassName(id) + ".interceptResponse";
  }

  // EXCEPTION MAPPER //////////////////////////////////////////////////////////////////////////////

  public static final String EXCEPTION_WRITER_SERVICE_LOADER_JAR_ENTRY_NAME =
      "META-INF/services/com.sigpwned.lammy.core.model.stream.ExceptionWriter";

  /**
   * Generates source code for an {@link ExceptionWriter} based on the parameters. The generated
   * class has the following properties:
   *
   * <ul>
   * <li>It is in package {@code com.example}</li>
   * <li>Its {@link #exceptionWriterSimpleClassName(String) simple name} is {@code
   * ExampleExceptionWriter} followed by the given {@code id} (e.g., for id {@code "A"}, the name
   * would be {@code ExampleExceptionWriterA})</li>
   * <li>It implements {@link ExceptionWriter}</li>
   * <li>Its default constructor prints {@link #exceptionWriterInitMessage(String, String) a
   * message} to {@link System#out}</li>
   * <li>Its
   * {@link ExceptionWriter#writeExceptionTo(Object, java.io.OutputStream, software.amazon.awssdk.services.lambda.runtime.Context)
   * implementation} prints {@link #exceptionWriterFilterMessage(String, String) a message} to
   * {@link System#out}</li>
   * <li>The body of the {@code writeExceptionTo} method is the given {@code expr}</li>
   * </ul>
   *
   * @param nonce a unique identifier for the generated source code
   * @param id the identifier for the generated class (e.g., {@code "A"})
   * @param exceptionType the type of the exception to write
   * @param expr the body of the {@code writeExceptionTo} method
   */
  public default String exceptionWriterSource(String nonce, String id, String exceptionType,
      String expr) {
    // @formatter:off
    return ""
        + "package " + PACKAGE_NAME + ";\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.stream.ExceptionWriter;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.io.OutputStream;\n"
        + "import java.nio.charset.StandardCharsets;\n"
        + "\n"
        + "public class " + exceptionWriterSimpleClassName(id) + " implements ExceptionWriter<" + exceptionType + "> {\n"
        + "  public " + exceptionWriterSimpleClassName(id) + "() {\n"
        + "    System.out.println(\"" + exceptionWriterInitMessage(nonce, id) + "\");\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeExceptionTo(" + exceptionType + " e, OutputStream out, Context context) {\n"
        + "    System.out.println(\"" + exceptionWriterFilterMessage(nonce, id) + "\");\n"
        + "    out.write((" + expr + ").getBytes(StandardCharsets.UTF_8));\n"
        + "  }\n"
        + "}\n";
    // @formatter:on
  }

  public default String exceptionWriterQualifiedClassName(String id) {
    return PACKAGE_NAME + "." + exceptionWriterSimpleClassName(id);
  }

  public default String exceptionWriterSimpleClassName(String id) {
    return "ExampleExceptionMapper" + id;
  }

  public default String exceptionWriterInitMessage(String nonce, String id) {
    return nonce + ": " + exceptionWriterSimpleClassName(id) + ".<init>";
  }

  public default String exceptionWriterFilterMessage(String nonce, String id) {
    return nonce + ": " + exceptionWriterSimpleClassName(id) + ".writeExceptionTo";
  }
}
