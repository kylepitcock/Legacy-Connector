package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.world.level.block.Block;

/** The modern item of a 1.12.2 ItemBlock: places its modern block. */
public class Legacy1122BlockItem extends net.minecraft.world.item.BlockItem {
    private final Item legacy;

    public Legacy1122BlockItem(Item legacy, Block block, Properties properties) {
        super(block, properties);
        this.legacy = legacy;
    }

    public Item legacy() {
        return legacy;
    }
}
