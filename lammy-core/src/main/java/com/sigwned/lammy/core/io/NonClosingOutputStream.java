package com.sigwned.lammy.core.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * An {@link OutputStream} that delegates all calls to another {@link OutputStream} except
 * {@link #close()}, which it implements as a call to {@link #flush()} instead. This is useful when
 * you want to prevent a wrapped {@link OutputStream} from being closed, even when used in a context
 * that would normally close it.
 */
public class NonClosingOutputStream extends FilterOutputStream {
  public NonClosingOutputStream(OutputStream delegate) {
    super(delegate);
  }

  @Override
  public void close() throws IOException {
    flush();
  }
}
