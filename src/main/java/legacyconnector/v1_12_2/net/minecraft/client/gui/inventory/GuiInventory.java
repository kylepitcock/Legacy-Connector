package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButtonImage;
import legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook.GuiRecipeBook;
import legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook.IRecipeShownListener;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.InventoryEffectRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's survival inventory over the player's inventory menu (slots numbered as 1.12's ContainerPlayer: result, 2x2 grid, armour, main,
 * hotbar, off hand). Displaying this class itself opens the modern inventory screen; mods' subclasses are drawn as legacy container GUIs
 * whose clicks go to the player's inventory menu. The game's own inventory screen is announced to mods as one of these (its legacy
 * stand-in), so {@code getGuiLeft}, the slots and the recipe book read the modern screen.
 */
public class GuiInventory extends InventoryEffectRenderer implements IRecipeShownListener {
    private float oldMouseX;
    private float oldMouseY;
    private GuiButtonImage recipeButton;
    private GuiRecipeBook recipeBookGui = new GuiRecipeBook();
    private boolean widthTooNarrow;
    private boolean buttonClicked;

    public GuiInventory(EntityPlayer player) {
        super(mcc$inventoryContainer(player));
        this.allowUserInput = true;
    }

    /** The legacy view of the player's modern inventory menu (what 1.12's {@code player.inventoryContainer} was). */
    public static Container mcc$inventoryContainer(EntityPlayer player) {
        net.minecraft.world.entity.player.Player modern = player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p
                : net.minecraft.client.Minecraft.getInstance().player;
        return modern == null ? null : ModernContainer1122.of(modern.inventoryMenu);
    }

    /** The legacy stand-in of the modern inventory screen shares that screen's recipe book. */
    protected void mcc$setRecipeBook(GuiRecipeBook book) {
        this.recipeBookGui = book;
    }

    private static boolean creative() {
        var gm = net.minecraft.client.Minecraft.getInstance().gameMode;
        return gm != null && gm.hasInfiniteItems();
    }

    @Override
    public void updateScreen() {
        if (creative()) mc.displayGuiScreen(new GuiContainerCreative(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(mc)));
        recipeBookGui.tick();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        if (creative()) mc.displayGuiScreen(new GuiContainerCreative(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(mc)));
        else super.initGui();
        widthTooNarrow = width < 379;
        recipeBookGui.func_194303_a(width, height, mc, widthTooNarrow, null);
        guiLeft = recipeBookGui.updateScreenPosition(widthTooNarrow, width, xSize);
        recipeButton = new GuiButtonImage(10, guiLeft + 104, height / 2 - 22, 20, 18, 178, 0, 19, INVENTORY_BACKGROUND);
        buttonList.add(recipeButton);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.crafting"), 97, 8, 4210752);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        hasActivePotionEffects = !recipeBookGui.isVisible();
        if (recipeBookGui.isVisible() && widthTooNarrow) {
            drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
            recipeBookGui.render(mouseX, mouseY, partialTicks);
        } else {
            recipeBookGui.render(mouseX, mouseY, partialTicks);
            super.drawScreen(mouseX, mouseY, partialTicks);
            recipeBookGui.renderGhostRecipe(guiLeft, guiTop, false, partialTicks);
        }
        renderHoveredToolTip(mouseX, mouseY);
        recipeBookGui.renderTooltip(guiLeft, guiTop, mouseX, mouseY);
        oldMouseX = (float) mouseX;
        oldMouseY = (float) mouseY;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        int i = guiLeft;
        int j = guiTop;
        drawTexturedModalRect(i, j, 0, 0, xSize, ySize);
        var player = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(mc);
        drawEntityOnScreen(i + 51, j + 75, 30, (float) (i + 51) - oldMouseX, (float) (j + 75 - 50) - oldMouseY, player);
    }

    @Override
    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return (!widthTooNarrow || !recipeBookGui.isVisible()) && super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!recipeBookGui.mouseClicked(mouseX, mouseY, mouseButton) && (!widthTooNarrow || !recipeBookGui.isVisible())) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (buttonClicked) buttonClicked = false;
        else super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected boolean hasClickedOutside(int mouseX, int mouseY, int left, int top) {
        boolean outside = mouseX < left || mouseY < top || mouseX >= left + xSize || mouseY >= top + ySize;
        return recipeBookGui.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, xSize, ySize) && outside;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 10) {
            recipeBookGui.initVisuals(widthTooNarrow, null);
            recipeBookGui.toggleVisibility();
            guiLeft = recipeBookGui.updateScreenPosition(widthTooNarrow, width, xSize);
            if (recipeButton != null) recipeButton.setPosition(guiLeft + 104, height / 2 - 22);
            buttonClicked = true;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!recipeBookGui.keyPressed(typedChar, keyCode)) super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
        recipeBookGui.slotClicked(slotIn);
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

    /**
     * Draws the entity standing at (x, y) of the screen, {@code scale} pixels per block, turned to look along the offsets
     * {@code mouseX}/{@code mouseY} (callers pass the anchor minus the mouse position), with the modern inventory's entity renderer.
     */
    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        var gui = RenderContext.gui();
        if (gui == null || ent == null || !(ent.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        float yawAngle = (float) Math.atan(mouseX / 40.0F);
        float pitchAngle = (float) Math.atan(mouseY / 40.0F);
        org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf camera = new org.joml.Quaternionf().rotateX(pitchAngle * 20.0F * ((float) Math.PI / 180.0F));
        pose.mul(camera);
        float bodyRot = living.yBodyRot, yRot = living.getYRot(), xRot = living.getXRot(), headRotO = living.yHeadRotO, headRot = living.yHeadRot;
        living.yBodyRot = 180.0F + yawAngle * 20.0F;
        living.setYRot(180.0F + yawAngle * 40.0F);
        living.setXRot(-pitchAngle * 20.0F);
        living.yHeadRot = living.getYRot();
        living.yHeadRotO = living.getYRot();
        try {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(gui, posX, posY, scale / Math.max(0.01F, living.getScale()),
                    new org.joml.Vector3f(), pose, camera, living);
        } finally {
            living.yBodyRot = bodyRot;
            living.setYRot(yRot);
            living.setXRot(xRot);
            living.yHeadRotO = headRotO;
            living.yHeadRot = headRot;
        }
    }
}
