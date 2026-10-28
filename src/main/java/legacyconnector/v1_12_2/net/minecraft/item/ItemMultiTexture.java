package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** A block item with subtypes named by metadata (its translation key gets ".name" of the subtype). */
public class ItemMultiTexture extends ItemBlock {
    protected final Block unused;
    protected final Mapper nameFunction;

    public ItemMultiTexture(Block block, Block block2, Mapper nameFunction) {
        super(block);
        this.unused = block2;
        this.nameFunction = nameFunction;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public ItemMultiTexture(Block block, Block block2, String[] namesByMeta) {
        this(block, block2, stack -> {
            int i = stack.getMetadata();
            if (i < 0 || i >= namesByMeta.length) i = 0;
            return namesByMeta[i];
        });
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + nameFunction.apply(stack);
    }

    /** The subtype name of a stack. */
    public interface Mapper {
        String apply(ItemStack stack);
    }
}
