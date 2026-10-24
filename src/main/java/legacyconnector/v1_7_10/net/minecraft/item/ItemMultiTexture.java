package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;

/** The item of a block with named subtypes: the metadata picks the name that is appended to the unlocalized name. */
public class ItemMultiTexture extends ItemBlock {
    protected final Block field_150945_b;
    protected final Mapper field_150946_c;

    public interface Mapper {
        String func_150783_a(ItemStack stack);
    }

    public ItemMultiTexture(Block block, Block iconBlock, Mapper mapper) {
        super(block);
        this.field_150945_b = iconBlock;
        this.field_150946_c = mapper;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public ItemMultiTexture(Block block, Block iconBlock, String[] names) {
        this(block, iconBlock, stack -> {
            int meta = stack.getItemDamage();
            return names[Math.max(0, Math.min(meta, names.length - 1))];
        });
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + field_150946_c.func_150783_a(stack);
    }
}
