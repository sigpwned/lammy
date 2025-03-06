package com.sigwned.lammy.core.util;

import java.util.Optional;

public final class MoreObjects {
  private MoreObjects() {}

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> coalesce(T first, T second, T... more) {
    if (first != null) {
      return Optional.of(first);
    }
    if (second != null) {
      return Optional.of(second);
    }
    for (T t : more) {
      if (t != null) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }
}
