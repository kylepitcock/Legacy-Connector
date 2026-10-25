package legacyconnector.v1_12_2.net.minecraft.item;

import com.google.common.collect.Sets;

public class ItemPickaxe extends ItemTool {
    private static final java.util.Set<legacyconnector.v1_12_2.net.minecraft.block.Block> EFFECTIVE_ON = Sets.newHashSet();

    public ItemPickaxe(ToolMaterial material) {
        super(1.0F, -2.8F, material, EFFECTIVE_ON);
    }
}
