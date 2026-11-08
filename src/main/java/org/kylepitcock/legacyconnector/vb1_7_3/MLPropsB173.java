package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import legacyconnector.vb1_7_3.net.minecraft.src.MLProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModLoader's {@code setupProperties}: the {@code @MLProp} static fields of a mod class (block and item ids, options) are read from
 * {@code config/<mod class>.cfg} before the mod is constructed; missing keys get the field's default and the file is written back.
 * Values outside {@code min..max} keep the default, as ModLoader did.
 */
final class MLPropsB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");

    private MLPropsB173() {}

    static void load(Class<?> modClass, Path configDir) {
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
            MLProp prop = f.getAnnotation(MLProp.class);
            if (prop == null || !Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                String key = prop.name().isEmpty() ? f.getName() : prop.name();
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
                if (!prop.info().isEmpty()) comments.append(key).append(" - ").append(prop.info()).append('\n');
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

    /**
     * Block ids two mods both default to (MineFactory's conveyor and RedPower Array: 125), or that a vanilla block has: players fixed these
     * in the config files. Before any mod is constructed, an {@code @MLProp} int field named {@code ...id} with a block id (1-255) that a
     * vanilla block, another mod's block or an earlier mod's field already has moves to the highest free id, in the field and its config
     * file. Mods that create blocks only in {@code ModsLoaded} (RedPower) would otherwise fail after every mod was constructed.
     */
    static void resolveClashes(java.util.List<Class<?>> modClasses, java.util.List<Object> owners, Path configDir) {
        var blocks = legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS;
        java.util.Map<Integer, Class<?>> claimed = new java.util.HashMap<>();
        java.util.List<Object[]> fields = new java.util.ArrayList<>(); // {class, field, owner}
        for (int i = 0; i < modClasses.size(); i++) {
            for (Field f : modClasses.get(i).getDeclaredFields()) {
                MLProp prop = f.getAnnotation(MLProp.class);
                if (prop == null || !Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) continue;
                if (!f.getName().toLowerCase(java.util.Locale.ROOT).endsWith("id") && !prop.name().toLowerCase(java.util.Locale.ROOT).endsWith("id")) continue;
                fields.add(new Object[] {modClasses.get(i), f, owners.get(i)});
            }
        }
        for (Object[] entry : fields) {
            Class<?> cls = (Class<?>) entry[0];
            Field f = (Field) entry[1];
            try {
                f.setAccessible(true);
                int v = f.getInt(null);
                if (v < 1 || v >= blocks.length) continue;
                var occupant = blocks[v];
                boolean clash = occupant != null && occupant.mcc$owner != entry[2] || claimed.containsKey(v) && claimed.get(v) != cls;
                if (!clash) {
                    claimed.putIfAbsent(v, cls);
                    ContentB173.reserve(v, entry[2]);
                    continue;
                }
                int free = -1;
                for (int id = blocks.length - 1; id > 0 && free < 0; id--) {
                    if (blocks[id] == null && !claimed.containsKey(id) && !usedBy(fields, id)) free = id;
                }
                if (free < 0) continue;
                MLProp prop = f.getAnnotation(MLProp.class);
                if (free < prop.min() || free > prop.max()) continue;
                f.setInt(null, free);
                claimed.put(free, cls);
                ContentB173.reserve(free, entry[2]);
                store(cls, configDir, prop.name().isEmpty() ? f.getName() : prop.name(), Integer.toString(free));
                LOG.info("b1.7.3: {}.{} = {} clashes with {}; using block id {} (saved in config/{}.cfg)", cls.getSimpleName(), f.getName(), v,
                        occupant != null ? occupant : claimed.get(v).getSimpleName(), free, cls.getSimpleName());
            } catch (ReflectiveOperationException | RuntimeException e) {
                // leaves the field as it is
            }
        }
    }

    private static boolean usedBy(java.util.List<Object[]> fields, int id) {
        for (Object[] e : fields) {
            try {
                if (((Field) e[1]).getInt(null) == id) return true;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                // unreadable
            }
        }
        return false;
    }

    /**
     * The mod's {@code @MLProp} int field holding block id {@code taken} gets the highest free block id not used by its other id
     * fields, in the field and in the config file. False if no field holds that id.
     */
    static boolean reassignBlockId(Class<?> modClass, int taken, Path configDir) {
        Field target = null;
        java.util.Set<Integer> own = new java.util.HashSet<>();
        for (Field f : modClass.getDeclaredFields()) {
            MLProp prop = f.getAnnotation(MLProp.class);
            if (prop == null || !Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) continue;
            try {
                f.setAccessible(true);
                int v = f.getInt(null);
                own.add(v);
                if (v == taken && target == null) target = f;
            } catch (ReflectiveOperationException e) {
                // not readable: not ours to move
            }
        }
        if (target == null) return false;
        MLProp prop = target.getAnnotation(MLProp.class);
        var blocks = legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS;
        for (int id = blocks.length - 1; id > 0; id--) {
            if (blocks[id] != null || own.contains(id) || id < prop.min() || id > prop.max()) continue;
            try {
                target.setInt(null, id);
            } catch (ReflectiveOperationException e) {
                return false;
            }
            store(modClass, configDir, prop.name().isEmpty() ? target.getName() : prop.name(), Integer.toString(id));
            return true;
        }
        return false;
    }

    private static void store(Class<?> modClass, Path configDir, String key, String value) {
        Path file = configDir.resolve(modClass.getSimpleName() + ".cfg");
        Properties props = new Properties();
        try {
            if (Files.isRegularFile(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
                    props.load(r);
                }
            }
            props.setProperty(key, value);
            Files.createDirectories(configDir);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.ISO_8859_1)) {
                props.store(w, null);
            }
        } catch (IOException e) {
            LOG.warn("Cannot update {}: {}", file, e.toString());
        }
    }

    private static Object parse(Class<?> type, String raw) {
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

    private static boolean inRange(Object value, MLProp prop) {
        if (!(value instanceof Number n)) return true;
        double d = n.doubleValue();
        return d >= prop.min() && d <= prop.max();
    }
}
