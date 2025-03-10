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
package io.aleph0.lammy.core.base.streamedbean;

import java.util.Objects;

/* default */ class StreamedBeanLambdaConfiguration {
  public static StreamedBeanLambdaConfiguration fromProcessorConfiguration(
      StreamedBeanLambdaProcessorConfiguration functionConfiguration) {
    return new StreamedBeanLambdaConfiguration()
        .withAutoloadInputInterceptors(functionConfiguration.getAutoloadInputInterceptors())
        .withAutoloadRequestFilters(functionConfiguration.getAutoloadRequestFilters());
  }

  public static StreamedBeanLambdaConfiguration fromConsumerConfiguration(
      StreamedBeanLambdaConsumerConfiguration consumerConfiguration) {
    return new StreamedBeanLambdaConfiguration()
        .withAutoloadInputInterceptors(consumerConfiguration.getAutoloadInputInterceptors())
        .withAutoloadRequestFilters(consumerConfiguration.getAutoloadRequestFilters());
  }

  private Boolean autoloadInputInterceptors;
  private Boolean autoloadRequestFilters;

  public StreamedBeanLambdaConfiguration() {}

  public Boolean getAutoloadInputInterceptors() {
    return autoloadInputInterceptors;
  }

  public void setAutoloadInputInterceptors(Boolean autoloadInputInterceptors) {
    this.autoloadInputInterceptors = autoloadInputInterceptors;
  }

  public StreamedBeanLambdaConfiguration withAutoloadInputInterceptors(
      Boolean autoloadInputInterceptors) {
    this.autoloadInputInterceptors = autoloadInputInterceptors;
    return this;
  }

  public Boolean getAutoloadRequestFilters() {
    return autoloadRequestFilters;
  }

  public void setAutoloadRequestFilters(Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
  }

  public StreamedBeanLambdaConfiguration withAutoloadRequestFilters(
      Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoloadInputInterceptors, autoloadRequestFilters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StreamedBeanLambdaConfiguration other = (StreamedBeanLambdaConfiguration) obj;
    return Objects.equals(autoloadInputInterceptors, other.autoloadInputInterceptors)
        && Objects.equals(autoloadRequestFilters, other.autoloadRequestFilters);
  }

  @Override
  public String toString() {
    return "StreamedBeanLambdaConfiguration [autoloadInputInterceptors=" + autoloadInputInterceptors
        + ", autoloadRequestFilters=" + autoloadRequestFilters + "]";
  }


}
