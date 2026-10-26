package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** The item of a block with tinted subtypes (vanilla's tall grass, vines): metadata is the subtype, named by {@link #setSubtypeNames}. */
public class ItemColored extends ItemBlock {
    private String[] subtypeNames;

    public ItemColored(Block block, boolean hasSubtypes) {
        super(block);
        if (hasSubtypes) {
            setMaxDamage(0);
            setHasSubtypes(true);
        }
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    public ItemColored setSubtypeNames(String[] names) {
        this.subtypeNames = names;
        return this;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String base = super.getUnlocalizedName(stack);
        if (subtypeNames == null) return base;
        int meta = stack.getMetadata();
        return meta >= 0 && meta < subtypeNames.length ? base + "." + subtypeNames[meta] : base;
    }
}
