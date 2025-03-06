package com.sigwned.lammy.core.bean;

import java.util.Objects;

public class BeanLambdaConfiguration {
  private Boolean autoloadRequestFilters;
  private Boolean autoloadResponseFilters;
  private Boolean autoloadExceptionMappers;

  public BeanLambdaConfiguration() {}

  public Boolean getAutoloadRequestFilters() {
    return autoloadRequestFilters;
  }

  public void setAutoloadRequestFilters(Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
  }

  public BeanLambdaConfiguration withAutoloadRequestFilters(Boolean autoloadRequestFilters) {
    this.autoloadRequestFilters = autoloadRequestFilters;
    return this;
  }

  public Boolean getAutoloadResponseFilters() {
    return autoloadResponseFilters;
  }

  public void setAutoloadResponseFilters(Boolean autoloadResponseFilters) {
    this.autoloadResponseFilters = autoloadResponseFilters;
  }

  public BeanLambdaConfiguration withAutoloadResponseFilters(Boolean autoloadResponseFilters) {
    this.autoloadResponseFilters = autoloadResponseFilters;
    return this;
  }

  public Boolean getAutoloadExceptionMappers() {
    return autoloadExceptionMappers;
  }

  public void setAutoloadExceptionMappers(Boolean autoloadExceptionMappers) {
    this.autoloadExceptionMappers = autoloadExceptionMappers;
  }

  public BeanLambdaConfiguration withAutoloadExceptionMappers(Boolean autoloadExceptionMappers) {
    this.autoloadExceptionMappers = autoloadExceptionMappers;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoloadExceptionMappers, autoloadRequestFilters, autoloadResponseFilters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BeanLambdaConfiguration other = (BeanLambdaConfiguration) obj;
    return Objects.equals(autoloadExceptionMappers, other.autoloadExceptionMappers)
        && Objects.equals(autoloadRequestFilters, other.autoloadRequestFilters)
        && Objects.equals(autoloadResponseFilters, other.autoloadResponseFilters);
  }

  @Override
  public String toString() {
    return "StreamLambdaConfiguration [autoloadRequestFilters=" + autoloadRequestFilters
        + ", autoloadResponseFilters=" + autoloadResponseFilters + ", autoloadExceptionMappers="
        + autoloadExceptionMappers + "]";
  }
}
