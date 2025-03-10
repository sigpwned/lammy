package io.aleph0.lammy.serialization.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.type.TypeReference;

public class JacksonCustomPojoSerializerTest {
  @Test
  public void givenValidJson_whenFromJsonString_thenGetExpectedValue() {
    final JacksonCustomPojoSerializer unit = new JacksonCustomPojoSerializer();

    final String stringInput = "{\"key\":\"value\"}";

    final Object result = unit.fromJson(stringInput, Object.class);

    final Map<String, String> expected = new HashMap<>();
    expected.put("key", "value");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void givenValidJson_whenFromJsonBytes_thenGetExpectedValue() {
    final JacksonCustomPojoSerializer unit = new JacksonCustomPojoSerializer();

    final byte[] bytesInput = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);

    final Object result = unit.fromJson(new ByteArrayInputStream(bytesInput), Object.class);

    final Map<String, String> expected = new HashMap<>();
    expected.put("key", "value");

    assertThat(result).isEqualTo(expected);
  }

  public static final Type MAP_OF_STRING_TO_STRING_TYPE =
      new TypeReference<Map<String, String>>() {}.getType();

  @Test
  public void givenValidObject_whenToJson_thenGetExpectedValue() {
    final JacksonCustomPojoSerializer unit = new JacksonCustomPojoSerializer();

    final Map<String, String> objectInput = new HashMap<>();
    objectInput.put("key", "value");

    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    unit.toJson(objectInput, output, MAP_OF_STRING_TO_STRING_TYPE);

    final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

    assertThat(result).isEqualTo("{\"key\":\"value\"}");
  }
}
