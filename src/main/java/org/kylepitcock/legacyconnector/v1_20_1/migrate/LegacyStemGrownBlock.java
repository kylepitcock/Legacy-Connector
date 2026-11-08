package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;

/** Stand-in for 1.20.1's {@code StemGrownBlock} (melons and pumpkins; gone in 1.21.1): a block that names the stems that grow it. */
public abstract class LegacyStemGrownBlock extends Block {
    public LegacyStemGrownBlock(Properties properties) {
        super(properties);
    }

    public abstract StemBlock getStem();

    public abstract AttachedStemBlock getAttachedStem();
}
