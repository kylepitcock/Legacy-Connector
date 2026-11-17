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

    /**
     * The pattern lines of a banner's tooltip (at most six, "Pattern Colour" as the language file names them), read from
     * {@code BlockEntityTag.Patterns}; the 1.12 {@code ItemBanner} adds them from its own {@code addInformation}.
     */
    public static void appendHoverTextFromTileEntityTag(ItemStack stack, java.util.List<String> tooltip) {
        NBTTagCompound tag = stack.getSubCompound("BlockEntityTag");
        if (tag == null || !tag.hasKey("Patterns")) return;
        NBTTagList patterns = tag.getTagList("Patterns", 10);
        for (int i = 0; i < patterns.tagCount() && i < 6; i++) {
            NBTTagCompound entry = patterns.getCompoundTagAt(i);
            EnumDyeColor color = EnumDyeColor.byDyeDamage(entry.getInteger("Color") & 15);
            legacyconnector.v1_12_2.net.minecraft.tileentity.BannerPattern pattern =
                    legacyconnector.v1_12_2.net.minecraft.tileentity.BannerPattern.byHash(entry.getString("Pattern"));
            if (pattern != null) {
                tooltip.add(legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(
                        "item.banner." + pattern.getFileName() + "." + color.getUnlocalizedName()));
            }
        }
    }

    @Override
    public void addInformation(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.List<String> tooltip,
            legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flagIn) {
        appendHoverTextFromTileEntityTag(stack, tooltip);
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
