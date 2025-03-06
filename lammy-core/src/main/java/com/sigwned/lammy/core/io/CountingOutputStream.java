package com.sigwned.lammy.core.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends FilterOutputStream {
  private long count;

  public CountingOutputStream(OutputStream delegate) {
    super(delegate);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    count = count + len;
    super.write(b, off, len);
  }

  @Override
  public void write(byte[] b) throws IOException {
    count = count + b.length;
    super.write(b);
  }

  @Override
  public void write(int b) throws IOException {
    count = count + 1L;
    super.write(b);
  }

  public long getCount() {
    return count;
  }
}