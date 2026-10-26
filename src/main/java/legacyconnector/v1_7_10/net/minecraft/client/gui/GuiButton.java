package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.components.WidgetSprites;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

public class GuiButton extends Gui {
    protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");
    private static final WidgetSprites SPRITES = new WidgetSprites(net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/button"),
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

    /** Public as FML's access transformer made them (its config entries resize their buttons). */
    public int width;
    public int height;
    public int xPosition;
    public int yPosition;
    public String displayString;
    public int id;
    public boolean enabled = true;
    public boolean visible = true;
    protected boolean hovered;
    public int packedFGColour;

    public GuiButton(int id, int x, int y, String text) {
        this(id, x, y, 200, 20, text);
    }

    public GuiButton(int id, int x, int y, int width, int height, String text) {
        this.id = id;
        this.xPosition = x;
        this.yPosition = y;
        this.width = width;
        this.height = height;
        this.displayString = text;
    }

    /** The button face for a hover state (0 disabled, 1 normal, 2 highlighted), nine-sliced to any size like the modern buttons. */
    public static void mcc$drawBackground(int hoverState, int x, int y, int width, int height) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blitSprite(SPRITES.get(hoverState != 0, hoverState == 2), x, y, width, height);
    }

    public int getHoverState(boolean hovered) {
        return !enabled ? 0 : hovered ? 2 : 1;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        var gui = RenderContext.gui();
        if (gui == null) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        mcc$drawBackground(getHoverState(hovered), xPosition, yPosition, width, height);
        mouseDragged(mc, mouseX, mouseY);
        int color = packedFGColour != 0 ? packedFGColour : !enabled ? 10526880 : hovered ? 16777120 : 14737632;
        drawCenteredString(mc.fontRenderer, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, color);
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
    }

    public void mouseReleased(int mouseX, int mouseY) {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return enabled && visible && mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
    }

    public boolean isMouseOver() {
        return hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY) {
    }

    public void playPressSound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundHandler soundHandler) {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getButtonWidth() {
        return width;
    }

    public int getButtonHeight() {
        return height;
    }
}
