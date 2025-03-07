package com.sigpwned.lammy.validation.hibernate;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.amazonaws.services.lambda.runtime.Context;
import com.sigpwned.lammy.core.model.bean.RequestContext;
import com.sigpwned.lammy.core.model.bean.RequestFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;

public class HibernateValidatorRequestFilter<T> implements RequestFilter<T> {
  private static final AtomicReference<Validator> VALIDATOR = new AtomicReference<>(
      jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator());

  public static void setValidator(Validator validator) {
    if (validator == null)
      throw new NullPointerException();
    VALIDATOR.set(validator);
  }

  @Override
  public void filterRequest(RequestContext<T> requestContext, Context lambdaContext) {
    Set<ConstraintViolation<T>> violations =
        getValidator().validate(requestContext.getInputValue());
    if (!violations.isEmpty()) {
      // Per https://stackoverflow.com/a/36556258
      ConstraintViolation<T> exampleViolation = violations.iterator().next();
      exampleViolation.getPropertyPath();
      throw new ValidationException(String.format("Property '%s' %s",
          exampleViolation.getPropertyPath(), exampleViolation.getMessage()));
    }
  }

  /**
   * test hook
   */
  protected Validator getValidator() {
    return VALIDATOR.get();
  }
}
