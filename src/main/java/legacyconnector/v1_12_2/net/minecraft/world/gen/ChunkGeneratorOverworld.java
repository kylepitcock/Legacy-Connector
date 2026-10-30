package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.init.Biomes;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.WorldType;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.InitMapGenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.PopulateChunkEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.TerrainGen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 1.12.2's overworld terrain generator, as vanilla computed it: 5x33x5 density noise from the biome heights (customized settings
 * honoured) interpolated to 4x8x4 cells, biome surfaces, caves and ravines; population with lakes, dungeons, biome decoration,
 * creatures, ice and snow. Mod dimensions that build "an overworld" instantiate or subclass it. Vanilla structures are the modern
 * game's, so the structure generators here place none.
 */
public class ChunkGeneratorOverworld implements IChunkGenerator {
    protected static final IBlockState STONE = state("stone");
    private final Random rand;
    private NoiseGeneratorOctaves minLimitPerlinNoise;
    private NoiseGeneratorOctaves maxLimitPerlinNoise;
    private NoiseGeneratorOctaves mainPerlinNoise;
    private NoiseGeneratorPerlin surfaceNoise;
    public NoiseGeneratorOctaves scaleNoise;
    public NoiseGeneratorOctaves depthNoise;
    public NoiseGeneratorOctaves forestNoise;
    private final World world;
    private final boolean mapFeaturesEnabled;
    private final WorldType terrainType;
    private final double[] heightMap;
    private final float[] biomeWeights;
    private ChunkGeneratorSettings settings;
    private IBlockState oceanBlock = state("water");
    private double[] depthBuffer = new double[256];
    private MapGenBase caveGenerator = new MapGenCaves();
    private legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenStronghold strongholdGenerator = new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenStronghold();
    private legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenVillage villageGenerator = new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenVillage();
    private legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenMineshaft mineshaftGenerator = new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenMineshaft();
    private legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenScatteredFeature scatteredFeatureGenerator =
            new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenScatteredFeature();
    private MapGenBase ravineGenerator = new MapGenRavine();
    private legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureOceanMonument oceanMonumentGenerator =
            new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureOceanMonument();
    private Biome[] biomesForGeneration;
    double[] mainNoiseRegion;
    double[] minLimitRegion;
    double[] maxLimitRegion;
    double[] depthRegion;

