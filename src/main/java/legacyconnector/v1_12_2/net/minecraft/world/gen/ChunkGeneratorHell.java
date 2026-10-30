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
import legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.InitMapGenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.OreGenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.PopulateChunkEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.TerrainGen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 1.12.2's nether generator: 128-high terrain of netherrack shaped by 5x17x5 density noise over a lava sea at y=32, soul sand and
 * gravel near y=64, bedrock floor and ceiling, nether caves; population with lava springs, fire, glowstone, mushrooms, quartz and
 * magma. Mod dimensions extending the nether instantiate or subclass it. Fortresses are the modern game's.
 */
public class ChunkGeneratorHell implements IChunkGenerator {
    protected static final IBlockState AIR = ChunkGeneratorOverworld.state("air");
    protected static final IBlockState NETHERRACK = ChunkGeneratorOverworld.state("netherrack");
    protected static final IBlockState BEDROCK = ChunkGeneratorOverworld.state("bedrock");
    protected static final IBlockState LAVA = ChunkGeneratorOverworld.state("lava");
    protected static final IBlockState GRAVEL = ChunkGeneratorOverworld.state("gravel");
    protected static final IBlockState SOUL_SAND = ChunkGeneratorOverworld.state("soul_sand");
    private final World world;
    private final boolean generateStructures;
    private final Random rand;
    private double[] slowsandNoise = new double[256];
    private double[] gravelNoise = new double[256];
    private double[] depthBuffer = new double[256];
    private double[] buffer;
    private NoiseGeneratorOctaves lperlinNoise1;
    private NoiseGeneratorOctaves lperlinNoise2;
    private NoiseGeneratorOctaves perlinNoise1;
    private NoiseGeneratorOctaves slowsandGravelNoiseGen;
    private NoiseGeneratorOctaves netherrackExculsivityNoiseGen;
    public NoiseGeneratorOctaves scaleNoise;
    public NoiseGeneratorOctaves depthNoise;
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenFire fireFeature = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenFire();
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenGlowStone1 lightGemGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenGlowStone1();
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenGlowStone2 hellPortalGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenGlowStone2();
    private final WorldGenerator quartzGen = minable("quartz_ore", 14);
    private final WorldGenerator magmaGen = minable("magma", 33);
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenHellLava lavaTrapGen = hellLava(true);
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenHellLava hellSpringGen = hellLava(false);
    public final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBush brownMushroomFeature = bush(net.minecraft.data.worldgen.features.VegetationFeatures.PATCH_BROWN_MUSHROOM);
    public final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBush redMushroomFeature = bush(net.minecraft.data.worldgen.features.VegetationFeatures.PATCH_RED_MUSHROOM);
    public legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenNetherBridge genNetherBridge = new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenNetherBridge();
    private MapGenBase genNetherCaves = new MapGenCavesHell();
    double[] pnr;
    double[] ar;
    double[] br;
    double[] noiseData4;
    double[] dr;

    private static WorldGenerator minable(String ore, int size) {
        IBlockState state = ChunkGeneratorOverworld.state(ore);
        if (state == null) return null;
        return new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenMinable(state, size,
                s -> s != null && org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(s).is(net.minecraft.world.level.block.Blocks.NETHERRACK));
    }

    private static legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenHellLava hellLava(boolean insideRock) {
        var lava = ChunkGeneratorOverworld.block("flowing_lava");
        if (lava == null) lava = ChunkGeneratorOverworld.block("lava");
        return lava == null ? null : new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenHellLava(lava, insideRock);
    }

