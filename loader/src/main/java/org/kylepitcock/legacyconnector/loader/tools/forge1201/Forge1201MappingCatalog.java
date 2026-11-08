package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Version-scoped inventory of Forge 47.x to NeoForge 21.1.x symbol migration decisions.
 *
 * <p>This catalog is intentionally an audit aid, not an active bytecode remapper. A package
 * relocation is only a candidate until the exact target class and every referenced member
 * signature have been verified against the pinned NeoForge artifact. APIs with changed
 * semantics are classified separately and must be handled by adapters or source-level rewrites.</p>
 */
public final class Forge1201MappingCatalog {
    public enum Kind {
        NOT_FORGE_API,
        PACKAGE_RELOCATION_CANDIDATE,
        EXPLICIT_TYPE_REPLACEMENT,
        MANUAL_ADAPTER_REQUIRED,
        UNMAPPED_FORGE_API
    }

    public record PackageMove(String sourcePrefix, String targetPrefix, String evidence) {}

    public record Decision(
            String sourceInternalName,
            Kind kind,
            String targetInternalName,
            String reason) {}

    private static final List<PackageMove> PACKAGE_MOVES = List.of(
            new PackageMove(
                    "net/minecraftforge/api/distmarker/",
                    "net/neoforged/api/distmarker/",
                    "NeoForge 1.21.1 distribution-marker namespace"),
            new PackageMove(
                    "net/minecraftforge/eventbus/",
                    "net/neoforged/bus/",
                    "NeoForge event-bus package change"),
            new PackageMove(
                    "net/minecraftforge/fml/",
                    "net/neoforged/fml/",
                    "NeoForge FML namespace"),
            new PackageMove(
                    "net/minecraftforge/client/",
                    "net/neoforged/neoforge/client/",
                    "NeoForge client API namespace"),
            new PackageMove(
                    "net/minecraftforge/common/",
                    "net/neoforged/neoforge/common/",
                    "NeoForge common API namespace"),
            new PackageMove(
                    "net/minecraftforge/event/",
                    "net/neoforged/neoforge/event/",
                    "NeoForge event API namespace"),
            new PackageMove(
                    "net/minecraftforge/network/",
                    "net/neoforged/neoforge/network/",
                    "NeoForge networking namespace"),
            new PackageMove(
                    "net/minecraftforge/registries/",
                    "net/neoforged/neoforge/registries/",
                    "NeoForge registry API namespace")
    );

    /**
     * Explicit type substitutions reviewed against the Forge 47.x and NeoForge 21.1.x APIs.
     * These are not namespace-wide moves: the config-spec family maps to ModConfigSpec,
     * RegistryObject maps to Supplier because the audited usage only calls get(), and
     * IForgeMenuType maps to the target IMenuTypeExtension contract.
     */
    private static final Map<String, String> EXPLICIT_TYPE_REPLACEMENTS = Map.ofEntries(
            Map.entry("net/minecraftforge/common/ForgeConfigSpec", "net/neoforged/neoforge/common/ModConfigSpec"),
            Map.entry("net/minecraftforge/common/ForgeConfigSpec$Builder", "net/neoforged/neoforge/common/ModConfigSpec$Builder"),
            Map.entry("net/minecraftforge/common/ForgeConfigSpec$BooleanValue", "net/neoforged/neoforge/common/ModConfigSpec$BooleanValue"),
            Map.entry("net/minecraftforge/common/ForgeConfigSpec$IntValue", "net/neoforged/neoforge/common/ModConfigSpec$IntValue"),
            Map.entry("net/minecraftforge/common/ForgeConfigSpec$DoubleValue", "net/neoforged/neoforge/common/ModConfigSpec$DoubleValue"),
            Map.entry("net/minecraftforge/registries/RegistryObject", "java/util/function/Supplier"),
            Map.entry("net/minecraftforge/common/extensions/IForgeMenuType", "net/neoforged/neoforge/common/extensions/IMenuTypeExtension"),
            Map.entry("net/minecraftforge/network/NetworkRegistry", "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkRegistry"),
            Map.entry("net/minecraftforge/network/NetworkDirection", "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkDirection"),
            Map.entry("net/minecraftforge/network/NetworkEvent", "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkEvent"),
            Map.entry("net/minecraftforge/network/NetworkEvent$Context", "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkEvent$Context"),
            Map.entry("net/minecraftforge/network/PacketDistributor", "legacyconnector/v1_20_1/net/minecraftforge/network/PacketDistributor"),
            Map.entry("net/minecraftforge/network/PacketDistributor$PacketTarget", "legacyconnector/v1_20_1/net/minecraftforge/network/PacketDistributor$PacketTarget"),
            Map.entry("net/minecraftforge/network/simple/SimpleChannel", "legacyconnector/v1_20_1/net/minecraftforge/network/simple/SimpleChannel"),
            Map.entry("net/minecraftforge/network/simple/SimpleChannel$MessageBuilder", "legacyconnector/v1_20_1/net/minecraftforge/network/simple/SimpleChannel$MessageBuilder")
    );

