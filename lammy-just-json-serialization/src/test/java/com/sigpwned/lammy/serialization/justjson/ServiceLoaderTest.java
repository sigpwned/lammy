package com.sigpwned.lammy.serialization.justjson;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;

public class ServiceLoaderTest {
  @Test
  public void givenStandardServiceLoader_whenLoadCustomPojoSerializer_thenGetExactlyJustJsonCustomPojoSerializer() {
    final Iterator<CustomPojoSerializer> services =
        ServiceLoader.load(CustomPojoSerializer.class).iterator();
    assertThat(services.next()).isExactlyInstanceOf(JustJsonCustomPojoSerializer.class);
    assertThat(services.hasNext()).isFalse();
  }
}
