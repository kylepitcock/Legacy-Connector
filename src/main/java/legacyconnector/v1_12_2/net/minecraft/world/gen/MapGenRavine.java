package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;

/**
 * 1.12.2 ravine carver: one ravine in fifty chunks, a tall narrow worm with lava at its bottom, kept away from water. Legacy chunk
 * generators (mod dimensions) run it next to the cave carver; the overworld's ravines are the modern game's.
 */
public class MapGenRavine extends MapGenBase {
    protected static final IBlockState FLOWING_LAVA = state("flowing_lava", "lava");
    protected static final IBlockState AIR = state("air", "air");
    private final float[] rs = new float[1024];

    private static IBlockState state(String name, String fallback) {
        try {
            var reg = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS;
            var b = reg.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", name));
            if (b == null) b = reg.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", fallback));
            return b == null ? null : b.getDefaultState();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static net.minecraft.world.level.block.state.BlockState modern(IBlockState s) {
        return s == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(s);
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    protected void addTunnel(long seed, int chunkX, int chunkZ, ChunkPrimer primer, double x, double y, double z, float width, float yaw, float pitch,
            int step, int maxStep, double heightRatio) {
        Random random = new Random(seed);
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        if (maxStep <= 0) {
            int r = this.range * 16 - 16;
            maxStep = r - random.nextInt(r / 4);
        }
        boolean single = false;
        if (step == -1) {
            step = maxStep / 2;
            single = true;
        }
        float wall = 1.0F;
        for (int i = 0; i < 256; i++) {
            if (i == 0 || random.nextInt(3) == 0) wall = 1.0F + random.nextFloat() * random.nextFloat();
            rs[i] = wall * wall;
        }
        for (; step < maxStep; step++) {
            double radius = 1.5D + Math.sin(step * (float) Math.PI / maxStep) * width;
            double radiusY = radius * heightRatio;
            radius *= random.nextFloat() * 0.25D + 0.75D;
            radiusY *= random.nextFloat() * 0.25D + 0.75D;
            float cosPitch = (float) Math.cos(pitch);
            float sinPitch = (float) Math.sin(pitch);
            x += Math.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Math.sin(yaw) * cosPitch;
            pitch *= 0.7F;
            pitch += pitchDrift * 0.05F;
            yaw += yawDrift * 0.05F;
            pitchDrift *= 0.8F;
            yawDrift *= 0.5F;
            pitchDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (!single && random.nextInt(4) == 0) continue;
            double dx = x - centerX;
            double dz = z - centerZ;
            double left = maxStep - step;
            double reach = width + 2.0F + 16.0F;
            if (dx * dx + dz * dz - left * left > reach * reach) return;
            if (x < centerX - 16.0D - radius * 2.0D || z < centerZ - 16.0D - radius * 2.0D || x > centerX + 16.0D + radius * 2.0D
                    || z > centerZ + 16.0D + radius * 2.0D) {
                continue;
            }
            int x1 = Math.max(0, floor(x - radius) - chunkX * 16 - 1);
            int x2 = Math.min(16, floor(x + radius) - chunkX * 16 + 1);
            int y1 = Math.max(1, floor(y - radiusY) - 1);
            int y2 = Math.min(248, floor(y + radiusY) + 1);
            int z1 = Math.max(0, floor(z - radius) - chunkZ * 16 - 1);
            int z2 = Math.min(16, floor(z + radius) - chunkZ * 16 + 1);
            boolean water = false;
            for (int bx = x1; !water && bx < x2; bx++) {
                for (int bz = z1; !water && bz < z2; bz++) {
                    for (int by = y2 + 1; !water && by >= y1 - 1; by--) {
                        if (by < 0 || by >= 256) continue;
                        if (isOceanBlock(primer, bx, by, bz, chunkX, chunkZ)) water = true;
                        if (by != y1 - 1 && bx != x1 && bx != x2 - 1 && bz != z1 && bz != z2 - 1) by = y1;
                    }
                }
            }
            if (water) continue;
            for (int bx = x1; bx < x2; bx++) {
                double ex = (bx + chunkX * 16 + 0.5D - x) / radius;
                for (int bz = z1; bz < z2; bz++) {
                    double ez = (bz + chunkZ * 16 + 0.5D - z) / radius;
                    boolean foundTop = false;
                    if (ex * ex + ez * ez < 1.0D) {
                        for (int by = y2; by > y1; by--) {
                            double ey = ((by - 1) + 0.5D - y) / radiusY;
                            if ((ex * ex + ez * ez) * rs[by - 1] + ey * ey / 6.0D < 1.0D) {
                                if (isTopBlock(primer, bx, by, bz, chunkX, chunkZ)) foundTop = true;
                                digBlock(primer, bx, by, bz, chunkX, chunkZ, foundTop);
                            }
                        }
                    }
                }
            }
            if (single) break;
        }
    }

    @Override
    protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer chunkPrimerIn) {
        if (this.rand.nextInt(50) != 0) return;
        double x = chunkX * 16 + this.rand.nextInt(16);
        double y = this.rand.nextInt(this.rand.nextInt(40) + 8) + 20;
        double z = chunkZ * 16 + this.rand.nextInt(16);
        float yaw = this.rand.nextFloat() * ((float) Math.PI * 2F);
        float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
        float width = (this.rand.nextFloat() * 2.0F + this.rand.nextFloat()) * 2.0F;
        this.addTunnel(this.rand.nextLong(), originalX, originalZ, chunkPrimerIn, x, y, z, width, yaw, pitch, 0, 0, 3.0D);
    }

    protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        return modern(data.getBlockState(x, y, z)).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
    }

    private static boolean isExceptionBiome(Biome biome) {
        return biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.BEACH || biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.DESERT
                || biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.MUSHROOM_ISLAND;
    }

    private Biome biomeAt(int x, int z, int chunkX, int chunkZ) {
        return world == null ? legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS : world.getBiome(new BlockPos(x + chunkX * 16, 0, z + chunkZ * 16));
    }

    private boolean isTopBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        Biome biome = biomeAt(x, z, chunkX, chunkZ);
        IBlockState state = data.getBlockState(x, y, z);
        return isExceptionBiome(biome) ? modern(state).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) : state == biome.topBlock;
    }

    /** Carves one block: stone and the biome's surface blocks become air (lava below y=10); dirt under opened grass turns to grass. */
    protected void digBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        Biome biome = biomeAt(x, z, chunkX, chunkZ);
        IBlockState state = data.getBlockState(x, y, z);
        IBlockState top = biome.topBlock;
        IBlockState filler = biome.fillerBlock;
        boolean stone = modern(state).is(net.minecraft.world.level.block.Blocks.STONE);
        if (!stone && state != top && state != filler) return;
        if (y - 1 < 10) {
            data.setBlockState(x, y, z, FLOWING_LAVA);
        } else {
            data.setBlockState(x, y, z, AIR);
            if (foundTop && data.getBlockState(x, y - 1, z) == filler) data.setBlockState(x, y - 1, z, top);
        }
    }
}
