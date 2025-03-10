package com.sigpwned.lammy.example.lambdafunction.stream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigpwned.lammy.core.base.stream.StreamLambdaProcessorBase;
import com.sigpwned.lammy.core.model.stream.InputContext;
import com.sigpwned.lammy.core.model.stream.InputInterceptor;
import com.sigpwned.lammy.core.model.stream.OutputContext;
import com.sigpwned.lammy.core.model.stream.OutputInterceptor;

/**
 * Implements a simple Lambda function that reads various system files and prints their contents to
 * the console and echoes the input back to the output.
 */
public class ExampleStreamLambdaProcessor extends StreamLambdaProcessorBase {
  public ExampleStreamLambdaProcessor() {
    registerInputInterceptor(new InputInterceptor() {
      @Override
      public void interceptRequest(InputContext requestContext, Context lambdaContext) {
        System.out.println("Hello from InputInterceptor!");
      }
    });
    registerOutputInterceptor(new OutputInterceptor() {
      @Override
      public void interceptResponse(InputContext requestContext, OutputContext responseContext,
          Context lambdaContext) {
        System.out.println("Hello from OutputInterceptor!");
      }
    });
  }

  @Override
  public void handleStreamingRequest(InputStream inputStream, OutputStream outputStream,
      Context context) throws IOException {
    System.out.println("==== MEMORY INFO ====");
    printFileContents(System.out, new File("/proc/meminfo"));
    System.out.println();

    System.out.println("==== CPU INFO ====");
    printFileContents(System.out, new File("/proc/cpuinfo"));
    System.out.println();

    System.out.println("==== VERSION INFO ====");
    printFileContents(System.out, new File("/proc/version"));
    System.out.println();

    System.out.println("==== DISK INFO ====");
    printFileContents(System.out, new File("/proc/diskstats"));
    System.out.println();

    System.out.println("==== COMMAND LINE ====");
    printFileContents(System.out, new File("/proc/self/cmdline"), s -> s.replace('\0', ' '));
    System.out.println();

    System.out.println("==== ENVIRONMENT ====");
    printFileContents(System.out, new File("/proc/self/environ"), s -> s.replace('\0', '\n'));
    System.out.println();

    System.out.println("==== CONTEXT ====");
    System.out.println("AwsRequestId: " + context.getAwsRequestId());
    System.out.println("FunctionName: " + context.getFunctionName());
    System.out.println("FunctionVersion: " + context.getFunctionVersion());
    System.out.println("LogGroupName: " + context.getLogGroupName());
    System.out.println("LogStreamName: " + context.getLogStreamName());
    System.out.println("MemoryLimitInMB: " + context.getMemoryLimitInMB());
    System.out.println("RemainingTimeInMillis: " + context.getRemainingTimeInMillis());
    System.out.println("InvokedFunctionArn: " + context.getInvokedFunctionArn());
    System.out.println("Identity: " + context.getIdentity());
    System.out.println("ClientContext.Custom: " + Optional.ofNullable(context.getClientContext())
        .map(ClientContext::getCustom).orElse(null));
    System.out.println("ClientContext.Environment: " + Optional
        .ofNullable(context.getClientContext()).map(ClientContext::getEnvironment).orElse(null));
    System.out.println();

    final byte[] inputBytes = toByteArray(inputStream);

    outputStream.write(inputBytes);
  }

  public static void printFileContents(PrintStream out, File f) {
    printFileContents(out, f, Function.identity());
  }

  public static void printFileContents(PrintStream out, File f,
      Function<String, String> transformation) {
    out.println(transformation.apply(readFileContents(f)));
    out.println();
  }

  public static String readFileContents(File f) {
    StringBuilder result = new StringBuilder();

    try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
      char[] buf = new char[16384];
      for (int nread = r.read(buf, 0, buf.length); nread != -1; nread =
          r.read(buf, 0, buf.length)) {
        result.append(buf, 0, nread);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read file " + f.getPath(), e);
    }

    return result.toString();
  }

  public static byte[] toByteArray(InputStream in) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      final byte[] buf = new byte[8192];
      for (int nread = in.read(buf); nread != -1; nread = in.read(buf))
        out.write(buf, 0, nread);
      return out.toByteArray();
    }
  }
}
