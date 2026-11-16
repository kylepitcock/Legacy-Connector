package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A trapdoor: metadata bit 4 is open, bit 8 is the top half. */
public class BlockTrapDoor extends Block {
    public BlockTrapDoor(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockTrapDoor(Material material) {
        super(material);
    }

    /** Forge's switch for mods whose trapdoors may hang without a support block; the modern trapdoor never needs one, so it only records the request. */
    public static boolean disableValidation = false;

    public static boolean func_150118_d(int meta) {
        return (meta & 4) != 0;
    }

    public static boolean isTrapdoorOpen(int meta) {
        return (meta & 4) != 0;
    }
}
