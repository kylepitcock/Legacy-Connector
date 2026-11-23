package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Reconstructs Forge 1.20.1's combined render-stage PoseStack for AlienEvo.
 *
 * <p>NeoForge 1.21.1 exposes the camera/model-view transform separately from the
 * stage-local PoseStack. Some stages, including AFTER_SOLID_BLOCKS, are dispatched
 * without a PoseStack and NeoForge supplies an identity fallback. AlienEvo's
 * prepass copies the event stack into cached bone matrices, while its deferred
 * effect renderer bakes those matrices into vertices before resetting the global
 * model-view stack for the final draw. The camera transform therefore must be
 * composed into the PoseStack before the legacy render callbacks consume it.</p>
 *
 * <p>The composition is modelView * stagePose, matching the order in which the
 * global view transform and the local stage transform act on a vertex. The event's
 * matrices are copied; neither the event nor its PoseStack is mutated.</p>
 */
public final class AlienEvoRenderStagePose1201Runtime {
    private AlienEvoRenderStagePose1201Runtime() {}

    public static PoseStack fromRenderStageEvent(RenderLevelStageEvent event) {
        Objects.requireNonNull(event, "event");
        return compose(event.getModelViewMatrix(), event.getPoseStack());
    }

    static PoseStack compose(Matrix4f modelViewMatrix, PoseStack stagePoseStack) {
        Objects.requireNonNull(modelViewMatrix, "modelViewMatrix");
        Objects.requireNonNull(stagePoseStack, "stagePoseStack");

        PoseStack combined = new PoseStack();
        combined.last().pose().set(modelViewMatrix).mul(stagePoseStack.last().pose());
        combined.last().normal()
                .set(modelViewMatrix.normal(new Matrix3f()))
                .mul(stagePoseStack.last().normal());
        return combined;
    }
}
