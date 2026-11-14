package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Stands in for 1.20.1's {@code Checkbox}: its constructors are package-private in 1.21.1 (a builder replaced them), so a mod class that extends it, or
 * creates one with {@code new Checkbox(x, y, w, h, message, selected[, showLabel])}, could not link. Same behaviour as 1.20.1: a 20x20 box drawn from the
 * vanilla checkbox sprites, an optional label to its right, {@code onPress} toggles {@code selected}.
 */
public class LegacyCheckbox extends AbstractButton {
    private static final ResourceLocation SELECTED_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final ResourceLocation SELECTED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
    private static final ResourceLocation PLAIN = ResourceLocation.withDefaultNamespace("widget/checkbox");

    private boolean selected;
    private final boolean showLabel;

    public LegacyCheckbox(int x, int y, int width, int height, Component message, boolean selected) {
        this(x, y, width, height, message, selected, true);
    }

    public LegacyCheckbox(int x, int y, int width, int height, Component message, boolean selected, boolean showLabel) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.showLabel = showLabel;
    }

    /** 1.21.1's {@code Checkbox.getBoxSize(Font)}; kept so a mod that reads it still links. */
    public static int getBoxSize(Font font) {
        return 17;
    }

    /** 1.20.1's {@code AbstractWidget.setTooltipDelay(int)} (milliseconds); the call rule of the plain widgets does not reach a stand-in class. */
    public void setTooltipDelay(int milliseconds) {
        this.setTooltipDelay(java.time.Duration.ofMillis(milliseconds));
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.checkbox.usage.focused" : "narration.checkbox.usage.hovered"));
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        Font font = mc.font;
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        ResourceLocation sprite = this.selected ? (this.isFocused() ? SELECTED_HIGHLIGHTED : SELECTED) : (this.isFocused() ? HIGHLIGHTED : PLAIN);
        graphics.blitSprite(sprite, this.getX(), this.getY(), 20, 20);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.showLabel) {
            graphics.drawString(font, this.getMessage(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
        }
    }
}
