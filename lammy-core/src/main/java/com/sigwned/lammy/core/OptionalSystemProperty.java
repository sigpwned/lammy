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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Used to support DRY, economical idioms for initializing serverless functions from
 * {@link System#getProperties() system properties}.
 */
public class OptionalSystemProperty<T> {
  public static OptionalSystemProperty<String> getProperty(String name) {
    return new OptionalSystemProperty<String>(name, System.getProperty(name));
  }

  private final String name;
  private final T value;

  private OptionalSystemProperty(String name, T value) {
    if (name == null)
      throw new NullPointerException();
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public boolean isPresent() {
    return !isEmpty();
  }

  public boolean isEmpty() {
    return value == null;
  }

  public <X> OptionalSystemProperty<X> map(Function<T, X> f) {
    return new OptionalSystemProperty<>(getName(), isPresent() ? f.apply(value) : null);
  }

  public <X> OptionalSystemProperty<X> flatMap(Function<T, OptionalSystemProperty<X>> f) {
    return new OptionalSystemProperty<>(getName(),
        isPresent() ? f.apply(value).orElse(null) : null);
  }

  public Stream<T> stream() {
    return isPresent() ? Stream.of(value) : Stream.empty();
  }

  public OptionalSystemProperty<T> filter(Predicate<T> test) {
    return new OptionalSystemProperty<>(getName(), isPresent() && test.test(value) ? value : null);
  }

  public void ifPresent(Consumer<? super T> action) {
    if (isPresent())
      action.accept(value);
  }

  public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
    if (isPresent()) {
      action.accept(value);
    } else {
      emptyAction.run();
    }
  }

  public OptionalSystemProperty<T> or(
      Supplier<? extends OptionalSystemProperty<? extends T>> supplier) {
    if (isPresent()) {
      return this;
    } else {
      @SuppressWarnings("unchecked")
      OptionalSystemProperty<T> r = (OptionalSystemProperty<T>) supplier.get();
      return Objects.requireNonNull(r);
    }
  }


  public T orElse(T defaultValue) {
    return isPresent() ? value : defaultValue;
  }

  public T orElseGet(Supplier<T> defaultValue) {
    return isPresent() ? value : defaultValue.get();
  }

  public <E extends Exception> T orElseThrow(Supplier<E> supplier) throws E {
    if (isEmpty())
      throw supplier.get();
    return value;
  }

  public <E extends Exception> T orElseThrow() throws E {
    return orElseThrow(
        () -> new NoSuchElementException("No value for system property " + getName()));
  }

  public T get() {
    return orElseThrow();
  }
}
