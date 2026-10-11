package mccompat.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 pickaxe. Effective blocks and harvest levels come from {@link ItemTool}. */
public class ItemPickaxe extends ItemTool {
    protected ItemPickaxe(Item.ToolMaterial material) {
        super(1.0F, material, java.util.Collections.emptySet());
    }
}
