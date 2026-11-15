package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Development check for the coremod hook mixins of 1.7.10 (round 3): the mixins inject with {@code require = 0}, so one that no longer matches its target is
 * silent. This looks for the merged handler methods ({@code legacyconnector$coremod...}) in the target classes and reports the ones that are missing.
 */
public final class CoremodMixinCheck1710 {
    private static final String[] COMMON = {
        "net.minecraft.world.level.Level=legacyconnector$coremodSetBlock",
        "net.minecraft.world.level.block.BasePressurePlateBlock=legacyconnector$coremodPressureEntities,legacyconnector$coremodPlateTick",
        "net.minecraft.world.entity.ai.targeting.TargetingConditions=legacyconnector$coremodTargetCandidates",
        "net.minecraft.world.entity.player.Player=legacyconnector$coremodUseReach,legacyconnector$coremodBlockReach",
        "net.minecraft.world.level.block.CrossCollisionBlock=legacyconnector$coremodCrossShape",
        "net.minecraft.world.entity.Entity=legacyconnector$coremodStepDown,legacyconnector$coremodNoClipMove",
        "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity=legacyconnector$coremodFurnaceTakenOver",
        "net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal=legacyconnector$coremodClosestVulnerable",
        "net.minecraft.server.level.ChunkMap$TrackedEntity=legacyconnector$coremodTrackingMax",
        "net.minecraft.server.network.ServerGamePacketListenerImpl$1=legacyconnector$coremodAttackArrow",
    };
    private static final String[] CLIENT = {
        "net.minecraft.client.Minecraft=legacyconnector$coremodPickBlock",
        "net.minecraft.client.MouseHandler=legacyconnector$coremodMouseOverride",
        "net.minecraft.client.renderer.ItemInHandRenderer=legacyconnector$coremodHandItemsEnd",
        "net.minecraft.client.renderer.ScreenEffectRenderer=legacyconnector$coremodScreenOverlays",
        "net.minecraft.client.renderer.entity.LivingEntityRenderer=legacyconnector$coremodLimbSwing,legacyconnector$coremodRotateCorpse",
    };

    private CoremodMixinCheck1710() {}

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
                Class<?> target = Class.forName(parts[0], false, CoremodMixinCheck1710.class.getClassLoader());
                for (var method : target.getDeclaredMethods()) names.removeIf(n -> method.getName().contains(n)); // Mixin may prefix a handler
            } catch (ClassNotFoundException | LinkageError e) {
                names.add("(class not loadable: " + e.getClass().getSimpleName() + ")");
            }
            if (!names.isEmpty()) out.put(parts[0], names);
        }
    }

    public static String report(boolean client) {
        Map<String, List<String>> missing = missing(client);
        return missing.isEmpty() ? "all 1.7.10 round 3 coremod mixins applied" : "1.7.10 coremod mixin handlers missing: " + missing;
    }
}
