package org.kylepitcock.legacyconnector.core;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.legacyconnector.mixinplugin.MixinSelfCheck;

/** Triggers {@link MixinSelfCheck#report} once the game has loaded the mixin targets: client title screen and server start. No per-tick cost. */
public final class MixinSelfCheckHook {
    private MixinSelfCheckHook() {}

    public static void register(IEventBus game) {
        boolean client = FMLEnvironment.dist.isClient();
        game.addListener((ServerStartedEvent e) -> MixinSelfCheck.report(client, "server started"));
        if (client) Client.register(game);
    }

    private static final class Client {
        static void register(IEventBus game) {
            game.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Opening e) -> {
                if (e.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen) MixinSelfCheck.reportOnce(true, "title screen");
            });
        }
    }
}
