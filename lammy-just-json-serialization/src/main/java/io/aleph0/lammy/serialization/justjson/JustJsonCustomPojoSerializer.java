package io.aleph0.lammy.serialization.justjson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.sigpwned.just.json.JustJson;

/**
 * A <a href="https://github.com/sigpwned/just-json-java">Just JSON</a>-based implementation of the
 * {@link CustomPojoSerializer} interface.
 *
 * <p>
 * This implementation is a simple wrapper around the {@link JustJson} class, which provides a
 * simple, fast, and lightweight JSON parser and emitter. It is extremely simple and supports only a
 * handful of types, which makes it a poor fit for most production use cases, but ideal for testing.
 *
 * @see <a href="https://github.com/sigpwned/just-json-java">
 *      https://github.com/sigpwned/just-json-java</a>
 */
public class JustJsonCustomPojoSerializer implements CustomPojoSerializer {
  @Override
  public <T> T fromJson(InputStream input, Type type) {
    try {
      return fromJson(new String(toByteArray(input), StandardCharsets.UTF_8), type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  private byte[] toByteArray(InputStream input) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buf = new byte[8192];
      for (int n = input.read(buf); n != -1; n = input.read(buf))
        out.write(buf, 0, n);
      return out.toByteArray();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T fromJson(String input, Type type) {
    return (T) JustJson.parseDocument(input);
  }

  @Override
  public <T> void toJson(T value, OutputStream output, Type type) {
    try {
      output.write(JustJson.emitDocument(value).getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write output", e);
    }
  }
}