    /**
     * Types whose migration is not a safe package-only rewrite. Their behavior, signatures,
     * ownership, or lifecycle contracts need a dedicated adapter or source-level change.
     * The audited Forge 1.20.1 SimpleChannel facade types are explicit connector-owned runtime shims.
     */
    private static final Map<String, String> MANUAL_ADAPTERS = Map.ofEntries(
            Map.entry("net/minecraftforge/common/MinecraftForge",
                    "Static event-bus owner changes to NeoForge; verify bus registration semantics."),
            Map.entry("net/minecraftforge/fml/ModLoadingContext",
                    "Use the injected ModContainer for configuration and extension-point registration."),
            Map.entry("net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext",
                    "Use constructor-injected IEventBus and ModContainer; the old singleton accessor is removed."),
            Map.entry("net/minecraftforge/fml/DistExecutor",
                    "No direct replacement; rework physical-side separation and class loading."),
            Map.entry("net/minecraftforge/registries/ForgeRegistries",
                    "Choose the correct vanilla BuiltInRegistries or NeoForgeRegistries key per registry."),
            Map.entry("net/minecraftforge/client/event/RegisterGuiOverlaysEvent",
                    "Rewrite overlay registration to the layered GUI API."),
            Map.entry("net/minecraftforge/client/gui/overlay/IGuiOverlay",
                    "Rewrite the callback against LayeredDraw.Layer."),
            Map.entry("net/minecraftforge/client/gui/overlay/VanillaGuiOverlay",
                    "Migrate overlay identifiers to VanillaGuiLayers."),
            Map.entry("net/minecraftforge/event/entity/player/PlayerSleepInBedEvent",
                    "Migrate to CanPlayerSleepEvent and preserve the sleep-problem semantics."),
            Map.entry("net/minecraftforge/event/entity/living/LivingHurtEvent",
                    "Split damage mutation and post-damage observation using LivingDamageEvent.Pre/Post."),
            Map.entry("net/minecraftforge/event/entity/living/LivingAttackEvent",
                    "NeoForge 21.1 uses LivingIncomingDamageEvent for the incoming-damage stage; migrate its DamageContainer semantics."),
            Map.entry("net/minecraftforge/event/entity/living/MobSpawnEvent$FinalizeSpawn",
                    "NeoForge 21.1 exposes this as the top-level FinalizeSpawnEvent, not a nested MobSpawnEvent type."),
            Map.entry("net/minecraftforge/fml/common/Mod$EventBusSubscriber",
                    "The mapped nested subscriber annotation is not present in the NeoForge target; migrate to the supported subscriber registration model."),
            Map.entry("net/minecraftforge/fml/common/Mod$EventBusSubscriber$Bus",
                    "The mapped nested subscriber Bus enum is not present in the NeoForge target; migrate bus selection with the subscriber registration model."),
            Map.entry("net/minecraftforge/network/NetworkHooks",
                    "No target class exists at the package-mapped path; rewrite menu opening against the 1.21.1 menu API and preserve extra-data synchronization."),
            Map.entry("net/minecraftforge/registries/IForgeRegistry",
                    "The old registry interface is not present at the mapped target; migrate registry access to the appropriate vanilla/NeoForge registry and holder APIs."),
            Map.entry("net/minecraftforge/common/ForgeMod",
                    "Migrate each referenced attribute to its exact vanilla Attributes or NeoForge equivalent."),
            Map.entry("net/minecraftforge/client/ConfigScreenHandler",
                    "Replace the old config screen extension-point API with IConfigScreenFactory registered through ModContainer."),
            Map.entry("net/minecraftforge/client/ConfigScreenHandler$ConfigScreenFactory",
                    "Replace the old config screen factory with IConfigScreenFactory registered through ModContainer."),
            Map.entry("net/minecraftforge/client/gui/overlay/ForgeGui",
                    "The old relative overlay-height fields have no equivalent contract; adapt HUD layout."),
            Map.entry("net/minecraftforge/eventbus/api/Event$Result",
                    "Do not globally map Result; inspect each concrete interaction event and use its event-specific controls."),
            Map.entry("net/minecraftforge/event/TickEvent",
                    "The old unified tick event model is split into context-specific Pre/Post event types."),
            Map.entry("net/minecraftforge/event/TickEvent$Phase",
                    "The old phase enum is replaced by distinct Pre/Post event classes."),
            Map.entry("net/minecraftforge/event/TickEvent$ClientTickEvent",
                    "Migrate to net.neoforged.neoforge.client.event.ClientTickEvent.Pre/Post."),
            Map.entry("net/minecraftforge/event/TickEvent$LevelTickEvent",
                    "Migrate to the level tick event Pre/Post types under net.neoforged.neoforge.event.tick."),
            Map.entry("net/minecraftforge/event/TickEvent$PlayerTickEvent",
                    "Migrate to PlayerTickEvent.Pre/Post under net.neoforged.neoforge.event.tick; phase logic must change."),
            Map.entry("net/minecraftforge/event/TickEvent$RenderTickEvent",
                    "Migrate render-frame callbacks to RenderFrameEvent.Pre/Post under the client event package."),
            Map.entry("net/minecraftforge/event/TickEvent$ServerTickEvent",
                    "Migrate to ServerTickEvent.Pre/Post under net.neoforged.neoforge.event.tick.")
    );

