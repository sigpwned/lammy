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
