package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockLiquid}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockLiquid extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockLiquid(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockLiquid(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public static float getLiquidHeightPercent(int meta) {
        if (meta >= 8) meta = 0;
        return (meta + 1) / 9.0F;
    }

    /** Still and flowing icons. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon[] field_149806_a;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        boolean lava = blockMaterial == legacyconnector.v1_7_10.net.minecraft.block.material.Material.lava;
        field_149806_a = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(lava ? "lava_still" : "water_still"), register.registerIcon(lava ? "lava_flow" : "water_flow")};
    }

    /** The vanilla liquid icons ("water_still", "water_flow", "lava_still", "lava_flow"). */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getLiquidIcon(String name) {
        BlockLiquid liquid = name.startsWith("lava") ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_lava : legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_water;
        int i = name.endsWith("_flow") ? 1 : 0;
        if (liquid != null && liquid.field_149806_a != null && liquid.field_149806_a[i] != null) return liquid.field_149806_a[i];
        return switch (name) {
            case "water_still", "water_flow", "lava_still", "lava_flow" -> mcc$vanillaIcon(name);
            default -> null;
        };
    }

    /** The flow level (0 source .. 7) of this liquid at a position, -1 for another material; falling liquid counts as a source. */
    protected int getEffectiveFlowDecay(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        if (world.getBlock(x, y, z).getMaterial() != blockMaterial) return -1;
        int meta = world.getBlockMetadata(x, y, z);
        return meta >= 8 ? 0 : meta;
    }

    /** The direction the liquid flows in at a position (the downhill gradient of its levels). */
    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 mcc$flowVector(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        legacyconnector.v1_7_10.net.minecraft.util.Vec3 vec = legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        int decay = getEffectiveFlowDecay(world, x, y, z);
        for (int side = 0; side < 4; side++) {
            int nx = x + (side == 0 ? -1 : side == 2 ? 1 : 0);
            int nz = z + (side == 1 ? -1 : side == 3 ? 1 : 0);
            int d = getEffectiveFlowDecay(world, nx, y, nz);
            if (d < 0) {
                if (!world.getBlock(nx, y, nz).getMaterial().blocksMovement()) {
                    d = getEffectiveFlowDecay(world, nx, y - 1, nz);
                    if (d >= 0) {
                        int f = d - (decay - 8);
                        vec = vec.addVector((nx - x) * f, 0.0D, (nz - z) * f);
                    }
                }
            } else {
                int f = d - decay;
                vec = vec.addVector((nx - x) * f, 0.0D, (nz - z) * f);
            }
        }
        if (world.getBlockMetadata(x, y, z) >= 8) {
            boolean wall = mcc$solid(world, x, y, z - 1, 2) || mcc$solid(world, x, y, z + 1, 3) || mcc$solid(world, x - 1, y, z, 4)
                    || mcc$solid(world, x + 1, y, z, 5) || mcc$solid(world, x, y + 1, z - 1, 2) || mcc$solid(world, x, y + 1, z + 1, 3)
                    || mcc$solid(world, x - 1, y + 1, z, 4) || mcc$solid(world, x + 1, y + 1, z, 5);
            if (wall) vec = vec.normalize().addVector(0.0D, -6.0D, 0.0D);
        }
        return vec.normalize();
    }

    private boolean mcc$solid(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        legacyconnector.v1_7_10.net.minecraft.block.material.Material m = world.getBlock(x, y, z).getMaterial();
        if (m == blockMaterial) return false;
        if (side == 1) return true;
        return m != legacyconnector.v1_7_10.net.minecraft.block.material.Material.ice && m.isSolid();
    }

    /** The angle of the flow at a position (for the flowing texture), or -1000 where it does not flow. */
    public static double getFlowDirection(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        BlockLiquid liquid = material == legacyconnector.v1_7_10.net.minecraft.block.material.Material.lava ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_lava : legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_water;
        legacyconnector.v1_7_10.net.minecraft.util.Vec3 vec = liquid.mcc$flowVector(world, x, y, z);
        return vec.xCoord == 0.0D && vec.zCoord == 0.0D ? -1000.0D : Math.atan2(vec.zCoord, vec.xCoord) - Math.PI / 2.0D;
    }
}
