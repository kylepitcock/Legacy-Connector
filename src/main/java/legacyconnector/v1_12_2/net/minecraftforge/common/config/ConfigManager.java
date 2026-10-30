package legacyconnector.v1_12_2.net.minecraftforge.common.config;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fills the static fields of {@link Config} classes from the mod's config file (and writes the defaults back). */
public final class ConfigManager {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Config");
    private static final Map<String, List<Class<?>>> CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, Configuration> FILES = new ConcurrentHashMap<>();
    /** Forge's table of loaded config files by absolute path (First Aid reads it by reflection). */
    private static final Map<String, Configuration> CONFIGS = new ConcurrentHashMap<>();
    private static File configDir = new File("config");

    private ConfigManager() {}

    /** The config directory (set by the platform). */
    public static void mcc$configDir(File dir) {
        configDir = dir;
    }

    /** A class annotated with {@link Config}, found while scanning the mod. */
    public static void mcc$register(Class<?> cls) {
        Config c = cls.getAnnotation(Config.class);
        if (c == null) return;
        List<Class<?>> list = CLASSES.computeIfAbsent(c.modid(), k -> new ArrayList<>());
        if (!list.contains(cls)) list.add(cls);
    }

    public static void sync(String modid, Config.Type type) {
        for (Class<?> cls : CLASSES.getOrDefault(modid, List.of())) {
            Config c = cls.getAnnotation(Config.class);
            if (c.type() != type) continue;
            try {
                String name = c.name().isEmpty() ? modid : c.name();
                Configuration cfg = FILES.computeIfAbsent(modid + "/" + name, k -> {
                    File file = new File(configDir, name + ".cfg");
                    Configuration created = new Configuration(file);
                    CONFIGS.put(file.getAbsolutePath(), created);
                    return created;
                });
                sync(cfg, cls, c.category());
                cfg.save();
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not sync the config of {}: {}", cls.getName(), e.toString());
            }
        }
    }

    /** The loaded config file of a mod's {@link Config} class (null before the class was synced, as in Forge). */
    public static Configuration getConfiguration(String modid, String name) {
        return FILES.get(modid + "/" + (name == null || name.isEmpty() ? modid : name));
    }

    /** The categories of a mod's {@link Config} classes in their config files (what Forge's config GUI lists for the mod). */
    public static List<ConfigCategory> mcc$categories(String modid) {
        List<ConfigCategory> out = new ArrayList<>();
        for (Class<?> cls : CLASSES.getOrDefault(modid, List.of())) {
            Config c = cls.getAnnotation(Config.class);
            String name = c.name().isEmpty() ? modid : c.name();
            Configuration cfg = FILES.get(modid + "/" + name);
            if (cfg != null && cfg.hasCategory(c.category())) out.add(cfg.getCategory(c.category()));
        }
        return out;
    }

    public static Class<?>[] getModConfigClasses(String modid) {
        return CLASSES.getOrDefault(modid, List.of()).toArray(new Class<?>[0]);
    }

    private static String nameOf(Field f) {
        Config.Name n = f.getAnnotation(Config.Name.class);
        return n != null ? n.value() : f.getName();
    }

