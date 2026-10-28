package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's trading screen. Its slots are the trading menu the server opened (the modern merchant menu: two payment slots and the result,
 * then the inventory), so a mod's subclass (its own trader's GUI, swapped in through GuiOpenEvent) trades for real; the arrows select a
 * trade as the modern screen does. The game's own trading screen is announced to mods as one of these, with {@link #getMerchant} its trader.
 */
public class GuiMerchant extends GuiContainer {
    private static final ResourceLocation MERCHANT_GUI_TEXTURE = new ResourceLocation("textures/gui/container/villager.png");
    private final IMerchant merchant;
    private MerchantButton nextButton;
    private MerchantButton previousButton;
    private int selectedMerchantRecipe;
    private final ITextComponent chatComponent;

    public GuiMerchant(InventoryPlayer playerInventory, IMerchant merchantIn, World worldIn) {
        super(mcc$container(playerInventory));
        this.merchant = merchantIn;
        this.chatComponent = merchantIn == null ? null : merchantIn.getDisplayName();
    }

    /** The open trading menu (the server's), or a client-side one with the same slots. */
    private static Container mcc$container(InventoryPlayer playerInventory) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return null;
        if (player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu open) return ModernContainer1122.of(open);
        return ModernContainer1122.of(new net.minecraft.world.inventory.MerchantMenu(0, player.getInventory()));
    }

    private net.minecraft.world.inventory.MerchantMenu mcc$menu() {
        return inventorySlots instanceof ModernContainer1122 c && c.mcc$modern() instanceof net.minecraft.world.inventory.MerchantMenu m ? m : null;
    }

    private MerchantRecipeList mcc$recipes() {
        MerchantRecipeList list = new MerchantRecipeList();
        var menu = mcc$menu();
        if (menu != null) {
            for (var offer : menu.getOffers()) list.add(MerchantRecipe.mcc$of(offer));
        } else if (merchant != null) {
            MerchantRecipeList own = merchant.getRecipes(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(mc));
            if (own != null) list.addAll(own);
        }
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        nextButton = addButton(new MerchantButton(1, i + 120 + 27, j + 24 - 1, true));
        previousButton = addButton(new MerchantButton(2, i + 36 - 19, j + 24 - 1, false));
        nextButton.enabled = false;
        previousButton.enabled = false;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = chatComponent == null ? I18n.format("entity.Villager.name") : chatComponent.getUnformattedText();
        fontRenderer.drawString(s, xSize / 2 - fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        MerchantRecipeList list = mcc$recipes();
        if (nextButton != null) nextButton.enabled = selectedMerchantRecipe < list.size() - 1;
        if (previousButton != null) previousButton.enabled = selectedMerchantRecipe > 0;
    }

    /** The arrows pick the trade the payment slots are for (sent to the server, which moves the payment items). */
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        boolean changed = false;
        if (button == nextButton) {
            ++selectedMerchantRecipe;
            MerchantRecipeList list = mcc$recipes();
            if (selectedMerchantRecipe >= list.size()) selectedMerchantRecipe = list.size() - 1;
            changed = true;
        } else if (button == previousButton) {
            --selectedMerchantRecipe;
            if (selectedMerchantRecipe < 0) selectedMerchantRecipe = 0;
            changed = true;
        }
        if (changed) {
            var menu = mcc$menu();
            if (menu != null) {
                menu.setSelectionHint(selectedMerchantRecipe);
                menu.tryMoveItems(selectedMerchantRecipe);
            }
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) connection.send(new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(selectedMerchantRecipe));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(MERCHANT_GUI_TEXTURE);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        drawTexturedModalRect(i, j, 0, 0, xSize, ySize);
        MerchantRecipeList list = mcc$recipes();
        if (!list.isEmpty() && selectedMerchantRecipe < list.size() && list.get(selectedMerchantRecipe).isRecipeDisabled()) {
            drawTexturedModalRect(guiLeft + 83, guiTop + 21, 212, 0, 28, 21);
            drawTexturedModalRect(guiLeft + 83, guiTop + 51, 212, 0, 28, 21);
        }
    }

    /** The selected trade drawn over the arrows: what it costs and what it gives, with their tooltips. */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        MerchantRecipeList list = mcc$recipes();
        var gui = RenderContext.gui();
        if (gui != null && !list.isEmpty() && selectedMerchantRecipe < list.size()) {
            int i = (width - xSize) / 2;
            int j = (height - ySize) / 2;
            MerchantRecipe recipe = list.get(selectedMerchantRecipe);
            ItemStack[] stacks = {recipe.getItemToBuy(), recipe.getSecondItemToBuy(), recipe.getItemToSell()};
            int[] xs = {i + 36, i + 62, i + 120};
            var font = net.minecraft.client.Minecraft.getInstance().font;
            for (int k = 0; k < 3; k++) {
                if (stacks[k] == null || stacks[k].isEmpty()) continue;
                var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stacks[k]);
                gui.renderItem(modern, xs[k], j + 24);
                gui.renderItemDecorations(font, modern, xs[k], j + 24);
            }
            for (int k = 0; k < 3; k++) {
                if (stacks[k] != null && !stacks[k].isEmpty() && isPointInRegion(xs[k] - guiLeft, 24, 16, 16, mouseX, mouseY)) renderToolTip(stacks[k], mouseX, mouseY);
            }
        }
        renderHoveredToolTip(mouseX, mouseY);
    }

    public IMerchant getMerchant() {
        return merchant;
    }

    /** The arrow buttons of the trade selector. */
    static class MerchantButton extends GuiButton {
        private final boolean forward;

        MerchantButton(int buttonID, int x, int y, boolean forward) {
            super(buttonID, x, y, 12, 19, "");
            this.forward = forward;
        }

        @Override
        public void drawButton(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            mc.getTextureManager().bindTexture(MERCHANT_GUI_TEXTURE);
            int u = forward ? 176 : 176 + width;
            int v = !enabled ? 2 * height : hovered ? height : 0;
            drawTexturedModalRect(x, y, u, v, width, height);
        }
    }

    /** The trader of the game's own trading screen as a legacy merchant (its trades, name and the player trading). */
    public static IMerchant mcc$clientMerchant(net.minecraft.world.inventory.MerchantMenu menu, net.minecraft.network.chat.Component title) {
        return new IMerchant() {
            @Override
            public void setCustomer(EntityPlayer player) {
            }

            @Override
            public EntityPlayer getCustomer() {
                return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
            }

            @Override
            public MerchantRecipeList getRecipes(EntityPlayer player) {
                MerchantRecipeList list = new MerchantRecipeList();
                for (var offer : menu.getOffers()) list.add(MerchantRecipe.mcc$of(offer));
                return list;
            }

            @Override
            public void setRecipes(MerchantRecipeList recipes) {
            }

            @Override
            public void useRecipe(MerchantRecipe recipe) {
            }

            @Override
            public void verifySellingItem(ItemStack stack) {
            }

            @Override
            public ITextComponent getDisplayName() {
                return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(title == null ? "" : title.getString());
            }

            @Override
            public World getWorld() {
                return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$world(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
            }

            @Override
            public BlockPos getPos() {
                var p = net.minecraft.client.Minecraft.getInstance().player;
                return p == null ? BlockPos.ORIGIN : new BlockPos(p.getBlockX(), p.getBlockY(), p.getBlockZ());
            }
        };
    }
}
