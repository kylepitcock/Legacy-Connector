package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;

/**
 * 1.7.10's book screen over a book stack ({@code pages}, {@code title}, {@code author} in its tag; {@code editable} for a writable one). Displaying one opens
 * the modern book reader on the stack's pages (see ClientGui). The modern reader only reads: an editable book shows its pages, and what the
 * player would type goes nowhere (the modern edit screen writes the modern book format to the server, which a legacy book does not have).
 */
public class GuiScreenBook extends GuiScreen {
    private final EntityPlayer editingPlayer;
    private final ItemStack bookObj;
    private final boolean bookIsUnsigned;

    public GuiScreenBook(EntityPlayer player, ItemStack book, boolean unsigned) {
        this.editingPlayer = player;
        this.bookObj = book;
        this.bookIsUnsigned = unsigned;
    }

    /** The modern reader of this book's pages. */
    public net.minecraft.client.gui.screens.inventory.BookViewScreen mcc$modern() {
        List<net.minecraft.network.chat.Component> pages = new ArrayList<>();
        NBTTagCompound tag = bookObj == null ? null : bookObj.getTagCompound();
        if (tag != null && tag.hasKey("pages", 9)) {
            NBTTagList list = tag.getTagList("pages", 8);
            for (int i = 0; i < list.tagCount(); i++) pages.add(net.minecraft.network.chat.Component.literal(list.getStringTagAt(i)));
        }
        if (pages.isEmpty()) pages.add(net.minecraft.network.chat.Component.empty());
        return new net.minecraft.client.gui.screens.inventory.BookViewScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess(pages));
    }
}
