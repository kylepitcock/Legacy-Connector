package legacyconnector.v1_12_2.net.minecraft.client.gui.advancements;

import legacyconnector.v1_12_2.net.minecraft.client.gui.Gui;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Where an advancement tab sits on the advancements window (above, below, left, right) and how many fit there. The tabs are drawn with
 * the modern advancement tab sprites (the 1.12 tab sheet is gone), at 1.12's positions.
 */
public enum AdvancementTabType {
    ABOVE(0, 0, 28, 32, 8, "above", "left", "middle", "right"),
    BELOW(84, 0, 28, 32, 8, "below", "left", "middle", "right"),
    LEFT(0, 64, 32, 28, 5, "left", "top", "middle", "bottom"),
    RIGHT(96, 64, 32, 28, 5, "right", "top", "middle", "bottom");

    public static final int MAX_TABS;

    static {
        int i = 0;
        for (AdvancementTabType t : values()) i += t.max;
        MAX_TABS = i;
    }

    private final int textureX;
    private final int textureY;
    private final int width;
    private final int height;
    private final int max;
    private final String side;
    private final String first;
    private final String middle;
    private final String last;

    AdvancementTabType(int textureX, int textureY, int width, int height, int max, String side, String first, String middle, String last) {
        this.textureX = textureX;
        this.textureY = textureY;
        this.width = width;
        this.height = height;
        this.max = max;
        this.side = side;
        this.first = first;
        this.middle = middle;
        this.last = last;
    }

    public int getMax() {
        return max;
    }

    public void draw(Gui guiIn, int x, int y, boolean selected, int index) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        String part = index == 0 ? first : index == max - 1 ? last : middle;
        var sprite = net.minecraft.resources.ResourceLocation.withDefaultNamespace("advancements/tab_" + side + "_" + part + (selected ? "_selected" : ""));
        gui.blitSprite(sprite, x + getX(index), y + getY(index), width, height);
    }

    public void drawIcon(int offsetX, int offsetY, int index, RenderItem renderItemIn, ItemStack stack) {
        var gui = RenderContext.gui();
        if (gui == null || stack == null || stack.isEmpty()) return;
        int i = offsetX + getX(index);
        int j = offsetY + getY(index);
        switch (this) {
            case ABOVE -> {
                i += 6;
                j += 9;
            }
            case BELOW -> {
                i += 6;
                j += 6;
            }
            case LEFT -> {
                i += 10;
                j += 5;
            }
            case RIGHT -> {
                i += 6;
                j += 5;
            }
        }
        gui.renderFakeItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack), i, j);
    }

    public int getX(int index) {
        return switch (this) {
            case ABOVE, BELOW -> (width + 4) * index;
            case LEFT -> -width + 4;
            case RIGHT -> 248;
        };
    }

    public int getY(int index) {
        return switch (this) {
            case ABOVE -> -height + 4;
            case BELOW -> 136;
            case LEFT, RIGHT -> height * index;
        };
    }

    public boolean isMouseOver(int offsetX, int offsetY, int index, int mouseX, int mouseY) {
        int i = offsetX + getX(index);
        int j = offsetY + getY(index);
        return mouseX > i && mouseX < i + width && mouseY > j && mouseY < j + height;
    }
}
