package org.kylepitcock.legacyconnector.v1_7_10.client;

/**
 * Fog parameters legacy fog handlers set with {@code GL11.glFogf}/{@code glFogi} (Biomes O' Plenty's biome fog, planet
 * fog). The modern game has no fixed-function fog: what a handler sets while its fog event runs is captured here and
 * applied to the modern fog of the frame; fog calls at other times change nothing.
 */
public final class LegacyFog {
    public static final int GL_FOG_DENSITY = 0x0B62;
    public static final int GL_FOG_START = 0x0B63;
    public static final int GL_FOG_END = 0x0B64;
    public static final int GL_FOG_MODE = 0x0B65;
    public static final int GL_EXP = 0x0800;
    public static final int GL_EXP2 = 0x0801;

    private static boolean capturing;
    public static float start;
    public static float end;
    public static float density;
    public static int mode;

    private LegacyFog() {}

    public static void begin() {
        capturing = true;
        start = Float.NaN;
        end = Float.NaN;
        density = Float.NaN;
        mode = -1;
    }

    public static void end() {
        capturing = false;
    }

    /** Called by the GL11 emulation for {@code glFogf} and {@code glFogi}. */
    public static void fog(int pname, float value) {
        if (!capturing || !com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return;
        switch (pname) {
            case GL_FOG_START -> start = value;
            case GL_FOG_END -> end = value;
            case GL_FOG_DENSITY -> density = value;
            case GL_FOG_MODE -> mode = (int) value;
            default -> {
            }
        }
    }

    /** Linear fog that looks like exponential fog of the density: it is nearly opaque at 3 / density. */
    public static float expEnd(float density) {
        return density <= 0.0F ? Float.MAX_VALUE : (mode == GL_EXP2 ? 2.0F : 3.0F) / density;
    }
}
