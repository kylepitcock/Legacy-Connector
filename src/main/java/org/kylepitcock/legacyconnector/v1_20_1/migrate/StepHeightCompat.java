package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Objects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Compatibility for the raw step-height field that 1.20.1 mods accessed on
 * Entity. In 1.21.1 the value is backed by the vanilla STEP_HEIGHT attribute.
 *
 * AlienEvo's surf controllers temporarily override the old field while they
 * move the player, then restore the previous base value. Reading/writing the
 * attribute base value preserves that temporary override without clearing any
 * independent attribute modifiers.
 */
public final class StepHeightCompat {
    private StepHeightCompat() {}

    public static float getRawMaxUpStep(Player player) {
        AttributeInstance stepHeight = stepHeight(player);
        return (float) stepHeight.getBaseValue();
    }

    public static void setRawMaxUpStep(Player player, float value) {
        stepHeight(player).setBaseValue(value);
    }

    private static AttributeInstance stepHeight(Player player) {
        Objects.requireNonNull(player, "player");
        AttributeInstance attribute = player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute == null) {
            throw new IllegalStateException("Player does not have the vanilla STEP_HEIGHT attribute");
        }
        return attribute;
    }
}
