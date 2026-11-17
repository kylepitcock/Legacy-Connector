package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Obsidian: stone that drops exactly one block of itself and maps as black. */
public class BlockObsidian extends BlockStone {
    public BlockObsidian(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockObsidian() {
        super();
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(mcc$vanilla != null ? this : Blocks.obsidian);
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.blackColor;
    }
}
