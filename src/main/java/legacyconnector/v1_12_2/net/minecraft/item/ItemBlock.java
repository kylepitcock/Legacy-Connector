package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** The item form of a block; registered under the block's name it becomes the block's modern item. */
public class ItemBlock extends Item {
    protected final Block block;

    public ItemBlock(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String getUnlocalizedName() {
        return block.getUnlocalizedName();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return block.getUnlocalizedName();
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs getCreativeTab() {
        var own = super.getCreativeTab();
        return own != null ? own : block.getCreativeTabToDisplayOn();
    }
}
