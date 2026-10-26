package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/**
 * Vanilla 1.7.10 stairs: look and properties come from the model block. Metadata bits 0-1 are the facing
 * (0 east, 1 west, 2 south, 3 north), bit 2 upside-down. Rendered with the modern stairs model.
 */
public class BlockStairs extends Block {
    public final Block field_150149_b;
    public final int field_150151_M;

    public BlockStairs(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150149_b = null;
        this.field_150151_M = 0;
    }

    protected BlockStairs(Block model, int modelMeta) {
        super(model == null ? null : model.getMaterial());
        this.field_150149_b = model;
        this.field_150151_M = modelMeta;
        if (model != null) {
            this.setHardness(model.mcc$hardness());
            this.setResistance(model.getExplosionResistance(null) / 3.0F);
            this.setStepSound(model.stepSound);
        }
        this.setLightOpacity(255);
        this.useNeighborBrightness = true;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return field_150149_b == null ? super.getIcon(side, meta) : field_150149_b.getIcon(side, field_150151_M);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 10;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        // icons come from the model block
    }

    public static boolean isBlockStairs(Block block) {
        return block instanceof BlockStairs;
    }

    /** The bounds of the stairs' slab half (upper half when bit 4 is set). */
    public void setBaseBounds(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        if ((world.getBlockMetadata(x, y, z) & 4) != 0) setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F);
        else setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
    }

    private boolean mcc$sameStairs(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int meta) {
        return isBlockStairs(world.getBlock(x, y, z)) && world.getBlockMetadata(x, y, z) == meta;
    }

    /**
     * Sets the bounds of the step (the half-height box on top of the slab), shortened where a neighbouring stair turns an outer
     * corner; false when it was shortened (the shape is then finished by the corner piece).
     */
    public boolean func_150145_f(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        int dir = meta & 3;
        float y1 = 0.5F, y2 = 1.0F;
        if ((meta & 4) != 0) {
            y1 = 0.0F;
            y2 = 0.5F;
        }
        float x1 = 0.0F, x2 = 1.0F, z1 = 0.0F, z2 = 0.5F;
        boolean full = true;
        if (dir == 0) {
            x1 = 0.5F;
            z2 = 1.0F;
            Block b = world.getBlock(x + 1, y, z);
            int m = world.getBlockMetadata(x + 1, y, z);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                int d = m & 3;
                if (d == 3 && !mcc$sameStairs(world, x, y, z + 1, meta)) {
                    z2 = 0.5F;
                    full = false;
                } else if (d == 2 && !mcc$sameStairs(world, x, y, z - 1, meta)) {
                    z1 = 0.5F;
                    full = false;
                }
            }
        } else if (dir == 1) {
            x2 = 0.5F;
            z2 = 1.0F;
            Block b = world.getBlock(x - 1, y, z);
            int m = world.getBlockMetadata(x - 1, y, z);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                int d = m & 3;
                if (d == 3 && !mcc$sameStairs(world, x, y, z + 1, meta)) {
                    z2 = 0.5F;
                    full = false;
                } else if (d == 2 && !mcc$sameStairs(world, x, y, z - 1, meta)) {
                    z1 = 0.5F;
                    full = false;
                }
            }
        } else if (dir == 2) {
            z1 = 0.5F;
            z2 = 1.0F;
            Block b = world.getBlock(x, y, z + 1);
            int m = world.getBlockMetadata(x, y, z + 1);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                int d = m & 3;
                if (d == 1 && !mcc$sameStairs(world, x + 1, y, z, meta)) {
                    x2 = 0.5F;
                    full = false;
                } else if (d == 0 && !mcc$sameStairs(world, x - 1, y, z, meta)) {
                    x1 = 0.5F;
                    full = false;
                }
            }
        } else if (dir == 3) {
            Block b = world.getBlock(x, y, z - 1);
            int m = world.getBlockMetadata(x, y, z - 1);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                int d = m & 3;
                if (d == 1 && !mcc$sameStairs(world, x + 1, y, z, meta)) {
                    x2 = 0.5F;
                    full = false;
                } else if (d == 0 && !mcc$sameStairs(world, x - 1, y, z, meta)) {
                    x1 = 0.5F;
                    full = false;
                }
            }
        }
        setBlockBounds(x1, y1, z1, x2, y2, z2);
        return full;
    }

    /** Sets the bounds of the extra quarter step of an inner corner; false (bounds unchanged) when there is none. */
    public boolean func_150144_g(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        int dir = meta & 3;
        float y1 = 0.5F, y2 = 1.0F;
        if ((meta & 4) != 0) {
            y1 = 0.0F;
            y2 = 0.5F;
        }
        float x1 = 0.0F, x2 = 0.5F, z1 = 0.5F, z2 = 1.0F;
        boolean corner = false;
        if (dir == 0) {
            Block b = world.getBlock(x - 1, y, z);
            int m = world.getBlockMetadata(x - 1, y, z);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                int d = m & 3;
                if (d == 3 && !mcc$sameStairs(world, x, y, z - 1, meta)) {
                    z1 = 0.0F;
                    z2 = 0.5F;
                    corner = true;
                } else if (d == 2 && !mcc$sameStairs(world, x, y, z + 1, meta)) {
                    z1 = 0.5F;
                    z2 = 1.0F;
                    corner = true;
                }
            }
        } else if (dir == 1) {
            Block b = world.getBlock(x + 1, y, z);
            int m = world.getBlockMetadata(x + 1, y, z);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                x1 = 0.5F;
                x2 = 1.0F;
                int d = m & 3;
                if (d == 3 && !mcc$sameStairs(world, x, y, z - 1, meta)) {
                    z1 = 0.0F;
                    z2 = 0.5F;
                    corner = true;
                } else if (d == 2 && !mcc$sameStairs(world, x, y, z + 1, meta)) {
                    z1 = 0.5F;
                    z2 = 1.0F;
                    corner = true;
                }
            }
        } else if (dir == 2) {
            Block b = world.getBlock(x, y, z - 1);
            int m = world.getBlockMetadata(x, y, z - 1);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                z1 = 0.0F;
                z2 = 0.5F;
                int d = m & 3;
                if (d == 1 && !mcc$sameStairs(world, x - 1, y, z, meta)) {
                    corner = true;
                } else if (d == 0 && !mcc$sameStairs(world, x + 1, y, z, meta)) {
                    x1 = 0.5F;
                    x2 = 1.0F;
                    corner = true;
                }
            }
        } else if (dir == 3) {
            Block b = world.getBlock(x, y, z + 1);
            int m = world.getBlockMetadata(x, y, z + 1);
            if (isBlockStairs(b) && (meta & 4) == (m & 4)) {
                int d = m & 3;
                if (d == 1 && !mcc$sameStairs(world, x - 1, y, z, meta)) {
                    corner = true;
                } else if (d == 0 && !mcc$sameStairs(world, x + 1, y, z, meta)) {
                    x1 = 0.5F;
                    x2 = 1.0F;
                    corner = true;
                }
            }
        }
        if (corner) setBlockBounds(x1, y1, z1, x2, y2, z2);
        return corner;
    }
}
