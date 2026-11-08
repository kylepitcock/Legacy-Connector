package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElementHelper;

/** Jade 11 (1.20.1) calls against Jade 15 (1.21.1). */
public final class JadeCompat {
    private JadeCompat() {
    }

    /** {@code ITooltip.getElementHelper()} became the static {@code IElementHelper.get()}. */
    public static IElementHelper elementHelper(ITooltip tooltip) {
        return IElementHelper.get();
    }

    /** {@code ITooltip.remove(ResourceLocation)} returned nothing; the new one reports whether a line was removed. */
    public static void remove(ITooltip tooltip, ResourceLocation tag) {
        tooltip.remove(tag);
    }
}