    /**
     * Vanilla target types whose members are included in focused source-to-target API audits.
     * These types are not NeoForge package moves; they are explicit Minecraft-version review
     * surfaces where signatures or registry-backed values can evolve between 1.20.1 and 1.21.1.
     */
    private static final Set<String> REVIEWED_TARGET_API_TYPES = Set.of(
            // Registry and menu-opening migration surface.
            "net/minecraft/core/registries/BuiltInRegistries",
            "net/minecraft/core/Registry",
            "net/minecraft/core/DefaultedRegistry",
            "net/minecraft/server/level/ServerPlayer",
            "net/minecraft/world/MenuProvider",
            "net/minecraft/network/FriendlyByteBuf",
            "net/minecraft/network/RegistryFriendlyByteBuf",

            // Attribute/effect API migration surface. Minecraft 1.21.1 moved these
            // public values and lookup parameters to Holder-based descriptors.
            "net/minecraft/client/player/LocalPlayer",
            "net/minecraft/world/entity/LivingEntity",
            "net/minecraft/world/entity/player/Player",
            "net/minecraft/world/entity/Mob",
            "net/minecraft/world/entity/PathfinderMob",
            "net/minecraft/world/entity/ai/attributes/Attribute",
            "net/minecraft/world/entity/ai/attributes/AttributeInstance",
            "net/minecraft/world/entity/ai/attributes/AttributeModifier",
            "net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation",
            "net/minecraft/world/entity/ai/attributes/AttributeSupplier",
            "net/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder",
            "net/minecraft/world/entity/ai/attributes/Attributes",
            "net/minecraft/world/effect/MobEffect",
            "net/minecraft/world/effect/MobEffectInstance",
            "net/minecraft/world/effect/MobEffects",

            // Legacy MobType was removed in 1.21.1; include its source owner and
            // the helper that consumed it so the audit exposes the full migration.
            "net/minecraft/world/entity/MobType",
            "net/minecraft/world/item/enchantment/Enchantment",
            "net/minecraft/world/item/enchantment/EnchantmentHelper",
            "net/minecraft/world/item/enchantment/Enchantments"
    );

