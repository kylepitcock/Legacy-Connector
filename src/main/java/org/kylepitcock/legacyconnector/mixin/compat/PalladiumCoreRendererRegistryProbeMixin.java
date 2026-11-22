package org.kylepitcock.legacyconnector.mixin.compat;

import java.util.Map;
import legacyconnector.v1_20_1.compat.PalladiumRendererRegistryDebugCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures PalladiumCore's pending entity-renderer registrations around the real NeoForge event callback.
 * The helper is disabled unless its dedicated JVM system property is enabled.
 */
@Pseudo
@Mixin(targets = "net.threetag.palladiumcore.registry.client.forge.EntityRendererRegistryImpl")
public abstract class PalladiumCoreRendererRegistryProbeMixin {
    @Shadow
    @Final
    private static Map<?, ?> RENDERERS;

    @Inject(method = "renderers", at = @At("HEAD"), require = 0)
    private static void legacyconnector$beforeRendererRegistration(
            net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event,
            CallbackInfo ci) {
        PalladiumRendererRegistryDebugCompat.logRegistryMap("before", RENDERERS);
    }

    @Inject(method = "renderers", at = @At("RETURN"), require = 0)
    private static void legacyconnector$afterRendererRegistration(
            net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event,
            CallbackInfo ci) {
        PalladiumRendererRegistryDebugCompat.logRegistryMap("after", RENDERERS);
    }
}
