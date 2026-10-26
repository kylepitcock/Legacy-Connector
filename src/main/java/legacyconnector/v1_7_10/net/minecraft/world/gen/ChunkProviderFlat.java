package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenDungeons;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenLakes;

/**
 * The superflat generator, for mod dimensions built from a flat preset ({@code "2;7,2x3,2;1;village"} or the 1.7.10
 * {@code "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village"} form): the preset's layers in every chunk, and
 * its lakes, dungeons and biome decoration when populating. Structures are the modern game's, so {@link #structureGenerators} stays empty.
 */
public class ChunkProviderFlat implements IChunkProvider {
    private static final String DEFAULT_PRESET = "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village";

    public World worldObj;
    public Random random;
    public Block[] cachedBlockIDs = new Block[256];
    public byte[] cachedBlockMetadata = new byte[256];
    public List structureGenerators = new ArrayList();
    public boolean hasDecoration;
    public boolean hasDungeons;
    public WorldGenLakes waterLakeGenerator;
    public WorldGenLakes lavaLakeGenerator;
    private int mcc$biomeId = 1;

    public ChunkProviderFlat(World world, long seed, boolean mapFeatures, String preset) {
        this.worldObj = world;
        this.random = new Random(seed);
        Set<String> features = new HashSet<>();
        if (!mcc$parse(preset, features)) {
            java.util.Arrays.fill(cachedBlockIDs, null);
            java.util.Arrays.fill(cachedBlockMetadata, (byte) 0);
            features.clear();
            mcc$parse(DEFAULT_PRESET, features);
        }
        if (features.contains("lake")) waterLakeGenerator = new WorldGenLakes(Blocks.water);
        if (features.contains("lava_lake")) lavaLakeGenerator = new WorldGenLakes(Blocks.lava);
        this.hasDungeons = features.contains("dungeon");
        this.hasDecoration = features.contains("decoration");
    }

    /** Reads the layers, biome and feature names of a preset (FlatGeneratorInfo's format, versions 0 to 3); false if it is malformed. */
    private boolean mcc$parse(String preset, Set<String> features) {
        if (preset == null || preset.isEmpty()) return false;
        String[] parts = preset.split(";", -1);
        int version = parts.length == 1 ? 0 : parseInt(parts[0], 0);
        if (version < 0 || version > 3) return false;
        int at = parts.length == 1 ? 0 : 1;
        int y = 0;
        boolean any = false;
        for (String layer : parts[at++].split(",")) {
            String[] count = version >= 3 ? layer.split("\\*", 2) : layer.split("x", 2);
            int thickness = 1;
            int meta = 0;
            Block block;
            try {
                if (count.length == 2) thickness = Math.max(0, Math.min(Integer.parseInt(count[0].trim()), 256 - y));
                String id = count[count.length - 1].trim();
                if (version < 3) {
                    String[] p = id.split(":", 2);
                    if (p.length > 1) meta = Integer.parseInt(p[1]);
                    block = Block.getBlockById(Integer.parseInt(p[0]));
                } else {
                    String[] p = id.split(":", 3);
                    block = p.length > 1 ? Block.getBlockFromName(p[0] + ":" + p[1]) : null;
                    if (block != null) {
                        meta = p.length > 2 ? Integer.parseInt(p[2]) : 0;
                    } else {
                        block = Block.getBlockFromName(p[0]);
                        if (block != null) meta = p.length == 2 ? Integer.parseInt(p[1]) : 0;
                    }
                    if (block == null) return false;
                }
            } catch (RuntimeException e) {
                return false;
            }
            if (block == null || block == Blocks.air) {
                block = null;
                meta = 0;
            }
            if (meta < 0 || meta > 15) meta = 0;
            for (int i = 0; i < thickness && y < 256; i++, y++) {
                cachedBlockIDs[y] = block;
                cachedBlockMetadata[y] = (byte) meta;
            }
            any = true;
        }
        if (!any) return false;
        if (version > 0 && parts.length > at) mcc$biomeId = parseInt(parts[at++], mcc$biomeId);
        if (version > 0 && parts.length > at) {
            for (String feature : parts[at].toLowerCase(Locale.ROOT).split(",")) {
                String name = feature.split("\\(", 2)[0];
                if (!name.isEmpty()) features.add(name);
            }
        } else {
            features.add("village");
        }
        return true;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    @Override
    public Chunk provideChunk(int cx, int cz) {
        Block[] blocks = new Block[65536];
        byte[] meta = new byte[65536];
        for (int y = 0; y < 256; y++) {
            Block block = cachedBlockIDs[y];
            if (block == null) continue;
            for (int column = 0; column < 256; column++) {
                blocks[column * 256 + y] = block;
                meta[column * 256 + y] = cachedBlockMetadata[y];
            }
        }
        Chunk chunk = new Chunk(worldObj, blocks, meta, cx, cz);
        byte[] biomes = new byte[256];
        BiomeGenBase[] generated = null;
        try {
            generated = worldObj.getWorldChunkManager().loadBlockGeneratorData(null, cx * 16, cz * 16, 16, 16);
        } catch (RuntimeException ignored) {
            // the preset's biome below
        }
        for (int i = 0; i < 256; i++) {
            biomes[i] = (byte) (generated != null && i < generated.length && generated[i] != null ? generated[i].biomeID : mcc$biomeId);
        }
        chunk.setBiomeArray(biomes);
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public Chunk loadChunk(int cx, int cz) {
        return provideChunk(cx, cz);
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public void populate(IChunkProvider provider, int cx, int cz) {
        int x = cx * 16;
        int z = cz * 16;
        BiomeGenBase biome = worldObj.getBiomeGenForCoords(x + 16, z + 16);
        random.setSeed(worldObj.getSeed());
        long a = random.nextLong() / 2L * 2L + 1L;
        long b = random.nextLong() / 2L * 2L + 1L;
        random.setSeed(cx * a + cz * b ^ worldObj.getSeed());
        if (waterLakeGenerator != null && random.nextInt(4) == 0) {
            waterLakeGenerator.generate(worldObj, random, x + random.nextInt(16) + 8, random.nextInt(256), z + random.nextInt(16) + 8);
        }
        if (lavaLakeGenerator != null && random.nextInt(8) == 0) {
            int lx = x + random.nextInt(16) + 8;
            int ly = random.nextInt(random.nextInt(248) + 8);
            int lz = z + random.nextInt(16) + 8;
            if (ly < 63 || random.nextInt(10) == 0) lavaLakeGenerator.generate(worldObj, random, lx, ly, lz);
        }
        if (hasDungeons) {
            for (int i = 0; i < 8; i++) {
                new WorldGenDungeons().generate(worldObj, random, x + random.nextInt(16) + 8, random.nextInt(256), z + random.nextInt(16) + 8);
            }
        }
        if (hasDecoration && biome != null) biome.decorate(worldObj, random, x, z);
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "FlatLevelSource";
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int x, int z) {
    }

    @Override
    public void saveExtraData() {
    }
}
