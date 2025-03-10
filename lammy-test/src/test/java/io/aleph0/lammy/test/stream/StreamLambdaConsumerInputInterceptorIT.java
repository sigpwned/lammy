package io.aleph0.lammy.test.stream;

import org.testcontainers.junit.jupiter.Testcontainers;
import io.aleph0.lammy.test.InputInterceptorTestBase;
import io.aleph0.lammy.test.StreamFunctionTesting;

@Testcontainers
public class StreamLambdaConsumerInputInterceptorIT extends InputInterceptorTestBase
    implements StreamFunctionTesting {

  @Override
  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadInputInterceptors,
      Boolean autoloadOutputInterceptors, Boolean autoloadExceptionWriters) {
    // @formatter:off
    return ""
        + "package " + PACKAGE_NAME + ";\n"
        + "\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import io.aleph0.lammy.core.base.stream.StreamLambdaConsumerBase;\n"
        + "import io.aleph0.lammy.core.base.stream.StreamLambdaConsumerConfiguration;\n"
        + "import java.io.IOException;\n"
        + "import java.io.UncheckedIOException;\n"
        + "import java.io.InputStream;\n"
        + "import java.io.OutputStream;\n"
        + "import java.io.ByteArrayOutputStream;\n"
        + "import java.nio.charset.StandardCharsets;\n"
        + "\n"
        + "public class LambdaFunction extends StreamLambdaConsumerBase {\n"
        + "  public LambdaFunction() {\n"
        + "    super(new StreamLambdaConsumerConfiguration()\n"
        + "      .withAutoloadInputInterceptors(" + autoloadInputInterceptors + "));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void consumeStreamingRequest(InputStream input, Context context) {\n"
        + "    try {\n"
        + "      final byte[] requestBytes = toByteArray(input);\n"
        + "      final String requestText = new String(requestBytes, StandardCharsets.UTF_8);\n"
        + "      final String responseText = \"Hello, \" + requestText + \"!\";\n"
        + "      System.out.println(responseText);\n"
        + "    } catch (IOException e) {\n"
        + "      throw new UncheckedIOException(e);\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  public byte[] toByteArray(InputStream input) throws IOException {\n"
        + "    try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {\n"
        + "      final byte[] buffer = new byte[8192];\n"
        + "      for(int nread=input.read(buffer); nread != -1; nread=input.read(buffer)) {\n"
        + "        output.write(buffer, 0, nread);\n"
        + "      }\n"
        + "      return output.toByteArray();\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  protected Boolean getAutoloadAll() {\n"
        + "    return " + autoloadAll + ";\n"
        + "  }\n"
        + "}\n";
    // @formatter:on
  }

  @Override
  protected String getExpectedResponseForName(String name) {
    return "";
  }
}
