package legacyconnector.v1_12_2.net.minecraft.item;

import com.google.common.collect.Sets;

public class ItemAxe extends ItemTool {
    private static final java.util.Set<legacyconnector.v1_12_2.net.minecraft.block.Block> EFFECTIVE_ON = Sets.newHashSet();

    public ItemAxe(ToolMaterial material) {
        super(6.0F, -3.2F, material, EFFECTIVE_ON);
    }

    protected ItemAxe(ToolMaterial material, float damage, float speed) {
        super(damage, speed, material, EFFECTIVE_ON);
    }
}
