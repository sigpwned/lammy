package io.aleph0.lammy.validation.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import io.aleph0.lammy.core.model.bean.RequestFilter;
import io.aleph0.lammy.validation.hibernate.HibernateValidatorRequestFilter;

public class ServiceLoaderTest {
  @Test
  public void givenStandardServiceLoader_whenLoadRequestFilter_thenGetExactlyHibernateValidatorRequestFilter() {
    final Iterator<RequestFilter> services = ServiceLoader.load(RequestFilter.class).iterator();
    assertThat(services.next()).isExactlyInstanceOf(HibernateValidatorRequestFilter.class);
    assertThat(services.hasNext()).isFalse();
  }
}
