package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** 1.12.2 banner item: the static helpers mods use to make banners and read their colours (stored in {@code BlockEntityTag}: {@code Base}, {@code Patterns}). */
public class ItemBanner extends Item {
    public ItemBanner() {
        setMaxStackSize(16);
    }

    /** A banner of the base colour with the given pattern list (compounds of {@code Pattern} and {@code Color}), or none. */
    public static ItemStack makeBanner(EnumDyeColor color, NBTTagList patterns) {
        ItemStack banner = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BANNER, 1, color.getDyeDamage());
        NBTTagCompound tile = banner.getOrCreateSubCompound("BlockEntityTag");
        tile.setInteger("Base", color.getDyeDamage());
        if (patterns != null) tile.setTag("Patterns", patterns.copy());
        return banner;
    }

    /** The banner's base colour (white when the stack names none). */
    public static EnumDyeColor getBaseColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("BlockEntityTag", 10) && tag.getCompoundTag("BlockEntityTag").hasKey("Base", 99)) {
            return EnumDyeColor.byDyeDamage(tag.getCompoundTag("BlockEntityTag").getInteger("Base") & 15);
        }
        return EnumDyeColor.byDyeDamage(stack.getMetadata() & 15);
    }
}
