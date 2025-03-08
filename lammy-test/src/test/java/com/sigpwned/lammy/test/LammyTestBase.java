package com.sigpwned.lammy.test;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import javax.tools.JavaFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.com.google.common.io.ByteStreams;
import org.testcontainers.utility.DockerImageName;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.sigpwned.lammy.core.model.OptionalSystemProperty;
import com.sigpwned.lammy.test.util.Matchers;
import com.sigpwned.lammy.test.util.Maven;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationResponse;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.State;

/**
 * Base class for Rapier tests.
 *
 * <p>
 * Any {@code public} methods are for use in test classes. Any {@code protected} methods are for
 * override in subclasses. Any {@code private} methods are for internal use only, and are not
 * visible to test classes anyway.
 */
public abstract class LammyTestBase {
  public static final Logger LOGGER = LoggerFactory.getLogger(LammyTestBase.class);

  @Container
  public static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
          .withEnv("DEBUG", "1") // Ensure our lambda functino logs are visible in container logs
          .withServices(LocalStackContainer.Service.LAMBDA);

  public Compilation doCompile(JavaFileObject... source) throws IOException {
    return Compiler.javac().withClasspath(getCompileClasspath()).compile(source);
  }

  /**
   * Assumes the lambda function is the class com.example.LambdaFunction. The handler should always
   * be named handleRequest, since we're using lammy base classes.
   */
  public String doDeployLambdaFunction(File deploymentPackageJar) throws IOException {
    // Read the generated JAR file into a ByteBuffer and then wrap it as SdkBytes.
    final ByteBuffer jarBytes = ByteBuffer.wrap(Files.readAllBytes(deploymentPackageJar.toPath()));
    final SdkBytes sdkJarBytes = SdkBytes.fromByteBuffer(jarBytes);

    final long now = System.currentTimeMillis();

    // TODO Let's not hard-code things, shall we?
    // Create the Lambda function. Adjust runtime as needed.
    final CreateFunctionRequest createFunctionRequest =
        CreateFunctionRequest.builder().functionName("LambdaFunction" + now).runtime("java8")
            .handler("com.example.LambdaFunction::handleRequest")
            // Dummy role; LocalStack does not enforce IAM.
            .role("arn:aws:iam::000000000000:role/lambda-role")
            .code(FunctionCode.builder().zipFile(sdkJarBytes).build()).build();

    final CreateFunctionResponse createFunctionResponse =
        getLambdaClient().createFunction(createFunctionRequest);

    final String functionArn = createFunctionResponse.functionArn();

    GetFunctionConfigurationResponse gfcr = getLambdaClient().getFunctionConfiguration(
        GetFunctionConfigurationRequest.builder().functionName(functionArn).build());
    while (gfcr.state() == State.PENDING) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        throw new InterruptedIOException();
      }
      gfcr = getLambdaClient().getFunctionConfiguration(
          GetFunctionConfigurationRequest.builder().functionName(functionArn).build());
    }

    return createFunctionResponse.functionArn();
  }

  public String doInvokeLambdaFunction(String functionName, String input) throws IOException {
    final InvokeRequest invokeRequest = InvokeRequest.builder().functionName(functionName)
        .payload(SdkBytes.fromString(input, StandardCharsets.UTF_8)).build();

    final InvokeResponse invokeResponse = getLambdaClient().invoke(invokeRequest);

    return invokeResponse.payload().asUtf8String();
  }

  private LambdaClient cachedLambdaClient;

  protected LambdaClient getLambdaClient() {
    if (cachedLambdaClient == null) {
      AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");
      cachedLambdaClient = LambdaClient.builder()
          .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.LAMBDA))
          .region(Region.of(localstack.getRegion()))
          .credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
    }
    return cachedLambdaClient;
  }

  /**
   * Check if a JavaFileObject contains a main method. A main method is considered to be present if
   * the file contains the literal string {@code "public static void main(String[] args)"}.
   *
   * @param file the file to check
   * @return {@code true} if the file contains a main method, {@code false} otherwise
   * @throws UncheckedIOException if an IOException occurs while reading the file
   */
  private static boolean containsMainMethod(JavaFileObject file) {
    try {
      return file.getCharContent(true).toString()
          .contains("public static void main(String[] args)");
    } catch (IOException e) {
      // This really ought never to happen, since it's all in memory.
      throw new UncheckedIOException("Failed to read JavaFileObject contents", e);
    }
  }

  /**
   * Extracts the qualified Java class name from a JavaFileObject.
   *
   * @param file the JavaFileObject representing the Java source code
   * @return the qualified class name
   */
  private static String toQualifiedClassName(JavaFileObject file) {
    // Get the name of the JavaFileObject
    String fileName = file.getName();

    // Remove directories or prefixes before the package
    // Example: "/com/example/HelloWorld.java"
    // becomes "com/example/HelloWorld.java"
    if (fileName.startsWith("/")) {
      fileName = fileName.substring(1);
    }

    // Remove the ".java" extension
    if (fileName.endsWith(".java")) {
      fileName = fileName.substring(0, fileName.length() - ".java".length());
    }

    // Replace '/' with '.' to form package and class hierarchy
    fileName = fileName.replace("/", ".");

    return fileName;
  }

  private static final Pattern PACKAGE_DECLARATION_PATTERN =
      Pattern.compile("^package\\s+(\\S+)\\s*;", Pattern.MULTILINE);
  private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
      "^public\\s+(?:abstract\\s+)?(?:class|@?interface)\\s+([\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}.]+)\\s*(?:extends|implements|\\{)",
      Pattern.MULTILINE);

  public JavaFileObject prepareSourceFile(String sourceCode) {
    final String packageName = Matchers.results(PACKAGE_DECLARATION_PATTERN.matcher(sourceCode))
        .findFirst().map(m -> m.group(1)).orElse(null);

    final String simpleClassName = Matchers.results(CLASS_DECLARATION_PATTERN.matcher(sourceCode))
        .findFirst().map(m -> m.group(1)).orElse(null);
    if (simpleClassName == null)
      throw new IllegalArgumentException("Failed to detect class name");

    final String qualifiedClassName =
        packageName != null ? packageName + "." + simpleClassName : simpleClassName;

    return JavaFileObjects.forSourceString(qualifiedClassName, sourceCode);
  }

  /**
   * The root directory of the current Maven module
   */
  private static final File MAVEN_PROJECT_BASEDIR =
      Optional.ofNullable(System.getProperty("maven.project.basedir")).map(File::new).orElseThrow(
          () -> new IllegalStateException("maven.project.basedir system property not set"));

  public File resolveProjectFile(String path) throws FileNotFoundException {
    final File result = new File(MAVEN_PROJECT_BASEDIR, path);
    if (!result.exists())
      throw new FileNotFoundException(result.toString());
    return result;
  }

  public static class ExtraJarEntry {
    public final JarEntry entry;
    public final byte[] contents;

    public ExtraJarEntry(JarEntry entry, byte[] contents) {
      this.entry = requireNonNull(entry);
      this.contents = requireNonNull(contents);
    }
  }

  protected File createDeploymentPackage(Compilation compilation, ExtraJarEntry... extraEntries)
      throws IOException {
    final List<File> classpath = getRunClasspath(compilation);

    final File deploymentPackageJar = File.createTempFile("deployment.", ".jar");
    try (JarOutputStream out = new JarOutputStream(new FileOutputStream(deploymentPackageJar))) {
      // Add the compiled classes to the JAR
      for (File classpathEntry : classpath) {
        if (classpathEntry.isDirectory()) {
          addToJar(out, classpathEntry);
        } else if (classpathEntry.isFile() && classpathEntry.getName().endsWith(".jar")) {
          try (JarFile jar = new JarFile(classpathEntry)) {
            addToJar(out, jar);
          }
        } else {
          throw new IOException("unrecognized classpath entry " + classpathEntry);
        }
      }

      // Add any extra entries to the JAR
      for (ExtraJarEntry extraEntry : extraEntries) {
        final JarEntry entry = extraEntry.entry;
        final byte[] contents = extraEntry.contents;
        out.putNextEntry(entry);
        out.write(contents);
        out.closeEntry();
      }
    }

    /**
     * Validate the deployment package. Ensure that it only contains the files we expect. In
     * particular, the JAR should not contain any Amazon classes. All of that stuff is provided by
     * the Lambda environment, so we don't want to bring our own.
     *
     * @param deploymentPackage the deployment package to validate
     * @throws IOException if there is an error reading the JAR file
     */
    try (JarFile jar = new JarFile(deploymentPackageJar)) {
      // Here, we're looking for a few specific entries in the JAR file:
      // - META-INF/ -- various and sundry metadata, which is all fine
      // - com/sigpwned/ -- the lammy prefix, and various testing stuff, e.g., Just JSON
      // - com/example/ -- this exmaple lambda function
      // - org/crac/ -- the Coordinated Restore at Checkpoint library
      assertThat(jar.stream()).map(JarEntry::getName)
          .allMatch(name -> name.startsWith("com/sigpwned/") || name.startsWith("com/example/")
              || name.startsWith("org/crac/") || name.startsWith("META-INF/"));
    }


    return deploymentPackageJar;
  }

  protected List<File> getRunClasspath(Compilation compilation) throws IOException {
    List<File> result = new ArrayList<>();

    // We do not want our whole classpath, since some of that is provided by the platform runtime
    // and that's part of what we're testing here. We only want the compiled files.
    // result.addAll(getCompileClasspath());

    // We want the lammy-core module
    result.add(findJarInBuild("lammy-core"));

    // We want the crac module
    result.add(findJarInLocalMavenRepository("io.github.crac", "org-crac", CRAC_VERSION));


    // Extract the compiled files into a temporary directory.
    final Path tmpdir = Files.createTempDirectory("test");
    for (JavaFileObject file : compilation.generatedFiles()) {
      if (file.getKind() == JavaFileObject.Kind.CLASS) {
        final String originalClassFileName = file.getName();

        String sanitizedClassFileName = originalClassFileName;
        sanitizedClassFileName = sanitizedClassFileName.replace("/", File.separator);
        if (sanitizedClassFileName.startsWith(File.separator))
          sanitizedClassFileName = sanitizedClassFileName.substring(1);
        if (sanitizedClassFileName.startsWith("CLASS_OUTPUT" + File.separator))
          sanitizedClassFileName = sanitizedClassFileName.substring("CLASS_OUTPUT".length() + 1,
              sanitizedClassFileName.length());

        final Path tmpdirClassFile = tmpdir.resolve(sanitizedClassFileName);

        Files.createDirectories(tmpdirClassFile.getParent());

        try (InputStream in = file.openInputStream()) {
          Files.copy(in, tmpdirClassFile);
        }
      }
    }

    // We also want the compiled classes to actually run the application
    final File tmpdirAsFile = tmpdir.toFile();
    result.add(tmpdirAsFile);
    tmpdirAsFile.deleteOnExit();

    return unmodifiableList(result);
  }

  /**
   * The {@link ClassLoader} to use as the parent of the class loader used to run the application.
   */
  protected ClassLoader getRunParentClassLoader() {
    // By default, we want to use the bootstrap class loader. This is represented as null in Java 8.
    // In later versions of Java, it's the "platform class loader".
    // return ClassLoader.getPlatformClassLoader();
    return null;
  }

  protected static final String AWS_LAMBDA_JAVA_CORE_VERSION =
      OptionalSystemProperty.getProperty("maven.aws-lambda-java-core.version").orElseThrow();

  protected static final String CRAC_VERSION =
      OptionalSystemProperty.getProperty("maven.crac.version").orElseThrow();

  protected static final String JUST_JSON_VERSION =
      OptionalSystemProperty.getProperty("maven.just-json.version").orElseThrow();

  protected List<File> getCompileClasspath() throws FileNotFoundException {
    // final File daggerJar =
    // findJarInLocalMavenRepository("com.google.dagger", "dagger", DAGGER_VERSION);
    // final File javaxInjectJar =
    // findJarInLocalMavenRepository("javax.inject", "javax.inject", JAVAX_INJECT_VERSION);
    // final File jakartaInjectApiJar = findJarInLocalMavenRepository("jakarta.inject",
    // "jakarta.inject-api", JAKARTA_INJECT_API_VERSION);
    // final File jsr305Jar =
    // findJarInLocalMavenRepository("com.google.code.findbugs", "jsr305", JSR_305_VERSION);
    // return Lists.of(daggerJar, javaxInjectJar, jakartaInjectApiJar, jsr305Jar);
    final File awsLambdaJavaCoreJar = findJarInLocalMavenRepository("com.amazonaws",
        "aws-lambda-java-core", AWS_LAMBDA_JAVA_CORE_VERSION);
    final File cracJar = findJarInLocalMavenRepository("io.github.crac", "org-crac", CRAC_VERSION);
    final File lammyCoreJar = findJarInBuild("lammy-core");
    return unmodifiableList(asList(awsLambdaJavaCoreJar, cracJar, lammyCoreJar));
  }

  /**
   * The lammy version to use for resolving intra-project dependencies. We use this to assemble test
   * lambda function deployment packages. This should be passed by maven-surefire-plugin using the
   * exact project version from the POM. See the parent POM for the specific details of the setup.
   */
  private static final String LAMMY_VERSION =
      OptionalSystemProperty.getProperty("maven.lammy.version").orElseThrow();

  protected File findJarInBuild(String moduleName) throws FileNotFoundException {
    return findJarInBuild(moduleName, LAMMY_VERSION);
  }

  protected File findJarInBuild(String moduleName, String version) throws FileNotFoundException {
    final File result =
        new File(String.format("../%s/target/%s-%s.jar", moduleName, moduleName, version));

    if (!result.exists())
      throw new FileNotFoundException("Could not find jar file for module " + moduleName);

    return result;
  }

  protected File findJarInLocalMavenRepository(String groupId, String artifactId, String version)
      throws FileNotFoundException {
    return Maven.findJarInLocalRepository(groupId, artifactId, version);
  }

  /**
   * Add the regular files contained in a directory to a JAR output stream. This includes all files
   * in the directory and its subdirectories. The entry names for each file in the JAR will be the
   * relative path of the file from the given directory.
   *
   * @param jar The JAR output stream to add the files
   * @param dir The directory
   * @throws IOException If an I/O error occurs while reading
   */
  protected void addToJar(JarOutputStream jar, File dir) throws IOException {
    final Path dirpath = dir.toPath();
    try {
      Files.walk(dirpath).forEach(path -> {
        try {
          if (path.toFile().isFile()) {
            jar.putNextEntry(new JarEntry(dirpath.relativize(path).toString()));
            Files.copy(path, jar);
            jar.closeEntry();
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Add the regular files contained in a JAR file to a JAR output stream. All files in the given
   * JAR file will be added. The entry names for each file in the JAR will be the same as the
   * original JAR.
   *
   * @param jar The JAR output stream to add the files
   * @param other The JAR file
   * @throws IOException If an I/O error occurs while reading
   */
  protected void addToJar(JarOutputStream jar, JarFile other) throws IOException {
    final Enumeration<JarEntry> otherEntries = other.entries();
    while (otherEntries.hasMoreElements()) {
      final JarEntry otherEntry = otherEntries.nextElement();
      if (otherEntry.isDirectory())
        continue;
      try {
        jar.putNextEntry(otherEntry);
      } catch (ZipException e) {
        if (e.getMessage().contains("duplicate entry")) {
          // What is our duplicate entry?
          if (otherEntry.getName().equals("META-INF/MANIFEST.MF")) {
            // We have a bunch of duplicate manifests, which doesn't hurt anything. Don't blow up
            // the build logs about it.
          } else {
            // I don't recognize this. Let's log it.
            System.err
                .println("WARNING: Ignoring duplicate entry in JAR file: " + otherEntry.getName());
          }
          continue;
        }
        throw e;
      }
      try (final InputStream in = other.getInputStream(otherEntry)) {
        ByteStreams.copy(in, jar);
      }
      jar.closeEntry();
    }
  }

  private final Random random = new Random();

  protected String nonce() {
    return nonce(8);
  }

  protected String nonce(int length) {
    if (length < 1)
      throw new IllegalArgumentException("length must be at least 1");
    final StringBuilder result = new StringBuilder();
    for (int i = 1; i <= length; i++)
      result.append((char) ('a' + random.nextInt(26)));
    return result.toString();
  }
}
