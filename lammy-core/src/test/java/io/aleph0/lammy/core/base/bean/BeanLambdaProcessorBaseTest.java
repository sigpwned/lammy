package io.aleph0.lammy.core.base.bean;


import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.collect.Lists;
import io.aleph0.lammy.core.model.bean.RequestContext;
import io.aleph0.lammy.core.model.bean.RequestFilter;
import io.aleph0.lammy.core.model.bean.ResponseContext;
import io.aleph0.lammy.core.model.bean.ResponseFilter;

public class BeanLambdaProcessorBaseTest {
  public static class TestRequestFilter implements RequestFilter<Map<String, Object>> {
    public final String id;

    public TestRequestFilter(String id) {
      this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void filterRequest(RequestContext<Map<String, Object>> requestContext,
        Context lambdaContext) {
      final Map<String, Object> originalValue = requestContext.getInputValue();

      final Map<String, Object> replacementValue = new HashMap<>(originalValue);
      List<String> ids =
          (List<String>) replacementValue.computeIfAbsent("requestFilters", k -> new ArrayList<>());
      ids.add(id);

      requestContext.setInputValue(replacementValue);
    }
  }

  public static class TestResponseFilter
      implements ResponseFilter<Map<String, Object>, Map<String, Object>> {
    public final String id;

    public TestResponseFilter(String id) {
      this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void filterResponse(RequestContext<Map<String, Object>> requestContext,
        ResponseContext<Map<String, Object>> responseContext, Context lambdaContext) {
      final Map<String, Object> originalValue = responseContext.getOutputValue();

      final Map<String, Object> replacementValue = new HashMap<>(originalValue);
      List<String> ids = (List<String>) replacementValue.computeIfAbsent("responseFilters",
          k -> new ArrayList<>());
      ids.add(id);

      responseContext.setOutputValue(replacementValue);
    }
  }

  public static class TestBeanLambdaProcessor
      extends BeanLambdaProcessorBase<Map<String, Object>, Map<String, Object>> {
    public TestBeanLambdaProcessor() {
      registerRequestFilter(new TestRequestFilter("A"));
      registerRequestFilter(new TestRequestFilter("B"));
      registerResponseFilter(new TestResponseFilter("X"));
      registerResponseFilter(new TestResponseFilter("Y"));
    }

    @Override
    public Map<String, Object> handleBeanRequest(Map<String, Object> input, Context context) {
      final String name = (String) input.get("name");

      final String greeting = "Hello, " + name + "!";

      final Map<String, Object> output = new HashMap<>(input);
      output.put("greeting", greeting);

      return output;
    }
  }

  @Test
  public void givenProcessorWithFilters_whenInvoke_thenFiltersRunAsExpected() {
    final TestBeanLambdaProcessor unit = new TestBeanLambdaProcessor();

    final Map<String, Object> input = new HashMap<>();
    input.put("name", "Gandalf");

    final Map<String, Object> output = unit.handleRequest(input, null);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("name", "Gandalf");
    expected.put("greeting", "Hello, Gandalf!");
    expected.put("requestFilters", Lists.newArrayList("A", "B"));
    expected.put("responseFilters", Lists.newArrayList("X", "Y"));

    assertThat(output).isEqualTo(expected);
  }
}
