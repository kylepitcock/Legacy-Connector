package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.org.lwjgl.input.Mouse;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's scrolling list (world list, server list, mod lists, config lists). Clicking and dragging poll the mouse inside
 * {@link #drawScreen} as 1.7.10 did; the wheel comes from the hosting screen's scroll events of this frame
 * ({@link GuiScreen#mcc$frameWheel()}) instead of LWJGL 2's event queue. The Tessellator quads of the original are drawn with the
 * same colours through the active {@code GuiGraphics}; the slots are clipped to the list.
 */
public abstract class GuiSlot {
    protected final Minecraft mc;
    public int width;
    public int height;
    public int top;
    public int bottom;
    public int right;
    public int left;
    public final int slotHeight;
    protected int scrollUpButtonID;
    protected int scrollDownButtonID;
    protected int mouseX;
    protected int mouseY;
    /** Lets a list shorter than its view scroll to a negative offset (centred); false pins it to the top. */
    protected boolean field_148163_i = true;
    protected float initialClickY = -2.0F;
    protected float scrollMultiplier;
    protected float amountScrolled;
    protected int selectedElement = -1;
    protected long lastClicked;
    protected boolean showSelectionBox = true;
    protected boolean hasListHeader;
    public int headerPadding;
    protected boolean enabled = true;

    public GuiSlot(Minecraft mc, int width, int height, int top, int bottom, int slotHeight) {
        this.mc = mc;
        this.width = width;
        this.height = height;
        this.top = top;
        this.bottom = bottom;
        this.slotHeight = slotHeight;
        this.left = 0;
        this.right = width;
    }

    public void setDimensions(int width, int height, int top, int bottom) {
        this.width = width;
        this.height = height;
        this.top = top;
        this.bottom = bottom;
        this.left = 0;
        this.right = width;
    }

    public void setShowSelectionBox(boolean show) {
        this.showSelectionBox = show;
    }

    protected void setHasListHeader(boolean has, int padding) {
        this.hasListHeader = has;
        this.headerPadding = padding;
        if (!has) this.headerPadding = 0;
    }

    protected abstract int getSize();

    protected abstract void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY);

    protected abstract boolean isSelected(int index);

    protected int getContentHeight() {
        return getSize() * slotHeight + headerPadding;
    }

    protected abstract void drawBackground();

    protected abstract void drawSlot(int index, int x, int y, int slotHeight, Tessellator tessellator, int mouseX, int mouseY);

    protected void drawListHeader(int x, int y, Tessellator tessellator) {
    }

    /** The header was clicked at the given position relative to the list. */
    protected void func_148132_a(int x, int y) {
    }

    /** Drawn last, over the list (tooltips). */
    protected void func_148142_b(int mouseX, int mouseY) {
    }

    public int getSlotIndexFromScreenCoords(int x, int y) {
        int minX = left + width / 2 - getListWidth() / 2;
        int maxX = left + width / 2 + getListWidth() / 2;
        int rel = y - top - headerPadding + (int) amountScrolled - 4;
        int index = rel / slotHeight;
        return x < getScrollBarX() && x >= minX && x <= maxX && index >= 0 && rel >= 0 && index < getSize() ? index : -1;
    }

    public void registerScrollButtons(int upId, int downId) {
        this.scrollUpButtonID = upId;
        this.scrollDownButtonID = downId;
    }

    private void bindAmountScrolled() {
        int max = func_148135_f();
        if (max < 0) max /= 2;
        if (!field_148163_i && max < 0) max = 0;
        // a list shorter than the view ends at a negative offset: it is drawn centred
        if (amountScrolled < 0.0F) amountScrolled = 0.0F;
        if (amountScrolled > max) amountScrolled = max;
    }

    /** How far the list can scroll (the content height minus the view). */
    public int func_148135_f() {
        return getContentHeight() - (bottom - top - 4);
    }

    public int getAmountScrolled() {
        return (int) amountScrolled;
    }

    public boolean isMouseYWithinSlotBounds(int y) {
        return y >= top && y <= bottom && mouseX >= left && mouseX <= right;
    }

    public void scrollBy(int amount) {
        amountScrolled += amount;
        bindAmountScrolled();
        initialClickY = -2.0F;
    }

    public void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == scrollUpButtonID) {
            amountScrolled -= slotHeight * 2 / 3;
            initialClickY = -2.0F;
            bindAmountScrolled();
        } else if (button.id == scrollDownButtonID) {
            amountScrolled += slotHeight * 2 / 3;
            initialClickY = -2.0F;
            bindAmountScrolled();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        drawBackground();
        int barLeft = getScrollBarX();
        int barRight = barLeft + 6;
        if (mouseX > left && mouseX < right && mouseY > top && mouseY < bottom) {
            if (Mouse.isButtonDown(0) && getEnabled()) {
                if (initialClickY == -1.0F) {
                    boolean drag = true;
                    if (mouseY >= top && mouseY <= bottom) {
                        int minX = width / 2 - getListWidth() / 2;
                        int maxX = width / 2 + getListWidth() / 2;
                        int rel = mouseY - top - headerPadding + (int) amountScrolled - 4;
                        int index = rel / slotHeight;
                        if (mouseX >= minX && mouseX <= maxX && index >= 0 && rel >= 0 && index < getSize()) {
                            boolean doubleClick = index == selectedElement && Minecraft.getSystemTime() - lastClicked < 250L;
                            elementClicked(index, doubleClick, mouseX, mouseY);
                            selectedElement = index;
                            lastClicked = Minecraft.getSystemTime();
                        } else if (mouseX >= minX && mouseX <= maxX && rel < 0) {
                            func_148132_a(mouseX - minX, mouseY - top + (int) amountScrolled - 4);
                            drag = false;
                        }
                        if (mouseX >= barLeft && mouseX <= barRight) {
                            scrollMultiplier = -1.0F;
                            int max = Math.max(1, func_148135_f());
                            int thumb = thumbHeight();
                            scrollMultiplier /= (float) (bottom - top - thumb) / (float) max;
                        } else {
                            scrollMultiplier = 1.0F;
                        }
                        initialClickY = drag ? mouseY : -2.0F;
                    } else {
                        initialClickY = -2.0F;
                    }
                } else if (initialClickY >= 0.0F) {
                    amountScrolled -= (mouseY - initialClickY) * scrollMultiplier;
                    initialClickY = mouseY;
                }
            } else {
                int wheel = GuiScreen.mcc$frameWheel();
                if (wheel != 0) amountScrolled += (wheel > 0 ? -1 : 1) * slotHeight / 2;
                initialClickY = -1.0F;
            }
        }
        bindAmountScrolled();

        var gui = RenderContext.gui();
        if (gui == null) return;
        Tessellator tessellator = Tessellator.instance;
        Gui.mcc$drawOptionsBackground(left, top, right, bottom, (int) amountScrolled, 0x202020);
        int x = left + width / 2 - getListWidth() / 2 + 2;
        int y = top + 4 - (int) amountScrolled;
        gui.enableScissor(left, top, right, bottom);
        try {
            if (hasListHeader) drawListHeader(x, y, tessellator);
            drawSelectionBox(x, y, mouseX, mouseY);
        } finally {
            gui.disableScissor();
        }
        overlayBackground(0, top);
        overlayBackground(bottom, height);
        // the shadows the list casts under the borders
        gui.fillGradient(left, top, right, top + 4, 0xFF000000, 0x00000000);
        gui.fillGradient(left, bottom - 4, right, bottom, 0x00000000, 0xFF000000);
        int max = func_148135_f();
        if (max > 0) {
            int thumb = thumbHeight();
            int thumbTop = Math.max(top, (int) amountScrolled * (bottom - top - thumb) / max + top);
            gui.fill(barLeft, top, barRight, bottom, 0xFF000000);
            gui.fill(barLeft, thumbTop, barRight, thumbTop + thumb, 0xFF808080);
            gui.fill(barLeft, thumbTop, barRight - 1, thumbTop + thumb - 1, 0xFFC0C0C0);
        }
        func_148142_b(mouseX, mouseY);
    }

    private int thumbHeight() {
        int span = bottom - top;
        int thumb = (int) ((float) (span * span) / (float) Math.max(1, getContentHeight()));
        if (thumb < 32) thumb = 32;
        if (thumb > span - 8) thumb = span - 8;
        return thumb;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public int getListWidth() {
        return 220;
    }

    protected void drawSelectionBox(int x, int y, int mouseX, int mouseY) {
        int size = getSize();
        Tessellator tessellator = Tessellator.instance;
        for (int i = 0; i < size; i++) {
            int slotTop = y + i * slotHeight + headerPadding;
            int h = slotHeight - 4;
            if (slotTop > bottom || slotTop + h < top) continue;
            if (showSelectionBox && isSelected(i)) {
                int minX = left + (width / 2 - getListWidth() / 2);
                int maxX = left + width / 2 + getListWidth() / 2;
                Gui.drawRect(minX, slotTop - 2, maxX, slotTop + h + 2, 0xFF808080);
                Gui.drawRect(minX + 1, slotTop - 1, maxX - 1, slotTop + h + 1, 0xFF000000);
            }
            drawSlot(i, x, slotTop, h, tessellator, mouseX, mouseY);
        }
    }

    protected int getScrollBarX() {
        return width / 2 + 124;
    }

    /** The dirt band 1.7.10 drew over the screen above and below the list (covering slots scrolled out of view). */
    private void overlayBackground(int y1, int y2) {
        Gui.mcc$drawOptionsBackground(left, y1, left + width, y2, 0, 0x404040);
    }

    public void setSlotXBoundsFromLeft(int left) {
        this.left = left;
        this.right = left + width;
    }

    public int getSlotHeight() {
        return slotHeight;
    }
}
