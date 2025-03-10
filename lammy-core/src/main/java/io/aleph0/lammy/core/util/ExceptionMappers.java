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
package io.aleph0.lammy.core.util;

import java.util.Collection;
import java.util.Optional;
import io.aleph0.lammy.core.model.bean.ExceptionMapper;

public final class ExceptionMappers {
  private ExceptionMappers() {}

  @SuppressWarnings("unchecked")
  public static <E extends Exception, ResponseT> Optional<ExceptionMapper<? super E, ResponseT>> findExceptionMapperForException(
      Collection<ExceptionMapper<?, ResponseT>> candidates, Exception e) {
    if (e == null)
      throw new NullPointerException();

    Class<? extends Exception> clazz = e.getClass();
    do {
      for (ExceptionMapper<? extends Exception, ResponseT> candidate : candidates) {
        final Class<? extends Exception> exceptionClazz =
            findExceptionMapperExceptionType(candidate).get();
        if (exceptionClazz.isAssignableFrom(clazz)) {
          final ExceptionMapper<? super E, ResponseT> result =
              (ExceptionMapper<? super E, ResponseT>) candidate;
          return Optional.of(result);
        }
      }

      Class<?> superclazz = clazz.getSuperclass();
      if (Exception.class.isAssignableFrom(superclazz))
        clazz = (Class<? extends Exception>) superclazz;
      else
        clazz = null;
    } while (clazz != null);

    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  public static <E extends Exception, ResponseT> Optional<Class<E>> findExceptionMapperExceptionType(
      ExceptionMapper<E, ResponseT> exceptionMapper) {
    final Class<E> result = (Class<E>) GenericTypes
        .findGenericParameter(exceptionMapper.getClass(), ExceptionMapper.class).orElse(null);
    if (result == null)
      return Optional.empty();

    assert Exception.class.isAssignableFrom(result);

    return Optional.of(result);
  }
}
