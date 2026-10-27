package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's container screen that lists the player's active effects to the left of the GUI (the survival and creative inventories and the
 * mods' inventory screens extend it). The modern game no longer moves the GUI right for them: the list is drawn where there is room.
 */
public abstract class InventoryEffectRenderer extends GuiContainer {
    protected boolean hasActivePotionEffects;

    public InventoryEffectRenderer(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Override
    public void initGui() {
        super.initGui();
        updateActivePotionEffects();
    }

    protected void updateActivePotionEffects() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        boolean visible = false;
        if (player != null) for (var e : player.getActiveEffects()) if (e.isVisible()) visible = true;
        guiLeft = (width - xSize) / 2;
        hasActivePotionEffects = visible;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hasActivePotionEffects) mcc$drawActivePotionEffects();
    }

    /** The effects as the modern inventory draws them: a 120x32 panel each with the icon, name and level, and the time left. */
    private void mcc$drawActivePotionEffects() {
        var gui = RenderContext.gui();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (gui == null || mc.player == null) return;
        int x = guiLeft - 124;
        if (x < 0) return;
        var effects = mc.player.getActiveEffects().stream().filter(net.minecraft.world.effect.MobEffectInstance::isVisible).sorted().toList();
        if (effects.isEmpty()) return;
        int step = effects.size() > 5 ? 132 / (effects.size() - 1) : 33;
        int y = guiTop;
        for (var effect : effects) {
            gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large"), x, y, 120, 32);
            gui.blit(x + 6, y + 7, 0, 18, 18, mc.getMobEffectTextures().get(effect.getEffect()));
            var name = effect.getEffect().value().getDisplayName().copy();
            if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
                name.append(net.minecraft.network.chat.CommonComponents.SPACE).append(net.minecraft.network.chat.Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
            }
            gui.drawString(mc.font, name, x + 10 + 18, y + 6, 0xFFFFFF);
            gui.drawString(mc.font, net.minecraft.world.effect.MobEffectUtil.formatDuration(effect, 1.0F, mc.level == null ? 20.0F : mc.level.tickRateManager().tickrate()),
                    x + 10 + 18, y + 6 + 10, 0x7F7F7F);
            y += step;
        }
    }
}