    private static void sync(Configuration cfg, Class<?> cls, String category) {
        for (Field f : cls.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod) || Modifier.isFinal(mod) || f.isAnnotationPresent(Config.Ignore.class)) continue;
            try {
                f.setAccessible(true);
                Config.Comment comment = f.getAnnotation(Config.Comment.class);
                String text = comment == null ? null : String.join("\n", comment.value());
                String key = nameOf(f);
                Class<?> t = f.getType();
                if (t == int.class) {
                    Config.RangeInt r = f.getAnnotation(Config.RangeInt.class);
                    int min = r == null ? Integer.MIN_VALUE : r.min();
                    int max = r == null ? Integer.MAX_VALUE : r.max();
                    f.setInt(null, Math.max(min, Math.min(max, cfg.get(category, key, f.getInt(null), text, min, max).getInt())));
                } else if (t == boolean.class) {
                    f.setBoolean(null, cfg.get(category, key, f.getBoolean(null), text).getBoolean());
                } else if (t == double.class) {
                    Config.RangeDouble r = f.getAnnotation(Config.RangeDouble.class);
                    double min = r == null ? -Double.MAX_VALUE : r.min();
                    double max = r == null ? Double.MAX_VALUE : r.max();
                    f.setDouble(null, Math.max(min, Math.min(max, cfg.get(category, key, f.getDouble(null), text, min, max).getDouble())));
                } else if (t == float.class) {
                    f.setFloat(null, (float) cfg.get(category, key, (double) f.getFloat(null), text).getDouble());
                } else if (t == long.class) {
                    f.setLong(null, (long) cfg.get(category, key, (double) f.getLong(null), text).getDouble());
                } else if (t == String.class) {
                    Object cur = f.get(null);
                    f.set(null, cfg.get(category, key, cur == null ? "" : (String) cur, text).getString());
                } else if (t == String[].class) {
                    String[] cur = (String[]) f.get(null);
                    f.set(null, cfg.get(category, key, cur == null ? new String[0] : cur, text).getStringList());
                } else if (t == int[].class) {
                    int[] cur = (int[]) f.get(null);
                    f.set(null, cfg.get(category, key, cur == null ? new int[0] : cur, text).getIntList());
                } else if (t == boolean[].class) {
                    boolean[] cur = (boolean[]) f.get(null);
                    f.set(null, cfg.get(category, key, cur == null ? new boolean[0] : cur, text).getBooleanList());
                } else if (t == double[].class) {
                    double[] cur = (double[]) f.get(null);
                    f.set(null, cfg.get(category, key, cur == null ? new double[0] : cur, text).getDoubleList());
                } else if (t.isEnum()) {
                    Object cur = f.get(null);
                    String s = cfg.get(category, key, cur == null ? t.getEnumConstants()[0].toString() : cur.toString(), text).getString();
                    for (Object constant : t.getEnumConstants()) if (constant.toString().equalsIgnoreCase(s)) f.set(null, constant);
                } else if (!t.isPrimitive() && !t.isArray() && t != Map.class) {
                    // a category object: its own public fields (static nested classes hold static fields, an instance keeps its fields)
                    Object holder = f.get(null);
                    if (holder != null) syncInstance(cfg, holder, category + "." + key);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.warn("Could not sync the config field {}: {}", f, e.toString());
            }
        }
        for (Class<?> nested : cls.getDeclaredClasses()) {
            if (!Modifier.isStatic(nested.getModifiers())) continue;
            Config.Name n = nested.getAnnotation(Config.Name.class);
            sync(cfg, nested, category + "." + (n != null ? n.value() : nested.getSimpleName()));
        }
    }

    private static void syncInstance(Configuration cfg, Object holder, String category) throws ReflectiveOperationException {
        cfg.getCategory(category); // the category exists even when it only holds sub-objects (EpicFight's custom_weaponry)
        for (Field f : holder.getClass().getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isPublic(mod) || Modifier.isStatic(mod) || Modifier.isFinal(mod) || f.isAnnotationPresent(Config.Ignore.class)) continue;
            f.setAccessible(true);
            Class<?> t = f.getType();
            String key = nameOf(f);
            if (t == int.class) f.setInt(holder, cfg.get(category, key, f.getInt(holder)).getInt());
            else if (t == boolean.class) f.setBoolean(holder, cfg.get(category, key, f.getBoolean(holder)).getBoolean());
            else if (t == double.class) f.setDouble(holder, cfg.get(category, key, f.getDouble(holder)).getDouble());
            else if (t == String.class) f.set(holder, cfg.get(category, key, (String) f.get(holder)).getString());
            else if (t == float.class) f.setFloat(holder, (float) cfg.get(category, key, (double) f.getFloat(holder)).getDouble());
            else if (t == long.class) f.setLong(holder, (long) cfg.get(category, key, (double) f.getLong(holder)).getDouble());
            else if (t == String[].class) {
                String[] cur = (String[]) f.get(holder);
                f.set(holder, cfg.get(category, key, cur == null ? new String[0] : cur).getStringList());
            } else if (t.isEnum()) {
                Object cur = f.get(holder);
                String v = cfg.get(category, key, cur == null ? t.getEnumConstants()[0].toString() : cur.toString()).getString();
                for (Object constant : t.getEnumConstants()) if (constant.toString().equalsIgnoreCase(v)) f.set(holder, constant);
            } else if (!t.isPrimitive() && !t.isArray() && !Map.class.isAssignableFrom(t) && !t.getName().startsWith("java.")) {
                Object child = f.get(holder);
                if (child != null) syncInstance(cfg, child, category + "." + key);
            }
        }
    }
}
