package legacyconnector.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 axe. Effective blocks and harvest levels come from {@link ItemTool}. */
public class ItemAxe extends ItemTool {
    /** Vanilla's blocks this tool is effective on; mod subclasses get them, as in 1.7.10. */
    public static final java.util.Set<legacyconnector.v1_7_10.net.minecraft.block.Block> field_150917_c = new java.util.HashSet<>(java.util.Arrays.asList(legacyconnector.v1_7_10.net.minecraft.init.Blocks.planks, legacyconnector.v1_7_10.net.minecraft.init.Blocks.bookshelf, legacyconnector.v1_7_10.net.minecraft.init.Blocks.log, legacyconnector.v1_7_10.net.minecraft.init.Blocks.log2, legacyconnector.v1_7_10.net.minecraft.init.Blocks.chest, legacyconnector.v1_7_10.net.minecraft.init.Blocks.pumpkin, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lit_pumpkin));
    /** 1.4.7-1.6.4: the same blocks as an array (CoFHCore's advanced tools iterate it in a static initializer). */
    public static final legacyconnector.v1_7_10.net.minecraft.block.Block[] blocksEffectiveAgainst = field_150917_c.toArray(new legacyconnector.v1_7_10.net.minecraft.block.Block[0]);

    public ItemAxe(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info, 3.0F);
    }

    protected ItemAxe(Item.ToolMaterial material) {
        super(3.0F, material, field_150917_c);
    }
}
