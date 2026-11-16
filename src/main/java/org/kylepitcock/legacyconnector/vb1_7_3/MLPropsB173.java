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
public final class MLPropsB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");

    private MLPropsB173() {}

    static void load(Class<?> modClass, Path configDir) {
        earlyDir = configDir;
        org.kylepitcock.legacyconnector.core.MLPropConfig.load(modClass, configDir, MLProp.class);
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
                for (int id = Math.min(blocks.length, legacyconnector.vb1_7_3.net.minecraft.block.Block.BETA_LIMIT) - 1; id > 0 && free < 0; id--) {
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
        for (int id = Math.min(blocks.length, legacyconnector.vb1_7_3.net.minecraft.block.Block.BETA_LIMIT) - 1; id > 0; id--) {
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

    private static volatile Path earlyDir;
    private static final java.util.Map<Class<?>, Properties> EARLY = new java.util.concurrent.ConcurrentHashMap<>();

    /** A static initializer's assignment of an {@code @MLProp} field (the loader routes it here): the config file's value when it has a valid one. */
    public static int early(int value, Class<?> owner, String field) {
        return earlyValue(owner, field, int.class) instanceof Integer configured ? configured : value;
    }
    public static boolean early(boolean value, Class<?> owner, String field) {
        return earlyValue(owner, field, boolean.class) instanceof Boolean configured ? configured : value;
    }
    public static String early(String value, Class<?> owner, String field) {
        return earlyValue(owner, field, String.class) instanceof String configured ? configured : value;
    }
    private static Object earlyValue(Class<?> owner, String fieldName, Class<?> type) {
        try {
            MLProp prop = owner.getDeclaredField(fieldName).getAnnotation(MLProp.class);
            if (prop == null) return null;
            Properties props = EARLY.computeIfAbsent(owner, c -> {
                Properties p = new Properties();
                Path dir = earlyDir != null ? earlyDir : net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
                Path file = dir.resolve(c.getSimpleName() + ".cfg");
                if (Files.isRegularFile(file)) try (Reader r = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) { p.load(r); } catch (IOException ignored) {}
                return p;
            });
            String raw = props.getProperty(prop.name().isEmpty() ? fieldName : prop.name());
            if (raw == null) return null;
            Object value = org.kylepitcock.legacyconnector.core.MLPropConfig.parse(type, raw.trim());
            return value != null && org.kylepitcock.legacyconnector.core.MLPropConfig.inRange(value, prop) ? value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
