package mccompat.v1_7_10.net.minecraft.util;

/** Legacy key -> object registry interface (1.7.10 {@code net.minecraft.util.IRegistry}). */
public interface IRegistry {
    Object getObject(Object key);

    void putObject(Object key, Object value);
}
