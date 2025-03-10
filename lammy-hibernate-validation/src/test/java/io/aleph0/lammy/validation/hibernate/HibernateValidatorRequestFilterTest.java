package io.aleph0.lammy.validation.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;
import io.aleph0.lammy.core.model.bean.RequestContext;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;

public class HibernateValidatorRequestFilterTest {
  public static final String NOT_NULL_MESSAGE = "must not be null FOOBAR";

  // A simple POJO with a validation constraint.
  public static class TestPojo {
    @NotNull(message = NOT_NULL_MESSAGE)
    private String field;

    public TestPojo(String field) {
      this.field = field;
    }

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void givenValidPojo_whenFilterRequest_thenNoException() {
    final HibernateValidatorRequestFilter<TestPojo> unit = new HibernateValidatorRequestFilter<>();
    final TestPojo validPojo = new TestPojo("value");
    final RequestContext<TestPojo> requestContext = mock(RequestContext.class);
    when(requestContext.getInputValue()).thenReturn(validPojo);
    final Context lambdaContext = mock(Context.class);

    unit.filterRequest(requestContext, lambdaContext);

    assertThat(requestContext.getInputValue()).isSameAs(validPojo);

    verify(requestContext, never()).setInputValue(any(TestPojo.class));
    verifyNoMoreInteractions(lambdaContext);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void givenInvalidPojo_whenFilterRequest_thenThrowValidationException() {
    final HibernateValidatorRequestFilter<TestPojo> unit = new HibernateValidatorRequestFilter<>();
    final TestPojo invalidPojo = new TestPojo(null);
    final RequestContext<TestPojo> requestContext = mock(RequestContext.class);
    when(requestContext.getInputValue()).thenReturn(invalidPojo);
    final Context lambdaContext = mock(Context.class);

    assertThatThrownBy(() -> unit.filterRequest(requestContext, lambdaContext))
        .isInstanceOf(ValidationException.class).hasMessageContaining(NOT_NULL_MESSAGE);

    verify(requestContext, never()).setInputValue(any(TestPojo.class));
    verifyNoMoreInteractions(lambdaContext);
  }

  @Test
  public void givenNullValidator_whenSetValidator_thenThrowNullPointerException() {
    assertThatThrownBy(() -> HibernateValidatorRequestFilter.setValidator(null))
        .isInstanceOf(NullPointerException.class);
  }
}
