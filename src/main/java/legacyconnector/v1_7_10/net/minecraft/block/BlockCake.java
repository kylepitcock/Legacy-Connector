package legacyconnector.v1_7_10.net.minecraft.block;

/** Cake: eaten slice by slice (metadata 0-6 slices gone). */
public class BlockCake extends Block {
    public BlockCake(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockCake() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.cake);
        setTickRandomly(true);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && world.getBlock(x, y - 1, z).getMaterial().isSolid();
    }

    @Override
    public boolean onBlockActivated(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!player.canEat(false)) return true;
        player.getFoodStats().addStats(2, 0.1F);
        int meta = world.getBlockMetadata(x, y, z) + 1;
        if (meta >= 6) world.setBlockToAir(x, y, z);
        else world.setBlockMetadataWithNotify(x, y, z, meta, 2);
        return true;
    }
}
