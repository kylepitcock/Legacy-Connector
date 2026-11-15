package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Development check for the coremod hook mixins of 1.12.2: the hook mixins inject with {@code require = 0} (a pack without the mods must not fail), so a
 * mixin that no longer matches its target is silent. This looks for the merged handler methods ({@code legacyconnector$coremod...}) in the target classes
 * and reports the ones that are missing. Runs in the client smoke test and the server probe.
 */
public final class CoremodMixinCheck1122 {
    private static final String[] COMMON = {
        "net.minecraft.world.level.SignalGetter=legacyconnector$coremodRedstonePower,legacyconnector$coremodStrongPower",
        "net.minecraft.world.entity.monster.Slime=legacyconnector$coremodSlimeChunk",
        "net.minecraft.world.entity.LivingEntity=legacyconnector$coremodSlipperiness",
        "net.minecraft.world.entity.player.Inventory=legacyconnector$coremodPlayerDrops",
        "net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer=legacyconnector$coremodTreeGround",
        "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase=legacyconnector$coremodCollision",
        "net.minecraft.world.level.block.entity.BeaconBlockEntity=legacyconnector$coremodBeaconEffects",
        "net.minecraft.world.item.ItemStack=legacyconnector$coremodStackComponent",
        "net.minecraft.world.item.BannerItem=legacyconnector$coremodBannerTooltipLayers",
        "net.neoforged.neoforge.common.extensions.IItemExtension=legacyconnector$coremodEnchantmentFits",
    };
    private static final String[] CLIENT = {
        "net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer=legacyconnector$coremodArmorHidden",
        "net.minecraft.client.renderer.entity.EntityRenderDispatcher=legacyconnector$coremodFlame",
        "net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer=legacyconnector$coremodShoulderTag,legacyconnector$coremodShoulderTagEnd",
        "net.minecraft.client.renderer.entity.ParrotRenderer=legacyconnector$coremodShoulderTexture",
    };

    private CoremodMixinCheck1122() {}

    /** Missing handlers by target class (empty when every mixin applied); {@code client} adds the client classes. */
    public static Map<String, List<String>> missing(boolean client) {
        Map<String, List<String>> out = new TreeMap<>();
        check(COMMON, out);
        if (client) check(CLIENT, out);
        return out;
    }

    private static void check(String[] entries, Map<String, List<String>> out) {
        for (String entry : entries) {
            String[] parts = entry.split("=");
            List<String> names = new ArrayList<>(List.of(parts[1].split(",")));
            try {
                Class<?> target = Class.forName(parts[0], false, CoremodMixinCheck1122.class.getClassLoader());
                for (var method : target.getDeclaredMethods()) names.removeIf(n -> method.getName().contains(n)); // Mixin may prefix a handler ("handler$...$name")
            } catch (ClassNotFoundException | LinkageError e) {
                names.add("(class not loadable: " + e.getClass().getSimpleName() + ")");
            }
            if (!names.isEmpty()) out.put(parts[0], names);
        }
    }

    public static String report(boolean client) {
        Map<String, List<String>> missing = missing(client);
        return missing.isEmpty() ? "all round 5 coremod mixins applied" : "coremod mixin handlers missing: " + missing;
    }
}
