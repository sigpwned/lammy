package com.sigwned.lammy.core.io;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * An {@link InputStream} that delegates all calls to another {@link InputStream} except
 * {@link #close()}, which it ignores. This is useful when you want to prevent a wrapped
 * {@link InputStream} from being closed, even when used in a context that would normally close it.
 */
public class NonClosingInputStream extends FilterInputStream {
  public NonClosingInputStream(InputStream delegate) {
    super(delegate);
  }

  @Override
  public void close() {
    // Do nothing. That's the point of this class!
  }
}
