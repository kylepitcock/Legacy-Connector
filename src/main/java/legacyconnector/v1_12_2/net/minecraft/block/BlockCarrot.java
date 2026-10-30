package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.item.Item;

/** 1.12.2 carrots: a crop whose seed and harvest are both carrots. */
public class BlockCarrot extends BlockCrops {
    @Override
    protected Item getSeed() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.CARROT;
    }

    @Override
    protected Item getCrop() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.CARROT;
    }
}
