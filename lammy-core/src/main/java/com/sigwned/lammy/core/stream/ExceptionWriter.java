package com.sigwned.lammy.core.stream;

import java.io.OutputStream;
import com.amazonaws.services.lambda.runtime.Context;

public interface ExceptionWriter {
  public boolean canWriteException(Exception e);

  public void writeExceptionTo(Exception e, OutputStream out, Context context);
}
