package legacyconnector.v1_7_10.cpw.mods.fml.client;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.Gui;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.org.lwjgl.input.Mouse;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * FML's scrolling list (the mod list's, and mods' own lists). As in 1.7.10 clicks and drags are polled in {@link #drawScreen};
 * the wheel is the hosting screen's movement of this frame. Slots are clipped to the list.
 */
public abstract class GuiScrollingList {
    protected final Minecraft client;
    protected final int listWidth;
    protected final int listHeight;
    protected final int top;
    protected final int bottom;
    protected final int right;
    protected final int left;
    protected final int slotHeight;
    protected int scrollUpActionId;
    protected int scrollDownActionId;
    protected int mouseX;
    protected int mouseY;
    protected float initialMouseClickY = -2.0F;
    protected float scrollFactor;
    protected float scrollDistance;
    protected int selectedIndex = -1;
    protected long lastClickTime = 0L;
    /** Highlight the selected entry. */
    protected boolean field_25123_p = true;
    /** Has a header of {@link #field_27261_r} pixels. */
    protected boolean field_27262_q;
    protected int field_27261_r;

    public GuiScrollingList(Minecraft client, int width, int height, int top, int bottom, int left, int entryHeight) {
        this.client = client;
        this.listWidth = width;
        this.listHeight = height;
        this.top = top;
        this.bottom = bottom;
        this.slotHeight = entryHeight;
        this.left = left;
        this.right = width + this.left;
    }

    public void func_27258_a(boolean highlightSelected) {
        this.field_25123_p = highlightSelected;
    }

    protected void func_27259_a(boolean hasHeader, int headerHeight) {
        this.field_27262_q = hasHeader;
        this.field_27261_r = hasHeader ? headerHeight : 0;
    }

    protected abstract int getSize();

    protected abstract void elementClicked(int index, boolean doubleClick);

    protected abstract boolean isSelected(int index);

    protected int getContentHeight() {
        return getSize() * slotHeight + field_27261_r;
    }

    protected abstract void drawBackground();

    protected abstract void drawSlot(int index, int right, int top, int height, Tessellator tessellator);

    /** Draws the header. */
    protected void func_27260_a(int right, int top, Tessellator tessellator) {
    }

    /** The header was clicked (position relative to the list). */
    protected void func_27255_a(int x, int y) {
    }

    /** Drawn last, over the list. */
    protected void func_27257_b(int mouseX, int mouseY) {
    }

    /** The entry under a screen position, or -1. */
    public int func_27256_c(int x, int y) {
        int minX = left + 1;
        int maxX = left + listWidth - 7;
        int rel = y - top - field_27261_r + (int) scrollDistance - 4;
        int index = rel / slotHeight;
        return x >= minX && x <= maxX && index >= 0 && rel >= 0 && index < getSize() ? index : -1;
    }

    public void registerScrollButtons(List buttons, int upActionId, int downActionId) {
        this.scrollUpActionId = upActionId;
        this.scrollDownActionId = downActionId;
    }

    private void applyScrollLimits() {
        int max = getContentHeight() - (bottom - top - 4);
        if (max < 0) max /= 2;
        if (scrollDistance < 0.0F) scrollDistance = 0.0F;
        if (scrollDistance > max) scrollDistance = max;
    }

