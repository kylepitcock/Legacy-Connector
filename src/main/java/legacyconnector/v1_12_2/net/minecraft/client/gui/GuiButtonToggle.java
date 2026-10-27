package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's two-state image button (the recipe book's filter and tabs): the texture region moves right by {@code xDiffTex} when triggered
 * and down by {@code yDiffTex} when hovered.
 */
public class GuiButtonToggle extends GuiButton {
    protected ResourceLocation resourceLocation;
    protected boolean stateTriggered;
    protected int xTexStart;
    protected int yTexStart;
    protected int xDiffTex;
    protected int yDiffTex;

    public GuiButtonToggle(int buttonId, int xIn, int yIn, int widthIn, int heightIn, boolean buttonText) {
        super(buttonId, xIn, yIn, widthIn, heightIn, "");
        this.stateTriggered = buttonText;
    }

    public void initTextureValues(int xTexStartIn, int yTexStartIn, int xDiffTexIn, int yDiffTexIn, ResourceLocation resourceLocationIn) {
        this.xTexStart = xTexStartIn;
        this.yTexStart = yTexStartIn;
        this.xDiffTex = xDiffTexIn;
        this.yDiffTex = yDiffTexIn;
        this.resourceLocation = resourceLocationIn;
    }

    public void setStateTriggered(boolean triggered) {
        this.stateTriggered = triggered;
    }

    public boolean isStateTriggered() {
        return stateTriggered;
    }

    public void setPosition(int xIn, int yIn) {
        this.x = xIn;
        this.y = yIn;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        if (resourceLocation == null) {
            super.drawButton(mc, mouseX, mouseY, partialTicks);
            return;
        }
        RenderContext.bindTexture(resourceLocation.mcc$modern());
        int u = xTexStart + (stateTriggered ? xDiffTex : 0);
        int v = yTexStart + (hovered ? yDiffTex : 0);
        drawTexturedModalRect(x, y, u, v, width, height);
    }
}
