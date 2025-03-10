package io.aleph0.lammy.serialization.justjson;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JustJsonCustomPojoSerializerTest {
  @Test
  public void givenValidJson_whenFromJsonString_thenGetExpectedValue() {
    final JustJsonCustomPojoSerializer unit = new JustJsonCustomPojoSerializer();

    final String stringInput = "{\"key\":\"value\"}";

    final Object result = unit.fromJson(stringInput, Object.class);

    final Map<String, String> expected = new HashMap<>();
    expected.put("key", "value");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void givenValidJson_whenFromJsonBytes_thenGetExpectedValue() {
    final JustJsonCustomPojoSerializer unit = new JustJsonCustomPojoSerializer();

    final byte[] bytesInput = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);

    final Object result = unit.fromJson(new ByteArrayInputStream(bytesInput), Object.class);

    final Map<String, String> expected = new HashMap<>();
    expected.put("key", "value");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void givenValidObject_whenToJson_thenGetExpectedValue() {
    final JustJsonCustomPojoSerializer unit = new JustJsonCustomPojoSerializer();

    final Map<String, String> objectInput = new HashMap<>();
    objectInput.put("key", "value");

    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    unit.toJson(objectInput, output, Map.class);

    final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

    assertThat(result).isEqualTo("{\"key\":\"value\"}");
  }
}
