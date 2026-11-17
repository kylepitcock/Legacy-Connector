package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A book and quill / written book: right-click opens it. */
public class ItemEditableBook extends Item {
    public ItemEditableBook() {
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.displayGUIBook(stack);
        return stack;
    }

    /** Whether a written book's tag is valid: a title of at most 16 characters, an author and a valid page list. */
    public static boolean validBookTagContents(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (!ItemWritableBook.func_150930_a(tag)) return false;
        if (!tag.hasKey("title", 8)) return false;
        return tag.getString("title").length() <= 16 && tag.hasKey("author", 8);
    }

    @Override
    public boolean getShareTag() {
        return true;
    }
}
