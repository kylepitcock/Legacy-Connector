package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 1.20.1's {@code MelonBlock} (a {@code StemGrownBlock}): 1.21.1 has no such class, the vanilla melon is a plain {@link Block}. Mods build melon-like blocks
 * with {@code new MelonBlock(properties)} or extend it; this stand-in is that block. APPROXIMATION: the vanilla melon is not an instance of it
 * ({@code instanceof MelonBlock} is false for it), and a stem is not looked up from the block ({@code getStem}/{@code getAttachedStem} are the mod's own).
 */
public class LegacyMelonBlock extends Block {
    public LegacyMelonBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(LegacyMelonBlock::new);
    }
}
