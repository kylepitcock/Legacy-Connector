package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * The in-game HUD ({@code Minecraft.ingameGUI}); mods mostly reach the chat window through it. The HUD itself is the
 * modern one; mods that subclass this to draw their own overlays (Witchery) get the hotbar slot helper below.
 */
public class GuiIngame extends Gui {
    private final GuiNewChat chat = new GuiNewChat();
    protected final Minecraft mc;
    protected final java.util.Random rand = new java.util.Random();

    public GuiIngame() {
        this(Minecraft.getMinecraft());
    }

    public GuiIngame(Minecraft mc) {
        this.mc = mc;
    }

    public GuiNewChat getChatGUI() {
        return chat;
    }

    public int getUpdateCounter() {
        return net.minecraft.client.Minecraft.getInstance().gui.getGuiTicks();
    }

    public void setRecordPlayingMessage(String message) {
        setRecordPlaying(legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format("record.nowPlaying", message), true);
    }

    /** The action-bar message above the hotbar; {@code isPlaying} cycles its colours as a playing record did. */
    public void setRecordPlaying(String message, boolean isPlaying) {
        net.minecraft.client.Minecraft.getInstance().gui.setOverlayMessage(net.minecraft.network.chat.Component.literal(message), isPlaying);
    }

    /** Draws hotbar slot {@code slot}'s item at (x, y) with its count and damage bar, bobbing while it was just picked up. */
    protected void renderInventorySlot(int slot, int x, int y, float partialTicks) {
        var gui = RenderContext.gui();
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (gui == null || player == null) return;
        var stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) return;
        float pop = stack.getPopTime() - partialTicks;
        if (pop > 0.0F) {
            float s = 1.0F + pop / 5.0F;
            gui.pose().pushPose();
            gui.pose().translate(x + 8, y + 12, 0.0F);
            gui.pose().scale(1.0F / s, (s + 1.0F) / 2.0F, 1.0F);
            gui.pose().translate(-(x + 8), -(y + 12), 0.0F);
        }
        gui.renderItem(player, stack, x, y, slot);
        if (pop > 0.0F) gui.pose().popPose();
        gui.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, stack, x, y);
    }
}
