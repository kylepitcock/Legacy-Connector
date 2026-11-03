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

    @Override
    public boolean getShareTag() {
        return true;
    }
}