    private static WorldGenerator patch(net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> key) {
        return new WorldGenerator() {
            @Override
            public boolean generate(World w, Random r, BlockPos p) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(w, key, r, p);
            }
        };
    }

    /** A mushroom patch as 1.12's {@code WorldGenBush} (the modern game's mushroom patch feature is placed). */
    private static legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBush bush(net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> key) {
        return new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBush(null) {
            @Override
            public boolean generate(World w, Random r, BlockPos p) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(w, key, r, p);
            }
        };
    }

    public ChunkGeneratorHell(World worldIn, boolean generateStructuresIn, long seed) {
        this.world = worldIn;
        this.generateStructures = generateStructuresIn;
        this.rand = new Random(seed);
        this.lperlinNoise1 = new NoiseGeneratorOctaves(this.rand, 16);
        this.lperlinNoise2 = new NoiseGeneratorOctaves(this.rand, 16);
        this.perlinNoise1 = new NoiseGeneratorOctaves(this.rand, 8);
        this.slowsandGravelNoiseGen = new NoiseGeneratorOctaves(this.rand, 4);
        this.netherrackExculsivityNoiseGen = new NoiseGeneratorOctaves(this.rand, 4);
        this.scaleNoise = new NoiseGeneratorOctaves(this.rand, 10);
        this.depthNoise = new NoiseGeneratorOctaves(this.rand, 16);
        if (worldIn != null) worldIn.setSeaLevel(63);
        try {
            var ctx = new InitNoiseGensEvent.ContextHell(lperlinNoise1, lperlinNoise2, perlinNoise1, slowsandGravelNoiseGen, netherrackExculsivityNoiseGen,
                    scaleNoise, depthNoise);
            ctx = TerrainGen.getModdedNoiseGenerators(worldIn, this.rand, ctx);
            this.lperlinNoise1 = ctx.getLPerlin1();
            this.lperlinNoise2 = ctx.getLPerlin2();
            this.perlinNoise1 = ctx.getPerlin();
            this.slowsandGravelNoiseGen = ctx.getPerlin2();
            this.netherrackExculsivityNoiseGen = ctx.getPerlin3();
            this.scaleNoise = ctx.getScale();
            this.depthNoise = ctx.getDepth();
            this.genNetherBridge = (legacyconnector.v1_12_2.net.minecraft.world.gen.structure.MapGenNetherBridge) TerrainGen.getModdedMapGen(genNetherBridge,
                    InitMapGenEvent.EventType.NETHER_BRIDGE);
            this.genNetherCaves = TerrainGen.getModdedMapGen(genNetherCaves, InitMapGenEvent.EventType.NETHER_CAVE);
        } catch (RuntimeException | LinkageError e) {
            // vanilla's generators
        }
    }

    /** Netherrack where the density is positive, lava below y=32 elsewhere. */
    public void prepareHeights(int x, int z, ChunkPrimer primer) {
        int sx = 5, sy = 17, sz = 5;
        int sea = 32;
        this.buffer = this.getHeights(this.buffer, x * 4, 0, z * 4, sx, sy, sz);
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                for (int k = 0; k < 16; ++k) {
                    double d1 = this.buffer[(i * sz + j) * sy + k];
                    double d2 = this.buffer[(i * sz + j + 1) * sy + k];
                    double d3 = this.buffer[((i + 1) * sz + j) * sy + k];
                    double d4 = this.buffer[((i + 1) * sz + j + 1) * sy + k];
                    double d5 = (this.buffer[(i * sz + j) * sy + k + 1] - d1) * 0.125D;
                    double d6 = (this.buffer[(i * sz + j + 1) * sy + k + 1] - d2) * 0.125D;
                    double d7 = (this.buffer[((i + 1) * sz + j) * sy + k + 1] - d3) * 0.125D;
                    double d8 = (this.buffer[((i + 1) * sz + j + 1) * sy + k + 1] - d4) * 0.125D;
                    for (int l = 0; l < 8; ++l) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;
                        for (int m = 0; m < 4; ++m) {
                            double d15 = d10;
                            double d16 = (d11 - d10) * 0.25D;
                            for (int n = 0; n < 4; ++n) {
                                int y = k * 8 + l;
                                IBlockState state = null;
                                if (y < sea) state = LAVA;
                                if (d15 > 0.0D) state = NETHERRACK;
                                primer.setBlockState(m + i * 4, y, n + j * 4, state);
                                d15 += d16;
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

    /** Soul sand and gravel near y=64, lava filling the low surface, bedrock floor and ceiling. */
    public void buildSurfaces(int x, int z, ChunkPrimer primer) {
        var event = new ChunkGeneratorEvent.ReplaceBiomeBlocks(this, x, z, primer, this.world);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return;
        int surface = 64;
        double scale = 0.03125D;
        this.slowsandNoise = this.slowsandGravelNoiseGen.generateNoiseOctaves(this.slowsandNoise, x * 16, z * 16, 0, 16, 16, 1, scale, scale, 1.0D);
        this.gravelNoise = this.slowsandGravelNoiseGen.generateNoiseOctaves(this.gravelNoise, x * 16, 109, z * 16, 16, 1, 16, scale, 1.0D, scale);
        this.depthBuffer = this.netherrackExculsivityNoiseGen.generateNoiseOctaves(this.depthBuffer, x * 16, z * 16, 0, 16, 16, 1, scale * 2.0D,
                scale * 2.0D, scale * 2.0D);
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                boolean soul = this.slowsandNoise[j + k * 16] + this.rand.nextDouble() * 0.2D > 0.0D;
                boolean gravel = this.gravelNoise[j + k * 16] + this.rand.nextDouble() * 0.2D > 0.0D;
                int depth = (int) (this.depthBuffer[j + k * 16] / 3.0D + 3.0D + this.rand.nextDouble() * 0.25D);
                int run = -1;
                IBlockState top = NETHERRACK;
                IBlockState filler = NETHERRACK;
                for (int y = 127; y >= 0; --y) {
                    if (y < 127 - this.rand.nextInt(5) && y > this.rand.nextInt(5)) {
                        IBlockState here = primer.getBlockState(k, y, j);
                        var m = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(here);
                        if (!m.isAir()) {
                            if (m.is(net.minecraft.world.level.block.Blocks.NETHERRACK)) {
                                if (run == -1) {
                                    if (depth <= 0) {
                                        top = AIR;
                                        filler = NETHERRACK;
                                    } else if (y >= surface - 4 && y <= surface + 1) {
                                        top = NETHERRACK;
                                        filler = NETHERRACK;
                                        if (gravel) {
                                            top = GRAVEL;
                                            filler = NETHERRACK;
                                        }
                                        if (soul) {
                                            top = SOUL_SAND;
                                            filler = SOUL_SAND;
                                        }
                                    }
                                    if (y < surface && (top == null || top == AIR)) top = LAVA;
                                    run = depth;
                                    primer.setBlockState(k, y, j, y >= surface - 1 ? top : filler);
                                } else if (run > 0) {
                                    --run;
                                    primer.setBlockState(k, y, j, filler);
                                }
                            }
                        } else {
                            run = -1;
                        }
                    } else {
                        primer.setBlockState(k, y, j, BEDROCK);
                    }
                }
            }
        }
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        this.rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
        ChunkPrimer primer = new ChunkPrimer();
        try {
            this.prepareHeights(x, z, primer);
            this.buildSurfaces(x, z, primer);
            this.genNetherCaves.generate(this.world, x, z, primer);
        } catch (RuntimeException | LinkageError e) {
            ChunkGeneratorOverworld.mcc$failed("generate (nether)", x, z, e);
        }
        Chunk chunk = new Chunk(this.world, primer, x, z);
        try {
            Biome[] biomes = this.world.getBiomeProvider().getBiomes(null, x * 16, z * 16, 16, 16);
            byte[] ids = chunk.getBiomeArray();
            for (int i = 0; i < ids.length && i < biomes.length; ++i) ids[i] = (byte) Biome.getIdForBiome(biomes[i]);
        } catch (RuntimeException | LinkageError e) {
            // biome ids stay unset
        }
        chunk.resetRelightChecks();
        return chunk;
    }

    /** The density noise of the nether: two blended octave noises, closed towards the floor and the ceiling. */
    private double[] getHeights(double[] field, int x, int y, int z, int sx, int sy, int sz) {
        var event = new ChunkGeneratorEvent.InitNoiseField(this, field, x, y, z, sx, sy, sz);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return event.getNoisefield();
        if (field == null) field = new double[sx * sy * sz];
        double horizontal = 684.412D;
        double vertical = 2053.236D;
        this.noiseData4 = this.scaleNoise.generateNoiseOctaves(this.noiseData4, x, y, z, sx, 1, sz, 1.0D, 0.0D, 1.0D);
        this.dr = this.depthNoise.generateNoiseOctaves(this.dr, x, y, z, sx, 1, sz, 100.0D, 0.0D, 100.0D);
        this.pnr = this.perlinNoise1.generateNoiseOctaves(this.pnr, x, y, z, sx, sy, sz, horizontal / 80.0D, vertical / 60.0D, horizontal / 80.0D);
        this.ar = this.lperlinNoise1.generateNoiseOctaves(this.ar, x, y, z, sx, sy, sz, horizontal, vertical, horizontal);
        this.br = this.lperlinNoise2.generateNoiseOctaves(this.br, x, y, z, sx, sy, sz, horizontal, vertical, horizontal);
        double[] falloff = new double[sy];
        for (int j = 0; j < sy; ++j) {
            falloff[j] = Math.cos((double) j * Math.PI * 6.0D / (double) sy) * 2.0D;
            double d = j;
            if (j > sy / 2) d = sy - 1 - j;
            if (d < 4.0D) {
                d = 4.0D - d;
                falloff[j] -= d * d * d * 10.0D;
            }
        }
        int index = 0;
        for (int i = 0; i < sx; ++i) {
            for (int k = 0; k < sz; ++k) {
                for (int j = 0; j < sy; ++j) {
                    double low = this.ar[index] / 512.0D;
                    double high = this.br[index] / 512.0D;
                    double blend = (this.pnr[index] / 10.0D + 1.0D) / 2.0D;
                    double d = blend < 0.0D ? low : blend > 1.0D ? high : low + (high - low) * blend;
                    d -= falloff[j];
                    if (j > sy - 4) {
                        double t = (double) ((float) (j - (sy - 4)) / 3.0F);
                        d = d * (1.0D - t) + -10.0D * t;
                    }
                    field[index] = d;
                    ++index;
                }
            }
        }
        return field;
    }

    @Override
    public void populate(int x, int z) {
        try {
            mcc$populate(x, z);
        } catch (RuntimeException | LinkageError e) {
            ChunkGeneratorOverworld.mcc$failed("populate (nether)", x, z, e);
        }
    }

    private void mcc$populate(int x, int z) {
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(this, this.world, this.rand, x, z, false));
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        ChunkPos chunkpos = new ChunkPos(x, z);
        if (hellSpringGen != null && TerrainGen.populate(this, this.world, this.rand, x, z, false, PopulateChunkEvent.Populate.EventType.NETHER_LAVA)) {
            for (int k = 0; k < 8; ++k) {
                this.hellSpringGen.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(120) + 4, this.rand.nextInt(16) + 8));
            }
        }
        if (TerrainGen.populate(this, this.world, this.rand, x, z, false, PopulateChunkEvent.Populate.EventType.FIRE)) {
            for (int i1 = 0; i1 < this.rand.nextInt(this.rand.nextInt(10) + 1) + 1; ++i1) {
                this.fireFeature.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(120) + 4, this.rand.nextInt(16) + 8));
            }
        }
        if (TerrainGen.populate(this, this.world, this.rand, x, z, false, PopulateChunkEvent.Populate.EventType.GLOWSTONE)) {
            for (int j1 = 0; j1 < this.rand.nextInt(this.rand.nextInt(10) + 1); ++j1) {
                this.lightGemGen.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(120) + 4, this.rand.nextInt(16) + 8));
            }
            for (int k1 = 0; k1 < 10; ++k1) {
                this.hellPortalGen.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(128), this.rand.nextInt(16) + 8));
            }
        }
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(this.world, this.rand, chunkpos));
        if (TerrainGen.decorate(this.world, this.rand, chunkpos, DecorateBiomeEvent.Decorate.EventType.SHROOM)) {
            if (this.rand.nextBoolean()) {
                this.brownMushroomFeature.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(128), this.rand.nextInt(16) + 8));
            }
            if (this.rand.nextBoolean()) {
                this.redMushroomFeature.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, this.rand.nextInt(128), this.rand.nextInt(16) + 8));
            }
        }
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(this.world, this.rand, blockpos));
        if (quartzGen != null && TerrainGen.generateOre(this.world, this.rand, this.quartzGen, blockpos, OreGenEvent.GenerateMinable.EventType.QUARTZ)) {
            for (int l1 = 0; l1 < 16; ++l1) {
                this.quartzGen.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16), this.rand.nextInt(108) + 10, this.rand.nextInt(16)));
            }
        }
        int sea = this.world.getSeaLevel() / 2 + 1;
        if (magmaGen != null && TerrainGen.populate(this, this.world, this.rand, x, z, false, PopulateChunkEvent.Populate.EventType.NETHER_MAGMA)) {
            for (int l = 0; l < 4; ++l) {
                this.magmaGen.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16), sea - 5 + this.rand.nextInt(10), this.rand.nextInt(16)));
            }
        }
        if (lavaTrapGen != null && TerrainGen.populate(this, this.world, this.rand, x, z, false, PopulateChunkEvent.Populate.EventType.NETHER_LAVA2)) {
            for (int j2 = 0; j2 < 16; ++j2) {
                this.lavaTrapGen.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16), this.rand.nextInt(108) + 10, this.rand.nextInt(16)));
            }
        }
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(this.world, this.rand, blockpos));
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(this.world, this.rand, chunkpos));
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(this, this.world, this.rand, x, z, false));
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
