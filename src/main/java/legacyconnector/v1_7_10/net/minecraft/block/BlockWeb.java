package legacyconnector.v1_7_10.net.minecraft.block;

/** Cobweb: a cross-shaped, non-solid block that slows entities inside it (the engine applies the slowdown to the vanilla block). */
public class BlockWeb extends Block {
    public BlockWeb(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockWeb() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.web);
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
    public int getRenderType() {
        return 1;
    }

    @Override
    public void onEntityCollidedWithBlock(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        entity.setInWeb();
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return null;
    }
}
