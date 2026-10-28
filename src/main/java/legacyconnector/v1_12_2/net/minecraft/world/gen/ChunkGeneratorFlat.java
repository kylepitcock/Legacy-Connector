package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;

/**
 * 1.12.2's superflat generator from a flat preset ({@code version;layers;biome;features}, layers like
 * {@code minecraft:bedrock,2*minecraft:dirt,minecraft:grass} with optional {@code :meta}): fills the layers, sets the preset's biome
 * and, with the "decoration", "lake" or "lava_lake" features, decorates and adds lakes. Default: the classic grass preset.
 */
public class ChunkGeneratorFlat implements IChunkGenerator {
    private final World world;
    private final Random random;
    private final IBlockState[] cachedBlockIDs = new IBlockState[256];
    private int biomeId = 1;
    private boolean hasDecoration;
    private boolean hasLake;
    private boolean hasLavaLake;

    public ChunkGeneratorFlat(World worldIn, long seed, boolean generateStructures, String flatGeneratorSettings) {
        this.world = worldIn;
        this.random = new Random(seed);
        String preset = flatGeneratorSettings == null || flatGeneratorSettings.isEmpty() ? "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village"
                : flatGeneratorSettings;
        try {
            parse(preset);
        } catch (RuntimeException e) {
            parse("3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village");
        }
        int top = 0;
        for (int y = 255; y >= 0; y--) {
            if (cachedBlockIDs[y] != null) {
                top = y;
                break;
            }
        }
        if (worldIn != null) worldIn.setSeaLevel(top + 1);
    }

    private void parse(String preset) {
        java.util.Arrays.fill(cachedBlockIDs, null);
        String[] parts = preset.split(";");
        int layersIndex = parts.length == 1 ? 0 : 1;
        int y = 0;
        for (String layer : parts[layersIndex].split(",")) {
            layer = layer.trim();
            if (layer.isEmpty()) continue;
            int count = 1;
            int star = layer.indexOf('*');
            if (star > 0) {
                count = Integer.parseInt(layer.substring(0, star));
                layer = layer.substring(star + 1);
            } else {
                int x = layer.indexOf('x');
                if (x > 0 && layer.substring(0, x).chars().allMatch(Character::isDigit)) {
                    count = Integer.parseInt(layer.substring(0, x));
                    layer = layer.substring(x + 1);
                }
            }
            String[] id = layer.split(":");
            String name = id.length >= 2 ? id[0] + ":" + id[1] : "minecraft:" + id[0];
            int meta = 0;
            if (id.length >= 3) meta = Integer.parseInt(id[2]);
            else if (id.length == 2 && id[1].chars().allMatch(Character::isDigit)) {
                name = "minecraft:" + id[0];
                meta = Integer.parseInt(id[1]);
            }
            var block = legacyconnector.v1_12_2.net.minecraft.block.Block.getBlockFromName(name);
            IBlockState state = null;
            if (block != null) {
                try {
                    state = block.getStateFromMeta(meta);
                } catch (RuntimeException e) {
                    state = block.getDefaultState();
                }
            }
            for (int i = 0; i < count && y < 256; i++) cachedBlockIDs[y++] = state;
        }
        if (parts.length > layersIndex + 1) biomeId = Integer.parseInt(parts[layersIndex + 1].trim());
        if (parts.length > layersIndex + 2) {
            String features = parts[layersIndex + 2];
            for (String f : features.split(",")) {
                String name = f.trim();
                int paren = name.indexOf('(');
                if (paren >= 0) name = name.substring(0, paren);
                if (name.equals("decoration")) hasDecoration = true;
                if (name.equals("lake")) hasLake = true;
                if (name.equals("lava_lake")) hasLavaLake = true;
            }
        }
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        ChunkPrimer primer = new ChunkPrimer();
        for (int y = 0; y < cachedBlockIDs.length; ++y) {
            IBlockState state = cachedBlockIDs[y];
            if (state == null) continue;
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) primer.setBlockState(i, y, j, state);
            }
        }
        Chunk chunk = new Chunk(this.world, primer, x, z);
        byte[] ids = chunk.getBiomeArray();
        java.util.Arrays.fill(ids, (byte) biomeId);
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
        try {
            int i = x * 16;
            int j = z * 16;
            BlockPos pos = new BlockPos(i, 0, j);
            Biome biome = this.world.getBiome(new BlockPos(i + 16, 0, j + 16));
            this.random.setSeed(this.world.getSeed());
            long k = this.random.nextLong() / 2L * 2L + 1L;
            long l = this.random.nextLong() / 2L * 2L + 1L;
            this.random.setSeed((long) x * k + (long) z * l ^ this.world.getSeed());
            if (hasLake && this.random.nextInt(4) == 0) {
                var water = ChunkGeneratorOverworld.block("water");
                if (water != null) new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenLakes(water).generate(this.world, this.random,
                        pos.add(this.random.nextInt(16) + 8, this.random.nextInt(256), this.random.nextInt(16) + 8));
            }
            if (hasLavaLake && this.random.nextInt(8) == 0) {
                BlockPos p = pos.add(this.random.nextInt(16) + 8, this.random.nextInt(this.random.nextInt(248) + 8), this.random.nextInt(16) + 8);
                var lava = ChunkGeneratorOverworld.block("lava");
                if (lava != null && (p.getY() < this.world.getSeaLevel() || this.random.nextInt(10) == 0)) {
                    new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenLakes(lava).generate(this.world, this.random, p);
                }
            }
            if (hasDecoration && biome != null) biome.decorate(this.world, this.random, pos);
        } catch (RuntimeException | LinkageError e) {
            ChunkGeneratorOverworld.mcc$failed("populate (flat)", x, z, e);
        }
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        Biome biome = this.world.getBiome(pos);
        return biome == null ? new java.util.ArrayList<>() : biome.getSpawnableList(creatureType);
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, structureName, position, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(worldIn, structureName, pos);
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
    }
}
