package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The book and quill: one per stack; right-click asks the player to open it. */
public class ItemWritableBook extends Item {
    public ItemWritableBook(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemWritableBook() {
        super();
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onItemRightClick(stack, world, player);
        player.displayGUIBook(stack);
        return stack;
    }

    /** {@code func_150930_a}: whether the tag holds a valid {@code pages} list (strings of at most 256 characters). */
    public static boolean func_150930_a(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("pages", 9)) return false;
        NBTTagList pages = tag.getTagList("pages", 8);
        for (int i = 0; i < pages.tagCount(); ++i) {
            String page = pages.getStringTagAt(i);
            if (page == null || page.length() > 256) return false;
        }
        return true;
    }
}
