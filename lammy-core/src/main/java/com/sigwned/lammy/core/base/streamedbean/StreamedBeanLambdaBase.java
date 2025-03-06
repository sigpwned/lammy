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
package com.sigwned.lammy.core.base.streamedbean;



import static java.util.Collections.unmodifiableList;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sigwned.lammy.core.base.LambdaFunctionBase;
import com.sigwned.lammy.core.model.bean.RequestFilter;
import com.sigwned.lammy.core.model.stream.InputInterceptor;
import com.sigwned.lammy.core.serialization.PlatformCustomPojoSerializer;
import com.sigwned.lammy.core.util.CustomPojoSerializers;
import com.sigwned.lammy.core.util.GenericTypes;
import com.sigwned.lammy.core.util.MoreObjects;

/* default */ abstract class StreamedBeanLambdaBase<RequestT, ResponseT> extends LambdaFunctionBase
    implements RequestStreamHandler {
  private CustomPojoSerializer serializer;
  private final Type requestType;
  private final List<InputInterceptor> inputInterceptors;
  private final List<RequestFilter<RequestT>> requestFilters;

  public StreamedBeanLambdaBase(CustomPojoSerializer serializer, Type requestType,
      StreamedBeanLambdaConfiguration configuration) {
    if (serializer == null)
      serializer = CustomPojoSerializers.loadSerializer();
    this.serializer = serializer;

    if (requestType == null)
      requestType = GenericTypes.findGenericParameter(getClass(), StreamedBeanLambdaBase.class, 0)
          .orElseThrow(() -> new IllegalArgumentException("Could not determine request type"));
    this.requestType = requestType;

    this.inputInterceptors = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadInputInterceptors(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(InputInterceptor.class).iterator()
          .forEachRemaining(inputInterceptors::add);
    }

    this.requestFilters = new ArrayList<>();
    if (MoreObjects.coalesce(configuration.getAutoloadRequestFilters(), AUTOLOAD_ALL)
        .orElse(false)) {
      ServiceLoader.load(RequestFilter.class).iterator().forEachRemaining(requestFilters::add);
    }
  }

  protected void completeInitialization(Context context) {
    if (serializer == null)
      serializer = PlatformCustomPojoSerializer.forContext(context, getRequestType());
  }

  protected CustomPojoSerializer getSerializer() {
    return serializer;
  }

  public Type getRequestType() {
    return requestType;
  }

  protected void registerInputInterceptor(InputInterceptor inputInterceptor) {
    if (inputInterceptor == null)
      throw new NullPointerException();
    if (isInitialized() == true)
      throw new IllegalStateException("initialized");
    inputInterceptors.add(inputInterceptor);
  }

  protected List<InputInterceptor> getInputInterceptors() {
    return unmodifiableList(inputInterceptors);
  }

  protected void registerRequestFilter(RequestFilter<RequestT> requestFilter) {
    if (requestFilter == null)
      throw new NullPointerException();
    if (isInitialized() == true)
      throw new IllegalStateException();
    requestFilters.add(requestFilter);
  }

  protected List<RequestFilter<RequestT>> getRequestFilters() {
    return unmodifiableList(requestFilters);
  }

  protected abstract boolean isInitialized();
}
