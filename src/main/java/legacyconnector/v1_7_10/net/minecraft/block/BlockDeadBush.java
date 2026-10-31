package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockDeadBush}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockDeadBush extends legacyconnector.v1_7_10.net.minecraft.block.BlockBush implements legacyconnector.v1_7_10.net.minecraftforge.common.IShearable {
    public BlockDeadBush(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDeadBush(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockDeadBush() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.plants);
    }

    /** Forge: shears take the plant itself. */
    @Override
    public boolean isShearable(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> onSheared(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int fortune) {
        java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> ret = new java.util.ArrayList<>();
        ret.add(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(this, 1, 0));
        return ret;
    }
}
