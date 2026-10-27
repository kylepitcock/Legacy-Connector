package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.components.WidgetSprites;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.12.2 button. The face is the modern button sprite, nine-sliced to any size; the label and hover colours are 1.12's. */
public class GuiButton extends Gui {
    protected static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation("textures/gui/widgets.png");
    private static final WidgetSprites SPRITES = new WidgetSprites(net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/button"),
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

    public int width;
    public int height;
    public int x;
    public int y;
    public String displayString;
    public int id;
    public boolean enabled = true;
    public boolean visible = true;
    protected boolean hovered;
    public int packedFGColour;

    public GuiButton(int buttonId, int x, int y, String buttonText) {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this.id = buttonId;
        this.x = x;
        this.y = y;
        this.width = widthIn;
        this.height = heightIn;
        this.displayString = buttonText;
    }

    /** The button face for a hover state (0 disabled, 1 normal, 2 highlighted). */
    public static void mcc$drawBackground(int hoverState, int x, int y, int width, int height) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blitSprite(SPRITES.get(hoverState != 0, hoverState == 2), x, y, width, height);
    }

    protected int getHoverState(boolean mouseOver) {
        return !enabled ? 0 : mouseOver ? 2 : 1;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        mcc$drawBackground(getHoverState(hovered), x, y, width, height);
        mouseDragged(mc, mouseX, mouseY);
        int color = packedFGColour != 0 ? packedFGColour : !enabled ? 10526880 : hovered ? 16777120 : 14737632;
        drawCenteredString(mc == null || mc.fontRenderer == null ? FontRenderer.mcc$INSTANCE : mc.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, color);
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
    }

    public void mouseReleased(int mouseX, int mouseY) {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return enabled && visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public boolean isMouseOver() {
        return hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY) {
    }

    public void playPressSound(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundHandler soundHandler) {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getButtonWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
