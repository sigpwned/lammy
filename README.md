# lammy [![Live Tests](https://github.com/aleph0io/lammy/actions/workflows/live-tests.yml/badge.svg)](https://github.com/aleph0io/lammy/actions/workflows/live-tests.yml) [![CodeQL](https://github.com/aleph0io/lammy/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/aleph0io/lammy/actions/workflows/github-code-scanning/codeql)

Lammy is a microframework for building AWS Lambda functions in Java 8+.

## Goals

* Make it easier to build and maintain Lambda functions using Java 8+
* Keep JAR size small

## Non-Goals

* Provide implementations of common use cases (e.g., "copy file to S3")
* Build a framework for microservices in general (i.e., don't rebuild [Quarkus](https://quarkus.io/) or [Micronaut](https://micronaut.io/))

## Design Philosophy

Lammy's design is heavily influenced by [JAX-RS](https://jakarta.ee/specifications/restful-ws/). Specifically, the framework implement a straightforward, mechanical control flow that centralizes business logic while exposing hooks that libraries and applications can use for things like customization, serialization, exception handling, and so on. Hopefully, Lammy's design feels clear and familiar.

## Examples

Here are some simple examples to illustrate how Lammy works:

### Hello World

This function takes a name and gives a gretting provided by an environment variable:

    public class HelloWorldFunction extends JacksonBeanLambdaFunctionBase<GreetingRequest,GreetingResponse> {
        public static final String GREETING=getenv("GREETING").orElse("Hello");

        @Override
        public GreetingResponse handleBeanRequest(GreetingRequest request, Context context) {
            return new GreetingResponse(String.format("%s, %s!", GREETING, request.name()));
        }
    }

    record GreetingRequest(String name) {}

    record GreetingResponse(String greeting) {}

The framework provides [Jackson](https://github.com/FasterXML/jackson) serialization out of the box and provides serialization for input and output types automatically. Of course, applications can also specialize or customize serialization as desired using custom serializers, either with or without Jackson.

Additionally, the framework provides some utility methods for configuration using environment variables, such as `getenv` shown here, which returns an `OptionalEnvironmentVariable`.

## Supported Lambda Function Varieties

### Logical Function Types

Lammy supports two logical types of Lambda functions:

* `Function` -- A Lambda function with an input and an output
* `Consumer` -- A Lambda function with an input and a constant, empty output

### Implementation Styles

Similarly, Lammy supports two implementation styles of Lambda functions:

* `Stream` -- A Lambda function implementation with business logic implemented using byte stream inputs and outputs
* `Bean` -- A Lambda function implementation with business logic implemented using POJO inputs and outputs

## Anatomy of a Lambda Function

This section describes the framework's design in terms of three cooperating entities:

1. `Application` -- The user's Lambda function
2. `Framework` -- The Lammy microframework
3. `Runtime` -- The AWS Lambda runtime

### Stream Function

A stream-style function has the following steps:

    ┌─────┐   ┌───────────┐   ┌─────────┐   ┌───────────┐   ┌──────┐
    │Input│   │  Request  │   │ Request │   │ Response  │   │Output│
    │  1  ├──►│     2     ├──►│    3    ├──►│     4     ├──►│  5   │
    │Bytes│   │Interceptor│   │ Handler │   │Interceptor│   │Bytes │
    └─────┘   └───────────┘   └─────────┘   └───────────┘   └──────┘

1. The framework consumes a byte stream from the runtime as input
2. The application can optionally register one or more "request interceptor" objects to preprocess the input byte stream, which the framework then runs in the order they were registered
3. The framework calls the application's request handler, which consumes the (optionally preprocessed) input bytes and produces output bytes according to the application's business logic
4. The application can optionally register one or more "response interceptor" objects to postprocess the output byte stream, which the framework then runs in the order they were registered
5. The framework produces the byte stream to the runtime as output

### Bean Function

A bean-style function has the following steps:

                                     ┌────────────┐   ┌───────────┐   ┌──────────┐   ┌───────────┐   ┌──────────┐
                                     │  Request   │   │  Request  │   │ iii Bean │   │ Response  │   │ Response │
                                 ┌──►│     i      ├──►│    ii     ├──►│ Request  ├──►│    iv     ├──►│    v     ├───┐
                                 │   │Deserializer│   │  Filter   │   │ Handler  │   │  Filter   │   │Serializer│   │
                                 │   └────────────┘   └───────────┘   └──────────┘   └───────────┘   └──────────┘   │
                                 │                                                                                  │
                                 │                                                                                  │
                                 │                                                                                  ▼
    ┌─────┐   ┌───────────┐   ┌──┴─────────────────────────────────────────────────────────────────────────────────────┐   ┌───────────┐   ┌──────┐
    │Input│   │  Request  │   │                                             3                                          │   │ Response  │   │Output│
    │  1  ├──►│     2     ├──►│                                   Stream Request Handler                               ├──►│     4     ├──►│  5   │
    │Bytes│   │Interceptor│   │                                                                                        │   │Interceptor│   │Bytes │
    └─────┘   └───────────┘   └────────────────────────────────────────────────────────────────────────────────────────┘   └───────────┘   └──────┘

The framework implements bean-style functions using stream-style functions, so the easiest way to understand bean-style functions is as a layer "on top of" stream-style functions.

1. The framework consumes a byte stream from the runtime as input
2. The application can optionally register one or more "request interceptor" objects to preprocess the input byte stream, which the framework then runs in the order they were registered
3. The framework calls the application's request handler, which consumes the (optionally preprocessed) input bytes and produces output bytes according to the application's business logic
    1. The application registers a "request deserializer" object, which converts the (optionally preprocessed) input bytes into a bean of the input type
    2. The application can optionally register one or more "request filter" objects to preprocess the input bean, which the framework then runs in the order they were registered
    3. The framework calls the application's request handler, which consumes the (optionally preprocessed) input bean and produces a bean of the output type according to the application's business logic
    4. The application can optionally register one or more "response filter" objects to postprocess the output bean, which the framework then runs in the order they were registered
    5. The application registers a "response serializer" object, which converts the (optionally postprocessed) output bean into output bytes
5. The application can optionally register one or more "response interceptor" objects to postprocess the output byte stream, which the framework then runs in the order they were registered
6. The framework produces the byte stream to the runtime as output

### Exception Mappers

The application can also optionally register one or more "exception mapper" objects, which are responsible for converting an exception into a byte stream or output bean for stream-style and bean-style functions, respectively.

If the application propagates an exception at any step, then the framework will capture the exception and look for a matching exception mapper. If the framework finds a matching exception mapper, then the exception mapper is used to generate a successful response to the runtime. If the framework cannot find a matching exception mapper, then the framework propagates the exception to the runtime, which then produces a failure response using [its default serialization format](https://docs.aws.amazon.com/lambda/latest/dg/java-exceptions.html#java-exceptions-createfunction):

    {
        "errorMessage": "Input must be a list that contains 2 numbers.",
        "errorType":"java.lang.InputLengthException",
        "stackTrace": [
            "example.HandlerDivide.handleRequest(HandlerDivide.java:23)",
            "example.HandlerDivide.handleRequest(HandlerDivide.java:14)"
        ]
    }

Unfortunately, the runtime does not offer a way for applications to generate an error response with a custom serialization format.

## Building

When building, you may notice tests failing, especially with timeout messages indicating that LocalStack has stopped responding to requests. Try running the offending tests individually like this:

    JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home" mvn -pl lammy-test test -Dtest='BeanLambdaFunctionIntegrationTest#givenExceptionMapperServicesAB_whenAutoloadExplicitlyEnabledAndThrowNonMatching_thenPropagate

If the tests pass when run individually but fail when run in the context of the larger build, then then it's likely that the underlying LocalStack instance is running out of memory and crashing. In this situation, try increasing the memory limit for your container engine. For example, in Docker Desktop, increasing the memory limit by going to Settings &rarr; Resources &rarr; Memory Limit to at least 4GB.