    private static final List<PackageMove> ORDERED_PACKAGE_MOVES = PACKAGE_MOVES.stream()
            .sorted(Comparator.comparingInt((PackageMove move) -> move.sourcePrefix().length()).reversed())
            .toList();

    private Forge1201MappingCatalog() {}

    public static List<PackageMove> packageMoves() {
        return PACKAGE_MOVES;
    }

    public static Set<String> manualAdapterTypes() {
        return MANUAL_ADAPTERS.keySet();
    }

    public static Map<String, String> explicitTypeReplacements() {
        return EXPLICIT_TYPE_REPLACEMENTS;
    }

    public static Decision classify(String sourceInternalName) {
        if (sourceInternalName == null || sourceInternalName.isBlank()) {
            throw new IllegalArgumentException("A source internal class name is required");
        }

        String explicitReplacement = EXPLICIT_TYPE_REPLACEMENTS.get(sourceInternalName);
        if (explicitReplacement != null) {
            return new Decision(
                    sourceInternalName,
                    Kind.EXPLICIT_TYPE_REPLACEMENT,
                    explicitReplacement,
                    "Explicit Forge-to-NeoForge API type replacement reviewed for this compatibility slice.");
        }

        String manualReason = MANUAL_ADAPTERS.get(sourceInternalName);
        if (manualReason != null) {
            return new Decision(sourceInternalName, Kind.MANUAL_ADAPTER_REQUIRED, null, manualReason);
        }

        for (PackageMove move : ORDERED_PACKAGE_MOVES) {
            if (sourceInternalName.startsWith(move.sourcePrefix())) {
                String target = move.targetPrefix()
                        + sourceInternalName.substring(move.sourcePrefix().length());
                return new Decision(
                        sourceInternalName,
                        Kind.PACKAGE_RELOCATION_CANDIDATE,
                        target,
                        move.evidence() + "; class/member compatibility still requires verification.");
            }
        }

        if (sourceInternalName.startsWith("net/minecraftforge/")) {
            return new Decision(
                    sourceInternalName,
                    Kind.UNMAPPED_FORGE_API,
                    null,
                    "No reviewed package relocation or explicit adapter is recorded for this type.");
        }

        return new Decision(
                sourceInternalName,
                Kind.NOT_FORGE_API,
                sourceInternalName,
                "Outside the Forge API namespace; inspect separately for Minecraft-version changes.");
    }

    /**
     * Returns whether an internal name is in a reviewed NeoForge package relocation or is
     * the target of an explicit Forge -> NeoForge type replacement.
     */
    public static boolean isNeoForgeRelocationTarget(String targetInternalName) {
        if (targetInternalName == null || targetInternalName.isBlank()) return false;
        return PACKAGE_MOVES.stream().anyMatch(move -> targetInternalName.startsWith(move.targetPrefix()))
                || (targetInternalName.startsWith("net/neoforged/")
                        && EXPLICIT_TYPE_REPLACEMENTS.containsValue(targetInternalName));
    }

    public static boolean isReviewedTargetApiType(String targetInternalName) {
        if (targetInternalName == null || targetInternalName.isBlank()) return false;
        return isNeoForgeRelocationTarget(targetInternalName)
                || EXPLICIT_TYPE_REPLACEMENTS.containsValue(targetInternalName)
                || REVIEWED_TARGET_API_TYPES.contains(targetInternalName);
    }

    public static String manualAdapterReason(String sourceInternalName) {
        return MANUAL_ADAPTERS.get(sourceInternalName);
    }
}
