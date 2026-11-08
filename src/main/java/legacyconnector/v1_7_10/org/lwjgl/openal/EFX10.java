package legacyconnector.v1_7_10.org.lwjgl.openal;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.openal.EXTEfx;

/**
 * LWJGL 2's {@code EFX10} (effects extension) over LWJGL 3's {@code EXTEfx}: the same calls under another class name. Constants are inlined
 * into mods when they compile, so only the functions are here.
 */
public final class EFX10 {
    private EFX10() {}

    public static int alGenAuxiliaryEffectSlots() {
        return EXTEfx.alGenAuxiliaryEffectSlots();
    }

    public static void alGenAuxiliaryEffectSlots(IntBuffer auxiliaryeffectslots) {
        EXTEfx.alGenAuxiliaryEffectSlots(auxiliaryeffectslots);
    }

    public static void alDeleteAuxiliaryEffectSlots(int auxiliaryeffectslot) {
        EXTEfx.alDeleteAuxiliaryEffectSlots(auxiliaryeffectslot);
    }

    public static void alDeleteAuxiliaryEffectSlots(IntBuffer auxiliaryeffectslots) {
        EXTEfx.alDeleteAuxiliaryEffectSlots(auxiliaryeffectslots);
    }

    public static boolean alIsAuxiliaryEffectSlot(int auxiliaryeffectslot) {
        return EXTEfx.alIsAuxiliaryEffectSlot(auxiliaryeffectslot);
    }

    public static void alAuxiliaryEffectSloti(int auxiliaryeffectslot, int param, int value) {
        EXTEfx.alAuxiliaryEffectSloti(auxiliaryeffectslot, param, value);
    }

    public static void alAuxiliaryEffectSlotf(int auxiliaryeffectslot, int param, float value) {
        EXTEfx.alAuxiliaryEffectSlotf(auxiliaryeffectslot, param, value);
    }

    public static int alGetAuxiliaryEffectSloti(int auxiliaryeffectslot, int param) {
        return EXTEfx.alGetAuxiliaryEffectSloti(auxiliaryeffectslot, param);
    }

    public static float alGetAuxiliaryEffectSlotf(int auxiliaryeffectslot, int param) {
        return EXTEfx.alGetAuxiliaryEffectSlotf(auxiliaryeffectslot, param);
    }

    public static int alGenEffects() {
        return EXTEfx.alGenEffects();
    }

    public static void alGenEffects(IntBuffer effects) {
        EXTEfx.alGenEffects(effects);
    }

    public static void alDeleteEffects(int effect) {
        EXTEfx.alDeleteEffects(effect);
    }

    public static void alDeleteEffects(IntBuffer effects) {
        EXTEfx.alDeleteEffects(effects);
    }

    public static boolean alIsEffect(int effect) {
        return EXTEfx.alIsEffect(effect);
    }

    public static void alEffecti(int effect, int param, int value) {
        EXTEfx.alEffecti(effect, param, value);
    }

    public static void alEffectf(int effect, int param, float value) {
        EXTEfx.alEffectf(effect, param, value);
    }

    public static void alEffect(int effect, int param, FloatBuffer values) {
        EXTEfx.alEffectfv(effect, param, values);
    }

    public static int alGetEffecti(int effect, int param) {
        return EXTEfx.alGetEffecti(effect, param);
    }

    public static float alGetEffectf(int effect, int param) {
        return EXTEfx.alGetEffectf(effect, param);
    }

    public static int alGenFilters() {
        return EXTEfx.alGenFilters();
    }

    public static void alGenFilters(IntBuffer filters) {
        EXTEfx.alGenFilters(filters);
    }

    public static void alDeleteFilters(int filter) {
        EXTEfx.alDeleteFilters(filter);
    }

    public static void alDeleteFilters(IntBuffer filters) {
        EXTEfx.alDeleteFilters(filters);
    }

    public static boolean alIsFilter(int filter) {
        return EXTEfx.alIsFilter(filter);
    }

    public static void alFilteri(int filter, int param, int value) {
        EXTEfx.alFilteri(filter, param, value);
    }

    public static void alFilterf(int filter, int param, float value) {
        EXTEfx.alFilterf(filter, param, value);
    }

    public static int alGetFilteri(int filter, int param) {
        return EXTEfx.alGetFilteri(filter, param);
    }

    public static float alGetFilterf(int filter, int param) {
        return EXTEfx.alGetFilterf(filter, param);
    }
}
