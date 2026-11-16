package org.kylepitcock.legacyconnector.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModLoader's {@code setupProperties} for every ModLoader-era version (Beta 1.7.3, 1.2.5 to 1.5.2): the {@code @MLProp} static fields of a
 * {@code mod_*} class (block and item ids, options) are read from {@code config/<mod class>.cfg} before the mod's {@code load()} runs; missing keys
 * get the field's default and the file is written back; a value outside {@code min..max} keeps the default, as ModLoader did. The annotation type is
 * the version's own shim {@code MLProp} (same members everywhere), read reflectively.
 */
public final class MLPropConfig {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/MLProp");

    private MLPropConfig() {}

    public static void load(Class<?> modClass, Path configDir, Class<? extends Annotation> annotation) {
        Path file = configDir.resolve(modClass.getSimpleName() + ".cfg");
        Properties props = new Properties();
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
                props.load(r);
            } catch (IOException e) {
                LOG.warn("Cannot read {}: {}", file, e.toString());
            }
        }
        StringBuilder comments = new StringBuilder();
        boolean changed = false;
        for (Field f : modClass.getDeclaredFields()) {
            Annotation prop = f.getAnnotation(annotation);
            if (prop == null || !Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                String name = (String) member(prop, "name");
                String info = (String) member(prop, "info");
                String key = name.isEmpty() ? f.getName() : name;
                Object current = f.get(null);
                String raw = props.getProperty(key);
                if (raw == null) {
                    if (current != null) {
                        props.setProperty(key, current.toString());
                        changed = true;
                    }
                } else {
                    Object value = parse(f.getType(), raw.trim());
                    if (value != null && inRange(value, prop)) f.set(null, value);
                    else LOG.warn("{} in {}: {} is not a valid value, keeping {}", key, file.getFileName(), raw, current);
                }
                if (!info.isEmpty()) comments.append(key).append(" - ").append(info).append('\n');
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.warn("Cannot apply the MLProp {} of {}: {}", f.getName(), modClass.getSimpleName(), e.toString());
            }
        }
        if (changed) {
            try {
                Files.createDirectories(configDir);
                try (Writer w = Files.newBufferedWriter(file, StandardCharsets.ISO_8859_1)) {
                    props.store(w, comments.length() == 0 ? null : comments.toString().trim());
                }
            } catch (IOException e) {
                LOG.warn("Cannot write {}: {}", file, e.toString());
            }
        }
    }

    private static Object member(Annotation a, String name) throws ReflectiveOperationException {
        Method m = a.annotationType().getMethod(name);
        return m.invoke(a);
    }

    public static Object parse(Class<?> type, String raw) {
        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(raw);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(raw);
            if (type == float.class || type == Float.class) return Float.parseFloat(raw);
            if (type == double.class || type == Double.class) return Double.parseDouble(raw);
            if (type == long.class || type == Long.class) return Long.parseLong(raw);
            if (type == short.class || type == Short.class) return Short.parseShort(raw);
            if (type == byte.class || type == Byte.class) return Byte.parseByte(raw);
            if (type == String.class) return raw;
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    public static boolean inRange(Object value, Annotation prop) throws ReflectiveOperationException {
        if (!(value instanceof Number n)) return true;
        double d = n.doubleValue();
        return d >= (Double) member(prop, "min") && d <= (Double) member(prop, "max");
    }
}
