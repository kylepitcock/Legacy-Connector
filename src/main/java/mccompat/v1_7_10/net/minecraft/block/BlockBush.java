package mccompat.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockBush}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockBush extends mccompat.v1_7_10.net.minecraft.block.Block implements mccompat.v1_7_10.net.minecraftforge.common.IPlantable {
    public BlockBush(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockBush(mccompat.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockBush() {
        this(mccompat.v1_7_10.net.minecraft.block.material.Material.plants);
    }

    @Override
    public mccompat.v1_7_10.net.minecraftforge.common.EnumPlantType getPlantType(mccompat.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y,
            int z) {
        return mccompat.v1_7_10.net.minecraftforge.common.EnumPlantType.Plains;
    }

    @Override
    public mccompat.v1_7_10.net.minecraft.block.Block getPlant(mccompat.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return this;
    }

    @Override
    public int getPlantMetadata(mccompat.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }
}
