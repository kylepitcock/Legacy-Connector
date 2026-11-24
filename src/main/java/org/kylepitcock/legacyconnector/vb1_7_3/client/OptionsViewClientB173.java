package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;

/** Beta's {@code GameSettings} fields as a view of the game's own options (volumes, sensitivity, view distance, bobbing, ambient occlusion, frame limit, difficulty). */
final class OptionsViewClientB173 implements legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions.NativeView {
    static void install() {
        legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions.mcc$native(new OptionsViewClientB173());
    }

    /** Beta's render distance step to chunks and back: far 16, normal 8, short 4, tiny 2 (in chunks, as Beta's 256/128/64/32 blocks). */
    private static final int[] CHUNKS = {16, 8, 4, 2};

    @Override public Object get(String key) {
        var o = Minecraft.getInstance().options;
        return switch (key) {
            case "music" -> o.getSoundSourceVolume(SoundSource.MUSIC);
            case "sound" -> o.getSoundSourceVolume(SoundSource.MASTER);
            case "sensitivity" -> o.sensitivity().get().floatValue();
            case "invertY" -> o.invertYMouse().get();
            case "bob" -> o.bobView().get();
            case "ao" -> o.ambientOcclusion().get();
            case "debug" -> Minecraft.getInstance().getDebugOverlay().showDebugScreen();
            case "viewDistance" -> {
                int chunks = o.renderDistance().get();
                yield chunks >= 12 ? 0 : chunks >= 6 ? 1 : chunks >= 3 ? 2 : 3;
            }
            case "fps" -> {
                int limit = o.framerateLimit().get();
                yield limit >= 260 ? 0 : limit >= 100 ? 1 : 2;
            }
            case "difficulty" -> {
                var level = Minecraft.getInstance().level;
                yield level == null ? 2 : level.getDifficulty().getId();
            }
            default -> 0;
        };
    }

    @Override public void set(String key, Object value) {
        var mc = Minecraft.getInstance();
        var o = mc.options;
        switch (key) {
            case "music" -> o.getSoundSourceOptionInstance(SoundSource.MUSIC).set(((Number) value).doubleValue());
            case "sound" -> o.getSoundSourceOptionInstance(SoundSource.MASTER).set(((Number) value).doubleValue());
            case "sensitivity" -> o.sensitivity().set(((Number) value).doubleValue());
            case "invertY" -> o.invertYMouse().set((Boolean) value);
            case "bob" -> o.bobView().set((Boolean) value);
            case "ao" -> o.ambientOcclusion().set((Boolean) value);
            case "debug" -> { var overlay = mc.getDebugOverlay(); if (overlay.showDebugScreen() != (Boolean) value) overlay.toggleOverlay(); }
            case "viewDistance" -> o.renderDistance().set(CHUNKS[((Number) value).intValue() & 3]);
            case "fps" -> o.framerateLimit().set(new int[]{260, 120, 40}[Math.floorMod(((Number) value).intValue(), 3)]);
            case "difficulty" -> {
                var server = mc.getSingleplayerServer();
                var difficulty = Difficulty.byId(((Number) value).intValue());
                if (server != null) server.execute(() -> server.setDifficulty(difficulty, true));
            }
            default -> {}
        }
        o.save();
    }
}
