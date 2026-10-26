package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.inventory.ContainerRepair;
import legacyconnector.v1_7_10.net.minecraft.inventory.ICrafting;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's anvil screen over a {@link ContainerRepair} (mods return it as the client element of their own anvils). The
 * name field and the error arrow are the modern anvil sprites. Renaming updates the container on this side.
 */
public class GuiRepair extends GuiContainer implements ICrafting {
    private static final ResourceLocation anvilResource = new ResourceLocation("textures/gui/container/anvil.png");
    private ContainerRepair anvil;
    private GuiTextField nameField;
    private InventoryPlayer playerInventory;

    public GuiRepair(InventoryPlayer inventory, World world, int x, int y, int z) {
        super(new ContainerRepair(inventory, world, x, y, z, inventory.player != null ? inventory.player : Minecraft.mcc$get$thePlayer(Minecraft.getMinecraft())));
        this.playerInventory = inventory;
        this.anvil = (ContainerRepair) this.inventorySlots;
    }

    @Override
    public void initGui() {
        super.initGui();
        int left = (width - xSize) / 2, top = (height - ySize) / 2;
        nameField = new GuiTextField(fontRendererObj, left + 62, top + 24, 103, 12);
        nameField.setTextColor(-1);
        nameField.setDisabledTextColour(-1);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(30);
        inventorySlots.removeCraftingFromCrafters(this);
        inventorySlots.addCraftingToCrafters(this);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        inventorySlots.removeCraftingFromCrafters(this);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("container.repair"), 60, 6, 4210752);
        if (anvil.maximumCost <= 0) return;
        int color = 8453920;
        boolean show = true;
        String text = I18n.format("container.repair.cost", anvil.maximumCost);
        EntityPlayer player = playerInventory.player;
        boolean creative = player != null && player.capabilities != null && legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities);
        if (anvil.maximumCost >= 40 && !creative) {
            text = I18n.format("container.repair.expensive");
            color = 16736352;
        } else if (!anvil.getSlot(2).getHasStack()) {
            show = false;
        } else if (player != null && !anvil.getSlot(2).canTakeStack(player)) {
            color = 16736352;
        }
        if (!show) return;
        int shadow = 0xFF000000 | (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
        int x = xSize - 8 - fontRendererObj.getStringWidth(text);
        int y = 67;
        fontRendererObj.drawString(text, x, y + 1, shadow);
        fontRendererObj.drawString(text, x + 1, y, shadow);
        fontRendererObj.drawString(text, x + 1, y + 1, shadow);
        fontRendererObj.drawString(text, x, y, color);
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (nameField != null && nameField.textboxKeyTyped(c, keyCode)) {
            rename();
        } else {
            super.keyTyped(c, keyCode);
        }
    }

    private void rename() {
        String name = nameField.getText();
        Slot slot = anvil.getSlot(0);
        if (slot != null && slot.getHasStack() && !slot.getStack().hasDisplayName() && name.equals(slot.getStack().getDisplayName())) name = "";
        anvil.updateItemName(name);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (nameField != null) nameField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (nameField != null) nameField.drawTextBox();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        int left = (width - xSize) / 2, top = (height - ySize) / 2;
        gui.blit(org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(anvilResource.getResourceDomain(), anvilResource.getResourcePath()),
                left, top, 0, 0, xSize, ySize);
        boolean hasInput = anvil.getSlot(0).getHasStack();
        gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace(hasInput ? "container/anvil/text_field" : "container/anvil/text_field_disabled"),
                left + 59, top + 20, 110, 16);
        if ((hasInput || anvil.getSlot(1).getHasStack()) && !anvil.getSlot(2).getHasStack()) {
            gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/anvil/error"), left + 99, top + 45, 28, 21);
        }
    }

    @Override
    public void sendContainerAndContentsToPlayer(Container container, List items) {
        sendSlotContents(container, 0, container.getSlot(0).getStack());
    }

    @Override
    public void sendSlotContents(Container container, int slot, ItemStack stack) {
        if (slot != 0 || nameField == null) return;
        nameField.setText(stack == null ? "" : stack.getDisplayName());
        nameField.setEnabled(stack != null);
        if (stack != null) rename();
    }

    @Override
    public void sendProgressBarUpdate(Container container, int id, int value) {
    }
}
