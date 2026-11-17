package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 members that 1.21.1 dropped or reshaped, ATM 9 gap round 7 (common side; the client side is {@link Round7ClientCompat}). Rows in {@code mc-calls.tsv}.
 */
public final class Round7Compat {
    private Round7Compat() {
    }

    /** {@code ObfuscationReflectionHelper.remapName(domain, name)}: names are Mojang names at run time, nothing to remap. */
    public static String remapName(Object domain, String name) {
        return name;
    }

    /** {@code DisplayInfo.getFrame()}: the frame type is the advancement type now. */
    public static AdvancementType displayFrame(DisplayInfo info) {
        return info.getType();
    }

    /** {@code DisplayInfo.getBackground()}: the background is an optional texture in a record accessor now (null when absent, as before). */
    public static net.minecraft.resources.ResourceLocation displayBackground(DisplayInfo info) {
        return info.getBackground().orElse(null);
    }

    /** {@code ShieldItem.getColor(stack)}: the base colour is a data component now (null without a banner pattern, as before). */
    public static DyeColor shieldColor(ItemStack stack) {
        return stack.get(DataComponents.BASE_COLOR);
    }
}
