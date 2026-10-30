package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** 1.12.2 book and quill: used, opens the editing screen (the modern writable book's). */
public class ItemWritableBook extends Item {
    public ItemWritableBook() {
        setMaxStackSize(1);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return mcc$useModern(net.minecraft.world.item.Items.WRITABLE_BOOK, worldIn, playerIn, handIn);
    }

    /** Whether a book tag is valid: a list of pages, none longer than 32767 characters. */
    public static boolean isNBTValid(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("pages", 9)) return false;
        NBTTagList pages = nbt.getTagList("pages", 8);
        for (int i = 0; i < pages.tagCount(); ++i) {
            String s = pages.getStringTagAt(i);
            if (s.length() > 32767) return false;
        }
        return true;
    }
}
