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
package com.sigwned.lammy.core.base.stream;

import java.util.Objects;

public class StreamLambdaConfiguration {
  private Boolean autoloadInputInterceptors;
  private Boolean autoloadOutputInterceptors;
  private Boolean autoloadExceptionWriters;

  public StreamLambdaConfiguration() {}

  public Boolean getAutoloadInputInterceptors() {
    return autoloadInputInterceptors;
  }

  public void setAutoloadInputInterceptors(Boolean autoloadInputInterceptors) {
    this.autoloadInputInterceptors = autoloadInputInterceptors;
  }

  public StreamLambdaConfiguration withAutoloadInputInterceptors(
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

  public StreamLambdaConfiguration withAutoloadOutputInterceptors(
      Boolean autoloadOutputInterceptors) {
    this.autoloadOutputInterceptors = autoloadOutputInterceptors;
    return this;
  }

  public Boolean getAutoloadExceptionWriters() {
    return autoloadExceptionWriters;
  }

  public void setAutoloadExceptionWriters(Boolean autoloadExceptionWriters) {
    this.autoloadExceptionWriters = autoloadExceptionWriters;
  }

  public StreamLambdaConfiguration withAutoloadExceptionWriters(Boolean autoloadExceptionWriters) {
    this.autoloadExceptionWriters = autoloadExceptionWriters;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoloadExceptionWriters, autoloadInputInterceptors,
        autoloadOutputInterceptors);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StreamLambdaConfiguration other = (StreamLambdaConfiguration) obj;
    return Objects.equals(autoloadExceptionWriters, other.autoloadExceptionWriters)
        && Objects.equals(autoloadInputInterceptors, other.autoloadInputInterceptors)
        && Objects.equals(autoloadOutputInterceptors, other.autoloadOutputInterceptors);
  }

  @Override
  public String toString() {
    return "StreamLambdaConfiguration [autoloadInputInterceptors=" + autoloadInputInterceptors
        + ", autoloadOutputInterceptors=" + autoloadOutputInterceptors
        + ", autoloadExceptionWriters=" + autoloadExceptionWriters + "]";
  }
}
