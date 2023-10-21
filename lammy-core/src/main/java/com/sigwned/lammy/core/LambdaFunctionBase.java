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
package com.sigwned.lammy.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import com.sigwned.lammy.core.util.GenericTypes;

public abstract class LambdaFunctionBase implements Resource {
  private final Map<Class<?>, ExceptionMapper<?>> exceptionMappers;

  public LambdaFunctionBase() {
    // Register ourselves as a CRaC handler for SnapStart
    Core.getGlobalContext().register(this);

    // Handle exception mapping
    exceptionMappers = new HashMap<>();
  }

  public <E extends Exception> void registerExceptionMapper(ExceptionMapper<E> exceptionMapper) {
    @SuppressWarnings("unchecked")
    Class<? super E> rawExceptionType = (Class<? super E>) GenericTypes
        .findGenericParameter(exceptionMapper.getClass(), ExceptionMapper.class, 0)
        .map(GenericTypes::getErasedType).get();
    registerExceptionMapper(rawExceptionType, exceptionMapper);
  }

  public <E extends Exception> void registerExceptionMapper(Class<? super E> exceptionType,
      ExceptionMapper<E> exceptionMapper) {
    getExceptionMappers().put(exceptionType, exceptionMapper);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public <E extends Exception> Optional<ExceptionMapper<? super E>> findExceptionMapperForException(
      E e) {
    if (e == null)
      throw new NullPointerException();
    return (Optional) Optional.ofNullable(findExceptionMapperForExceptionType(e.getClass()));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public <E extends Exception> Optional<ExceptionMapper<? super E>> findExceptionMapperForExceptionType(
      Class<E> exceptionType) {
    if (exceptionType == null)
      throw new NullPointerException();
    ExceptionMapper<? super E> result = null;
    for (Class<?> t = exceptionType; t != null; t = t.getSuperclass()) {
      result = (ExceptionMapper) exceptionMappers.get(exceptionType);
      if (result != null)
        break;
    }
    return Optional.ofNullable(result);
  }

  private Map<Class<?>, ExceptionMapper<?>> getExceptionMappers() {
    return exceptionMappers;
  }

  @Override
  public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {}

  @Override
  public void afterRestore(Context<? extends Resource> context) throws Exception {}
}
