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
package com.sigpwned.lammy.core.base.streamedbean;

import java.util.Objects;

public class StreamedBeanLambdaProcessorConfiguration {
  private Boolean autoloadInputInterceptors;
  private Boolean autoloadOutputInterceptors;
  private Boolean autoloadRequestFilters;
  private Boolean autoloadResponseFilters;
  private Boolean autoloadExceptionMappers;

  public StreamedBeanLambdaProcessorConfiguration() {}

  public Boolean getAutoloadInputInterceptors() {
    return autoloadInputInterceptors;
  }

  public void setAutoloadInputInterceptors(Boolean autoloadInputInterceptors) {
    this.autoloadInputInterceptors = autoloadInputInterceptors;
  }

  public StreamedBeanLambdaProcessorConfiguration withAutoloadInputInterceptors(
      Boolean autoloadInputInterceptors) {
    this.autoloadInputInterceptors = autoloadInputInterceptors;
    return this;
  }

  public Boolean getAutoloadOutputInterceptors() {
    return autoloadOutputInterceptors;
  }

  public void setAutoloadOutputInterceptors(Boolean autoloadOutputInterceptors) {
    this.autoloadOutputInterceptors = autoloadOutputInterceptors;
  }

  public StreamedBeanLambdaProcessorConfiguration withAutoloadOutputInterceptors(
      Boolean autoloadOutputInterceptors) {
    this.autoloadOutputInterceptors = autoloadOutputInterceptors;
    return this;
  }

  public Boolean getAutoloadRequestFilters() {
    return autoloadRequestFilters;
  }

  public void setAutoloadRequestFilters(Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
  }

  public StreamedBeanLambdaProcessorConfiguration withAutoloadRequestFilters(
      Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
    return this;
  }

  public Boolean getAutoloadResponseFilters() {
    return autoloadResponseFilters;
  }

  public void setAutoloadResponseFilters(Boolean autoloadResponseFilters) {
    this.autoloadResponseFilters = autoloadResponseFilters;
  }

  public StreamedBeanLambdaProcessorConfiguration withAutoloadResponseFilters(
      Boolean autoloadResponseFilters) {
    this.autoloadResponseFilters = autoloadResponseFilters;
    return this;
  }

  public Boolean getAutoloadExceptionMappers() {
    return autoloadExceptionMappers;
  }

  public void setAutoloadExceptionMappers(Boolean autoloadExceptionMappers) {
    this.autoloadExceptionMappers = autoloadExceptionMappers;
  }

  public StreamedBeanLambdaProcessorConfiguration withAutoloadExceptionMappers(
      Boolean autoloadExceptionMappers) {
    this.autoloadExceptionMappers = autoloadExceptionMappers;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoloadExceptionMappers, autoloadInputInterceptors,
        autoloadOutputInterceptors, autoloadRequestFilters, autoloadResponseFilters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StreamedBeanLambdaProcessorConfiguration other = (StreamedBeanLambdaProcessorConfiguration) obj;
    return Objects.equals(autoloadExceptionMappers, other.autoloadExceptionMappers)
        && Objects.equals(autoloadInputInterceptors, other.autoloadInputInterceptors)
        && Objects.equals(autoloadOutputInterceptors, other.autoloadOutputInterceptors)
        && Objects.equals(autoloadRequestFilters, other.autoloadRequestFilters)
        && Objects.equals(autoloadResponseFilters, other.autoloadResponseFilters);
  }

  @Override
  public String toString() {
    return "StreamedBeanLambdaConfiguration [autoloadInputInterceptors=" + autoloadInputInterceptors
        + ", autoloadOutputInterceptors=" + autoloadOutputInterceptors + ", autoloadRequestFilters="
        + autoloadRequestFilters + ", autoloadResponseFilters=" + autoloadResponseFilters
        + ", autoloadExceptionMappers=" + autoloadExceptionMappers + "]";
  }
}
