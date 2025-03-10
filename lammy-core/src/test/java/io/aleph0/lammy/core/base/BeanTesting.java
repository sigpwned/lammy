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
package io.aleph0.lammy.core.base;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import io.aleph0.lammy.core.model.bean.ExceptionMapper;
import io.aleph0.lammy.core.model.bean.RequestContext;
import io.aleph0.lammy.core.model.bean.RequestFilter;
import io.aleph0.lammy.core.model.bean.ResponseContext;
import io.aleph0.lammy.core.model.bean.ResponseFilter;

public interface BeanTesting {
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

  public static class TestExceptionMapper
      implements ExceptionMapper<IllegalArgumentException, Map<String, Object>> {
    @Override
    public Map<String, Object> mapExceptionTo(IllegalArgumentException e, Type responseType,
        Context context) {
      final Map<String, Object> output = new HashMap<>();
      output.put("error", e.getMessage());
      return output;
    }
  }
}
