package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The bed: metadata bits 0-1 are the facing, bit 2 occupied, bit 3 the head half. Sleeping itself is the modern bed's;
 * these are the static helpers mods use on beds (theirs and vanilla's).
 */
public class BlockBed extends BlockDirectional {
    /** x/z offset from the foot to the head for each facing. */
    public static final int[][] bedDirections = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};

    public BlockBed(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockBed() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.cloth);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5625F, 1.0F);
    }

    public static boolean isBlockHeadOfBed(int meta) {
        return (meta & 8) != 0;
    }

    public static boolean isBedOccupied(int meta) {
        return (meta & 4) != 0;
    }

    public static void setBedOccupied(World world, int x, int y, int z, boolean occupied) {
        int meta = world.getBlockMetadata(x, y, z);
        meta = occupied ? meta | 4 : meta & ~4;
        world.setBlockMetadataWithNotify(x, y, z, meta, 4);
    }

    /** A free spot next to the bed to stand on after waking ({@code skip} skips that many free spots), or null. */
    public static ChunkCoordinates getSafeExitLocation(World world, int x, int y, int z, int skip) {
        int facing = BlockDirectional.getDirection(world.getBlockMetadata(x, y, z));
        for (int half = 0; half <= 1; ++half) {
            int minX = x - bedDirections[facing][0] * half - 1;
            int minZ = z - bedDirections[facing][1] * half - 1;
            for (int px = minX; px <= minX + 2; ++px) {
                for (int pz = minZ; pz <= minZ + 2; ++pz) {
                    if (World.doesBlockHaveSolidTopSurface(world, px, y - 1, pz) && !world.getBlock(px, y, pz).getMaterial().isOpaque()
                            && !world.getBlock(px, y + 1, pz).getMaterial().isOpaque()) {
                        if (skip <= 0) return new ChunkCoordinates(px, y, pz);
                        --skip;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 14;
    }

    @Override
    public void setBlockBoundsBasedOnState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5625F, 1.0F);
    }

    @Override
    public int getMobilityFlag() {
        return 1;
    }

    /** Forge: a bed of this class counts as a bed. */
    @Override
    public boolean isBed(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase player) {
        return true;
    }
}
