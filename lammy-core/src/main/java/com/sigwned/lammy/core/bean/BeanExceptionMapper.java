package com.sigwned.lammy.core.bean;

import java.io.IOException;
import java.io.OutputStream;
import com.sigwned.lammy.core.ExceptionMapper;

public abstract class BeanExceptionMapper<E extends Exception, T> implements ExceptionMapper<E> {
  private BeanWriter<T> beanWriter;

  protected BeanExceptionMapper() {}

  public BeanExceptionMapper(BeanWriter<T> beanWriter) {
    if (beanWriter == null)
      throw new NullPointerException();
    this.beanWriter = beanWriter;
  }

  public abstract T mapExceptionToBean(E exception);

  @Override
  public void writeExceptionTo(E exception, OutputStream out) throws IOException {
    // Trigger any IllegalStateExceptions early
    BeanWriter<T> beanWriter = getBeanWriter();
    T bean = mapExceptionToBean(exception);
    beanWriter.writeBeanTo(bean, out);
  }

  protected BeanWriter<T> getBeanWriter() {
    if (beanWriter == null)
      throw new IllegalStateException("beanWriter not set");
    return beanWriter;
  }

  protected void setBeanWriter(BeanWriter<T> newBeanWriter) {
    if (newBeanWriter == null)
      throw new NullPointerException();
    if (beanWriter != null)
      throw new IllegalStateException("beanWriter already set");
    this.beanWriter = newBeanWriter;
  }
}
