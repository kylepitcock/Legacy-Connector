package legacyconnector.v1_7_10.net.minecraft.block;

/** The 1.7.10 class {@code net.minecraft.block.BlockSapling}: type hierarchy for casts and instanceof checks (bone meal itself is handled by the modern block). */
public class BlockSapling extends legacyconnector.v1_7_10.net.minecraft.block.BlockBush implements legacyconnector.v1_7_10.net.minecraft.block.IGrowable {
    public BlockSapling(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockSapling() {
        super();
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
    }
}
