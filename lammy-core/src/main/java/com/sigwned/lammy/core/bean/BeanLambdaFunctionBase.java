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
package com.sigwned.lammy.core.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigwned.lammy.core.stream.StreamLambdaFunctionBase;

public abstract class BeanLambdaFunctionBase<InputT, OutputT> extends StreamLambdaFunctionBase {
  private BeanReader<InputT> beanReader;
  private BeanWriter<OutputT> beanWriter;
  private final List<BeanLambdaRequestFilter<InputT>> requestFilters;
  private final List<BeanLambdaResponseFilter<InputT, OutputT>> responseFilters;

  protected BeanLambdaFunctionBase() {
    this.requestFilters = new ArrayList<>();
    this.responseFilters = new ArrayList<>();
  }

  public BeanLambdaFunctionBase(BeanReader<InputT> beanReader, BeanWriter<OutputT> beanWriter) {
    if (beanReader == null)
      throw new NullPointerException();
    if (beanWriter == null)
      throw new NullPointerException();
    this.beanReader = beanReader;
    this.beanWriter = beanWriter;
    this.requestFilters = new ArrayList<>();
    this.responseFilters = new ArrayList<>();
  }

  @Override
  public void handleStreamingRequest(InputStream inputStream, OutputStream outputStream,
      Context context) throws IOException {
    // Pull these up front to trigger any IllegalStateException we might get early
    final BeanReader<InputT> beanReader = getBeanReader();
    final BeanWriter<OutputT> beanWriter = getBeanWriter();

    final InputT originalInput = beanReader.readBeanFrom(inputStream);
    final BeanLambdaRequestContext<InputT> requestContext =
        new DefaultBeanLambdaRequestContext<>(originalInput);

    prepareBeanInput(requestContext, context);

    final OutputT originalOutput = handleBeanRequest(requestContext.getInputValue(), context);
    final BeanLambdaResponseContext<OutputT> responseContext =
        new DefaultBeanLambdaResponseContext<>(originalOutput);

    prepareBeanOutput(requestContext, responseContext, context);

    beanWriter.writeBeanTo(responseContext.getOutputValue(), outputStream);
  }

  public abstract OutputT handleBeanRequest(InputT input, Context context) throws IOException;

  private void prepareBeanInput(BeanLambdaRequestContext<InputT> requestContext,
      Context lambdaContext) {
    for (BeanLambdaRequestFilter<InputT> requestFilter : getRequestFilters())
      requestFilter.filterRequest(requestContext, lambdaContext);
  }

  private List<BeanLambdaRequestFilter<InputT>> getRequestFilters() {
    return requestFilters;
  }

  public void registerBeanRequestFilter(BeanLambdaRequestFilter<InputT> requestFilter) {
    getRequestFilters().add(requestFilter);
  }

  private void prepareBeanOutput(BeanLambdaRequestContext<InputT> requestContext,
      BeanLambdaResponseContext<OutputT> responseContext, Context lambdaContext) {
    for (BeanLambdaResponseFilter<InputT, OutputT> responseFilter : getResponseFilters())
      responseFilter.filterResponse(requestContext, responseContext, lambdaContext);
  }

  private List<BeanLambdaResponseFilter<InputT, OutputT>> getResponseFilters() {
    return responseFilters;
  }

  public void registerBeanResponseFilter(BeanLambdaResponseFilter<InputT, OutputT> responseFilter) {
    getResponseFilters().add(responseFilter);
  }

  protected BeanReader<InputT> getBeanReader() {
    if (beanReader == null)
      throw new IllegalStateException("beanReader not set");
    return beanReader;
  }

  protected void setBeanReader(BeanReader<InputT> newBeanReader) {
    if (newBeanReader == null)
      throw new NullPointerException();
    if (beanReader != null)
      throw new IllegalStateException("beanReader already set");
    this.beanReader = newBeanReader;
  }

  private BeanWriter<OutputT> getBeanWriter() {
    if (beanWriter == null)
      throw new IllegalStateException("beanWriter not set");
    return beanWriter;
  }

  protected void setBeanWriter(BeanWriter<OutputT> newBeanWriter) {
    if (newBeanWriter == null)
      throw new NullPointerException();
    if (beanWriter != null)
      throw new IllegalStateException("beanWriter already set");
    this.beanWriter = newBeanWriter;
  }
}
