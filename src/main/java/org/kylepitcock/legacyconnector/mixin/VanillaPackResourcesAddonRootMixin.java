package org.kylepitcock.legacyconnector.mixin;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.AddonResourceRoots1201;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes extracted mod-layer addon roots visible to Minecraft's vanilla pack builder. */
@Mixin(VanillaPackResourcesBuilder.class)
public abstract class VanillaPackResourcesAddonRootMixin {
    @Shadow
    private void pushRootPath(Path rootPath) {
        throw new AssertionError();
    }

    @Redirect(
            method = "pushJarResources",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/ClassLoader;getResources(Ljava/lang/String;)Ljava/util/Enumeration;"),
            require = 0)
    private Enumeration<URL> legacyconnector$appendAddonResources(
            ClassLoader classLoader, String resourceName) throws IOException {
        return AddonResourceRoots1201.appendAddonRootMarkers(
                resourceName, classLoader.getResources(resourceName));
    }

    @Redirect(
            method = "pushJarResources",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/ClassLoader;getSystemResources(Ljava/lang/String;)Ljava/util/Enumeration;"),
            require = 0)
    private static Enumeration<URL> legacyconnector$appendSystemAddonResources(
            String resourceName) throws IOException {
        return AddonResourceRoots1201.appendAddonRootMarkers(
                resourceName, ClassLoader.getSystemResources(resourceName));
    }

    @Inject(method = "pushJarResources", at = @At("RETURN"), require = 0)
    private void legacyconnector$registerExtractedAddonRoots(
            CallbackInfoReturnable<VanillaPackResourcesBuilder> callback) {
        for (Path addonRoot : AddonResourceRoots1201.addonRoots()) {
            this.pushRootPath(addonRoot);
        }
    }
}
