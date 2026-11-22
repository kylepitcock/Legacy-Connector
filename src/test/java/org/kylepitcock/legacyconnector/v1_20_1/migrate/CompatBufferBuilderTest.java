package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CompatBufferBuilderTest {

    private static final class CapturingDraw implements Consumer<MeshData> {
        private MeshData drawn;

        @Override
        public void accept(MeshData mesh) {
            drawn = mesh;
        }
    }

    @Test
    void renderTypeCompatibilityPathSortsTranslucentQuadsBeforeDrawing() {
        CompatBufferBuilder builder = new CompatBufferBuilder(256);
        CapturingDraw draw = new CapturingDraw();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(0.0, 0.0, 0.0);
        builder.vertex(1.0, 0.0, 0.0);
        builder.vertex(1.0, 1.0, 0.0);
        builder.vertex(0.0, 1.0, 0.0);

        try {
            CompatBufferBuilder.drawWithRenderType(builder, true, VertexSorting.DISTANCE_TO_ORIGIN, draw);

            assertNotNull(draw.drawn, "the draw callback must receive the built mesh");
            assertNotNull(draw.drawn.indexBuffer(), "translucent quad indices must be sorted before draw");
        } finally {
            if (draw.drawn != null) draw.drawn.close();
            builder.discard();
        }
    }

    @Test
    void nonSortingRenderTypeDrawsWithoutCreatingSortedIndices() {
        CompatBufferBuilder builder = new CompatBufferBuilder(256);
        CapturingDraw draw = new CapturingDraw();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(0.0, 0.0, 0.0);
        builder.vertex(1.0, 0.0, 0.0);
        builder.vertex(1.0, 1.0, 0.0);
        builder.vertex(0.0, 1.0, 0.0);

        try {
            CompatBufferBuilder.drawWithRenderType(builder, false, VertexSorting.DISTANCE_TO_ORIGIN, draw);

            assertNotNull(draw.drawn, "the draw callback must receive the built mesh");
            org.junit.jupiter.api.Assertions.assertNull(draw.drawn.indexBuffer(),
                    "non-translucent layers should not allocate sorted quad indices");
        } finally {
            if (draw.drawn != null) draw.drawn.close();
            builder.discard();
        }
    }

    @Test
    void legacyBuildingAliasTracksTheActiveBuffer() {
        CompatBufferBuilder builder = new CompatBufferBuilder(256);

        assertFalse(builder.building());
        assertFalse(builder.isBuilding());

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        assertTrue(builder.building());
        assertTrue(builder.isBuilding());

        builder.discard();

        assertFalse(builder.building());
        assertFalse(builder.isBuilding());
    }
}
