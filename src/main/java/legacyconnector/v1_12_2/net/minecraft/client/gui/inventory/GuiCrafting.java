package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook.GuiRecipeBook;
import legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook.IRecipeShownListener;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's crafting table screen. Its slots are those of a client-side modern crafting menu, numbered as 1.12's ContainerWorkbench
 * (result, 3x3 grid, inventory, hotbar), so a crafting container the server opened fills them in place; the game's own crafting screen
 * is announced to mods as one of these.
 */
public class GuiCrafting extends GuiContainer implements IRecipeShownListener {
    private static final ResourceLocation CRAFTING_TABLE_GUI_TEXTURES = new ResourceLocation("textures/gui/container/crafting_table.png");
    private GuiRecipeBook recipeBookGui = new GuiRecipeBook();

    public GuiCrafting(InventoryPlayer playerInv, World worldIn) {
        this(playerInv, worldIn, BlockPos.ORIGIN);
    }

    public GuiCrafting(InventoryPlayer playerInv, World worldIn, BlockPos blockPosition) {
        super(mcc$container(playerInv));
    }

    private static Container mcc$container(InventoryPlayer playerInv) {
        net.minecraft.world.entity.player.Player player = playerInv != null && playerInv.player != null ? playerInv.player.mcc$player()
                : net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? null : ModernContainer1122.of(new net.minecraft.world.inventory.CraftingMenu(0, player.getInventory()));
    }

    /** The stand-in of the modern crafting screen shares that screen's recipe book. */
    protected void mcc$setRecipeBook(GuiRecipeBook book) {
        this.recipeBookGui = book;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.crafting"), 28, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(CRAFTING_TABLE_GUI_TEXTURES);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void recipesUpdated() {
        recipeBookGui.recipesUpdated();
    }

    @Override
    public void onGuiClosed() {
        recipeBookGui.removed();
        super.onGuiClosed();
    }

    @Override
    public GuiRecipeBook func_194310_f() {
        return recipeBookGui;
    }
}
