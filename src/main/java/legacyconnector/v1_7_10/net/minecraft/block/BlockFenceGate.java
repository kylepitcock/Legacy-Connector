package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A gate between fences: metadata bit 4 is open. */
public class BlockFenceGate extends BlockDirectional {
    public BlockFenceGate(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockFenceGate() {
        super(Material.wood);
    }

    public static boolean isFenceGateOpen(int meta) {
        return (meta & 4) != 0;
    }
}
