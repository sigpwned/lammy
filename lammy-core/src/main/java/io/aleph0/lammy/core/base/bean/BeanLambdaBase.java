/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 Andy Boothe
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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.aleph0.lammy.core.base.LambdaFunctionBase;
import io.aleph0.lammy.core.model.bean.ExceptionMapper;
import io.aleph0.lammy.core.model.bean.RequestFilter;
import io.aleph0.lammy.core.model.bean.ResponseFilter;
import io.aleph0.lammy.core.util.GenericTypes;
import io.aleph0.lammy.core.util.MoreObjects;

/**
 * A base class for a Lambda function that accepts a bean as input and returns a bean as output. It
 * uses platform serialization, possibly using a custom serializer as loaded by the platform. It
 * also supports {@link RequestFilter}s and {@link ResponseFilter}s for preprocessing the input and
 * output, respsectively. It also supports {@link ExceptionMapper}s for mapping exceptions to
 * responses, if desired.
 *
 * @param <RequestT> The type of the input bean
 * @param <ResponseT> The type of the output bean
 *
 * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html">
 *      https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html</a>
 */
/* default */ abstract class BeanLambdaBase<RequestT, ResponseT> extends LambdaFunctionBase
    implements RequestHandler<RequestT, ResponseT> {
  private final Type requestType;
  private final List<RequestFilter<RequestT>> requestFilters;

  protected BeanLambdaBase() {
    this(new BeanLambdaConfiguration());
  }

  protected BeanLambdaBase(BeanLambdaConfiguration configuration) {
    this(null, configuration);
  }

  protected BeanLambdaBase(Type requestType) {
    this(requestType, new BeanLambdaConfiguration());
  }

  protected BeanLambdaBase(Type requestType, BeanLambdaConfiguration configuration) {
    if (requestType == null)
      requestType = GenericTypes.findGenericParameter(getClass(), BeanLambdaBase.class, 0)
          .orElseThrow(() -> new IllegalArgumentException("Could not determine request type"));
    this.requestType = requireNonNull(requestType);

    requestFilters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadRequestFilters(), getAutoloadAll())
        .orElse(false)) {
      ServiceLoader.load(RequestFilter.class).iterator().forEachRemaining(requestFilters::add);
    }
  }

  public Type getRequestType() {
    return requestType;
  }

  protected List<RequestFilter<RequestT>> getRequestFilters() {
    return unmodifiableList(requestFilters);
  }

  protected void registerRequestFilter(RequestFilter<RequestT> requestFilter) {
    if (requestFilter == null)
      throw new NullPointerException();
    if (isInitialized())
      throw new IllegalStateException();
    requestFilters.add(requestFilter);
  }

  protected abstract boolean isInitialized();

  /**
   * hook
   */
  protected void completeInitialization(Context context) {}
}
