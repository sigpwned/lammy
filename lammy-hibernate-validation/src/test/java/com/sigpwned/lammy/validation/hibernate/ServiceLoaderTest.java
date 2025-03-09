package com.sigpwned.lammy.validation.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import com.sigpwned.lammy.core.model.bean.RequestFilter;

public class ServiceLoaderTest {
  @Test
  public void givenStandardServiceLoader_whenLoadRequestFilter_thenGetExactlyHibernateValidatorRequestFilter() {
    final Iterator<RequestFilter> services = ServiceLoader.load(RequestFilter.class).iterator();
    assertThat(services.next()).isExactlyInstanceOf(HibernateValidatorRequestFilter.class);
    assertThat(services.hasNext()).isFalse();
  }
}
