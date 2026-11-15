package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Development check for the coremod hook mixins of 1.7.10 (the counterpart of {@code CoremodMixinCheck1122}): the hook mixins inject with
 * {@code require = 0} (a pack without the mods must not fail), so a mixin that no longer matches its target is silent. This looks for the merged handler
 * methods ({@code legacyconnector$coremod...}) in the target classes and reports the ones that are missing. Runs in the client smoke test and the server
 * probe ({@code tools/dev/coremod-sites-1710-probe.js}).
 */
public final class CoremodMixinCheck1710 {
    private static final String[] COMMON = {
        "net.minecraft.world.inventory.AnvilMenu=legacyconnector$coremodMaxCost",
        "net.minecraft.server.network.ServerGamePacketListenerImpl$1=legacyconnector$coremodAttackArrow",
        "net.minecraft.world.level.block.Block=legacyconnector$coremodExploded",
        "net.minecraft.world.level.CollisionGetter=legacyconnector$coremodCollisionBoxes",
        "net.minecraft.world.level.block.CrossCollisionBlock=legacyconnector$coremodCrossShape",
        "net.minecraft.world.item.enchantment.EnchantmentHelper=legacyconnector$coremodEnchantability",
        "net.minecraft.world.entity.Entity=legacyconnector$coremodLightHeight,legacyconnector$coremodNoClipMove,legacyconnector$coremodSprintParticles,legacyconnector$coremodStepDistance,legacyconnector$coremodStepDown",
        "net.minecraft.world.level.Explosion=legacyconnector$coremodKnockback",
        "net.minecraft.world.entity.projectile.FireworkRocketEntity=legacyconnector$coremodFireworkLift",
        "net.minecraft.world.entity.FlyingMob=legacyconnector$coremodFlyingTravelEnd",
        "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity=legacyconnector$coremodFurnaceTakenOver",
        "net.minecraft.world.item.ItemStack=legacyconnector$coremodItemTick,legacyconnector$coremodItemUsed",
        "net.minecraft.world.level.Level=legacyconnector$coremodRain,legacyconnector$coremodSetBlock",
        "net.minecraft.world.level.block.LiquidBlock=legacyconnector$coremodLiquidAdded",
        "net.minecraft.world.entity.LivingEntity=legacyconnector$coremodArmorVisibility,legacyconnector$coremodLimbSwing,legacyconnector$coremodPotionParticleName,legacyconnector$coremodSlowFall,legacyconnector$coremodSprint,legacyconnector$coremodSwingTicks",
        "net.minecraft.world.entity.Mob=legacyconnector$coremodAiTasks",
        "net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal=legacyconnector$coremodClosestVulnerable",
        "net.minecraft.world.entity.player.Player=legacyconnector$coremodAttack,legacyconnector$coremodHurtSound,legacyconnector$coremodUseReach",
        "net.minecraft.server.level.ServerPlayer=legacyconnector$coremodCrouching,legacyconnector$coremodDiving,legacyconnector$coremodInWater,legacyconnector$coremodSprinting,legacyconnector$coremodSwimming,legacyconnector$coremodWalking",
        "net.minecraft.world.level.block.BasePressurePlateBlock=legacyconnector$coremodPlateTick,legacyconnector$coremodPressureEntities",
        "net.minecraft.server.level.ServerLevel=legacyconnector$coremodNeighborNotified,legacyconnector$coremodNeighborsStart,legacyconnector$coremodNeighborsStop,legacyconnector$coremodWeather",
        "net.minecraft.server.network.ServerGamePacketListenerImpl=legacyconnector$coremodSprint",
        "net.minecraft.world.level.SignalGetter=legacyconnector$coremodNeighborPowered,legacyconnector$coremodPowerLevel,legacyconnector$coremodRedstonePower,legacyconnector$coremodStrongPower",
        "net.minecraft.world.entity.ai.targeting.TargetingConditions=legacyconnector$coremodTargetCandidates",
        "net.minecraft.server.level.ChunkMap$TrackedEntity=legacyconnector$coremodTrackingMax,legacyconnector$coremodTrackingRange"
    };
    private static final String[] CLIENT = {
        "net.minecraft.client.player.AbstractClientPlayer=legacyconnector$coremodFovWalkSpeed",
        "net.minecraft.client.gui.screens.inventory.AnvilScreen=legacyconnector$coremodMaxCost",
        "net.minecraft.client.renderer.GameRenderer=legacyconnector$coremodMouseOver",
        "net.minecraft.client.gui.screens.InBedChatScreen=legacyconnector$coremodLeaveBed",
        "net.minecraft.client.renderer.LightTexture=legacyconnector$coremodNightVision,legacyconnector$coremodNightVisionAmount",
        "net.minecraft.client.Minecraft=legacyconnector$coremodRightClickDelay"
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
                for (var method : target.getDeclaredMethods()) names.removeIf(n -> method.getName().contains(n)); // Mixin may prefix a handler ("handler$...$name")
            } catch (ClassNotFoundException | LinkageError e) {
                names.add("(class not loadable: " + e.getClass().getSimpleName() + ")");
            }
            if (!names.isEmpty()) out.merge(parts[0], names, (a, b) -> {
                List<String> all = new ArrayList<>(a);
                all.addAll(b);
                return all;
            });
        }
    }

    public static String report(boolean client) {
        Map<String, List<String>> missing = missing(client);
        return missing.isEmpty() ? "all 1.7.10 coremod mixins applied" : "1.7.10 coremod mixin handlers missing: " + missing;
    }
}
