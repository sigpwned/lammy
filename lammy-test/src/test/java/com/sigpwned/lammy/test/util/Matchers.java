package com.sigpwned.lammy.test.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Matchers {
  private Matchers() {}

  /**
   * Backport of Java 9 {@code Matcher#results()} method.
   *
   * @param m The matcher to get results from
   * @return A stream of match results
   */
  public static Stream<MatchResult> results(Matcher m) {
    final Iterator<MatchResult> iterator = new Iterator<MatchResult>() {
      private Boolean hasNext = null;

      @Override
      public boolean hasNext() {
        if (hasNext == null)
          hasNext = m.find();
        return hasNext;
      }

      @Override
      public MatchResult next() {
        if (!hasNext())
          throw new NoSuchElementException();
        MatchResult result = m.toMatchResult();
        hasNext = null;
        return result;
      }
    };

    final Spliterator<MatchResult> spliterator = Spliterators.spliteratorUnknownSize(iterator,
        Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.IMMUTABLE);

    return StreamSupport.stream(spliterator, false);
  }
}
