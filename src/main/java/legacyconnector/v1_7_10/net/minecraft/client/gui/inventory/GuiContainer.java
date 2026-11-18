package legacyconnector.v1_7_10.net.minecraft.client.gui.inventory;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10 container screen. Slot rendering, item dragging and clicking are performed by the hosting modern
 * {@code AbstractContainerScreen}; the legacy class contributes its background, foreground and buttons.
 */
public abstract class GuiContainer extends GuiScreen {
    protected static final ResourceLocation inventoryBackground = new ResourceLocation("textures/gui/container/inventory.png");
    protected int xSize = 176;
    protected int ySize = 166;
    public Container inventorySlots;
    protected int guiLeft;
    protected int guiTop;

    public GuiContainer(Container container) {
        this.inventorySlots = container;
        this.allowUserInput = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    protected abstract void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY);

    /** The slots an item stack is being spread over while dragging (the hosting screen's quick-craft slots, kept in sync). */
    protected final java.util.Set dragSplittingSlots = new java.util.HashSet();
    protected boolean dragSplitting;

    @SuppressWarnings("unchecked")
    public void mcc$setDragSplitting(boolean active, Iterable<Slot> slots) {
        dragSplitting = active;
        dragSplittingSlots.clear();
        if (active) for (Slot s : slots) dragSplittingSlots.add(s);
    }

    protected boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        return isPointInRegion(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY);
    }

    protected boolean isPointInRegion(int x, int y, int w, int h, int mouseX, int mouseY) {
        mouseX -= guiLeft;
        mouseY -= guiTop;
        return mouseX >= x - 1 && mouseX < x + w + 1 && mouseY >= y - 1 && mouseY < y + h + 1;
    }

    /** NEI 1.4.7 / 1.5.2 patched {@code GuiContainer} with a {@code manager} field (see {@link #mcc$get$manager}). */
    public legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager manager;
    /** The manager a stand-in screen was given (the recipe screen of the JEI bridge); other screens get theirs from the manager table. */
    public legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager mcc$manager;

    /** The virtual {@code GuiContainer.manager} of NEI 1.4.7 / 1.5.2: the screen's drawing hub. */
    public static legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager mcc$get$manager(GuiContainer self) {
        if (self != null && self.mcc$manager != null) return self.mcc$manager;
        return legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager.getManager(self);
    }

    /** The slot under GUI coordinates (x, y), or null. */
    public Slot mcc$slotAt(int x, int y) {
        return inventorySlots == null ? null : getSlotAtPosition(x, y);
    }

    protected Slot getSlotAtPosition(int x, int y) {
        for (Object o : inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            if (isPointInRegion(s.xDisplayPosition, s.yDisplayPosition, 16, 16, x, y)) return s;
        }
        return null;
    }

    protected boolean checkHotbarKeys(int key) {
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // Bridge accessors for the hosting modern screen.
    public int mcc$xSize() {
        return xSize;
    }

    public int mcc$ySize() {
        return ySize;
    }

    public void mcc$setOrigin(int left, int top) {
        this.guiLeft = left;
        this.guiTop = top;
    }

    public void mcc$drawBackground(float partialTicks, int mouseX, int mouseY) {
        drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    }

    public void mcc$drawForeground(int mouseX, int mouseY) {
        drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    /** Clicks a slot as the player would; {@code mode}: 0 pickup, 1 quick move, 2 swap, 3 clone, 4 throw, 5 quick craft, 6 pickup all (the server runs the container's slotClick). */
    protected void handleMouseClick(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot, int slotId, int button, int mode) {
        if (slot != null) slotId = slot.slotNumber;
        var mcm = net.minecraft.client.Minecraft.getInstance();
        if (mcm.gameMode == null || mcm.player == null || inventorySlots == null) return;
        var types = net.minecraft.world.inventory.ClickType.values();
        mcm.gameMode.handleInventoryMouseClick(inventorySlots.windowId, slotId, button, types[Math.max(0, Math.min(mode, types.length - 1))], mcm.player);
    }

    /** NEI's public entry to {@link #handleMouseClick} (it clicks slots for its recipe overlay and item delete). */
    public void sendMouseClick(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot, int slotId, int button, int mode) {
        handleMouseClick(slot, slotId, button, mode);
    }

    /** 1.6.4's name of the slot drawing. */
    protected void drawSlotInventory(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot) {
        drawSlot(slot);
    }

    protected void drawSlot(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot) {
    }

    // Members125 batch 3: the accessors NEI's patched GuiContainer of 1.2.5 added

    /** The game instance ({@code mc} of the screen, which NEI's patch exposed as a method). */
    public legacyconnector.v1_7_10.net.minecraft.client.Minecraft mcc125$mc() {
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
    }

    public legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc() {
        return mcc125$mc();
    }

    public legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer fontRenderer() {
        return fontRendererObj;
    }

    public void setZLevel(int level) {
        this.zLevel = level;
    }

    /** NEI's {@code clickSlot(slot, slotId, button, shift)}: a click on a slot, shift as click mode 1. */
    public void clickSlot(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot, int slotId, int button, boolean shift) {
        handleMouseClick(slot, slotId, button, shift ? 1 : 0);
    }

    /**
     * 1.7.10's GuiContainer closed on Escape and on the inventory key, so a screen that handles keys itself (a focused text
     * field of its own) keeps the keystroke by not calling super. The hosting modern screen leaves the inventory key to this method
     * when the legacy GUI overrides keyTyped (a name field would otherwise close the GUI on every "e").
     */
    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == 0) return;
        boolean inventoryKey = false;
        try {
            var settings = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$gameSettings(mc);
            inventoryKey = settings != null && settings.keyBindInventory.getKeyCode() == keyCode;
        } catch (RuntimeException | LinkageError ignored) {
            // no settings yet
        }
        if (keyCode == 1 || inventoryKey) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) player.closeContainer();
            else super.keyTyped(c, keyCode);
        }
    }

    /** NEI's {@code containerKeyPress(char, key)}: hands a key to the container screen. */
    public void containerKeyPress(char typed, int key) {
        keyTyped(typed, key);
    }
}
