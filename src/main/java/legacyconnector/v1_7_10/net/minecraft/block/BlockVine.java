package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Vines: climbable, spread over solid blocks. As 1.7.10: the metadata bits are the sides the vine hangs on (1 south, 2 west,
 * 4 north, 8 east; 0 under a block), it has no collision box and its outline is a thin plate on those sides. Mods' vines inherit
 * this (Witchery's Spanish moss, drawn as a full cube while these were missing).
 */
public class BlockVine extends Block {
    public BlockVine(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockVine() {
        super(Material.vine);
        setTickRandomly(true);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float f = 0.0625F;
        int meta = world.getBlockMetadata(x, y, z);
        float x1 = 1.0F, y1 = 1.0F, z1 = 1.0F, x2 = 0.0F, y2 = 0.0F, z2 = 0.0F;
        boolean any = meta > 0;
        if ((meta & 2) != 0) {
            x2 = Math.max(x2, f);
            x1 = 0.0F;
            y1 = 0.0F;
            y2 = 1.0F;
            z1 = 0.0F;
            z2 = 1.0F;
        }
        if ((meta & 8) != 0) {
            x1 = Math.min(x1, 1.0F - f);
            x2 = 1.0F;
            y1 = 0.0F;
            y2 = 1.0F;
            z1 = 0.0F;
            z2 = 1.0F;
        }
        if ((meta & 4) != 0) {
            z2 = Math.max(z2, f);
            z1 = 0.0F;
            x1 = 0.0F;
            x2 = 1.0F;
            y1 = 0.0F;
            y2 = 1.0F;
        }
        if ((meta & 1) != 0) {
            z1 = Math.min(z1, 1.0F - f);
            z2 = 1.0F;
            x1 = 0.0F;
            x2 = 1.0F;
            y1 = 0.0F;
            y2 = 1.0F;
        }
        if (!any) {
            y1 = Math.min(y1, 1.0F - f);
            x1 = 0.0F;
            x2 = 1.0F;
            y2 = 1.0F;
            z1 = 0.0F;
            z2 = 1.0F;
        }
        setBlockBounds(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
        return true;
    }

    /** Hangs on the block it was placed against (clicked face 2 -> bit 1, 3 -> 4, 4 -> 8, 5 -> 2), as 1.7.10. */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        int bits = switch (side) {
            case 2 -> 1;
            case 3 -> 4;
            case 4 -> 8;
            case 5 -> 2;
            default -> 0;
        };
        return bits != 0 ? bits : meta;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        // 1.7.10: against the block behind the clicked face (side 1 tests the block above, side 0 never holds)
        if (side == 0) return false;
        int ox = side == 4 ? 1 : side == 5 ? -1 : 0, oy = side == 1 ? 1 : 0, oz = side == 2 ? 1 : side == 3 ? -1 : 0;
        Block against = world.getBlock(x + ox, y + oy, z + oz);
        return against != null && against.renderAsNormalBlock() && against.getMaterial().blocksMovement();
    }

    @Override
    public int getBlockColor() {
        return legacyconnector.v1_7_10.net.minecraft.world.ColorizerFoliage.getFoliageColorBasic();
    }

    @Override
    public int getRenderColor(int meta) {
        return legacyconnector.v1_7_10.net.minecraft.world.ColorizerFoliage.getFoliageColorBasic();
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return world.mcc$blockTint(x, y, z, 1);
    }
}
