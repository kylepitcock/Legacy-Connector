package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Stands in for 1.20.1's {@code TextAndImageButton} (a button with a label and a small icon cut from a texture sheet; 1.21.1 dropped it). The label is drawn in
 * the left part of the button and the icon in the right part, offset by {@link Builder#offset}; the icon's sheet position moves down by {@code yDiffTex} while the
 * button is hovered. APPROXIMATION: the exact icon placement of 1.20.1 (it centered the icon inside the part reserved next to the label) is reproduced from its
 * documented fields, not from its code.
 */
public class LegacyTextAndImageButton extends Button {
    protected final ResourceLocation resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final int usedTextureWidth;
    protected final int usedTextureHeight;
    protected final int textureWidth;
    protected final int textureHeight;
    protected final int xOffset;
    protected final int yOffset;

    protected LegacyTextAndImageButton(Component message, ResourceLocation texture, int xTexStart, int yTexStart, int yDiffTex, int usedWidth, int usedHeight,
                                       int textureWidth, int textureHeight, int xOffset, int yOffset, Button.OnPress onPress) {
        super(0, 0, 150, 20, message, onPress, DEFAULT_NARRATION);
        this.resourceLocation = texture;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.usedTextureWidth = usedWidth;
        this.usedTextureHeight = usedHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public static Builder builder(Component message, ResourceLocation texture, Button.OnPress onPress) {
        return new Builder(message, texture, onPress);
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        int left = this.getX() + 2;
        int right = this.getX() + this.getWidth() - this.usedTextureWidth - 6;
        renderScrollingString(graphics, font, this.getMessage(), left, this.getY(), right, this.getY() + this.getHeight(), color);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        int x = this.getX() + this.getWidth() - this.usedTextureWidth - 6 + this.xOffset;
        int y = this.getY() + (this.getHeight() - this.usedTextureHeight) / 2 + this.yOffset;
        int v = this.yTexStart + (this.isHoveredOrFocused() ? this.yDiffTex : 0);
        graphics.blit(this.resourceLocation, x, y, (float) this.xTexStart, (float) v, this.usedTextureWidth, this.usedTextureHeight, this.textureWidth, this.textureHeight);
    }

    public static class Builder {
        private final Component message;
        private final ResourceLocation texture;
        private final Button.OnPress onPress;
        private int xTexStart;
        private int yTexStart;
        private int yDiffTex;
        private int usedWidth = 16;
        private int usedHeight = 16;
        private int textureWidth = 256;
        private int textureHeight = 256;
        private int xOffset;
        private int yOffset;

        Builder(Component message, ResourceLocation texture, Button.OnPress onPress) {
            this.message = message;
            this.texture = texture;
            this.onPress = onPress;
        }

        public Builder texStart(int x, int y) {
            this.xTexStart = x;
            this.yTexStart = y;
            return this;
        }

        public Builder offset(int x, int y) {
            this.xOffset = x;
            this.yOffset = y;
            return this;
        }

        public Builder yDiffTex(int diff) {
            this.yDiffTex = diff;
            return this;
        }

        public Builder usedTextureSize(int width, int height) {
            this.usedWidth = width;
            this.usedHeight = height;
            return this;
        }

        public Builder textureSize(int width, int height) {
            this.textureWidth = width;
            this.textureHeight = height;
            return this;
        }

        public LegacyTextAndImageButton build() {
            return new LegacyTextAndImageButton(message, texture, xTexStart, yTexStart, yDiffTex, usedWidth, usedHeight, textureWidth, textureHeight, xOffset, yOffset, onPress);
        }
    }
}
