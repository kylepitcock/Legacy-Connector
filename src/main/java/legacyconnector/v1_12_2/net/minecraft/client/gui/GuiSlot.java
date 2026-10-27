package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's scrolling list (world list, config lists, mods' lists). Clicks, drags and the wheel arrive through {@link #handleMouseInput},
 * which the owning screen calls from its own handleMouseInput as in 1.12 (the event state is the hosting screen's mouse event); the list
 * is drawn through the active {@code GuiGraphics} with the slots clipped to it.
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
    private int scrollUpButtonID;
    private int scrollDownButtonID;
    protected int mouseX;
    protected int mouseY;
    protected boolean centerListVertically = true;
    protected int initialClickY = -2;
    protected float scrollMultiplier;
    protected float amountScrolled;
    protected int selectedElement = -1;
    protected long lastClicked;
    protected boolean visible = true;
    protected boolean showSelectionBox = true;
    protected boolean hasListHeader;
    public int headerPadding;
    private boolean enabled = true;

    public GuiSlot(Minecraft mcIn, int width, int height, int topIn, int bottomIn, int slotHeightIn) {
        this.mc = mcIn;
        this.width = width;
        this.height = height;
        this.top = topIn;
        this.bottom = bottomIn;
        this.slotHeight = slotHeightIn;
        this.left = 0;
        this.right = width;
    }

    public void setDimensions(int widthIn, int heightIn, int topIn, int bottomIn) {
        this.width = widthIn;
        this.height = heightIn;
        this.top = topIn;
        this.bottom = bottomIn;
        this.left = 0;
        this.right = widthIn;
    }

    public void setShowSelectionBox(boolean showSelectionBoxIn) {
        this.showSelectionBox = showSelectionBoxIn;
    }

    protected void setHasListHeader(boolean hasListHeaderIn, int headerPaddingIn) {
        this.hasListHeader = hasListHeaderIn;
        this.headerPadding = hasListHeaderIn ? headerPaddingIn : 0;
    }

    protected abstract int getSize();

    protected abstract void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY);

    protected abstract boolean isSelected(int slotIndex);

    protected int getContentHeight() {
        return getSize() * slotHeight + headerPadding;
    }

    protected abstract void drawBackground();

    protected void updateItemPos(int entryID, int insideLeft, int yPos, float partialTicks) {
    }

    protected abstract void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks);

    protected void clickedHeader(int x, int y) {
    }

    protected void renderDecorations(int mouseXIn, int mouseYIn) {
    }

    public int getSlotIndexFromScreenCoords(int posX, int posY) {
        int i = left + width / 2 - getListWidth() / 2;
        int j = left + width / 2 + getListWidth() / 2;
        int k = posY - top - headerPadding + (int) amountScrolled - 4;
        int l = k / slotHeight;
        return posX < getScrollBarX() && posX >= i && posX <= j && l >= 0 && k >= 0 && l < getSize() ? l : -1;
    }

    public void registerScrollButtons(int scrollUpButtonIDIn, int scrollDownButtonIDIn) {
        this.scrollUpButtonID = scrollUpButtonIDIn;
        this.scrollDownButtonID = scrollDownButtonIDIn;
    }

    protected void bindAmountScrolled() {
        amountScrolled = Math.max(0.0F, Math.min(amountScrolled, (float) getMaxScroll()));
    }

    public int getMaxScroll() {
        return Math.max(0, getContentHeight() - (bottom - top - 4));
    }

    public int getAmountScrolled() {
        return (int) amountScrolled;
    }

    public boolean isMouseYWithinSlotBounds(int y) {
        return y >= top && y <= bottom && mouseX >= left && mouseX <= right;
    }

    public void scrollBy(int amount) {
        amountScrolled += (float) amount;
        bindAmountScrolled();
        initialClickY = -2;
    }

    public void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == scrollUpButtonID) {
            amountScrolled -= (float) (slotHeight * 2 / 3);
            initialClickY = -2;
            bindAmountScrolled();
        } else if (button.id == scrollDownButtonID) {
            amountScrolled += (float) (slotHeight * 2 / 3);
            initialClickY = -2;
            bindAmountScrolled();
        }
    }

    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
        if (!visible) return;
        this.mouseX = mouseXIn;
        this.mouseY = mouseYIn;
        drawBackground();
        int i = getScrollBarX();
        int j = i + 6;
        bindAmountScrolled();
        var gui = RenderContext.gui();
        if (gui == null) return;
        var tessellator = legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.getInstance();
        drawContainerBackground(tessellator);
        int k = left + width / 2 - getListWidth() / 2 + 2;
        int l = top + 4 - (int) amountScrolled;
        gui.enableScissor(left, top, right, bottom);
        try {
            if (hasListHeader) drawListHeader(k, l, tessellator);
            drawSelectionBox(k, l, mouseXIn, mouseYIn, partialTicks);
        } finally {
            gui.disableScissor();
        }
        overlayBackground(0, top, 255, 255);
        overlayBackground(bottom, height, 255, 255);
        gui.fillGradient(left, top, right, top + 4, 0xFF000000, 0x00000000);
        gui.fillGradient(left, bottom - 4, right, bottom, 0x00000000, 0xFF000000);
        int max = getMaxScroll();
        if (max > 0) {
            int thumb = (bottom - top) * (bottom - top) / Math.max(1, getContentHeight());
            thumb = Math.max(32, Math.min(thumb, bottom - top - 8));
            int thumbTop = Math.max(top, (int) amountScrolled * (bottom - top - thumb) / max + top);
            gui.fill(i, top, j, bottom, 0xFF000000);
            gui.fill(i, thumbTop, j, thumbTop + thumb, 0xFF808080);
            gui.fill(i, thumbTop, j - 1, thumbTop + thumb - 1, 0xFFC0C0C0);
        }
        renderDecorations(mouseXIn, mouseYIn);
    }

    private static boolean leftButtonDown() {
        try {
            long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Clicks, drags of the list or its scroll bar, and the wheel, from the mouse event being dispatched. */
    public void handleMouseInput() {
        if (!isMouseYWithinSlotBounds(mouseY)) return;
        if (InputEvent1122.eventButton == 0 && InputEvent1122.eventButtonState && mouseY >= top && mouseY <= bottom) {
            int i = (width - getListWidth()) / 2;
            int j = (width + getListWidth()) / 2;
            int k = mouseY - top - headerPadding + (int) amountScrolled - 4;
            int l = k / slotHeight;
            if (l < getSize() && mouseX >= i && mouseX <= j && l >= 0 && k >= 0) {
                elementClicked(l, false, mouseX, mouseY);
                selectedElement = l;
            } else if (mouseX >= i && mouseX <= j && k < 0) {
                clickedHeader(mouseX - i, mouseY - top + (int) amountScrolled - 4);
            }
        }
        if (leftButtonDown() && getEnabled()) {
            if (initialClickY == -1) {
                boolean drag = true;
                if (mouseY >= top && mouseY <= bottom) {
                    int i = (width - getListWidth()) / 2;
                    int j = (width + getListWidth()) / 2;
                    int k = mouseY - top - headerPadding + (int) amountScrolled - 4;
                    int l = k / slotHeight;
                    if (l < getSize() && mouseX >= i && mouseX <= j && l >= 0 && k >= 0) {
                        boolean doubleClick = l == selectedElement && Minecraft.getSystemTime() - lastClicked < 250L;
                        elementClicked(l, doubleClick, mouseX, mouseY);
                        selectedElement = l;
                        lastClicked = Minecraft.getSystemTime();
                    } else if (mouseX >= i && mouseX <= j && k < 0) {
                        clickedHeader(mouseX - i, mouseY - top + (int) amountScrolled - 4);
                        drag = false;
                    }
                    int bar = getScrollBarX();
                    if (mouseX >= bar && mouseX <= bar + 6) {
                        scrollMultiplier = -1.0F;
                        int max = Math.max(1, getMaxScroll());
                        int thumb = (bottom - top) * (bottom - top) / Math.max(1, getContentHeight());
                        thumb = Math.max(32, Math.min(thumb, bottom - top - 8));
                        scrollMultiplier /= (float) (bottom - top - thumb) / (float) max;
                    } else {
                        scrollMultiplier = 1.0F;
                    }
                    initialClickY = drag ? mouseY : -2;
                } else {
                    initialClickY = -2;
                }
            } else if (initialClickY >= 0) {
                amountScrolled -= (float) (mouseY - initialClickY) * scrollMultiplier;
                initialClickY = mouseY;
            }
        } else {
            initialClickY = -1;
        }
        int wheel = InputEvent1122.eventDWheel;
        if (wheel != 0) {
            amountScrolled += (float) ((wheel > 0 ? -1 : 1) * slotHeight / 2);
            bindAmountScrolled();
        }
    }

    public void setEnabled(boolean enabledIn) {
        this.enabled = enabledIn;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public int getListWidth() {
        return 220;
    }

    protected void drawSelectionBox(int insideLeft, int insideTop, int mouseXIn, int mouseYIn, float partialTicks) {
        int i = getSize();
        for (int j = 0; j < i; ++j) {
            int k = insideTop + j * slotHeight + headerPadding;
            int l = slotHeight - 4;
            if (k > bottom || k + l < top) updateItemPos(j, insideLeft, k, partialTicks);
            if (k + l < top || k > bottom) continue;
            if (showSelectionBox && isSelected(j)) {
                int i1 = left + (width / 2 - getListWidth() / 2);
                int j1 = left + width / 2 + getListWidth() / 2;
                Gui.drawRect(i1, k - 2, j1, k + l + 2, 0xFF808080);
                Gui.drawRect(i1 + 1, k - 1, j1 - 1, k + l + 1, 0xFF000000);
            }
            drawSlot(j, insideLeft, k, l, mouseXIn, mouseYIn, partialTicks);
        }
    }

    protected int getScrollBarX() {
        return width / 2 + 124;
    }

    /** The dirt band 1.12 drew above and below the list, covering slots scrolled out of view. */
    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        tile(left, startY, left + width, endY, 0.0F, 0x404040);
    }

    public void setSlotXBoundsFromLeft(int leftIn) {
        this.left = leftIn;
        this.right = leftIn + width;
    }

    public int getSlotHeight() {
        return slotHeight;
    }

    protected void drawContainerBackground() {
        tile(left, top, right, bottom, (int) amountScrolled, 0x202020);
    }

    /** Forge: the dark dirt behind the entries (lists override it to draw their own background). */
    protected void drawContainerBackground(legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator tessellator) {
        drawContainerBackground();
    }

    /** The header above the first entry when {@link #setHasListHeader} enabled one (scrolls with the list). */
    protected void drawListHeader(int insideLeft, int insideTop, legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator tessellatorIn) {
    }

    private static void tile(int x1, int y1, int x2, int y2, float vScroll, int rgb) {
        var gui = RenderContext.gui();
        if (gui == null || x2 <= x1 || y2 <= y1) return;
        gui.setColor((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F, 1.0F);
        gui.blit(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/block/dirt.png"), x1, y1, x2 - x1, y2 - y1, x1, y1 + vScroll,
                x2 - x1, y2 - y1, 32, 32);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visibleIn) {
        this.visible = visibleIn;
    }
}
