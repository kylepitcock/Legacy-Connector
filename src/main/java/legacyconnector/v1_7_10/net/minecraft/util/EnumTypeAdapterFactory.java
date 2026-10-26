package legacyconnector.v1_7_10.net.minecraft.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Gson adapters writing enum constants by their lower-case names (how chat JSON names colours). */
public class EnumTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();
        if (!raw.isEnum()) return null;
        Map<String, T> byName = new HashMap<>();
        for (Object constant : raw.getEnumConstants()) byName.put(func_151232_a(constant), (T) constant);
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) out.nullValue();
                else out.value(func_151232_a(value));
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return byName.get(in.nextString());
            }
        };
    }

    private String func_151232_a(Object value) {
        return value instanceof Enum<?> e ? e.name().toLowerCase(Locale.US) : value.toString().toLowerCase(Locale.US);
    }
}
