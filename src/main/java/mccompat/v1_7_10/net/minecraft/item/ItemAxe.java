package mccompat.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 axe. Effective blocks and harvest levels come from {@link ItemTool}. */
public class ItemAxe extends ItemTool {
    public ItemAxe(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info, 3.0F);
    }

    protected ItemAxe(Item.ToolMaterial material) {
        super(3.0F, material, java.util.Collections.emptySet());
    }
}
