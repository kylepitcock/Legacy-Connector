package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 container screen, hosted by a modern container screen ({@code LegacyContainerScreen1122}). Its {@link #drawScreen} runs the modern
 * container rendering, which calls back for the legacy background layer (then the buttons) and foreground layer around the slots, so the
 * usual override ({@code drawDefaultBackground(); super.drawScreen(..); renderHoveredToolTip(..)}) draws as in 1.12. Slot clicking and
 * dragging are modern.
 */
public abstract class GuiContainer extends GuiScreen {
    public static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");
    protected int xSize = 176;
    protected int ySize = 166;
    public Container inventorySlots;
    protected int guiLeft;
    protected int guiTop;
    protected Slot hoveredSlot;
    protected boolean dragSplitting;
    protected final Set<Slot> dragSplittingSlots = new HashSet<>();
    protected ItemStack draggedStack = ItemStack.EMPTY;
    protected boolean doubleClick;

    /** What the hosting modern screen does for the legacy screen. */
    public interface Host {
        void mcc$renderContainer(int mouseX, int mouseY, float partialTicks);

        void mcc$renderTooltip(int mouseX, int mouseY);

        Slot mcc$hoveredSlot();
    }

    private Host mcc$host;

    public GuiContainer(Container inventorySlotsIn) {
        this.inventorySlots = inventorySlotsIn;
        this.allowUserInput = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    /** Draws the container: legacy background, buttons, slots, legacy foreground and the carried stack (through the modern screen). */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        hoveredSlot = mcc$host == null ? null : mcc$host.mcc$hoveredSlot();
        if (mcc$host != null) mcc$host.mcc$renderContainer(mouseX, mouseY, partialTicks);
        else super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** The tooltip of the stack under the mouse when nothing is carried. */
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        if (mcc$host != null) mcc$host.mcc$renderTooltip(mouseX, mouseY);
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    protected abstract void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY);

    protected boolean isMouseOverSlot(Slot slotIn, int mouseX, int mouseY) {
        return isPointInRegion(slotIn.xPos, slotIn.yPos, 16, 16, mouseX, mouseY);
    }

    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        pointX -= guiLeft;
        pointY -= guiTop;
        return pointX >= rectX - 1 && pointX < rectX + rectWidth + 1 && pointY >= rectY - 1 && pointY < rectY + rectHeight + 1;
    }

    protected Slot getSlotAtPosition(int x, int y) {
        for (Slot s : inventorySlots.inventorySlots) if (isMouseOverSlot(s, x, y) && s.isEnabled()) return s;
        return null;
    }

    protected boolean hasClickedOutside(int mouseX, int mouseY, int left, int top) {
        return mouseX < left || mouseY < top || mouseX >= left + xSize || mouseY >= top + ySize;
    }

    /** Clicks a slot as the player would (the server runs the container's slotClick). */
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        if (slotIn != null) slotId = slotIn.slotNumber;
        var mcm = net.minecraft.client.Minecraft.getInstance();
        if (mcm.gameMode == null || mcm.player == null) return;
        mcm.gameMode.handleInventoryMouseClick(inventorySlots.windowId, slotId, mouseButton, type == null ? net.minecraft.world.inventory.ClickType.PICKUP : type.mcc$modern(), mcm.player);
    }

    /** Escape and the inventory key close the screen; the hotbar keys swap the hovered slot; the drop key throws its stack. */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        var mcm = net.minecraft.client.Minecraft.getInstance();
        if (keyCode == 1 || keyMatches(mcm.options.keyInventory, keyCode)) {
            if (mcm.player != null) mcm.player.closeContainer();
            else mc.displayGuiScreen(null);
            return;
        }
        checkHotbarKeys(keyCode);
        Slot hovered = mcc$host == null ? hoveredSlot : mcc$host.mcc$hoveredSlot();
        if (hovered != null && hovered.getHasStack()) {
            if (keyMatches(mcm.options.keyPickItem, keyCode)) handleMouseClick(hovered, hovered.slotNumber, 0, ClickType.CLONE);
            else if (keyMatches(mcm.options.keyDrop, keyCode)) handleMouseClick(hovered, hovered.slotNumber, isCtrlKeyDown() ? 1 : 0, ClickType.THROW);
        }
    }

    private static boolean keyMatches(net.minecraft.client.KeyMapping mapping, int legacyKey) {
        if (legacyKey <= 0) return false;
        int glfw = org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.toGlfw(legacyKey);
        return glfw > 0 && mapping.matches(glfw, 0);
    }

    protected boolean checkHotbarKeys(int keyCode) {
        var mcm = net.minecraft.client.Minecraft.getInstance();
        Slot hovered = mcc$host == null ? hoveredSlot : mcc$host.mcc$hoveredSlot();
        if (mcm.player == null || mcm.player.containerMenu.getCarried().isEmpty() == false || hovered == null) return false;
        for (int i = 0; i < 9; ++i) {
            if (keyMatches(mcm.options.keyHotbarSlots[i], keyCode)) {
                handleMouseClick(hovered, hovered.slotNumber, i, ClickType.SWAP);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onGuiClosed() {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    public Slot getSlotUnderMouse() {
        return mcc$host == null ? hoveredSlot : mcc$host.mcc$hoveredSlot();
    }

    public int getGuiLeft() {
        return guiLeft;
    }

    public int getGuiTop() {
        return guiTop;
    }

    public int getXSize() {
        return xSize;
    }

    public int getYSize() {
        return ySize;
    }

    // ------------------------------------------------------------------ the hosting modern screen

    public void mcc$attach(Host host) {
        this.mcc$host = host;
    }

    public int mcc$xSize() {
        return xSize;
    }

    public int mcc$ySize() {
        return ySize;
    }

    public void mcc$setSize(int xSize, int ySize) {
        this.xSize = xSize;
        this.ySize = ySize;
    }

    public void mcc$setOrigin(int left, int top) {
        this.guiLeft = left;
        this.guiTop = top;
    }

    /** The legacy background layer and then, as GuiScreen.drawScreen does, the buttons and labels. */
    public void mcc$drawBackground(float partialTicks, int mouseX, int mouseY) {
        drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void mcc$drawForeground(int mouseX, int mouseY) {
        drawGuiContainerForegroundLayer(mouseX, mouseY);
        mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiContainerEvent.DrawForeground(this, mouseX, mouseY));
        for (var b : buttonList) {
            if (b.isMouseOver()) {
                b.drawButtonForegroundLayer(mouseX - guiLeft, mouseY - guiTop);
                break;
            }
        }
    }

    public void mcc$setDragSplitting(boolean active, Iterable<Slot> slots) {
        dragSplitting = active;
        dragSplittingSlots.clear();
        if (active) for (Slot s : slots) dragSplittingSlots.add(s);
    }
}
