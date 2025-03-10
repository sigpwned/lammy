package io.aleph0.lammy.serialization.gson;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import io.aleph0.lammy.serialization.gson.GsonCustomPojoSerializer;

public class ServiceLoaderTest {
  @Test
  public void givenStandardServiceLoader_whenLoadCustomPojoSerializer_thenGetExactlyGsonCustomPojoSerializer() {
    final Iterator<CustomPojoSerializer> services =
        ServiceLoader.load(CustomPojoSerializer.class).iterator();
    assertThat(services.next()).isExactlyInstanceOf(GsonCustomPojoSerializer.class);
    assertThat(services.hasNext()).isFalse();
  }
}
