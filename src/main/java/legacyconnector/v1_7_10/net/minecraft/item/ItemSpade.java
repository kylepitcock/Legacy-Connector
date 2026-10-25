package legacyconnector.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 shovel. Effective blocks and harvest levels come from {@link ItemTool}. */
public class ItemSpade extends ItemTool {
    /** Vanilla's blocks this tool is effective on; mod subclasses get them, as in 1.7.10. */
    public static final java.util.Set<legacyconnector.v1_7_10.net.minecraft.block.Block> field_150916_c = new java.util.HashSet<>(java.util.Arrays.asList(legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass, legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sand, legacyconnector.v1_7_10.net.minecraft.init.Blocks.gravel, legacyconnector.v1_7_10.net.minecraft.init.Blocks.snow_layer, legacyconnector.v1_7_10.net.minecraft.init.Blocks.snow, legacyconnector.v1_7_10.net.minecraft.init.Blocks.clay, legacyconnector.v1_7_10.net.minecraft.init.Blocks.farmland, legacyconnector.v1_7_10.net.minecraft.init.Blocks.soul_sand, legacyconnector.v1_7_10.net.minecraft.init.Blocks.mycelium));

    public ItemSpade(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info, 1.0F);
    }

    protected ItemSpade(Item.ToolMaterial material) {
        super(1.0F, material, field_150916_c);
    }
}
