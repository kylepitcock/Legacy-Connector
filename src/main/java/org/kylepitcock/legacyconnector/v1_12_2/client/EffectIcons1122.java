package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.potion.Potion;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.kylepitcock.legacyconnector.v1_12_2.entity.Effects1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Status icons of 1.12.2 mod potions. A modern effect's icon is the sprite {@code mob_effect/<name>}, which mod potions do not have
 * (the HUD showed magenta squares, e.g. Scape and Run: Parasites' Call of the Hive). 1.12 drew a potion with {@code hasStatusIcon()} as
 * the 18x18 cell {@code getStatusIconIndex()} of the texture bound at that moment (inventory.png, or the mod's own sheet bound inside
 * that method), then called Forge's {@code renderInventoryEffect} / {@code renderHUDEffect}, where most mods draw their icon themselves.
 */
public final class EffectIcons1122 {
    /** 1.12's inventory.png, bound while the potion picks its icon. */
    private static final ResourceLocation VANILLA_SHEET = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    /** The vanilla effects by their 1.12 icon cell (x + 8 * y): a mod potion pointing at one shows that effect's sprite. */
    private static final Map<Integer, Holder<MobEffect>> VANILLA = Map.ofEntries(
            Map.entry(0, MobEffects.MOVEMENT_SPEED), Map.entry(1, MobEffects.MOVEMENT_SLOWDOWN), Map.entry(2, MobEffects.DIG_SPEED),
            Map.entry(3, MobEffects.DIG_SLOWDOWN), Map.entry(4, MobEffects.DAMAGE_BOOST), Map.entry(5, MobEffects.WEAKNESS),
            Map.entry(6, MobEffects.POISON), Map.entry(7, MobEffects.REGENERATION), Map.entry(8, MobEffects.INVISIBILITY),
            Map.entry(9, MobEffects.HUNGER), Map.entry(10, MobEffects.JUMP), Map.entry(11, MobEffects.CONFUSION),
            Map.entry(12, MobEffects.NIGHT_VISION), Map.entry(13, MobEffects.BLINDNESS), Map.entry(14, MobEffects.DAMAGE_RESISTANCE),
            Map.entry(15, MobEffects.FIRE_RESISTANCE), Map.entry(16, MobEffects.WATER_BREATHING), Map.entry(17, MobEffects.WITHER),
            Map.entry(18, MobEffects.ABSORPTION), Map.entry(19, MobEffects.LEVITATION), Map.entry(20, MobEffects.GLOWING),
            Map.entry(21, MobEffects.LUCK), Map.entry(22, MobEffects.UNLUCK), Map.entry(23, MobEffects.HEALTH_BOOST));

    private EffectIcons1122() {}

    public static void register(RegisterClientExtensionsEvent event) {
        var effects = Effects1122.all();
        if (effects.isEmpty()) return;
        event.registerMobEffect(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) {
                Potion potion = Effects1122.legacy(instance.getEffect().value());
                return potion == null || safe(() -> potion.shouldRender(PotionEffect.mcc$fromModern(instance)), true);
            }

            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                Potion potion = Effects1122.legacy(instance.getEffect().value());
                return potion == null || safe(() -> potion.shouldRenderHUD(PotionEffect.mcc$fromModern(instance)), true);
            }

            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics, int x, int y,
                    int blitOffset) {
                // x is the panel's left edge + 7 (the icon column); 1.12 passed the panel's corner and drew the icon at +6, +7
                draw(instance, graphics, x - 1, y + 7, 1.0F, false, x - 7, y, blitOffset);
                return true; // the icon only: names and durations are drawn separately
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui, GuiGraphics graphics, int x, int y, float z,
                    float alpha) {
                draw(instance, graphics, x + 3, y + 3, alpha, true, x, y, z);
                return true;
            }
        }, effects.toArray(new MobEffect[0]));
    }

    private static boolean safe(java.util.function.BooleanSupplier test, boolean fallback) {
        try {
            return test.getAsBoolean();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    /** The icon cell (when the potion has one), then the mod's own drawing hook at the 1.12 corner (cornerX, cornerY). */
    private static void draw(MobEffectInstance instance, GuiGraphics graphics, int x, int y, float alpha, boolean hud, int cornerX, int cornerY,
            float z) {
        Potion potion = Effects1122.legacy(instance.getEffect().value());
        if (potion == null) return;
        ResourceLocation previous = RenderContext.texture();
        RenderContext.beginGui(graphics);
        try {
            if (potion.hasStatusIcon()) {
                RenderContext.bindTexture(VANILLA_SHEET);
                int index = potion.getStatusIconIndex();
                ResourceLocation sheet = RenderContext.texture();
                graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                try {
                    if (sheet == null || sheet.equals(VANILLA_SHEET)) {
                        Holder<MobEffect> vanilla = VANILLA.get(index);
                        if (vanilla != null) graphics.blit(x, y, 0, 18, 18, net.minecraft.client.Minecraft.getInstance().getMobEffectTextures().get(vanilla));
                    } else {
                        graphics.blit(sheet, x, y, index % 8 * 18, 198 + index / 8 * 18, 18, 18, 256, 256);
                    }
                } finally {
                    graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            PotionEffect effect = PotionEffect.mcc$fromModern(instance);
            var mc = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
            if (hud) potion.renderHUDEffect(effect, mc.ingameGUI, cornerX, cornerY, z, alpha);
            else potion.renderInventoryEffect(effect, mc.ingameGUI, cornerX, cornerY, z);
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("drawing the status icon of potion " + potion.getClass().getName(), e);
        } finally {
            RenderContext.end();
            if (previous != null) RenderContext.bindTexture(previous);
        }
    }
}
