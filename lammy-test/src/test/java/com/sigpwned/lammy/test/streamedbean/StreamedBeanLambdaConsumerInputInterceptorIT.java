package com.sigpwned.lammy.test.streamedbean;

import static java.util.Collections.unmodifiableList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.google.testing.compile.Compilation;
import com.sigpwned.lammy.test.InputInterceptorTestBase;
import com.sigpwned.lammy.test.StreamFunctionTesting;

@Testcontainers
public class StreamedBeanLambdaConsumerInputInterceptorIT extends InputInterceptorTestBase
    implements StreamFunctionTesting {
  static {
    // Enable this when needed for debugging
    // localstack.followOutput(new Slf4jLogConsumer(LOGGER));
  }

  public static final String GREETING_PROCESSOR_REQUEST_TYPE = "Map<String, Object>";

  @Override
  public String greetingProcessorRequest(String name) {
    return "{\"name\":\"" + name + "\"}";
  }

  @Override
  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadInputInterceptors,
      Boolean autoloadOutputInterceptors, Boolean autoloadExceptionMappers) {
    if (autoloadOutputInterceptors != null)
      throw new IllegalArgumentException("autoloadOutputInterceptors is not supported");
    if (autoloadExceptionMappers != null)
      throw new IllegalArgumentException("autoloadExceptionMappers is not supported");

    // @formatter:off
    return ""
      + "package " + PACKAGE_NAME + ";\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.sigpwned.lammy.core.base.streamedbean.StreamedBeanLambdaConsumerBase;\n"
      + "import com.sigpwned.lammy.core.base.streamedbean.StreamedBeanLambdaConsumerConfiguration;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class LambdaFunction extends StreamedBeanLambdaConsumerBase<" + GREETING_PROCESSOR_REQUEST_TYPE + "> {\n"
      + "  public LambdaFunction() {\n"
      + "    super(new StreamedBeanLambdaConsumerConfiguration()\n"
      + "      .withAutoloadInputInterceptors(" + autoloadInputInterceptors + "));\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public void consumeStreamedBeanRequest(" + GREETING_PROCESSOR_REQUEST_TYPE + " input, Context context) {\n"
      + "    String name = input.get(\"name\") != null ? input.get(\"name\").toString() : \"world\";\n"
      + "    System.out.println(\"Hello, \" + name + \"!\");\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  protected Boolean getAutoloadAll() {\n"
      + "    return " + autoloadAll + ";\n"
      + "  }\n"
      + "}\n";
    // @formatter:on
  }

  /**
   * We don't seem to have access to the runtime client when running in LocalStack, so we can't use
   * the default serializer. Use Just JSON for testing.
   */
  @Override
  protected List<File> getRunClasspath(Compilation compilation) throws IOException {
    final List<File> result = new ArrayList<>(super.getRunClasspath(compilation));
    result.add(findJarInBuild("lammy-just-json-serialization"));
    result.add(findJarInLocalMavenRepository("com.sigpwned", "just-json", JUST_JSON_VERSION));
    return unmodifiableList(result);
  }

  @Override
  protected String getExpectedResponseForName(String name) {
    return "";
  }
}
