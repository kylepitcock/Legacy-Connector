package legacyconnector.v1_12_2.net.minecraft.world;

/**
 * A 1.12.2 world type (generator preset). The vanilla ones keep their ids and names; mods' types take the next free id as Forge
 * did ({@code new WorldType(name)}). The modern world reports the type it was created with through {@link #mcc$of}.
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

    /** Forge: a mod's world type, at the next free id. */
    public WorldType(String name) {
        this(getNextID(), name);
    }

    private WorldType(int id, String name) {
        this(id, name, 0);
    }

    private WorldType(int id, String name, int version) {
        this.name = name;
        this.version = version;
        this.id = id;
        synchronized (WorldType.class) {
            if (id >= WORLD_TYPES.length) WORLD_TYPES = java.util.Arrays.copyOf(WORLD_TYPES, Math.max(id + 1, WORLD_TYPES.length + 16));
            WORLD_TYPES[id] = this;
        }
    }

    private static synchronized int getNextID() {
        for (int x = 0; x < WORLD_TYPES.length; x++) {
            if (WORLD_TYPES[x] == null) return x;
        }
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

    private WorldType setCanBeCreated(boolean enable) {
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
        for (WorldType t : WORLD_TYPES) {
            if (t != null && t.name.equalsIgnoreCase(type)) return t;
        }
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

    // Forge hooks with plain types (the ones naming generator classes are left to the mod's own overrides)

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

    public void onGUICreateWorldPress() {
    }

    public float getCloudHeight() {
        return 128.0F;
    }

    public boolean isCustomizable() {
        return this == FLAT || this == CUSTOMIZED;
    }

    public boolean hasVoidParticles(boolean flag) {
        return this != FLAT && !flag;
    }

    /** The type a modern level was generated with: flat and debug worlds are recognised from their chunk generator. */
    public static WorldType mcc$of(net.minecraft.world.level.LevelAccessor level) {
        net.minecraft.world.level.Level full = level instanceof net.minecraft.world.level.Level l ? l
                : level instanceof net.minecraft.world.level.WorldGenLevel g ? g.getLevel() : null;
        if (full == null) return DEFAULT;
        if (full.isDebug()) return DEBUG_ALL_BLOCK_STATES;
        if (full instanceof net.minecraft.server.level.ServerLevel s
                && s.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource) return FLAT;
        return DEFAULT;
    }

    @Override
    public String toString() {
        return name;
    }

    /** Forge: the layer turning climate codes into biomes (vanilla's GenLayerBiome; mod world types override it). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer getBiomeLayer(long worldSeed, legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer parentLayer,
            legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings chunkSettings) {
        var biome = new legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayerBiome(200L, parentLayer, this, chunkSettings);
        var zoomed = legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayerZoom.magnify(1000L, biome, 2);
        return new legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayerBiomeEdge(1000L, zoomed);
    }
}
