package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.BlockLeaves;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** The item of a leaves block: placed leaves get the no-decay bit (4); named and tinted per wood type. */
public class ItemLeaves extends ItemBlock {
    private final BlockLeaves field_150940_b;

    public ItemLeaves(BlockLeaves block) {
        super(block);
        this.field_150940_b = block;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage | 4;
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return field_150940_b.getIcon(0, damage);
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return field_150940_b.getRenderColor(stack.getItemDamage());
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String[] names = field_150940_b.func_150125_e();
        if (names == null || names.length == 0) return super.getUnlocalizedName();
        int damage = stack.getItemDamage();
        if (damage < 0 || damage >= names.length) damage = 0;
        return super.getUnlocalizedName() + "." + names[damage];
    }
}
