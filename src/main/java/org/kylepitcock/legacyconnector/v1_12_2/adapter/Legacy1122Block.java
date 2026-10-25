package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** The modern block standing for a 1.12.2 block (a single state for now; behaviour is forwarded as the shim grows). */
public class Legacy1122Block extends net.minecraft.world.level.block.Block {
    private final Block legacy;

    public Legacy1122Block(Block legacy, Properties properties) {
        super(properties);
        this.legacy = legacy;
    }

    public Block legacy() {
        return legacy;
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }
}
