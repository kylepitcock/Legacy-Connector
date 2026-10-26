package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenFire;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenFlowers;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenGlowStone1;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenGlowStone2;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenHellLava;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenMinable;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.MapGenNetherBridge;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.ChunkProviderEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.InitMapGenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.PopulateChunkEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen;

/**
 * Vanilla's nether generator: 128-high chunks of netherrack shaped by 5x17x5 density noise over a lava sea at y=32, soul sand and
 * gravel near y=64, bedrock floor and ceiling, nether caves; populated with lava springs, fire, glowstone, mushrooms and quartz.
 * Dimensions of mods that extend {@code WorldProviderHell} without their own generator get this terrain.
 */
public class ChunkProviderHell implements IChunkProvider {
    private final Random hellRNG;
    private NoiseGeneratorOctaves netherNoiseGen1;
    private NoiseGeneratorOctaves netherNoiseGen2;
    private NoiseGeneratorOctaves netherNoiseGen3;
    private NoiseGeneratorOctaves slowsandGravelNoiseGen;
    private NoiseGeneratorOctaves netherrackExculsivityNoiseGen;
    public NoiseGeneratorOctaves netherNoiseGen6;
    public NoiseGeneratorOctaves netherNoiseGen7;
    private final World worldObj;
    private double[] noiseField;
    public MapGenNetherBridge genNetherBridge = new MapGenNetherBridge();
    private MapGenBase netherCaveGenerator = new MapGenCavesHell();
    private double[] slowsandNoise = new double[256];
    private double[] gravelNoise = new double[256];
    private double[] netherrackExclusivityNoise = new double[256];
    double[] noiseData1;
    double[] noiseData2;
    double[] noiseData3;
    double[] noiseData4;
    double[] noiseData5;

    public ChunkProviderHell(World world, long seed) {
        this.worldObj = world;
        this.hellRNG = new Random(seed);
        this.netherNoiseGen1 = new NoiseGeneratorOctaves(hellRNG, 16);
        this.netherNoiseGen2 = new NoiseGeneratorOctaves(hellRNG, 16);
        this.netherNoiseGen3 = new NoiseGeneratorOctaves(hellRNG, 8);
        this.slowsandGravelNoiseGen = new NoiseGeneratorOctaves(hellRNG, 4);
        this.netherrackExculsivityNoiseGen = new NoiseGeneratorOctaves(hellRNG, 4);
        this.netherNoiseGen6 = new NoiseGeneratorOctaves(hellRNG, 10);
        this.netherNoiseGen7 = new NoiseGeneratorOctaves(hellRNG, 16);
        NoiseGenerator[] gens = {netherNoiseGen1, netherNoiseGen2, netherNoiseGen3, slowsandGravelNoiseGen, netherrackExculsivityNoiseGen, netherNoiseGen6,
                netherNoiseGen7};
        gens = TerrainGen.getModdedNoiseGenerators(world, hellRNG, gens);
        this.netherNoiseGen1 = (NoiseGeneratorOctaves) gens[0];
        this.netherNoiseGen2 = (NoiseGeneratorOctaves) gens[1];
        this.netherNoiseGen3 = (NoiseGeneratorOctaves) gens[2];
        this.slowsandGravelNoiseGen = (NoiseGeneratorOctaves) gens[3];
        this.netherrackExculsivityNoiseGen = (NoiseGeneratorOctaves) gens[4];
        this.netherNoiseGen6 = (NoiseGeneratorOctaves) gens[5];
        this.netherNoiseGen7 = (NoiseGeneratorOctaves) gens[6];
        this.genNetherBridge = TerrainGen.getModdedMapGen(genNetherBridge, InitMapGenEvent.EventType.NETHER_BRIDGE);
        this.netherCaveGenerator = TerrainGen.getModdedMapGen(netherCaveGenerator, InitMapGenEvent.EventType.NETHER_CAVE);
    }

