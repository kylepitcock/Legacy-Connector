package legacyconnector.v1_7_10.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayer;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayerBiome;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayerBiomeEdge;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayerZoom;
import legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager;

/**
 * 1.7.10 world type. Mods register their own (Biomes O' Plenty's "BIOMESOP") to supply a biome layer through
 * {@link #getBiomeLayer}; Legacy Connector uses the registered mod world type's layers to place legacy biomes.
 */
public class WorldType {
    public static final WorldType[] worldTypes = new WorldType[16];
    public static final WorldType DEFAULT = new WorldType(0, "default", 1).setVersioned();
    public static final WorldType FLAT = new WorldType(1, "flat");
    public static final WorldType LARGE_BIOMES = new WorldType(2, "largeBiomes");
    public static final WorldType AMPLIFIED = new WorldType(3, "amplified").setNotificationData();
    public static final WorldType DEFAULT_1_1 = new WorldType(8, "default_1_1", 0).setCanBeCreated(false);

    /** The terrain generator of worlds of this type (Forge: vanilla's overworld generator unless a mod type overrides it). */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderGenerate(world, world.getSeed(), true);
    }

    /** Modern id of the legacy mod that registered this world type (null for vanilla types). */
    public final String mcc$owner;
    private final int worldTypeId;
    private final String worldType;
    private final int generatorVersion;
    private boolean canBeCreated = true;
    private boolean isWorldTypeVersioned;
    private boolean hasNotificationData;

    private WorldType(int id, String name) {
        this(id, name, 0);
    }

    /** Vanilla types (no owner, even when this class is first loaded while a legacy mod is initialising). */
    private WorldType(int id, String name, int version) {
        this(id, name, version, null);
    }

    private WorldType(int id, String name, int version, String owner) {
        this.worldType = name;
        this.generatorVersion = version;
        this.worldTypeId = id;
        this.mcc$owner = owner;
        worldTypes[id] = this;
    }

    /** Forge: mod world types take the next free id. */
    public WorldType(String name) {
        this(getNextID(), name, 0, mcc$activeMod());
    }

    private static String mcc$activeMod() {
        var owner = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        return owner == null ? "legacyconnector" : owner.record().modernId;
    }

    private static int getNextID() {
        for (int i = 0; i < worldTypes.length; i++) if (worldTypes[i] == null) return i;
        throw new IllegalStateException("No more world type ids available");
    }

    /** Id of the world preset generated for a mod's world type. */
    public net.minecraft.resources.ResourceLocation mcc$presetId() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(mcc$owner == null ? "legacyconnector" : mcc$owner,
                org.kylepitcock.legacyconnector.core.Ids.normalizePath(worldType).replace('/', '_'));
    }

    public String getWorldTypeName() {
        return worldType;
    }

    public String getTranslateName() {
        return "generator." + worldType;
    }

    public String func_151359_c() {
        return getTranslateName() + ".info";
    }

    public int getGeneratorVersion() {
        return generatorVersion;
    }

    public WorldType getWorldTypeForGeneratorVersion(int version) {
        return this == DEFAULT && version == 0 ? DEFAULT_1_1 : this;
    }

    public WorldType setCanBeCreated(boolean enable) {
        this.canBeCreated = enable;
        return this;
    }

    public boolean getCanBeCreated() {
        return canBeCreated;
    }

    public WorldType setVersioned() {
        this.isWorldTypeVersioned = true;
        return this;
    }

    public boolean isVersioned() {
        return isWorldTypeVersioned;
    }

    public WorldType setNotificationData() {
        this.hasNotificationData = true;
        return this;
    }

    public boolean showWorldInfoNotice() {
        return hasNotificationData;
    }

    public int getWorldTypeID() {
        return worldTypeId;
    }

    public static WorldType parseWorldType(String name) {
        for (WorldType t : worldTypes) if (t != null && t.worldType.equalsIgnoreCase(name)) return t;
        return null;
    }

    // Forge additions

    public WorldChunkManager getChunkManager(World world) {
        return new WorldChunkManager(world);
    }

    public int getMinimumSpawnHeight(World world) {
        return this == FLAT ? 4 : 64;
    }

    public double getHorizon(World world) {
        return this == FLAT ? 0.0D : 63.0D;
    }

    public boolean hasVoidParticles(boolean flag) {
        return this != FLAT && !flag;
    }

    public double voidFadeMagnitude() {
        return this == FLAT ? 1.0D : 0.03125D;
    }

    public boolean handleSlimeSpawnReduction(java.util.Random random, World world) {
        return this == FLAT && random.nextInt(4) != 1;
    }

    public int getSpawnFuzz() {
        return 20;
    }

    public boolean isCustomizable() {
        return this == FLAT;
    }

    public float getCloudHeight() {
        return 128.0F;
    }

    public void addNewBiome(BiomeGenBase biome) {
        BiomeManager.addBiome(BiomeManager.BiomeType.WARM, new BiomeManager.BiomeEntry(biome, 10));
    }

    public void removeBiome(BiomeGenBase biome) {
    }

    /** Biome selection on top of the land/climate layers; mods return their own layer chain here. */
    public GenLayer getBiomeLayer(long seed, GenLayer parent) {
        GenLayer ret = new GenLayerBiome(200L, parent, this);
        ret = GenLayerZoom.magnify(1000L, ret, 2);
        return new GenLayerBiomeEdge(1000L, ret);
    }

    @Override
    public String toString() {
        return "LegacyWorldType{" + worldType + "}";
    }
}
