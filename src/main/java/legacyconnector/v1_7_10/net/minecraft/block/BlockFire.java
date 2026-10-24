package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockFire}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockFire extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockFire(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockFire(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockFire() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.fire);
    }

    private static final java.util.Map<legacyconnector.v1_7_10.net.minecraft.block.Block, int[]> FIRE_INFO = new java.util.concurrent.ConcurrentHashMap<>();

    /** How readily fire spreads onto the block ({@code encouragement}) and how fast it burns away ({@code flammability}). */
    public void setFireInfo(legacyconnector.v1_7_10.net.minecraft.block.Block block, int encouragement, int flammability) {
        if (block == null) return;
        FIRE_INFO.put(block, new int[] {encouragement, flammability});
    }

    public int getEncouragement(legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        int[] info = FIRE_INFO.get(block);
        return info == null ? 0 : info[0];
    }

    public int getFlammability(legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        int[] info = FIRE_INFO.get(block);
        return info == null ? 0 : info[1];
    }
}
