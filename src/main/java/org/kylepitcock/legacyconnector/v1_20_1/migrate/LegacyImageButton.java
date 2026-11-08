package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Stands in for 1.20.1's {@code ImageButton} (a region of a texture, drawn with an offset when hovered). 1.21.1's takes sprites from the GUI atlas; this subclass
 * keeps the old constructors and drawing, so mods' screens that draw their buttons from their own texture sheets look the same.
 */
public class LegacyImageButton extends ImageButton {
    private static final WidgetSprites NONE = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/button"), ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

    protected final ResourceLocation resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final int textureWidth;
    protected final int textureHeight;

    public LegacyImageButton(int x, int y, int w, int h, int xTexStart, int yTexStart, ResourceLocation texture, Button.OnPress onPress) {
        this(x, y, w, h, xTexStart, yTexStart, h, texture, 256, 256, onPress);
    }

    public LegacyImageButton(int x, int y, int w, int h, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation texture, Button.OnPress onPress) {
        this(x, y, w, h, xTexStart, yTexStart, yDiffTex, texture, 256, 256, onPress);
    }

    public LegacyImageButton(int x, int y, int w, int h, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation texture, int textureWidth, int textureHeight,
                             Button.OnPress onPress) {
        this(x, y, w, h, xTexStart, yTexStart, yDiffTex, texture, textureWidth, textureHeight, onPress, CommonComponents.EMPTY);
    }

    public LegacyImageButton(int x, int y, int w, int h, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation texture, int textureWidth, int textureHeight,
                             Button.OnPress onPress, Component message) {
        super(x, y, w, h, NONE, onPress, message);
        this.resourceLocation = texture;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int v = this.yTexStart;
        if (!this.isActive()) v += this.yDiffTex * 2;
        else if (this.isHoveredOrFocused()) v += this.yDiffTex;
        graphics.blit(this.resourceLocation, this.getX(), this.getY(), (float) this.xTexStart, (float) v, this.width, this.height, this.textureWidth, this.textureHeight);
    }
}
