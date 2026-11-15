package org.kylepitcock.legacyconnector.v1_20_1.migrate;

/**
 * Stands in for {@code ItemStack.TooltipPart} (1.20.1): the parts of an item tooltip a stack's {@code HideFlags} could hide. 1.21.1 keeps that per component
 * ({@code show_in_tooltip}); mods only name the constants and ask for the mask of the flag word they test.
 */
public enum LegacyTooltipPart {
    ENCHANTMENTS,
    MODIFIERS,
    UNBREAKABLE,
    CAN_DESTROY,
    CAN_PLACE_ON,
    ADDITIONAL,
    DYE,
    UPGRADES;

    public int getMask() {
        return 1 << ordinal();
    }
}
