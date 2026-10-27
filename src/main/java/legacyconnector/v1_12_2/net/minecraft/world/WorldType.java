package legacyconnector.v1_12_2.net.minecraft.world;

/**
 * 1.12.2 world type: the vanilla presets plus mod types made with {@code new WorldType(name)} (they take the next free id, as in
 * Forge). A world's type is derived from its modern generator ({@link #mcc$of}).
 */
public class WorldType {
    public static WorldType[] WORLD_TYPES = new WorldType[16];
    public static final WorldType DEFAULT = new WorldType(0, "default", 1).setVersioned();
    public static final WorldType FLAT = new WorldType(1, "flat");
    public static final WorldType LARGE_BIOMES = new WorldType(2, "largeBiomes");
    public static final WorldType AMPLIFIED = new WorldType(3, "amplified").enableInfoNotice();
    public static final WorldType CUSTOMIZED = new WorldType(4, "customized");
    public static final WorldType DEBUG_ALL_BLOCK_STATES = new WorldType(5, "debug_all_block_states");
    public static final WorldType DEFAULT_1_1 = new WorldType(8, "default_1_1", 0).setCanBeCreated(false);

    private final int id;
    private final String name;
    private final int version;
    private boolean canBeCreated = true;
    private boolean versioned;
    private boolean hasInfoNotice;

    private WorldType(int id, String name) {
        this(id, name, 0);
    }

    private WorldType(int id, String name, int version) {
        if (name.length() > 16 && !"debug_all_block_states".equals(name)) throw new IllegalArgumentException("World type names must not be longer than 16: " + name);
        this.name = name;
        this.version = version;
        this.id = id;
        WORLD_TYPES[id] = this;
    }

    /** A mod world type (Forge): the next free id, the array grows when all 16 are taken. */
    public WorldType(String name) {
        this(getNextID(), name);
    }

    private static int getNextID() {
        for (int i = 0; i < WORLD_TYPES.length; i++) if (WORLD_TYPES[i] == null) return i;
        int old = WORLD_TYPES.length;
        WORLD_TYPES = java.util.Arrays.copyOf(WORLD_TYPES, old + 16);
        return old;
    }

    public String getName() {
        return name;
    }

    public String getTranslationKey() {
        return "generator." + name;
    }

    public String getInfoTranslationKey() {
        return getTranslationKey() + ".info";
    }

    public int getVersion() {
        return version;
    }

    public WorldType getWorldTypeForGeneratorVersion(int version) {
        return this == DEFAULT && version == 0 ? DEFAULT_1_1 : this;
    }

    public WorldType setCanBeCreated(boolean enable) {
        this.canBeCreated = enable;
        return this;
    }

    public boolean canBeCreated() {
        return canBeCreated;
    }

    private WorldType setVersioned() {
        this.versioned = true;
        return this;
    }

    public boolean isVersioned() {
        return versioned;
    }

    public static WorldType byName(String type) {
        for (WorldType t : WORLD_TYPES) if (t != null && t.name.equalsIgnoreCase(type)) return t;
        return null;
    }

    public int getId() {
        return id;
    }

    public boolean hasInfoNotice() {
        return hasInfoNotice;
    }

    public WorldType enableInfoNotice() {
        this.hasInfoNotice = true;
        return this;
    }

    // ------------------------------------------------------------------ Forge

    public int getMinimumSpawnHeight(World world) {
        return this == FLAT ? 4 : world.getSeaLevel() + 1;
    }

    public double getHorizon(World world) {
        return this == FLAT ? 0.0D : 63.0D;
    }

    public double voidFadeMagnitude() {
        return this == FLAT ? 1.0D : 0.03125D;
    }

    public boolean handleSlimeSpawnReduction(java.util.Random random, World world) {
        return this == FLAT && random.nextInt(4) != 1;
    }

    public int getSpawnFuzz(WorldServer world, legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server) {
        var level = world == null ? null : world.mcc$level();
        return level == null ? 10 : Math.max(0, level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS));
    }

    public float getCloudHeight() {
        return 128.0F;
    }

    public boolean isCustomizable() {
        return this == FLAT || this == CUSTOMIZED;
    }

    public void onGUICreateWorldPress() {
    }

    public void onCustomizeButton(Object mc, Object guiCreateWorld) {
    }

    public legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider getBiomeProvider(World world) {
        return world.getBiomeProvider();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return null;
    }

    /** The type standing in for a modern level's generator: flat, amplified, large biomes, debug, else default. */
    public static WorldType mcc$of(net.minecraft.world.level.LevelAccessor level) {
        net.minecraft.server.level.ServerLevel server = level instanceof net.minecraft.server.level.ServerLevel s ? s
                : level instanceof net.minecraft.world.level.WorldGenLevel g ? g.getLevel() : null;
        if (server == null) return DEFAULT;
        var generator = server.getChunkSource().getGenerator();
        if (generator instanceof net.minecraft.world.level.levelgen.FlatLevelSource) return FLAT;
        if (generator instanceof net.minecraft.world.level.levelgen.DebugLevelSource) return DEBUG_ALL_BLOCK_STATES;
        if (generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise) {
            if (noise.generatorSettings().is(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.AMPLIFIED)) return AMPLIFIED;
            if (noise.generatorSettings().is(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.LARGE_BIOMES)) return LARGE_BIOMES;
        }
        return DEFAULT;
    }
}