    /** The terrain: netherrack where the density is positive, lava below y=32 elsewhere. */
    public void func_147419_a(int cx, int cz, Block[] blocks) {
        final int cells = 4;
        final int lavaLevel = 32;
        final int sx = cells + 1;
        final int sy = 17;
        final int sz = cells + 1;
        noiseField = initializeNoiseField(noiseField, cx * cells, 0, cz * cells, sx, sy, sz);
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                for (int k = 0; k < 16; k++) {
                    double d1 = noiseField[(i * sz + j) * sy + k];
                    double d2 = noiseField[(i * sz + j + 1) * sy + k];
                    double d3 = noiseField[((i + 1) * sz + j) * sy + k];
                    double d4 = noiseField[((i + 1) * sz + j + 1) * sy + k];
                    double d5 = (noiseField[(i * sz + j) * sy + k + 1] - d1) * 0.125D;
                    double d6 = (noiseField[(i * sz + j + 1) * sy + k + 1] - d2) * 0.125D;
                    double d7 = (noiseField[((i + 1) * sz + j) * sy + k + 1] - d3) * 0.125D;
                    double d8 = (noiseField[((i + 1) * sz + j + 1) * sy + k + 1] - d4) * 0.125D;
                    for (int l = 0; l < 8; l++) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;
                        for (int m = 0; m < 4; m++) {
                            int index = m + i * 4 << 11 | j * 4 << 7 | k * 8 + l;
                            double d15 = d10;
                            double d16 = (d11 - d10) * 0.25D;
                            for (int n = 0; n < 4; n++) {
                                Block block = null;
                                if (k * 8 + l < lavaLevel) block = Blocks.lava;
                                if (d15 > 0.0D) block = Blocks.netherrack;
                                blocks[index] = block;
                                index += 128;
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

    /** The surface: soul sand and gravel near y=64, lava filling the low surface, bedrock at the floor and ceiling. */
    public void func_147418_b(int cx, int cz, Block[] blocks) {
        ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(this, cx, cz, blocks, (BiomeGenBase[]) null);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return;
        final int surface = 64;
        final double scale = 0.03125D;
        slowsandNoise = slowsandGravelNoiseGen.generateNoiseOctaves(slowsandNoise, cx * 16, cz * 16, 0, 16, 16, 1, scale, scale, 1.0D);
        gravelNoise = slowsandGravelNoiseGen.generateNoiseOctaves(gravelNoise, cx * 16, 109, cz * 16, 16, 1, 16, scale, 1.0D, scale);
        netherrackExclusivityNoise = netherrackExculsivityNoiseGen.generateNoiseOctaves(netherrackExclusivityNoise, cx * 16, cz * 16, 0, 16, 16, 1,
                scale * 2.0D, scale * 2.0D, scale * 2.0D);
        for (int k = 0; k < 16; k++) {
            for (int l = 0; l < 16; l++) {
                boolean soulSand = slowsandNoise[k + l * 16] + hellRNG.nextDouble() * 0.2D > 0.0D;
                boolean gravel = gravelNoise[k + l * 16] + hellRNG.nextDouble() * 0.2D > 0.0D;
                int depth = (int) (netherrackExclusivityNoise[k + l * 16] / 3.0D + 3.0D + hellRNG.nextDouble() * 0.25D);
                int run = -1;
                Block top = Blocks.netherrack;
                Block filler = Blocks.netherrack;
                for (int y = 127; y >= 0; y--) {
                    int index = (l * 16 + k) * 128 + y;
                    if (y < 127 - hellRNG.nextInt(5) && y > hellRNG.nextInt(5)) {
                        Block here = blocks[index];
                        if (here != null && here.getMaterial() != Material.air) {
                            if (here != Blocks.netherrack) continue;
                            if (run == -1) {
                                if (depth <= 0) {
                                    top = null;
                                    filler = Blocks.netherrack;
                                } else if (y >= surface - 4 && y <= surface + 1) {
                                    top = Blocks.netherrack;
                                    filler = Blocks.netherrack;
                                    if (gravel) {
                                        top = Blocks.gravel;
                                        filler = Blocks.netherrack;
                                    }
                                    if (soulSand) {
                                        top = Blocks.soul_sand;
                                        filler = Blocks.soul_sand;
                                    }
                                }
                                if (y < surface && (top == null || top.getMaterial() == Material.air)) top = Blocks.lava;
                                run = depth;
                                blocks[index] = y >= surface - 1 ? top : filler;
                            } else if (run > 0) {
                                run--;
                                blocks[index] = filler;
                            }
                        } else {
                            run = -1;
                        }
                    } else {
                        blocks[index] = Blocks.bedrock;
                    }
                }
            }
        }
    }

    public Chunk loadChunk(int cx, int cz) {
        return provideChunk(cx, cz);
    }

    @Override
    public Chunk provideChunk(int cx, int cz) {
        hellRNG.setSeed(cx * 341873128712L + cz * 132897987541L);
        Block[] blocks = new Block[32768];
        func_147419_a(cx, cz, blocks);
        func_147418_b(cx, cz, blocks);
        netherCaveGenerator.generate(this, worldObj, cx, cz, blocks);
        genNetherBridge.generate(this, worldObj, cx, cz, blocks);
        Chunk chunk = new Chunk(worldObj, blocks, cx, cz);
        BiomeGenBase[] biomes = worldObj.getWorldChunkManager().loadBlockGeneratorData(null, cx * 16, cz * 16, 16, 16);
        byte[] ids = chunk.getBiomeArray();
        for (int i = 0; i < ids.length && biomes != null && i < biomes.length; i++) ids[i] = (byte) (biomes[i] == null ? 8 : biomes[i].biomeID);
        chunk.resetRelightChecks();
        return chunk;
    }

    /** The density noise of the nether: two blended octave noises, closed off towards the floor and the ceiling. */
    private double[] initializeNoiseField(double[] field, int x, int y, int z, int sx, int sy, int sz) {
        ChunkProviderEvent.InitNoiseField event = new ChunkProviderEvent.InitNoiseField(this, field, x, y, z, sx, sy, sz);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return event.noisefield;
        if (field == null) field = new double[sx * sy * sz];
        final double horizontal = 684.412D;
        final double vertical = 2053.236D;
        noiseData4 = netherNoiseGen6.generateNoiseOctaves(noiseData4, x, y, z, sx, 1, sz, 1.0D, 0.0D, 1.0D);
        noiseData5 = netherNoiseGen7.generateNoiseOctaves(noiseData5, x, y, z, sx, 1, sz, 100.0D, 0.0D, 100.0D);
        noiseData1 = netherNoiseGen3.generateNoiseOctaves(noiseData1, x, y, z, sx, sy, sz, horizontal / 80.0D, vertical / 60.0D, horizontal / 80.0D);
        noiseData2 = netherNoiseGen1.generateNoiseOctaves(noiseData2, x, y, z, sx, sy, sz, horizontal, vertical, horizontal);
        noiseData3 = netherNoiseGen2.generateNoiseOctaves(noiseData3, x, y, z, sx, sy, sz, horizontal, vertical, horizontal);
        double[] falloff = new double[sy];
        for (int i = 0; i < sy; i++) {
            falloff[i] = Math.cos(i * Math.PI * 6.0D / sy) * 2.0D;
            double d = i;
            if (i > sy / 2) d = sy - 1 - i;
            if (d < 4.0D) {
                d = 4.0D - d;
                falloff[i] -= d * d * d * 10.0D;
            }
        }
        int index = 0;
        for (int i = 0; i < sx; i++) {
            for (int k = 0; k < sz; k++) {
                for (int j = 0; j < sy; j++) {
                    double low = noiseData2[index] / 512.0D;
                    double high = noiseData3[index] / 512.0D;
                    double blend = (noiseData1[index] / 10.0D + 1.0D) / 2.0D;
                    double d = blend < 0.0D ? low : blend > 1.0D ? high : low + (high - low) * blend;
                    d -= falloff[j];
                    if (j > sy - 4) {
                        double t = (float) (j - (sy - 4)) / 3.0F;
                        d = d * (1.0D - t) + -10.0D * t;
                    }
                    field[index] = d;
                    index++;
                }
            }
        }
        return field;
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public void populate(IChunkProvider provider, int cx, int cz) {
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(provider, worldObj, hellRNG, cx, cz, false));
        int x = cx * 16;
        int z = cz * 16;
        genNetherBridge.generateStructuresInChunk(worldObj, hellRNG, cx, cz);
        boolean doGen = TerrainGen.populate(provider, worldObj, hellRNG, cx, cz, false, PopulateChunkEvent.Populate.EventType.NETHER_LAVA);
        for (int i = 0; doGen && i < 8; i++) {
            new WorldGenHellLava(Blocks.flowing_lava, false).generate(worldObj, hellRNG, x + hellRNG.nextInt(16) + 8, hellRNG.nextInt(120) + 4,
                    z + hellRNG.nextInt(16) + 8);
        }
        int count = hellRNG.nextInt(hellRNG.nextInt(10) + 1) + 1;
        doGen = TerrainGen.populate(provider, worldObj, hellRNG, cx, cz, false, PopulateChunkEvent.Populate.EventType.FIRE);
        for (int i = 0; doGen && i < count; i++) {
            new WorldGenFire().generate(worldObj, hellRNG, x + hellRNG.nextInt(16) + 8, hellRNG.nextInt(120) + 4, z + hellRNG.nextInt(16) + 8);
        }
        count = hellRNG.nextInt(hellRNG.nextInt(10) + 1);
        doGen = TerrainGen.populate(provider, worldObj, hellRNG, cx, cz, false, PopulateChunkEvent.Populate.EventType.GLOWSTONE);
        for (int i = 0; doGen && i < count; i++) {
            new WorldGenGlowStone1().generate(worldObj, hellRNG, x + hellRNG.nextInt(16) + 8, hellRNG.nextInt(120) + 4, z + hellRNG.nextInt(16) + 8);
        }
        for (int i = 0; doGen && i < 10; i++) {
            new WorldGenGlowStone2().generate(worldObj, hellRNG, x + hellRNG.nextInt(16) + 8, hellRNG.nextInt(128), z + hellRNG.nextInt(16) + 8);
        }
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(worldObj, hellRNG, x, z));
        doGen = TerrainGen.decorate(worldObj, hellRNG, x, z, DecorateBiomeEvent.Decorate.EventType.SHROOM);
        if (doGen && hellRNG.nextInt(1) == 0) {
            new WorldGenFlowers(Blocks.brown_mushroom).generate(worldObj, hellRNG, x + hellRNG.nextInt(16) + 8, hellRNG.nextInt(128), z + hellRNG.nextInt(16) + 8);
        }
        if (doGen && hellRNG.nextInt(1) == 0) {
            new WorldGenFlowers(Blocks.red_mushroom).generate(worldObj, hellRNG, x + hellRNG.nextInt(16) + 8, hellRNG.nextInt(128), z + hellRNG.nextInt(16) + 8);
        }
        WorldGenMinable quartz = new WorldGenMinable(Blocks.quartz_ore, 13, Blocks.netherrack);
        doGen = TerrainGen.generateOre(worldObj, hellRNG, quartz, x, z, OreGenEvent.GenerateMinable.EventType.QUARTZ);
        for (int i = 0; doGen && i < 16; i++) {
            quartz.generate(worldObj, hellRNG, x + hellRNG.nextInt(16), hellRNG.nextInt(108) + 10, z + hellRNG.nextInt(16));
        }
        for (int i = 0; i < 16; i++) {
            new WorldGenHellLava(Blocks.flowing_lava, true).generate(worldObj, hellRNG, x + hellRNG.nextInt(16), hellRNG.nextInt(108) + 10, z + hellRNG.nextInt(16));
        }
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(worldObj, hellRNG, x, z));
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(provider, worldObj, hellRNG, cx, cz, false));
    }

    public boolean saveChunks(boolean all, Object progress) {
        return true;
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
        return "HellRandomLevelSource";
    }

    /** Fortress mobs inside fortresses (and on their nether brick), else the biome's. */
    public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        if (type == EnumCreatureType.monster) {
            if (genNetherBridge.hasStructureAt(x, y, z)) return genNetherBridge.getSpawnList();
            if (genNetherBridge.func_142038_b(x, y, z) && worldObj.getBlock(x, y - 1, z) == Blocks.nether_brick) return genNetherBridge.getSpawnList();
        }
        BiomeGenBase biome = worldObj.getBiomeGenForCoords(x, z);
        return biome == null ? java.util.List.of() : biome.getSpawnableList(type);
    }

    public legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition func_147416_a(World world, String name, int x, int y, int z) {
        return null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int cx, int cz) {
        genNetherBridge.generate(this, worldObj, cx, cz, null);
    }

    @Override
    public void saveExtraData() {
    }
}
