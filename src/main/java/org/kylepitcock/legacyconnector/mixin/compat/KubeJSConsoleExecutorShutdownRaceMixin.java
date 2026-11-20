package org.kylepitcock.legacyconnector.mixin.compat;

import java.util.concurrent.Executor;
import legacyconnector.v1_20_1.KubeJSConsoleExecutorCompat;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * KubeJS 2101 can unload scripts after its background executor was shut down.
 * Its ConsoleJS.resetFile() submits the log-clear task without checking the
 * executor state; perform that one task synchronously on a shutdown rejection.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.script.ConsoleJS", remap = false)
public abstract class KubeJSConsoleExecutorShutdownRaceMixin {
    @Unique
    private static final Logger LEGACYCONNECTOR_LOGGER = LogUtils.getLogger();

    @Redirect(
            method = "resetFile",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/Executor;execute(Ljava/lang/Runnable;)V"
            ),
            require = 0,
            expect = 1
    )
    @Unique
    private void legacyconnector$executeOrRunAfterShutdown(Executor executor, Runnable task) {
        if (!KubeJSConsoleExecutorCompat.executeOrRunAfterShutdown(executor, task)) {
            LEGACYCONNECTOR_LOGGER.warn(
                    "KubeJS ConsoleJS.resetFile() found its background executor shut down; "
                            + "cleared the script log synchronously to keep reload alive"
            );
        }
    }
}
