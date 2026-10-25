package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.IGuiHandler;

/** GUI handlers mods registered for their mod instance; opening a GUI through them needs the menu bridge. */
public final class GuiBridge1122 {
    private static final Map<Object, IGuiHandler> HANDLERS = new ConcurrentHashMap<>();

    private GuiBridge1122() {}

    public static void register(Object mod, IGuiHandler handler) {
        if (mod != null && handler != null) HANDLERS.put(mod, handler);
    }

    public static IGuiHandler handler(Object mod) {
        return HANDLERS.get(mod);
    }
}
