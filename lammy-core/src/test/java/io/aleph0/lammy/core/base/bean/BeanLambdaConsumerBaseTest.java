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

public class BeanLambdaConsumerBaseTest {
  public Map<String, Object> output;

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

  public class FilterTestBeanLambdaProcessor extends BeanLambdaConsumerBase<Map<String, Object>> {
    public FilterTestBeanLambdaProcessor() {
      registerRequestFilter(new TestRequestFilter("A"));
      registerRequestFilter(new TestRequestFilter("B"));
    }

    @Override
    public void consumeBeanRequest(Map<String, Object> input, Context context) {
      final String name = (String) input.get("name");

      final String greeting = "Hello, " + name + "!";

      output = new HashMap<>(input);
      output.put("greeting", greeting);
    }
  }

  @Test
  public void givenProcessorWithFilters_whenInvoke_thenFiltersRunAsExpected() {
    final FilterTestBeanLambdaProcessor unit = new FilterTestBeanLambdaProcessor();

    final Map<String, Object> input = new HashMap<>();
    input.put("name", "Gandalf");

    unit.handleRequest(input, null);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("name", "Gandalf");
    expected.put("greeting", "Hello, Gandalf!");
    expected.put("requestFilters", Lists.newArrayList("A", "B"));

    assertThat(output).isEqualTo(expected);
  }
}