    public void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == scrollUpActionId) {
            scrollDistance -= slotHeight * 2 / 3;
            initialMouseClickY = -2.0F;
            applyScrollLimits();
        } else if (button.id == scrollDownActionId) {
            scrollDistance += slotHeight * 2 / 3;
            initialMouseClickY = -2.0F;
            applyScrollLimits();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        drawBackground();
        int listLength = getSize();
        int scrollBarXStart = left + listWidth - 6;
        int scrollBarXEnd = scrollBarXStart + 6;
        int boxLeft = left;
        int boxRight = scrollBarXStart - 1;
        if (Mouse.isButtonDown(0)) {
            if (initialMouseClickY == -1.0F) {
                boolean drag = true;
                if (mouseY >= top && mouseY <= bottom) {
                    int rel = mouseY - top - field_27261_r + (int) scrollDistance - 4;
                    int index = rel / slotHeight;
                    if (mouseX >= boxLeft && mouseX <= boxRight && index >= 0 && rel >= 0 && index < listLength) {
                        boolean doubleClick = index == selectedIndex && System.currentTimeMillis() - lastClickTime < 250L;
                        elementClicked(index, doubleClick);
                        selectedIndex = index;
                        lastClickTime = System.currentTimeMillis();
                    } else if (mouseX >= boxLeft && mouseX <= boxRight && rel < 0) {
                        func_27255_a(mouseX - boxLeft, mouseY - top + (int) scrollDistance - 4);
                        drag = false;
                    }
                    if (mouseX >= scrollBarXStart && mouseX <= scrollBarXEnd) {
                        scrollFactor = -1.0F;
                        int max = Math.max(1, getContentHeight() - (bottom - top - 4));
                        scrollFactor /= (float) (bottom - top - thumbHeight()) / (float) max;
                    } else {
                        scrollFactor = 1.0F;
                    }
                    initialMouseClickY = drag ? mouseY : -2.0F;
                } else {
                    initialMouseClickY = -2.0F;
                }
            } else if (initialMouseClickY >= 0.0F) {
                scrollDistance -= (mouseY - initialMouseClickY) * scrollFactor;
                initialMouseClickY = mouseY;
            }
        } else {
            int wheel = GuiScreen.mcc$frameWheel();
            if (wheel != 0 && mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
                scrollDistance += (wheel > 0 ? -1 : 1) * slotHeight / 2;
            }
            initialMouseClickY = -1.0F;
        }
        applyScrollLimits();

        var gui = RenderContext.gui();
        if (gui == null) return;
        Tessellator tessellator = Tessellator.instance;
        boolean inWorld = Minecraft.mcc$get$theWorld(client) != null;
        if (inWorld) {
            drawGradientRect(left, top, right, bottom, 0xC0101010, 0xD0101010);
        } else {
            Gui.mcc$drawOptionsBackground(left, top, right, bottom, (int) scrollDistance, 0x202020);
        }
        int baseY = top + 4 - (int) scrollDistance;
        gui.enableScissor(left, top, right, bottom);
        try {
            if (field_27262_q) func_27260_a(boxRight, baseY, tessellator);
            for (int i = 0; i < listLength; i++) {
                int slotTop = baseY + i * slotHeight + field_27261_r;
                int h = slotHeight - 4;
                if (slotTop > bottom || slotTop + h < top) continue;
                if (field_25123_p && isSelected(i)) {
                    Gui.drawRect(boxLeft, slotTop - 2, boxRight, slotTop + h + 2, 0xFF808080);
                    Gui.drawRect(boxLeft + 1, slotTop - 1, boxRight - 1, slotTop + h + 1, 0xFF000000);
                }
                drawSlot(i, boxRight, slotTop, h, tessellator);
            }
        } finally {
            gui.disableScissor();
        }
        if (!inWorld) {
            overlayBackground(0, top);
            overlayBackground(bottom, listHeight);
        }
        gui.fillGradient(left, top, right, top + 4, 0xFF000000, 0x00000000);
        gui.fillGradient(left, bottom - 4, right, bottom, 0x00000000, 0xFF000000);
        int max = getContentHeight() - (bottom - top - 4);
        if (max > 0) {
            int thumb = thumbHeight();
            int thumbTop = Math.max(top, (int) scrollDistance * (bottom - top - thumb) / max + top);
            gui.fill(scrollBarXStart, top, scrollBarXEnd, bottom, 0xFF000000);
            gui.fill(scrollBarXStart, thumbTop, scrollBarXEnd, thumbTop + thumb, 0xFF808080);
            gui.fill(scrollBarXStart, thumbTop, scrollBarXEnd - 1, thumbTop + thumb - 1, 0xFFC0C0C0);
        }
        func_27257_b(mouseX, mouseY);
    }

    private int thumbHeight() {
        int span = bottom - top;
        int thumb = (int) ((float) (span * span) / (float) Math.max(1, getContentHeight()));
        if (thumb < 32) thumb = 32;
        if (thumb > span - 8) thumb = span - 8;
        return thumb;
    }

    /** The dirt band FML drew above and below the list, from the screen's left edge to 30 pixels past the list. */
    private void overlayBackground(int y1, int y2) {
        Gui.mcc$drawOptionsBackground(0, y1, listWidth + 30, y2, 0, 0x404040);
    }

    protected void drawGradientRect(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }
}
