package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_7_10.cpw.mods.fml.common.IFuelHandler;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/** Legacy IFuelHandlers answer NeoForge's burn time event for any item, legacy or modern. */
public final class FuelBridge {
    private static final List<IFuelHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private FuelBridge() {}

    public static void register(IFuelHandler handler) {
        HANDLERS.add(handler);
    }

    public static int burnTime(ItemStack stack) {
        if (stack == null) return 0;
        int best = 0;
        for (IFuelHandler h : HANDLERS) best = Math.max(best, h.getBurnTime(stack));
        return best;
    }

    public static void onBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (HANDLERS.isEmpty()) return;
        int t = burnTime(ItemStack.mcc$wrap(event.getItemStack()));
        if (t > 0) event.setBurnTime(t);
    }
}
