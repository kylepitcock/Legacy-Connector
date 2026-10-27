package legacyconnector.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 pickaxe. Effective blocks and harvest levels come from {@link ItemTool}. */
public class ItemPickaxe extends ItemTool {
    /** Vanilla's blocks this tool is effective on; mod subclasses get them, as in 1.7.10. */
    public static final java.util.Set<legacyconnector.v1_7_10.net.minecraft.block.Block> effectiveBlocks = new java.util.HashSet<>(java.util.Arrays.asList(legacyconnector.v1_7_10.net.minecraft.init.Blocks.cobblestone, legacyconnector.v1_7_10.net.minecraft.init.Blocks.double_stone_slab, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone_slab, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sandstone, legacyconnector.v1_7_10.net.minecraft.init.Blocks.mossy_cobblestone, legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_block, legacyconnector.v1_7_10.net.minecraft.init.Blocks.coal_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.gold_block, legacyconnector.v1_7_10.net.minecraft.init.Blocks.gold_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.diamond_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.diamond_block, legacyconnector.v1_7_10.net.minecraft.init.Blocks.ice, legacyconnector.v1_7_10.net.minecraft.init.Blocks.netherrack, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lapis_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lapis_block, legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lit_redstone_ore, legacyconnector.v1_7_10.net.minecraft.init.Blocks.rail, legacyconnector.v1_7_10.net.minecraft.init.Blocks.detector_rail, legacyconnector.v1_7_10.net.minecraft.init.Blocks.golden_rail, legacyconnector.v1_7_10.net.minecraft.init.Blocks.activator_rail));

    public ItemPickaxe(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info, 2.0F);
    }

    protected ItemPickaxe(Item.ToolMaterial material) {
        super(1.0F, material, effectiveBlocks);
    }
}
