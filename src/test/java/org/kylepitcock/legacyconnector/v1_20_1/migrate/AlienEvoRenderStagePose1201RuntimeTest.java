package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class AlienEvoRenderStagePose1201RuntimeTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void composesCameraModelViewBeforeTheStageLocalPose() {
        Matrix4f modelView = new Matrix4f()
                .rotationY(0.61f)
                .translate(3.0f, -2.0f, 5.0f);
        PoseStack stagePose = new PoseStack();
        stagePose.translate(7.0f, 11.0f, -13.0f);
        stagePose.scale(2.0f, 3.0f, 4.0f);

        Matrix4f modelViewBefore = new Matrix4f(modelView);
        Matrix4f stagePoseBefore = new Matrix4f(stagePose.last().pose());
        Matrix3f stageNormalBefore = new Matrix3f(stagePose.last().normal());

        Matrix4f expectedPose = new Matrix4f(modelViewBefore).mul(stagePoseBefore);
        Matrix3f expectedNormal = modelViewBefore.normal(new Matrix3f()).mul(stageNormalBefore);

        PoseStack combined = AlienEvoRenderStagePose1201Runtime.compose(modelView, stagePose);

        assertTrue(expectedPose.equals(combined.last().pose(), EPSILON),
                "the combined pose must be modelView * stagePose");
        assertTrue(expectedNormal.equals(combined.last().normal(), EPSILON),
                "the combined normal must be inverse-transpose(modelView) * stageNormal");
        assertTrue(modelViewBefore.equals(modelView, EPSILON), "the event model-view matrix must not be mutated");
        assertTrue(stagePoseBefore.equals(stagePose.last().pose(), EPSILON),
                "the event stage pose must not be mutated");
        assertTrue(stageNormalBefore.equals(stagePose.last().normal(), EPSILON),
                "the event stage normal must not be mutated");
    }

    @Test
    void identityStagePosePreservesTheModelViewForStagesWithoutAPoseStack() {
        Matrix4f modelView = new Matrix4f().rotationXYZ(0.27f, -0.43f, 0.19f);
        PoseStack identityStagePose = new PoseStack();

        PoseStack combined = AlienEvoRenderStagePose1201Runtime.compose(modelView, identityStagePose);

        assertTrue(modelView.equals(combined.last().pose(), EPSILON));
        assertTrue(modelView.normal(new Matrix3f()).equals(combined.last().normal(), EPSILON));
    }
}
