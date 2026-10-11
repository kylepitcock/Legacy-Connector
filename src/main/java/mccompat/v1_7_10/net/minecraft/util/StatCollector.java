package mccompat.v1_7_10.net.minecraft.util;

import net.minecraft.locale.Language;

public class StatCollector {
    public static String translateToLocal(String key) {
        Language lang = Language.getInstance();
        if (key == null || lang.has(key)) return lang.getOrDefault(key);
        String modern = mcc$modernKey(key);
        return modern != null && lang.has(modern) ? lang.getOrDefault(modern) : lang.getOrDefault(key);
    }

    /** Legacy vanilla keys whose 1.21 counterparts were renamed (vanilla entity names, the spawn egg prefix). */
    private static final java.util.Map<String, String> MCC$ENTITY_RENAMES = java.util.Map.ofEntries(
            java.util.Map.entry("PigZombie", "zombified_piglin"), java.util.Map.entry("LavaSlime", "magma_cube"),
            java.util.Map.entry("Ozelot", "ocelot"), java.util.Map.entry("VillagerGolem", "iron_golem"),
            java.util.Map.entry("SnowMan", "snow_golem"), java.util.Map.entry("MushroomCow", "mooshroom"),
            java.util.Map.entry("EntityHorse", "horse"), java.util.Map.entry("WitherBoss", "wither"),
            java.util.Map.entry("EnderDragon", "ender_dragon"), java.util.Map.entry("CaveSpider", "cave_spider"),
            java.util.Map.entry("PrimedTnt", "tnt"), java.util.Map.entry("FallingSand", "falling_block"),
            java.util.Map.entry("XPOrb", "experience_orb"), java.util.Map.entry("MinecartRideable", "minecart"));

    public static String mcc$modernKey(String key) {
        if (key.equals("item.monsterPlacer.name")) return "retrofit.spawn_egg_prefix";
        if (key.startsWith("entity.") && key.endsWith(".name") && key.indexOf('.', 7) == key.length() - 5) {
            String name = key.substring(7, key.length() - 5);
            String id = MCC$ENTITY_RENAMES.getOrDefault(name, name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT));
            return "entity.minecraft." + id;
        }
        return null;
    }

    public static String translateToLocalFormatted(String key, Object... args) {
        String pattern = translateToLocal(key);
        try {
            return String.format(pattern, args);
        } catch (RuntimeException e) {
            return "Format error: " + pattern;
        }
    }

    public static String translateToFallback(String key) {
        return translateToLocal(key);
    }

    public static boolean canTranslate(String key) {
        return Language.getInstance().has(key);
    }

    public static long getLastTranslationUpdateTimeInMilliseconds() {
        return 0L;
    }
}
