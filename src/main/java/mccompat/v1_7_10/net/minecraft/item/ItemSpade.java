package mccompat.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 shovel. Effective blocks and harvest levels come from {@link ItemTool}. */
public class ItemSpade extends ItemTool {
    public ItemSpade(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info, 1.0F);
    }

    protected ItemSpade(Item.ToolMaterial material) {
        super(1.0F, material, java.util.Collections.emptySet());
    }
}
