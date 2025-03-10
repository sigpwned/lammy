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
package io.aleph0.lammy.core.base.stream;

import java.util.Objects;

/* default */ class StreamLambdaConfiguration {
  public static StreamLambdaConfiguration fromProcessorConfiguration(
      StreamLambdaProcessorConfiguration c) {
    return new StreamLambdaConfiguration()
        .withAutoloadInputInterceptors(c.getAutoloadInputInterceptors());
  }

  public static StreamLambdaConfiguration fromConsumerConfiguration(
      StreamLambdaConsumerConfiguration c) {
    return new StreamLambdaConfiguration()
        .withAutoloadInputInterceptors(c.getAutoloadInputInterceptors());
  }

  private Boolean autoloadInputInterceptors;

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

  @Override
  public int hashCode() {
    return Objects.hash(autoloadInputInterceptors);
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
    return Objects.equals(autoloadInputInterceptors, other.autoloadInputInterceptors);
  }

  @Override
  public String toString() {
    return "StreamLambdaConfiguration [autoloadInputInterceptors=" + autoloadInputInterceptors
        + "]";
  }
}
