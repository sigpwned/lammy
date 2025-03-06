package com.sigwned.lammy.core.bean;

import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;

public interface ExceptionMapper {
  public boolean canMapException(Exception e, Type target);

  public Object mapExceptionTo(Exception e, Type target, Context context);
}
