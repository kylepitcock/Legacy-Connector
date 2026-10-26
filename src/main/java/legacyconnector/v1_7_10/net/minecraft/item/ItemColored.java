package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** The item of a block tinted by its metadata (tall grass, vines, lily pads): it draws and tints itself as the block does. */
public class ItemColored extends ItemBlock {
    private final Block field_150944_b;
    private String[] field_150945_c;

    public ItemColored(Block block, boolean hasSubtypes) {
        super(block);
        this.field_150944_b = block;
        if (hasSubtypes) {
            setMaxDamage(0);
            setHasSubtypes(true);
        }
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return field_150944_b.getRenderColor(stack.getItemDamage());
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return field_150944_b.getIcon(0, damage);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    /** Names the subtypes: the unlocalized name of damage n gets "." + names[n] appended. */
    public ItemColored func_150943_a(String[] names) {
        this.field_150945_c = names;
        return this;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        if (field_150945_c == null) return super.getUnlocalizedName(stack);
        int damage = stack.getItemDamage();
        return damage >= 0 && damage < field_150945_c.length ? super.getUnlocalizedName(stack) + "." + field_150945_c[damage] : super.getUnlocalizedName(stack);
    }
}
