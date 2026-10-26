package legacyconnector.v1_7_10.net.minecraft.client.gui.inventory;

import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.ContainerWorkbench;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.7.10's crafting table screen over a {@link ContainerWorkbench} (mods open it for their own tables). */
public class GuiCrafting extends GuiContainer {
    private static final ResourceLocation craftingTableGuiTextures = new ResourceLocation("textures/gui/container/crafting_table.png");

    public GuiCrafting(InventoryPlayer inventory, World world, int x, int y, int z) {
        super(new ContainerWorkbench(inventory, world, x, y, z));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("container.crafting"), 28, 6, 4210752);
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(craftingTableGuiTextures);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
    }
}
