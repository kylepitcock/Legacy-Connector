package org.kylepitcock.legacyconnector.v1_20_1;

import legacyconnector.v1_20_1.net.minecraftforge.event.TickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only part of {@link Forge1201Runtime} (kept apart so the server never loads client classes). */
final class Forge1201Client {
    private Forge1201Client() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener(Forge1201Client::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(Forge1201Client::onClientTickPost);
    }

    private static void onClientTickPre(ClientTickEvent.Pre e) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
    }

    private static void onClientTickPost(ClientTickEvent.Post e) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    }
}
