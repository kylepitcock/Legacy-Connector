package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

/**
 * Compatibility for Palladium's legacy AddonCrossbowItem calls after
 * Minecraft moved crossbow state into ChargedProjectiles components and made
 * projectile firing an instance method.
 */
public final class PalladiumCrossbowCompat {
    private PalladiumCrossbowCompat() {}

    /** Equivalent to 1.21.1 CrossbowItem's private getShootingPower helper. */
    public static float getShootingPower(ItemStack weapon) {
        ChargedProjectiles projectiles = weapon.get(DataComponents.CHARGED_PROJECTILES);
        return projectiles != null && projectiles.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }

    /**
     * Preserve the AddonCrossbowItem's configured speed multiplier while using
     * the current instance API. Vanilla's performShooting clears the charged
     * projectile component and performs launch, durability, and sound handling.
     */
    public static void performShooting(CrossbowItem crossbow, Level level, LivingEntity shooter,
            InteractionHand hand, ItemStack weapon, float velocity, float inaccuracy) {
        crossbow.performShooting(level, shooter, hand, weapon, velocity, inaccuracy, null);
    }
}
