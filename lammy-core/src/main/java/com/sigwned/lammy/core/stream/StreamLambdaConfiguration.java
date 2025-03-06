package com.sigwned.lammy.core.stream;

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
