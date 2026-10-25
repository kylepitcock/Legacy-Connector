package legacyconnector.v1_12_2.net.minecraft.item;

import com.google.common.collect.Sets;

public class ItemSpade extends ItemTool {
    private static final java.util.Set<legacyconnector.v1_12_2.net.minecraft.block.Block> EFFECTIVE_ON = Sets.newHashSet();

    public ItemSpade(ToolMaterial material) {
        super(1.5F, -3.0F, material, EFFECTIVE_ON);
    }
}
