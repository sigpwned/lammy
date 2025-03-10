package io.aleph0.lammy.serialization.gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.google.gson.Gson;

/**
 * Jackson-based implementation of the {@link CustomPojoSerializer} interface.
 */
public class GsonCustomPojoSerializer implements CustomPojoSerializer {
  private static AtomicReference<Gson> GSON = new AtomicReference<>(new Gson());

  public static void setGson(Gson gson) {
    if (gson == null)
      throw new NullPointerException();
    GSON.getAndSet(gson);
  }

  @Override
  public <T> T fromJson(InputStream in, Type type) {
    try {
      return fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  @Override
  public <T> T fromJson(String s, Type type) {
    try {
      return fromJson(new StringReader(s), type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read input", e);
    }
  }

  private <T> T fromJson(Reader input, Type type) throws IOException {
    return getGson().fromJson(input, type);
  }

  @Override
  public <T> void toJson(T value, OutputStream out, Type type) {
    try (Writer output = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      getGson().toJson(value, output);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write output", e);
    }
  }

  /**
   * test hook
   */
  protected Gson getGson() {
    return GSON.get();
  }
}
