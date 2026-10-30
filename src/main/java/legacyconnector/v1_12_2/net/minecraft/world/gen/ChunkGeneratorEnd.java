package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.PopulateChunkEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.TerrainGen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 1.12.2's End generator: the central island and the outer islands from the island noise, end stone shaped by 3x33x3 density noise;
 * outer islands get small floating islands and chorus plants (the modern game's features). End cities are the modern game's.
 */
public class ChunkGeneratorEnd implements IChunkGenerator {
    protected static final IBlockState END_STONE = ChunkGeneratorOverworld.state("end_stone");
    protected static final IBlockState AIR = ChunkGeneratorOverworld.state("air");
    private final Random rand;
    private NoiseGeneratorOctaves lperlinNoise1;
    private NoiseGeneratorOctaves lperlinNoise2;
    private NoiseGeneratorOctaves perlinNoise1;
    public NoiseGeneratorOctaves noiseGen5;
    public NoiseGeneratorOctaves noiseGen6;
    private final World world;
    private final boolean mapFeaturesEnabled;
    private final BlockPos spawnPoint;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenEndCity endCityGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenEndCity();
    private NoiseGeneratorSimplex islandNoise;
    /** The small floating islands of the outer End (1.12's generator for them). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenEndIsland endIslands = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenEndIsland();
    private double[] buffer;
    private Biome[] biomesForGeneration;
    double[] pnr;
    double[] ar;
    double[] br;

    public ChunkGeneratorEnd(World worldIn, boolean mapFeaturesEnabledIn, long seed, BlockPos spawnCoord) {
        this.world = worldIn;
        this.mapFeaturesEnabled = mapFeaturesEnabledIn;
        this.spawnPoint = spawnCoord;
        this.rand = new Random(seed);
        this.lperlinNoise1 = new NoiseGeneratorOctaves(this.rand, 16);
        this.lperlinNoise2 = new NoiseGeneratorOctaves(this.rand, 16);
        this.perlinNoise1 = new NoiseGeneratorOctaves(this.rand, 8);
        this.noiseGen5 = new NoiseGeneratorOctaves(this.rand, 10);
        this.noiseGen6 = new NoiseGeneratorOctaves(this.rand, 16);
        this.islandNoise = new NoiseGeneratorSimplex(this.rand);
        try {
            var ctx = new InitNoiseGensEvent.ContextEnd(lperlinNoise1, lperlinNoise2, perlinNoise1, noiseGen5, noiseGen6, islandNoise);
            ctx = TerrainGen.getModdedNoiseGenerators(worldIn, this.rand, ctx);
            this.lperlinNoise1 = ctx.getLPerlin1();
            this.lperlinNoise2 = ctx.getLPerlin2();
            this.perlinNoise1 = ctx.getPerlin();
            this.noiseGen5 = ctx.getScale();
            this.noiseGen6 = ctx.getDepth();
            this.islandNoise = ctx.getIsland();
        } catch (RuntimeException | LinkageError e) {
            // vanilla's generators
        }
    }

    public void setBlocksInChunk(int x, int z, ChunkPrimer primer) {
        this.buffer = this.getHeights(this.buffer, x * 2, 0, z * 2, 3, 33, 3);
        for (int i1 = 0; i1 < 2; ++i1) {
            for (int j1 = 0; j1 < 2; ++j1) {
                for (int k1 = 0; k1 < 32; ++k1) {
                    double d0 = this.buffer[(i1 * 3 + j1) * 33 + k1];
                    double d1 = this.buffer[(i1 * 3 + j1 + 1) * 33 + k1];
                    double d2 = this.buffer[((i1 + 1) * 3 + j1) * 33 + k1];
                    double d3 = this.buffer[((i1 + 1) * 3 + j1 + 1) * 33 + k1];
                    double d4 = (this.buffer[(i1 * 3 + j1) * 33 + k1 + 1] - d0) * 0.25D;
                    double d5 = (this.buffer[(i1 * 3 + j1 + 1) * 33 + k1 + 1] - d1) * 0.25D;
                    double d6 = (this.buffer[((i1 + 1) * 3 + j1) * 33 + k1 + 1] - d2) * 0.25D;
                    double d7 = (this.buffer[((i1 + 1) * 3 + j1 + 1) * 33 + k1 + 1] - d3) * 0.25D;
                    for (int l1 = 0; l1 < 4; ++l1) {
                        double d9 = d0;
                        double d10 = d1;
                        double d11 = (d2 - d0) * 0.125D;
                        double d12 = (d3 - d1) * 0.125D;
                        for (int i2 = 0; i2 < 8; ++i2) {
                            double d14 = d9;
                            double d15 = (d10 - d9) * 0.125D;
                            for (int j2 = 0; j2 < 8; ++j2) {
                                primer.setBlockState(i2 + i1 * 8, l1 + k1 * 4, j2 + j1 * 8, d14 > 0.0D ? END_STONE : AIR);
                                d14 += d15;
                            }
                            d9 += d11;
                            d10 += d12;
                        }
                        d0 += d4;
                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                    }
                }
            }
        }
    }

    public void buildSurfaces(ChunkPrimer primer) {
        var event = new ChunkGeneratorEvent.ReplaceBiomeBlocks(this, 0, 0, primer, this.world);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        this.rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
        ChunkPrimer primer = new ChunkPrimer();
        try {
            this.biomesForGeneration = this.world.getBiomeProvider().getBiomes(this.biomesForGeneration, x * 16, z * 16, 16, 16);
            this.setBlocksInChunk(x, z, primer);
            this.buildSurfaces(primer);
        } catch (RuntimeException | LinkageError e) {
            ChunkGeneratorOverworld.mcc$failed("generate (end)", x, z, e);
        }
        Chunk chunk = new Chunk(this.world, primer, x, z);
        byte[] ids = chunk.getBiomeArray();
        if (this.biomesForGeneration != null) {
            for (int i = 0; i < ids.length && i < this.biomesForGeneration.length; ++i) ids[i] = (byte) Biome.getIdForBiome(this.biomesForGeneration[i]);
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    /** The island height at a position: the central island fading out by 1000 blocks, outer islands from the island noise. */
    private float getIslandHeightValue(int x, int z, int p1, int p2) {
        float f = (float) (x * 2 + p1);
        float f1 = (float) (z * 2 + p2);
        float f2 = 100.0F - (float) Math.sqrt(f * f + f1 * f1) * 8.0F;
        f2 = Math.max(-100.0F, Math.min(80.0F, f2));
        for (int i = -12; i <= 12; ++i) {
            for (int j = -12; j <= 12; ++j) {
                long k = (long) (x + i);
                long l = (long) (z + j);
                if (k * k + l * l > 4096L && this.islandNoise.getValue((double) k, (double) l) < -0.8999999761581421D) {
                    float f3 = (Math.abs((float) k) * 3439.0F + Math.abs((float) l) * 147.0F) % 13.0F + 9.0F;
                    f = (float) (p1 - i * 2);
                    f1 = (float) (p2 - j * 2);
                    float f4 = 100.0F - (float) Math.sqrt(f * f + f1 * f1) * f3;
                    f4 = Math.max(-100.0F, Math.min(80.0F, f4));
                    if (f4 > f2) f2 = f4;
                }
            }
        }
        return f2;
    }

    public boolean isIslandChunk(int x, int z) {
        return (long) x * (long) x + (long) z * (long) z > 4096L && this.getIslandHeightValue(x, z, 1, 1) >= 0.0F;
    }

    private double[] getHeights(double[] field, int x, int y, int z, int xSize, int ySize, int zSize) {
        var event = new ChunkGeneratorEvent.InitNoiseField(this, field, x, y, z, xSize, ySize, zSize);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return event.getNoisefield();
        if (field == null) field = new double[xSize * ySize * zSize];
        double d0 = 684.412D * 2.0D;
        double d1 = 684.412D;
        this.pnr = this.perlinNoise1.generateNoiseOctaves(this.pnr, x, y, z, xSize, ySize, zSize, d0 / 80.0D, d1 / 160.0D, d0 / 80.0D);
        this.ar = this.lperlinNoise1.generateNoiseOctaves(this.ar, x, y, z, xSize, ySize, zSize, d0, d1, d0);
        this.br = this.lperlinNoise2.generateNoiseOctaves(this.br, x, y, z, xSize, ySize, zSize, d0, d1, d0);
        int i = x / 2;
        int j = z / 2;
        int k = 0;
        for (int l = 0; l < xSize; ++l) {
            for (int i1 = 0; i1 < zSize; ++i1) {
                float f = this.getIslandHeightValue(i, j, l, i1);
                for (int j1 = 0; j1 < ySize; ++j1) {
                    double d2 = this.ar[k] / 512.0D;
                    double d3 = this.br[k] / 512.0D;
                    double d5 = (this.pnr[k] / 10.0D + 1.0D) / 2.0D;
                    double d4 = d5 < 0.0D ? d2 : d5 > 1.0D ? d3 : d2 + (d3 - d2) * d5;
                    d4 = d4 - 8.0D + (double) f;
                    int k1 = 2;
                    if (j1 > ySize / 2 - k1) {
                        double d6 = Math.max(0.0D, Math.min(1.0D, (double) ((float) (j1 - (ySize / 2 - k1)) / 64.0F)));
                        d4 = d4 * (1.0D - d6) + -3000.0D * d6;
                    }
                    k1 = 8;
                    if (j1 < k1) {
                        double d7 = (double) ((float) (k1 - j1) / ((float) k1 - 1.0F));
                        d4 = d4 * (1.0D - d7) + -30.0D * d7;
                    }
                    field[k] = d4;
                    ++k;
                }
            }
        }
        return field;
    }

    @Override
    public void populate(int x, int z) {
        try {
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(this, this.world, this.rand, x, z, false));
            BlockPos pos = new BlockPos(x * 16, 0, z * 16);
            this.world.getBiome(pos.add(16, 0, 16)).decorate(this.world, this.world.rand, pos);
            long i = (long) x * (long) x + (long) z * (long) z;
            if (i > 4096L) {
                float f = this.getIslandHeightValue(x, z, 1, 1);
                if (f < -20.0F && this.rand.nextInt(14) == 0) {
                    this.endIslands.generate(this.world, this.rand, pos.add(this.rand.nextInt(16) + 8, 55 + this.rand.nextInt(16), this.rand.nextInt(16) + 8));
                }
                if (this.getIslandHeightValue(x, z, 1, 1) > 40.0F) {
                    int count = this.rand.nextInt(5);
                    for (int c = 0; c < count; ++c) {
                        int px = this.rand.nextInt(16) + 8;
                        int pz = this.rand.nextInt(16) + 8;
                        BlockPos top = this.world.getHeight(pos.add(px, 0, pz));
                        if (top.getY() > 0) {
                            org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(this.world,
                                    net.minecraft.data.worldgen.features.EndFeatures.CHORUS_PLANT, this.rand, top);
                        }
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(this, this.world, this.rand, x, z, false));
        } catch (RuntimeException | LinkageError e) {
            ChunkGeneratorOverworld.mcc$failed("populate (end)", x, z, e);
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
