package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.potion.Potion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge;

/**
 * Status icons of mod potions. A modern effect's icon is the sprite {@code mob_effect/<name>}, which mod potions do not have
 * (the HUD and the inventory showed the missing texture). 1.7.10 drew {@code getStatusIconIndex()} as an 18x18 cell of the
 * texture bound at that moment: vanilla's inventory.png, or the mod's own sheet when the potion binds one inside that method
 * (the usual way). Cells of vanilla's sheet are drawn as the vanilla effect with that icon; a potion without an icon draws none.
 */
public final class LegacyEffectIcons {
    /** Stands for 1.7.10's inventory.png while the potion picks its icon. */
    private static final ResourceLocation VANILLA_SHEET = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");

    private LegacyEffectIcons() {}

    public static void register(RegisterClientExtensionsEvent event) {
        var effects = PotionBridge.legacyEffects();
        if (effects.isEmpty()) return;
        event.registerMobEffect(new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics, int x, int y,
                    int blitOffset) {
                draw(instance, graphics, x, y + 7, 1.0F);
                return true; // the icon only: names and durations are drawn separately
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui, GuiGraphics graphics, int x, int y, float z,
                    float alpha) {
                draw(instance, graphics, x + 3, y + 3, alpha);
                return true;
            }
        }, effects.toArray(new MobEffect[0]));
    }

    private static void draw(MobEffectInstance instance, GuiGraphics graphics, int x, int y, float alpha) {
        Potion potion = PotionBridge.legacyOf(instance.getEffect().value());
        if (potion == null) return;
        ResourceLocation previous = RenderContext.texture();
        try {
            if (!potion.hasStatusIcon()) return;
            RenderContext.bindTexture(VANILLA_SHEET);
            int index = potion.getStatusIconIndex();
            ResourceLocation sheet = RenderContext.texture();
            graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            try {
                if (sheet == null || sheet.equals(VANILLA_SHEET)) {
                    var vanilla = PotionBridge.vanillaWithIcon(index);
                    if (vanilla != null) graphics.blit(x, y, 0, 18, 18, Minecraft.getInstance().getMobEffectTextures().get(vanilla));
                } else {
                    graphics.blit(sheet, x, y, index % 8 * 18, 198 + index / 8 * 18, 18, 18, 256, 256);
                }
            } finally {
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } catch (RuntimeException | LinkageError e) {
            // no icon rather than a broken HUD
        } finally {
            if (previous != null) RenderContext.bindTexture(previous);
        }
    }
}
