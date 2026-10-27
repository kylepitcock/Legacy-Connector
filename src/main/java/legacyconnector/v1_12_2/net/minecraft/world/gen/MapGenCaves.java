package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;

/** 1.12.2 cave carver: worm tunnels and rooms started from the chunks around the one being generated, carving its primer. */
public class MapGenCaves extends MapGenBase {
    protected static final IBlockState BLK_LAVA = state("lava");
    protected static final IBlockState BLK_AIR = state("air");
    protected static final IBlockState BLK_SANDSTONE = state("sandstone");
    protected static final IBlockState BLK_RED_SANDSTONE = state("red_sandstone");

    private static IBlockState state(String name) {
        try {
            var b = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", name));
            return b == null ? null : b.getDefaultState();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static net.minecraft.world.level.block.state.BlockState modern(IBlockState s) {
        return s == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(s);
    }

    protected void addRoom(long seed, int originalX, int originalZ, ChunkPrimer primer, double x, double y, double z) {
        this.addTunnel(seed, originalX, originalZ, primer, x, y, z, 1.0F + this.rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    protected void addTunnel(long seed, int originalX, int originalZ, ChunkPrimer primer, double x, double y, double z, float width, float yaw, float pitch,
            int step, int maxStep, double scaleY) {
        double centerX = originalX * 16 + 8;
        double centerZ = originalZ * 16 + 8;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        Random random = new Random(seed);
        if (maxStep <= 0) {
            int limit = this.range * 16 - 16;
            maxStep = limit - random.nextInt(limit / 4);
        }
        boolean room = false;
        if (step == -1) {
            step = maxStep / 2;
            room = true;
        }
        int branchAt = random.nextInt(maxStep / 2) + maxStep / 4;
        for (boolean steep = random.nextInt(6) == 0; step < maxStep; ++step) {
            double radius = 1.5D + (double) (Math.sin((float) step * (float) Math.PI / (float) maxStep) * width);
            double radiusY = radius * scaleY;
            float cos = (float) Math.cos(pitch);
            float sin = (float) Math.sin(pitch);
            x += Math.cos(yaw) * cos;
            y += sin;
            z += Math.sin(yaw) * cos;
            pitch = steep ? pitch * 0.92F : pitch * 0.7F;
            pitch += pitchDrift * 0.1F;
            yaw += yawDrift * 0.1F;
            pitchDrift *= 0.9F;
            yawDrift *= 0.75F;
            pitchDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (!room && step == branchAt && width > 1.0F && maxStep > 0) {
                this.addTunnel(random.nextLong(), originalX, originalZ, primer, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - ((float) Math.PI / 2F),
                        pitch / 3.0F, step, maxStep, 1.0D);
                this.addTunnel(random.nextLong(), originalX, originalZ, primer, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + ((float) Math.PI / 2F),
                        pitch / 3.0F, step, maxStep, 1.0D);
                return;
            }
            if (room || random.nextInt(4) != 0) {
                double dx = x - centerX;
                double dz = z - centerZ;
                double remaining = maxStep - step;
                double reach = width + 2.0F + 16.0F;
                if (dx * dx + dz * dz - remaining * remaining > reach * reach) return;
                if (x >= centerX - 16.0D - radius * 2.0D && z >= centerZ - 16.0D - radius * 2.0D && x <= centerX + 16.0D + radius * 2.0D
                        && z <= centerZ + 16.0D + radius * 2.0D) {
                    int minX = Math.max(floor(x - radius) - originalX * 16 - 1, 0);
                    int maxX = Math.min(floor(x + radius) - originalX * 16 + 1, 16);
                    int minY = Math.max(floor(y - radiusY) - 1, 1);
                    int maxY = Math.min(floor(y + radiusY) + 1, 248);
                    int minZ = Math.max(floor(z - radius) - originalZ * 16 - 1, 0);
                    int maxZ = Math.min(floor(z + radius) - originalZ * 16 + 1, 16);
                    boolean water = false;
                    for (int bx = minX; !water && bx < maxX; ++bx) {
                        for (int bz = minZ; !water && bz < maxZ; ++bz) {
                            for (int by = maxY + 1; !water && by >= minY - 1; --by) {
                                if (by >= 0 && by < 256) {
                                    if (isOceanBlock(primer, bx, by, bz, originalX, originalZ)) water = true;
                                    if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) by = minY;
                                }
                            }
                        }
                    }
                    if (!water) {
                        for (int bx = minX; bx < maxX; ++bx) {
                            double rx = ((double) (bx + originalX * 16) + 0.5D - x) / radius;
                            for (int bz = minZ; bz < maxZ; ++bz) {
                                double rz = ((double) (bz + originalZ * 16) + 0.5D - z) / radius;
                                boolean foundTop = false;
                                if (rx * rx + rz * rz < 1.0D) {
                                    for (int by = maxY; by > minY; --by) {
                                        double ry = ((double) (by - 1) + 0.5D - y) / radiusY;
                                        if (ry > -0.7D && rx * rx + ry * ry + rz * rz < 1.0D) {
                                            IBlockState here = primer.getBlockState(bx, by, bz);
                                            IBlockState above = primer.getBlockState(bx, by + 1, bz);
                                            if (isTopBlock(primer, bx, by, bz, originalX, originalZ)) foundTop = true;
                                            digBlock(primer, bx, by, bz, originalX, originalZ, foundTop, here, above);
                                        }
                                    }
                                }
                            }
                        }
                        if (room) break;
                    }
                }
            }
        }
    }

    protected boolean canReplaceBlock(IBlockState state, IBlockState above) {
        var m = modern(state);
        var b = m.getBlock();
        if (m.is(net.minecraft.world.level.block.Blocks.STONE) || m.is(net.minecraft.world.level.block.Blocks.DIRT) || m.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || m.is(net.minecraft.world.level.block.Blocks.TERRACOTTA) || b instanceof net.minecraft.world.level.block.StainedGlassBlock
                || m.is(net.minecraft.world.level.block.Blocks.SANDSTONE) || m.is(net.minecraft.world.level.block.Blocks.RED_SANDSTONE)
                || m.is(net.minecraft.world.level.block.Blocks.MYCELIUM) || m.is(net.minecraft.world.level.block.Blocks.SNOW)
                || m.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT) || m.is(net.minecraft.world.level.block.Blocks.PODZOL)
                || m.getTags().anyMatch(t -> t.location().getPath().equals("terracotta"))) {
            return true;
        }
        return (m.is(net.minecraft.world.level.block.Blocks.SAND) || m.is(net.minecraft.world.level.block.Blocks.RED_SAND)
                || m.is(net.minecraft.world.level.block.Blocks.GRAVEL)) && !modern(above).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
    }

    @Override
    protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer chunkPrimerIn) {
        int count = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(15) + 1) + 1);
        if (this.rand.nextInt(7) != 0) count = 0;
        for (int j = 0; j < count; ++j) {
            double x = chunkX * 16 + this.rand.nextInt(16);
            double y = this.rand.nextInt(this.rand.nextInt(120) + 8);
            double z = chunkZ * 16 + this.rand.nextInt(16);
            int tunnels = 1;
            if (this.rand.nextInt(4) == 0) {
                this.addRoom(this.rand.nextLong(), originalX, originalZ, chunkPrimerIn, x, y, z);
                tunnels += this.rand.nextInt(4);
            }
            for (int l = 0; l < tunnels; ++l) {
                float yaw = this.rand.nextFloat() * ((float) Math.PI * 2F);
                float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();
                if (this.rand.nextInt(10) == 0) width *= this.rand.nextFloat() * this.rand.nextFloat() * 3.0F + 1.0F;
                this.addTunnel(this.rand.nextLong(), originalX, originalZ, chunkPrimerIn, x, y, z, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }

    protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        return modern(data.getBlockState(x, y, z)).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
    }

    private boolean isExceptionBiome(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome) {
        return biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.BEACH || biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.DESERT;
    }

    private boolean isTopBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        var biome = world.getBiome(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(x + chunkX * 16, 0, z + chunkZ * 16));
        IBlockState state = data.getBlockState(x, y, z);
        return isExceptionBiome(biome) ? modern(state).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) : state == biome.topBlock;
    }

    /** Carves one block: lava below y=11, else air; dirt under carved grass becomes the biome's top block. */
    protected void digBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop, IBlockState state, IBlockState up) {
        var biome = world.getBiome(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(x + chunkX * 16, 0, z + chunkZ * 16));
        IBlockState top = biome.topBlock;
        IBlockState filler = biome.fillerBlock;
        if (this.canReplaceBlock(state, up) || state == top || state == filler) {
            if (y - 1 < 10) {
                data.setBlockState(x, y, z, BLK_LAVA);
            } else {
                data.setBlockState(x, y, z, BLK_AIR);
                if (foundTop && data.getBlockState(x, y - 1, z) == filler) {
                    data.setBlockState(x, y - 1, z, top);
                }
            }
        }
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
