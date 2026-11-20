package org.kylepitcock.legacyconnector.mixin.compat;

import java.util.concurrent.Executor;

import com.mojang.logging.LogUtils;
import legacyconnector.v1_20_1.KubeJSConsoleExecutorCompat;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * KubeJS 2101 can unload scripts after its background executor was shut down.
 * Its ConsoleJS.resetFile() and flush(false) submit log tasks without checking
 * executor state; run those tasks synchronously only after shutdown completes.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.script.ConsoleJS", remap = false)
public abstract class KubeJSConsoleExecutorShutdownRaceMixin {
    @Unique
    private static final Logger LEGACYCONNECTOR_LOGGER = LogUtils.getLogger();

    @Redirect(
            method = {"resetFile", "flush"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/Executor;execute(Ljava/lang/Runnable;)V"
            ),
            require = 0,
            expect = 2
    )
    private void legacyconnector$executeOrRunAfterShutdown(Executor executor, Runnable task) {
        if (!KubeJSConsoleExecutorCompat.executeOrRunAfterShutdown(executor, task)) {
            LEGACYCONNECTOR_LOGGER.warn(
                    "KubeJS ConsoleJS log task found its background executor terminated; "
                            + "ran the task synchronously to keep reload/flush alive"
            );
        }
    }
}
