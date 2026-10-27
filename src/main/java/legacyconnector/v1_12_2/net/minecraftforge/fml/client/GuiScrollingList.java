package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import java.io.IOException;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.Gui;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Forge 1.12.2's scrolling list (the mod list's, and mods' own lists). As in Forge, clicks and drags are polled in {@link #drawScreen}
 * while the left button is held; the wheel arrives through {@link #handleMouseInput} from the screen's mouse event. Entries are clipped
 * to the list.
 */
public abstract class GuiScrollingList {
    private final Minecraft client;
    protected final int listWidth;
    protected final int listHeight;
    protected final int screenWidth;
    protected final int screenHeight;
    protected final int top;
    protected final int bottom;
    protected final int right;
    protected final int left;
    protected final int slotHeight;
    private int scrollUpActionId;
    private int scrollDownActionId;
    protected int mouseX;
    protected int mouseY;
    private float initialMouseClickY = -2.0F;
    private float scrollFactor;
    private float scrollDistance;
    protected int selectedIndex = -1;
    private long lastClickTime = 0L;
    private boolean highlightSelected = true;
    private boolean hasHeader;
    private int headerHeight;
    protected boolean captureMouse = true;

    @Deprecated
    public GuiScrollingList(Minecraft client, int width, int height, int top, int bottom, int left, int entryHeight) {
        this(client, width, height, top, bottom, left, entryHeight, width, height);
    }

    public GuiScrollingList(Minecraft client, int width, int height, int top, int bottom, int left, int entryHeight, int screenWidth, int screenHeight) {
        this.client = client;
        this.listWidth = width;
        this.listHeight = height;
        this.top = top;
        this.bottom = bottom;
        this.slotHeight = entryHeight;
        this.left = left;
        this.right = width + this.left;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Deprecated
    public void func_27258_a(boolean highlight) {
        this.highlightSelected = highlight;
    }

    protected void setHeaderInfo(boolean hasHeader, int headerHeight) {
        this.hasHeader = hasHeader;
        this.headerHeight = headerHeight;
        if (!hasHeader) this.headerHeight = 0;
    }

    protected abstract int getSize();

    protected abstract void elementClicked(int index, boolean doubleClick);

    protected abstract boolean isSelected(int index);

    protected int getContentHeight() {
        return getSize() * slotHeight + headerHeight;
    }

    protected abstract void drawBackground();

    /** Draws an entry; {@code slotBuffer} is the entry height without the 4 pixel gap. */
    protected abstract void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess);

    protected void drawHeader(int entryRight, int relativeY, Tessellator tess) {
    }

    protected void clickHeader(int x, int y) {
    }

    /** Drawn last, over the list. */
    protected void drawScreen(int mouseX, int mouseY) {
    }

    /** The entry under a screen position, or -1. */
    public int func_27256_c(int x, int y) {
        int entryLeft = left + 1;
        int entryRight = left + listWidth - 7;
        int relativeY = y - top - headerHeight + (int) scrollDistance - 4;
        int ret = relativeY / slotHeight;
        return x >= entryLeft && x <= entryRight && ret >= 0 && relativeY >= 0 && ret < getSize() ? ret : -1;
    }

    public void registerScrollButtons(List<GuiButton> buttons, int upActionID, int downActionID) {
        this.scrollUpActionId = upActionID;
        this.scrollDownActionId = downActionID;
    }

    private void applyScrollLimits() {
        int listHeight = getContentHeight() - (bottom - top - 4);
        if (listHeight < 0) listHeight /= 2;
        if (scrollDistance < 0.0F) scrollDistance = 0.0F;
        if (scrollDistance > (float) listHeight) scrollDistance = (float) listHeight;
    }

    public void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == scrollUpActionId) {
            scrollDistance -= (float) (slotHeight * 2 / 3);
            initialMouseClickY = -2.0F;
            applyScrollLimits();
        } else if (button.id == scrollDownActionId) {
            scrollDistance += (float) (slotHeight * 2 / 3);
            initialMouseClickY = -2.0F;
            applyScrollLimits();
        }
    }

    /** The wheel of the mouse event being dispatched scrolls the list when the mouse is over it. */
    public void handleMouseInput(int mouseX, int mouseY) throws IOException {
        boolean isHovering = mouseX >= left && mouseX <= left + listWidth && mouseY >= top && mouseY <= bottom;
        if (!isHovering) return;
        int scroll = InputEvent1122.eventDWheel;
        if (scroll != 0) scrollDistance += (float) (-1 * scroll / 120.0F * slotHeight / 2);
    }

    private static boolean leftButtonDown() {
        try {
            long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        drawBackground();
        boolean isHovering = mouseX >= left && mouseX <= left + listWidth && mouseY >= top && mouseY <= bottom;
        int listLength = getSize();
        int scrollBarWidth = 6;
        int scrollBarRight = left + listWidth;
        int scrollBarLeft = scrollBarRight - scrollBarWidth;
        int entryLeft = left;
        int entryRight = scrollBarLeft - 1;
        int viewHeight = bottom - top;
        int border = 4;
        if (leftButtonDown()) {
            if (initialMouseClickY == -1.0F) {
                if (isHovering) {
                    int mouseListY = mouseY - top - headerHeight + (int) scrollDistance - border;
                    int slotIndex = mouseListY / slotHeight;
                    if (mouseX >= entryLeft && mouseX <= entryRight && slotIndex >= 0 && mouseListY >= 0 && slotIndex < listLength) {
                        elementClicked(slotIndex, slotIndex == selectedIndex && System.currentTimeMillis() - lastClickTime < 250L);
                        selectedIndex = slotIndex;
                        lastClickTime = System.currentTimeMillis();
                    } else if (mouseX >= entryLeft && mouseX <= entryRight && mouseListY < 0) {
                        clickHeader(mouseX - entryLeft, mouseY - top + (int) scrollDistance - border);
                    }
                    if (mouseX >= scrollBarLeft && mouseX <= scrollBarRight) {
                        scrollFactor = -1.0F;
                        int scrollHeight = getContentHeight() - viewHeight - border;
                        if (scrollHeight < 1) scrollHeight = 1;
                        int var13 = (int) ((float) (viewHeight * viewHeight) / (float) getContentHeight());
                        if (var13 < 32) var13 = 32;
                        if (var13 > viewHeight - border * 2) var13 = viewHeight - border * 2;
                        scrollFactor /= (float) (viewHeight - var13) / (float) scrollHeight;
                    } else {
                        scrollFactor = 1.0F;
                    }
                    initialMouseClickY = (float) mouseY;
                } else {
                    initialMouseClickY = -2.0F;
                }
            } else if (initialMouseClickY >= 0.0F) {
                scrollDistance -= ((float) mouseY - initialMouseClickY) * scrollFactor;
                initialMouseClickY = (float) mouseY;
            }
        } else {
            initialMouseClickY = -1.0F;
        }
        applyScrollLimits();

        var gui = RenderContext.gui();
        if (gui == null) return;
        Tessellator tess = Tessellator.getInstance();
        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            drawGradientRect(left, top, right, bottom, 0xC0101010, 0xD0101010);
        } else {
            var saved = RenderContext.argb();
            gui.setColor(0.125F, 0.125F, 0.125F, 1.0F);
            gui.blit(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/block/dirt.png"), left, top, right - left, bottom - top, left,
                    top + (int) scrollDistance, right - left, bottom - top, 32, 32);
            gui.setColor((saved >> 16 & 255) / 255.0F, (saved >> 8 & 255) / 255.0F, (saved & 255) / 255.0F, (saved >>> 24) / 255.0F);
        }
        int baseY = top + border - (int) scrollDistance;
        gui.enableScissor(left, top, right, bottom);
        try {
            if (hasHeader) drawHeader(entryRight, baseY, tess);
            for (int slotIdx = 0; slotIdx < listLength; ++slotIdx) {
                int slotTop = baseY + slotIdx * slotHeight + headerHeight;
                int slotBuffer = slotHeight - border;
                if (slotTop <= bottom && slotTop + slotBuffer >= top) {
                    if (highlightSelected && isSelected(slotIdx)) {
                        Gui.drawRect(entryLeft, slotTop - 2, entryRight, slotTop + slotBuffer + 2, 0xFF808080);
                        Gui.drawRect(entryLeft + 1, slotTop - 1, entryRight - 1, slotTop + slotBuffer + 1, 0xFF000000);
                    }
                    drawSlot(slotIdx, entryRight, slotTop, slotBuffer, tess);
                }
            }
        } finally {
            gui.disableScissor();
        }
        int extraHeight = getContentHeight() + border - viewHeight;
        if (extraHeight > 0) {
            int height = viewHeight * viewHeight / getContentHeight();
            if (height < 32) height = 32;
            if (height > viewHeight - border * 2) height = viewHeight - border * 2;
            int barTop = (int) scrollDistance * (viewHeight - height) / extraHeight + top;
            if (barTop < top) barTop = top;
            gui.fill(scrollBarLeft, top, scrollBarRight, bottom, 0xFF000000);
            gui.fill(scrollBarLeft, barTop, scrollBarRight, barTop + height, 0xFF808080);
            gui.fill(scrollBarLeft, barTop, scrollBarRight - 1, barTop + height - 1, 0xFFC0C0C0);
        }
        drawScreen(mouseX, mouseY);
    }

    protected void drawGradientRect(int left, int top, int right, int bottom, int color1, int color2) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fillGradient(left, top, right, bottom, color1, color2);
    }
}
