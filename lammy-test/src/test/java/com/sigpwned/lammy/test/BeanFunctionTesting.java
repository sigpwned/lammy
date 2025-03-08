package com.sigpwned.lammy.test;

import java.util.List;
import java.util.Map;
import com.sigpwned.lammy.core.model.bean.ExceptionMapper;
import com.sigpwned.lammy.core.model.bean.RequestContext;
import com.sigpwned.lammy.core.model.bean.RequestFilter;
import com.sigpwned.lammy.core.model.bean.ResponseFilter;
import com.sigpwned.lammy.test.util.CodeGenerating;

public interface BeanFunctionTesting extends CodeGenerating {
  // REQUEST FILTER ////////////////////////////////////////////////////////////////////////////////

  public static final String REQUEST_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME =
      "META-INF/services/com.sigpwned.lammy.core.model.bean.RequestFilter";

  /**
   * Equivalent to {@code requestFilterSource(nonce, id, requestType, "")}.
   *
   * @see #requestFilterSource(String, String, String, String)
   */
  public default String requestFilterSource(String nonce, String id, String requestType) {
    return requestFilterSource(nonce, id, requestType, "");
  }

  /**
   * Generates source code for a {@link RequestFilter request filter} based on the parameters. The
   * generated class has the following properties:
   *
   * <ul>
   * <li>It is in package {@code com.example}</li>
   * <li>Its {@link #requestFilterSimpleClassName(String) simple name} {@code ExampleRequestFilter}
   * followed by the given {@code id} (e.g., for id {@code "A"}, the name would be
   * {@code ExampleRequestFilterA})</li>
   * <li>It implements {@link RequestFilter}</li>
   * <li>Its default constructor prints {@link #requestFilterInitMessage(String, String) a message}
   * to {@link System#out}</li>
   * <li>Its
   * {@link RequestFilter#filterRequest(RequestContext, com.amazonaws.services.lambda.runtime.Context)}
   * implementation prints {@link #requestFilterFilterMessage(String, String) a message} to
   * {@link System#out}</li>
   * <li>The body of the {@code filterRequest} method is the given {@code body}</li>
   * </ul>
   *
   * @param nonce a unique identifier for the generated source code
   * @param id the identifier for the generated class (e.g., {@code "A"})
   * @param requestType the type of the request object. The generated code imports {@link Map} and
   *        {@link List} for this type, if necessary.
   * @param body the body of the {@code filterRequest} method
   * @return the generated source code
   */
  public default String requestFilterSource(String nonce, String id, String requestType,
      String body) {
    // @formatter:off
    return ""
      + "package " + PACKAGE_NAME + ";\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.sigpwned.lammy.core.model.bean.RequestFilter;\n"
      + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class " + requestFilterSimpleClassName(id) + " implements RequestFilter<" + requestType + "> {\n"
      + "  public " + requestFilterSimpleClassName(id) + "() {\n"
      + "    System.out.println(\"" + requestFilterInitMessage(nonce, id) + "\");\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public void filterRequest(RequestContext<" + requestType + "> requestContext, Context lambdaContext) {\n"
      + "    System.out.println(\"" + requestFilterFilterMessage(nonce, id) + "\");\n"
      + "    " + body
      + "  }\n"
      + "}\n";
    // @formatter:on
  }

  public default String requestFilterQualifiedClassName(String id) {
    return PACKAGE_NAME + "." + requestFilterSimpleClassName(id);
  }

  public default String requestFilterSimpleClassName(String id) {
    return "ExampleRequestFilter" + id;
  }

  public default String requestFilterInitMessage(String nonce, String id) {
    return nonce + ": " + requestFilterSimpleClassName(id) + ".<init>";
  }

  public default String requestFilterFilterMessage(String nonce, String id) {
    return nonce + ": " + requestFilterSimpleClassName(id) + ".filterRequest";
  }

  // RESPONSE FILTER ///////////////////////////////////////////////////////////////////////////////

  public static final String RESPONSE_FILTER_SERVICE_LOADER_JAR_ENTRY_NAME =
      "META-INF/services/com.sigpwned.lammy.core.model.bean.ResponseFilter";

  /**
   * Equivalent to {@code responseFilterSource(nonce, id, requestType, "")}.
   *
   * @see #responseFilterSource(String, String, String, String)
   */
  public default String responseFilterSource(String nonce, String id, String requestType,
      String responseType) {
    return responseFilterSource(nonce, id, requestType, responseType, "");
  }

  /**
   * Generates source code for a {@link ResponseFilter response filter} based on the parameters. The
   * generated class has the following properties:
   *
   * <ul>
   * <li>It is in package {@code com.example}</li>
   * <li>Its is {@link #responseFilterSimpleClassName(String) simple name}
   * {@code ExampleresponseFilter} followed by the given {@code id} (e.g., for id {@code "A"}, the
   * name would be {@code ExampleresponseFilterA})</li>
   * <li>It implements {@link ResponseFilter}</li>
   * <li>Its default constructor prints {@link #responseFilterInitMessage(String, String) a message}
   * to {@link System#out}</li>
   * <li>Its
   * {@link ResponseFilter#filterRequest(RequestContext, com.amazonaws.services.lambda.runtime.Context)}
   * implementation prints {@link #responseFilterFilterMessage(String, String) a message} to
   * {@link System#out}</li>
   * <li>The body of the {@code filterRequest} method is the given {@code body}</li>
   * </ul>
   *
   * @param nonce a unique identifier for the generated source code
   * @param id the identifier for the generated class (e.g., {@code "A"})
   * @param requestType the type of the request object. The generated code imports {@link Map} and
   *        {@link List} for this type, if necessary.
   * @param body the body of the {@code filterRequest} method
   * @return the generated source code
   */
  public default String responseFilterSource(String nonce, String id, String requestType,
      String responseType, String body) {
    // @formatter:off
    return ""
      + "package " + PACKAGE_NAME + ";\n"
      + "\n"
      + "import com.amazonaws.services.lambda.runtime.Context;\n"
      + "import com.sigpwned.lammy.core.model.bean.ResponseFilter;\n"
      + "import com.sigpwned.lammy.core.model.bean.RequestContext;\n"
      + "import com.sigpwned.lammy.core.model.bean.ResponseContext;\n"
      + "import java.util.List;\n"
      + "import java.util.Map;\n"
      + "\n"
      + "public class " + responseFilterSimpleClassName(id) + " implements ResponseFilter<" + requestType + ", " + responseType + "> {\n"
      + "  public " + responseFilterSimpleClassName(id) + "() {\n"
      + "    System.out.println(\"" + responseFilterInitMessage(nonce, id) + "\");\n"
      + "  }\n"
      + "\n"
      + "  @Override\n"
      + "  public void filterResponse(RequestContext<" + requestType + "> requestContext, ResponseContext<" + responseType + "> responseContext, Context lambdaContext) {\n"
      + "    System.out.println(\"" + responseFilterFilterMessage(nonce, id) + "\");\n"
      + "    " + body
      + "  }\n"
      + "}\n";
    // @formatter:on
  }

  public default String responseFilterQualifiedClassName(String id) {
    return PACKAGE_NAME + "." + responseFilterSimpleClassName(id);
  }

  public default String responseFilterSimpleClassName(String id) {
    return "ExampleResponseFilter" + id;
  }

  public default String responseFilterInitMessage(String nonce, String id) {
    return nonce + ": " + responseFilterSimpleClassName(id) + ".<init>";
  }

  public default String responseFilterFilterMessage(String nonce, String id) {
    return nonce + ": " + responseFilterSimpleClassName(id) + ".filterResponse";
  }

  // EXCEPTION MAPPER //////////////////////////////////////////////////////////////////////////////

  public static final String EXCEPTION_MAPPER_SERVICE_LOADER_JAR_ENTRY_NAME =
      "META-INF/services/com.sigpwned.lammy.core.model.bean.ExceptionMapper";


  /**
   * Generates source code for a {@link ExceptionMapper exception mapper} based on the parameters.
   * The generated class has the following properties:
   *
   * <ul>
   * <li>It is in package {@code com.example}</li>
   * <li>Its is {@link #exceptionMapperSimpleClassName(String) simple name}
   * {@code ExampleExceptionMapper} followed by the given {@code id} (e.g., for id {@code "A"}, the
   * name would be {@code ExampleExceptionMapperA})</li>
   * <li>It implements {@link ExceptionMapper}</li>
   * <li>Its default constructor prints {@link #exceptionMapperInitMessage(String, String) a
   * message} to {@link System#out}</li>
   * <li>Its
   * {@link ExceptionMapper#mapExceptionTo(Exception, java.lang.reflect.Type, com.sigpwned.lammy.core.model.bean.Context)}
   * implementation prints {@link #exceptionMapperMapMessage(String, String) a message} to
   * {@link System#out}</li>
   * <li>The body of the {@code mapExceptionTo} method is the given {@code body}</li>
   * </ul>
   *
   * @param nonce a unique identifier for the generated source code
   * @param id the identifier for the generated class (e.g., {@code "A"})
   * @param requestType the type of the request object. The generated code imports {@link Map} and
   *        {@link List} for this type, if necessary.
   * @param body the body of the {@code filterRequest} method
   * @return the generated source code
   */
  public default String exceptionMapperSource(String nonce, String id, String exceptionType,
      String responseType, String expr) {
    // @formatter:off
    return ""
        + "package " + PACKAGE_NAME + ";\n"
        + "\n"
        + "import com.sigpwned.lammy.core.model.bean.ExceptionMapper;\n"
        + "import com.amazonaws.services.lambda.runtime.Context;\n"
        + "import java.lang.reflect.Type;\n"
        + "\n"
        + "public class " + exceptionMapperSimpleClassName(id) + " implements ExceptionMapper<" + exceptionType + ", " + responseType + "> {\n"
        + "  public " + exceptionMapperSimpleClassName(id) + "() {\n"
        + "    System.out.println(\"" + exceptionMapperInitMessage(nonce, id) + "\");\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String mapExceptionTo(" + exceptionType + " e, Type responseType, Context context) {\n"
        + "    System.out.println(\"" + exceptionMapperMapMessage(nonce, id) + "\");\n"
        + "    return " + expr + ";\n"
        + "  }\n"
        + "}\n";
    // @formatter:on
  }

  public default String exceptionMapperQualifiedClassName(String id) {
    return PACKAGE_NAME + "." + exceptionMapperSimpleClassName(id);
  }

  public default String exceptionMapperSimpleClassName(String id) {
    return "ExampleExceptionMapper" + id;
  }

  public default String exceptionMapperInitMessage(String nonce, String id) {
    return nonce + ": " + exceptionMapperSimpleClassName(id) + ".<init>";
  }

  public default String exceptionMapperMapMessage(String nonce, String id) {
    return nonce + ": " + exceptionMapperSimpleClassName(id) + ".mapExceptionTo";
  }
}
