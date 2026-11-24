package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.world.WorldType;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldPresetPack;

/**
 * 1.12.2 mod world types (Forge's {@code new WorldType(name)}: Botania's skyblock, Lost Cities, TerraFirmaCraft, Biomes O' Plenty, ...) as
 * 1.21 world presets listed on the Create World screen ({@link LegacyWorldPresetPack}). A world created with one reports the type through
 * {@code World.getWorldType()} ({@code WorldType.mcc$of}), so mods keyed on it (skyblock islands, ...) see their own type.
 */
public final class LegacyWorldPresets1122 {
    private LegacyWorldPresets1122() {}

    /** World types registered by legacy mods that can be created. */
    public static List<WorldType> modTypes() {
        List<WorldType> out = new ArrayList<>();
        for (WorldType t : WorldType.WORLD_TYPES) if (t != null && t.mcc$owner != null && t.canBeCreated()) out.add(t);
        return out;
    }

    private static List<LegacyWorldPresetPack.Preset> presets() {
        List<LegacyWorldPresetPack.Preset> out = new ArrayList<>();
        for (WorldType t : modTypes()) out.add(new LegacyWorldPresetPack.Preset(t.mcc$presetId(), t.getName(), t.mcc$owner, t.getTranslationKey(), null));
        return out;
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        LegacyWorldPresetPack.contribute(out, presets());
    }

    /** Button names: the mod's own translation of {@code generator.<name>}, else the type name. */
    public static void registerNames() {
        LegacyWorldPresetPack.registerNames(presets());
    }
}
