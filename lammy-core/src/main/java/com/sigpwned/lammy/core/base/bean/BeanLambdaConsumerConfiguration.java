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
package com.sigpwned.lammy.core.base.bean;

import java.util.Objects;

public class BeanLambdaConsumerConfiguration {
  private Boolean autoloadRequestFilters;

  public BeanLambdaConsumerConfiguration() {}

  public Boolean getAutoloadRequestFilters() {
    return autoloadRequestFilters;
  }

  public void setAutoloadRequestFilters(Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
  }

  public BeanLambdaConsumerConfiguration withAutoloadRequestFilters(
      Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoloadRequestFilters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BeanLambdaConsumerConfiguration other = (BeanLambdaConsumerConfiguration) obj;
    return Objects.equals(autoloadRequestFilters, other.autoloadRequestFilters);
  }

  @Override
  public String toString() {
    return "BeanLambdaConsumerConfiguration [autoloadRequestFilters=" + autoloadRequestFilters
        + "]";
  }
}
