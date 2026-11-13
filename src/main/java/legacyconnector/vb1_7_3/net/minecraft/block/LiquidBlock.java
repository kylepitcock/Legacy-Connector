package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta's {@code BlockFluids}: the base of water, lava and the fluids mods add (BuildCraft's oil copies the flowing and
 * still blocks and extends this). The metadata is the flow decay: 0 is a source, 1 to 7 flow one block further each, 8 and over
 * is falling fluid. A mod's own flow code reads it through {@link #getLiquidState} and schedules itself with {@link #getTickRate}.
 */
public class LiquidBlock extends Block {
    protected LiquidBlock(int id, Material material) {
        super(id, (material == Material.LAVA ? 14 : 12) * 16 + 13, material);
        setBoundingBox(0, 0, 0, 1, 1, 1);
    }

    @Override public int getColorMultiplier(BlockView view, int x, int y, int z) { return 0xFFFFFF; }

    /** MCP {@code setFluidHeight}: the surface height of a fluid block of this metadata. */
    public static float getFluidHeightFromMeta(int meta) {
        if (meta >= 8) meta = 0;
        return (meta + 1) / 9.0F;
    }

    @Override public int getTexture(int side, int meta) { return side == 0 || side == 1 ? textureId : textureId + 1; }
    @Override public int getTexture(int side) { return side == 0 || side == 1 ? textureId : textureId + 1; }

    /** MCP {@code getFlowDecay}: -1 when the position holds another material. */
    protected int getLiquidState(World world, int x, int y, int z) {
        return world.getMaterial(x, y, z) != material ? -1 : world.getBlockMeta(x, y, z);
    }

    /** MCP {@code getEffectiveFlowDecay}: as above, with falling fluid counted as a source. */
    protected int getLiquidDepth(BlockView view, int x, int y, int z) {
        if (view.getMaterial(x, y, z) != material) return -1;
        int meta = view.getBlockMeta(x, y, z);
        return meta >= 8 ? 0 : meta;
    }

    @Override public boolean isFullCube() { return false; }
    @Override public boolean isOpaque() { return false; }
    @Override public boolean hasCollision(int meta, boolean allowLiquids) { return allowLiquids && meta == 0; }

    public boolean isSolidFace(BlockView view, int x, int y, int z, int side) {
        Material other = view.getMaterial(x, y, z);
        if (other == material) return false;
        if (side == 1) return true;
        if (other == Material.ICE) return false;
        return view.shouldSuffocate(x, y, z);
    }

    @Override public boolean isSideVisible(BlockView view, int x, int y, int z, int side) {
        Material other = view.getMaterial(x, y, z);
        if (other == material) return false;
        if (side == 1) return true;
        if (other == Material.ICE) return false;
        return super.isSideVisible(view, x, y, z, side);
    }

    @Override public Box getCollisionShape(World world, int x, int y, int z) { return null; }
    @Override public int getRenderType() { return 4; }
    @Override public int getDroppedItemId(int meta, Random random) { return 0; }
    @Override public int getDroppedItemCount(Random random) { return 0; }

    /** Water 5 ticks, lava 30, any other fluid every tick (Beta's {@code tickRate}). */
    @Override public int getTickRate() {
        if (material == Material.WATER) return 5;
        if (material == Material.LAVA) return 30;
        return 0;
    }

    @Override public float getLuminance(BlockView view, int x, int y, int z) {
        return Math.max(view.method_1782(x, y, z), view.method_1782(x, y + 1, z));
    }

    @Override public int getRenderLayer() { return material == Material.WATER ? 1 : 0; }

    /** The direction a fluid block flows, for the angle of its flowing texture. */
    private Vec3d flowVector(BlockView view, int x, int y, int z) {
        double vx = 0, vy = 0, vz = 0;
        int here = getLiquidDepth(view, x, y, z);
        for (int i = 0; i < 4; i++) {
            int nx = x, nz = z;
            if (i == 0) nx--;
            if (i == 1) nz--;
            if (i == 2) nx++;
            if (i == 3) nz++;
            int there = getLiquidDepth(view, nx, y, nz);
            if (there < 0) {
                if (!view.getMaterial(nx, y, nz).blocksMovement()) {
                    there = getLiquidDepth(view, nx, y - 1, nz);
                    if (there >= 0) {
                        int d = there - (here - 8);
                        vx += (nx - x) * d;
                        vz += (nz - z) * d;
                    }
                }
            } else {
                int d = there - here;
                vx += (nx - x) * d;
                vz += (nz - z) * d;
            }
        }
        Vec3d vector = Vec3d.create(vx, vy, vz);
        if (view.getBlockMeta(x, y, z) >= 8) {
            boolean walled = isSolidFace(view, x, y, z - 1, 2) || isSolidFace(view, x, y, z + 1, 3) || isSolidFace(view, x - 1, y, z, 4)
                    || isSolidFace(view, x + 1, y, z, 5) || isSolidFace(view, x, y + 1, z - 1, 2) || isSolidFace(view, x, y + 1, z + 1, 3)
                    || isSolidFace(view, x - 1, y + 1, z, 4) || isSolidFace(view, x + 1, y + 1, z, 5);
            if (walled) {
                vector = vector.normalize();
                vector = Vec3d.create(vector.x, vector.y - 6, vector.z);
            }
        }
        return vector.normalize();
    }

    /** MCP {@code func_293_a}: the angle water or lava flows, or -1000 for still fluid and any other material. */
    public static double getFlowingAngle(BlockView view, int x, int y, int z, Material material) {
        if (material != Material.WATER && material != Material.LAVA) return -1000.0D;
        Block block = Block.BLOCKS[material == Material.WATER ? 8 : 10];
        if (!(block instanceof LiquidBlock fluid)) return -1000.0D;
        Vec3d v = fluid.flowVector(view, x, y, z);
        if (v.x == 0.0D && v.z == 0.0D) return -1000.0D;
        return Math.atan2(v.z, v.x) - Math.PI / 2D;
    }

    @Override public void randomDisplayTick(World world, int x, int y, int z, Random random) {}

    @Override public void onPlaced(World world, int x, int y, int z) { checkForHarden(world, x, y, z); }
    @Override public void neighborUpdate(World world, int x, int y, int z, int neighborId) { checkForHarden(world, x, y, z); }

    /** Lava next to water becomes obsidian (a source) or cobblestone. */
    private void checkForHarden(World world, int x, int y, int z) {
        if (world.getBlockId(x, y, z) != id || material != Material.LAVA) return;
        boolean water = world.getMaterial(x, y, z - 1) == Material.WATER || world.getMaterial(x, y, z + 1) == Material.WATER
                || world.getMaterial(x - 1, y, z) == Material.WATER || world.getMaterial(x + 1, y, z) == Material.WATER
                || world.getMaterial(x, y + 1, z) == Material.WATER;
        if (!water) return;
        int meta = world.getBlockMeta(x, y, z);
        if (meta == 0) world.setBlock(x, y, z, 49);
        else if (meta <= 4) world.setBlock(x, y, z, 4);
        fizz(world, x, y, z);
    }

    protected void fizz(World world, int x, int y, int z) {
        world.playSound(x + 0.5F, y + 0.5F, z + 0.5F, "random.fizz", 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
        for (int i = 0; i < 8; i++) world.addParticle("largesmoke", x + Math.random(), y + 1.2D, z + Math.random(), 0.0D, 0.0D, 0.0D);
    }
}
