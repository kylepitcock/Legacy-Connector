package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.audio.MusicTicker;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.LoggerFactory;

/**
 * Ticks a mod's own {@code Minecraft.mcMusicTicker} (Galacticraft and GalaxySpace replace it to play space music). While
 * one is installed it owns the background music, so the modern music manager is told to play nothing.
 */
public final class LegacyMusic {
    private static boolean failed;

    private LegacyMusic() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LegacyMusic::onTick);
        NeoForge.EVENT_BUS.addListener(LegacyMusic::onSelectMusic);
    }

    private static MusicTicker replaced() {
        MusicTicker ticker = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().mcMusicTicker;
        return ticker == null || ticker.getClass() == MusicTicker.class || failed ? null : ticker;
    }

    private static void onTick(ClientTickEvent.Post event) {
        MusicTicker ticker = replaced();
        if (ticker == null) return;
        try {
            ticker.update();
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            LoggerFactory.getLogger("LegacyConnector/Sound").warn("The legacy music ticker {} failed; vanilla music plays again: {}",
                    ticker.getClass().getName(), e.toString());
        }
    }

    private static void onSelectMusic(SelectMusicEvent event) {
        if (replaced() != null) event.setMusic(null);
    }
}