    static IBlockState state(String name) {
        try {
            var b = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", name));
            return b == null ? null : b.getDefaultState();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    static legacyconnector.v1_12_2.net.minecraft.block.Block block(String name) {
        try {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", name));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public ChunkGeneratorOverworld(World worldIn, long seed, boolean mapFeaturesEnabledIn, String generatorOptions) {
        this.world = worldIn;
        this.mapFeaturesEnabled = mapFeaturesEnabledIn;
        WorldType type = WorldType.DEFAULT;
        try {
            if (worldIn != null && worldIn.getWorldInfo().getTerrainType() != null) type = worldIn.getWorldInfo().getTerrainType();
        } catch (RuntimeException | LinkageError e) {
            // default
        }
        this.terrainType = type;
        this.rand = new Random(seed);
        this.minLimitPerlinNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.maxLimitPerlinNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.mainPerlinNoise = new NoiseGeneratorOctaves(this.rand, 8);
        this.surfaceNoise = new NoiseGeneratorPerlin(this.rand, 4);
        this.scaleNoise = new NoiseGeneratorOctaves(this.rand, 10);
        this.depthNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.forestNoise = new NoiseGeneratorOctaves(this.rand, 8);
        this.heightMap = new double[825];
        this.biomeWeights = new float[25];
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                this.biomeWeights[i + 2 + (j + 2) * 5] = 10.0F / (float) Math.sqrt((float) (i * i + j * j) + 0.2F);
            }
        }
        this.settings = ChunkGeneratorSettings.Factory.jsonToFactory(generatorOptions).build();
        this.oceanBlock = this.settings.useLavaOceans ? state("lava") : state("water");
        if (worldIn != null) worldIn.setSeaLevel(this.settings.seaLevel);
        try {
            var ctx = new InitNoiseGensEvent.ContextOverworld(minLimitPerlinNoise, maxLimitPerlinNoise, mainPerlinNoise, surfaceNoise, scaleNoise, depthNoise,
                    forestNoise);
            ctx = TerrainGen.getModdedNoiseGenerators(worldIn, this.rand, ctx);
            this.minLimitPerlinNoise = ctx.getLPerlin1();
            this.maxLimitPerlinNoise = ctx.getLPerlin2();
            this.mainPerlinNoise = ctx.getPerlin();
            this.surfaceNoise = ctx.getHeight();
            this.scaleNoise = ctx.getScale();
            this.depthNoise = ctx.getDepth();
            this.forestNoise = ctx.getForest();
            this.caveGenerator = TerrainGen.getModdedMapGen(caveGenerator, InitMapGenEvent.EventType.CAVE);
            this.ravineGenerator = TerrainGen.getModdedMapGen(ravineGenerator, InitMapGenEvent.EventType.RAVINE);
        } catch (RuntimeException | LinkageError e) {
            // keep vanilla's generators
        }
    }

    /** The density terrain: stone where the interpolated noise is positive, the ocean block below sea level elsewhere. */
    public void setBlocksInChunk(int x, int z, ChunkPrimer primer) {
        this.biomesForGeneration = this.world.getBiomeProvider().getBiomesForGeneration(this.biomesForGeneration, x * 4 - 2, z * 4 - 2, 10, 10);
        this.generateHeightmap(x * 4, 0, z * 4);
        int sea = this.settings.seaLevel;
        for (int i = 0; i < 4; ++i) {
            int j = i * 5;
            int k = (i + 1) * 5;
            for (int l = 0; l < 4; ++l) {
                int i1 = (j + l) * 33;
                int j1 = (j + l + 1) * 33;
                int k1 = (k + l) * 33;
                int l1 = (k + l + 1) * 33;
                for (int i2 = 0; i2 < 32; ++i2) {
                    double d1 = this.heightMap[i1 + i2];
                    double d2 = this.heightMap[j1 + i2];
                    double d3 = this.heightMap[k1 + i2];
                    double d4 = this.heightMap[l1 + i2];
                    double d5 = (this.heightMap[i1 + i2 + 1] - d1) * 0.125D;
                    double d6 = (this.heightMap[j1 + i2 + 1] - d2) * 0.125D;
                    double d7 = (this.heightMap[k1 + i2 + 1] - d3) * 0.125D;
                    double d8 = (this.heightMap[l1 + i2 + 1] - d4) * 0.125D;
                    for (int j2 = 0; j2 < 8; ++j2) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;
                        for (int k2 = 0; k2 < 4; ++k2) {
                            double d16 = (d11 - d10) * 0.25D;
                            double lvt_45_1_ = d10 - d16;
                            for (int l2 = 0; l2 < 4; ++l2) {
                                int y = i2 * 8 + j2;
                                if ((lvt_45_1_ += d16) > 0.0D) primer.setBlockState(i * 4 + k2, y, l * 4 + l2, STONE);
                                else if (y < sea) primer.setBlockState(i * 4 + k2, y, l * 4 + l2, this.oceanBlock);
                            }
                            d10 += d12;
                            d11 += d13;
                        }
                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    /** Biome surfaces (Forge's ReplaceBiomeBlocks event may take over). */
    public void replaceBiomeBlocks(int x, int z, ChunkPrimer primer, Biome[] biomesIn) {
        var event = new ChunkGeneratorEvent.ReplaceBiomeBlocks(this, x, z, primer, this.world);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return;
        this.depthBuffer = this.surfaceNoise.getRegion(this.depthBuffer, (double) (x * 16), (double) (z * 16), 16, 16, 0.0625D, 0.0625D, 1.0D);
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                Biome biome = biomesIn[j + i * 16];
                if (biome == null) biome = Biomes.PLAINS;
                biome.genTerrainBlocks(this.world, this.rand, primer, x * 16 + i, z * 16 + j, this.depthBuffer[j + i * 16]);
            }
        }
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        this.rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
        ChunkPrimer primer = new ChunkPrimer();
        try {
            this.setBlocksInChunk(x, z, primer);
            this.biomesForGeneration = this.world.getBiomeProvider().getBiomes(this.biomesForGeneration, x * 16, z * 16, 16, 16);
            this.replaceBiomeBlocks(x, z, primer, this.biomesForGeneration);
            if (this.settings.useCaves) this.caveGenerator.generate(this.world, x, z, primer);
            if (this.settings.useRavines) this.ravineGenerator.generate(this.world, x, z, primer);
        } catch (RuntimeException | LinkageError e) {
            mcc$failed("generate", x, z, e);
        }
        Chunk chunk = new Chunk(this.world, primer, x, z);
        byte[] biomes = chunk.getBiomeArray();
        if (this.biomesForGeneration != null) {
            for (int i = 0; i < biomes.length && i < this.biomesForGeneration.length; ++i) biomes[i] = (byte) Biome.getIdForBiome(this.biomesForGeneration[i]);
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    private void generateHeightmap(int x, int y, int z) {
        var s = this.settings;
        this.depthRegion = this.depthNoise.generateNoiseOctaves(this.depthRegion, x, z, 5, 5, s.depthNoiseScaleX, s.depthNoiseScaleZ, s.depthNoiseScaleExponent);
        float f = s.coordinateScale;
        float f1 = s.heightScale;
        this.mainNoiseRegion = this.mainPerlinNoise.generateNoiseOctaves(this.mainNoiseRegion, x, y, z, 5, 33, 5, f / s.mainNoiseScaleX,
                f1 / s.mainNoiseScaleY, f / s.mainNoiseScaleZ);
        this.minLimitRegion = this.minLimitPerlinNoise.generateNoiseOctaves(this.minLimitRegion, x, y, z, 5, 33, 5, f, f1, f);
        this.maxLimitRegion = this.maxLimitPerlinNoise.generateNoiseOctaves(this.maxLimitRegion, x, y, z, 5, 33, 5, f, f1, f);
        int i = 0;
        int j = 0;
        for (int k = 0; k < 5; ++k) {
            for (int l = 0; l < 5; ++l) {
                float f2 = 0.0F;
                float f3 = 0.0F;
                float f4 = 0.0F;
                Biome biome = biome(k + 2 + (l + 2) * 10);
                for (int j1 = -2; j1 <= 2; ++j1) {
                    for (int k1 = -2; k1 <= 2; ++k1) {
                        Biome biome1 = biome(k + j1 + 2 + (l + k1 + 2) * 10);
                        float f5 = s.biomeDepthOffSet + biome1.getBaseHeight() * s.biomeDepthWeight;
                        float f6 = s.biomeScaleOffset + biome1.getHeightVariation() * s.biomeScaleWeight;
                        if (this.terrainType == WorldType.AMPLIFIED && f5 > 0.0F) {
                            f5 = 1.0F + f5 * 2.0F;
                            f6 = 1.0F + f6 * 4.0F;
                        }
                        float f7 = this.biomeWeights[j1 + 2 + (k1 + 2) * 5] / (f5 + 2.0F);
                        if (biome1.getBaseHeight() > biome.getBaseHeight()) f7 /= 2.0F;
                        f2 += f6 * f7;
                        f3 += f5 * f7;
                        f4 += f7;
                    }
                }
                f2 = f2 / f4;
                f3 = f3 / f4;
                f2 = f2 * 0.9F + 0.1F;
                f3 = (f3 * 4.0F - 1.0F) / 8.0F;
                double d7 = this.depthRegion[j] / 8000.0D;
                if (d7 < 0.0D) d7 = -d7 * 0.3D;
                d7 = d7 * 3.0D - 2.0D;
                if (d7 < 0.0D) {
                    d7 = d7 / 2.0D;
                    if (d7 < -1.0D) d7 = -1.0D;
                    d7 = d7 / 1.4D;
                    d7 = d7 / 2.0D;
                } else {
                    if (d7 > 1.0D) d7 = 1.0D;
                    d7 = d7 / 8.0D;
                }
                ++j;
                double d8 = f3;
                double d9 = f2;
                d8 = d8 + d7 * 0.2D;
                d8 = d8 * (double) s.baseSize / 8.0D;
                double d0 = (double) s.baseSize + d8 * 4.0D;
                for (int l1 = 0; l1 < 33; ++l1) {
                    double d1 = ((double) l1 - d0) * (double) s.stretchY * 128.0D / 256.0D / d9;
                    if (d1 < 0.0D) d1 *= 4.0D;
                    double d2 = this.minLimitRegion[i] / (double) s.lowerLimitScale;
                    double d3 = this.maxLimitRegion[i] / (double) s.upperLimitScale;
                    double d4 = (this.mainNoiseRegion[i] / 10.0D + 1.0D) / 2.0D;
                    double d5 = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.clampedLerp(d2, d3, d4) - d1;
                    if (l1 > 29) {
                        double d6 = (double) ((float) (l1 - 29) / 3.0F);
                        d5 = d5 * (1.0D - d6) + -10.0D * d6;
                    }
                    this.heightMap[i] = d5;
                    ++i;
                }
            }
        }
    }

    private Biome biome(int index) {
        Biome b = this.biomesForGeneration == null || index >= this.biomesForGeneration.length ? null : this.biomesForGeneration[index];
        return b == null ? Biomes.PLAINS : b;
    }

    @Override
    public void populate(int x, int z) {
        try {
            mcc$populate(x, z);
        } catch (RuntimeException | LinkageError e) {
            mcc$failed("populate", x, z, e);
        }
    }

    private void mcc$populate(int x, int z) {
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        Biome biome = this.world.getBiome(blockpos.add(16, 0, 16));
        this.rand.setSeed(this.world.getSeed());
        long k = this.rand.nextLong() / 2L * 2L + 1L;
        long l = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long) x * k + (long) z * l ^ this.world.getSeed());
        boolean village = false;
        ChunkPos chunkpos = new ChunkPos(x, z);
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(this, this.world, this.rand, x, z, village));
        if (biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS && this.settings.useWaterLakes && !village
                && this.rand.nextInt(Math.max(1, this.settings.waterLakeChance)) == 0
                && TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.LAKE)) {
            var water = block("water");
            if (water != null) {
                new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenLakes(water).generate(this.world, this.rand,
                        blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(256), this.rand.nextInt(16) + 8));
            }
        }
        if (!village && this.rand.nextInt(Math.max(1, this.settings.lavaLakeChance / 10)) == 0 && this.settings.useLavaLakes
                && TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.LAVA)) {
            int i2 = this.rand.nextInt(16) + 8;
            int l2 = this.rand.nextInt(this.rand.nextInt(248) + 8);
            int k3 = this.rand.nextInt(16) + 8;
            var lava = block("lava");
            if (lava != null && (l2 < this.world.getSeaLevel() || this.rand.nextInt(Math.max(1, this.settings.lavaLakeChance / 8)) == 0)) {
                new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenLakes(lava).generate(this.world, this.rand, blockpos.add(i2, l2, k3));
            }
        }
        if (this.settings.useDungeons && TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.DUNGEON)) {
            for (int j2 = 0; j2 < this.settings.dungeonChance; ++j2) {
                new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenDungeons().generate(this.world, this.rand,
                        blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(256), this.rand.nextInt(16) + 8));
            }
        }
        biome.decorate(this.world, this.rand, new BlockPos(i, 0, j));
        if (TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.ANIMALS)) {
            legacyconnector.v1_12_2.net.minecraft.world.WorldEntitySpawner.performWorldGenSpawning(this.world, biome, i + 8, j + 8, 16, 16, this.rand);
        }
        blockpos = blockpos.add(8, 0, 8);
        if (TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.ICE)) {
            IBlockState ice = state("ice");
            IBlockState snow = state("snow_layer");
            for (int k2 = 0; k2 < 16; ++k2) {
                for (int j3 = 0; j3 < 16; ++j3) {
                    BlockPos top = this.world.getPrecipitationHeight(blockpos.add(k2, 0, j3));
                    BlockPos below = top.down();
                    if (ice != null && this.world.canBlockFreezeWater(below)) this.world.setBlockState(below, ice, 2);
                    if (snow != null && this.world.canSnowAt(top, true)) this.world.setBlockState(top, snow, 2);
                }
            }
        }
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(this, this.world, this.rand, x, z, village));
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
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(worldIn, structureName, pos);
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, structureName, position, findUnexplored);
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
    }

    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    /** Generation must never throw: a failure is logged (throttled) and the chunk keeps what was made. */
    static void mcc$failed(String what, int x, int z, Throwable e) {
        int n = FAILURES.incrementAndGet();
        if (n <= 3 || n % 500 == 0) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen").error("1.12.2 chunk generator {} failed at chunk {},{} ({} failures so far)", what, x, z, n, e);
        }
    }
}
