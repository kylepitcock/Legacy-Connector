package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockGrass}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockGrass extends legacyconnector.v1_7_10.net.minecraft.block.Block implements legacyconnector.v1_7_10.net.minecraft.block.IGrowable {
    public BlockGrass(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockGrass(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockGrass() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.grass);
    }

    @Override
    public boolean canFertilize(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, boolean isClient) {
        return true;
    }

    @Override
    public boolean shouldFertilize(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.Random random, int x, int y, int z) {
        return true;
    }

    @Override
    public void fertilize(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.Random random, int x, int y, int z) {
        // the vanilla block's own bone meal handling runs on the modern block
    }
}
