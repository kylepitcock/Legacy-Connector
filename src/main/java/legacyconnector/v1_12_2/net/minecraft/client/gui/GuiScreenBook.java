package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/**
 * The book screen for a written or writable book. Displaying it shows the modern book screen for the stack: the reading screen, or the
 * editing screen for a writable book in the player's hand when {@code isUnsigned}.
 */
public class GuiScreenBook extends GuiScreen {
    private final EntityPlayer editingPlayer;
    private final ItemStack book;
    private final boolean bookIsUnsigned;

    public GuiScreenBook(EntityPlayer player, ItemStack bookIn, boolean isUnsigned) {
        this.editingPlayer = player;
        this.book = bookIn;
        this.bookIsUnsigned = isUnsigned;
    }

    /** The modern book screen this one stands for (null when there is no book to show). */
    public net.minecraft.client.gui.screens.Screen mcc$modern() {
        var stack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(book);
        if (stack.isEmpty()) return null;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (bookIsUnsigned && player != null && stack.is(net.minecraft.world.item.Items.WRITABLE_BOOK)) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                var held = player.getItemInHand(hand);
                if (held.is(net.minecraft.world.item.Items.WRITABLE_BOOK)) {
                    return new net.minecraft.client.gui.screens.inventory.BookEditScreen(player, held, hand);
                }
            }
        }
        var access = net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess.fromItem(stack);
        return access == null ? null : new net.minecraft.client.gui.screens.inventory.BookViewScreen(access);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
