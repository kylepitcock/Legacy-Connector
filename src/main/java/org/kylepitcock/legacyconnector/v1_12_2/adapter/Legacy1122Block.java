package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The modern block standing for a 1.12.2 block. A 1.12 block saved its state as 4 metadata bits, so the modern block keeps exactly
 * that: one {@link #META} property, mapped to the legacy state through the block's own {@code getStateFromMeta}/{@code getMetaFromState}.
 */
public class Legacy1122Block extends net.minecraft.world.level.block.Block {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    private final Block legacy;

    public Legacy1122Block(Block legacy, Properties properties) {
        super(properties);
        this.legacy = legacy;
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    public Block legacy() {
        return legacy;
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }
}
