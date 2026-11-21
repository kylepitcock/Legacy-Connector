package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CompatBufferBuilderTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final class CapturingRenderType extends RenderType {
        private MeshData drawn;

        private CapturingRenderType(boolean sortOnUpload) {
            super("legacy_connector_test", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS,
                    256, false, sortOnUpload, () -> {}, () -> {});
        }

        @Override
        public void draw(MeshData mesh) {
            drawn = mesh;
        }
    }

    @Test
    void renderTypeCompatibilityPathSortsTranslucentQuadsBeforeDrawing() {
        CompatBufferBuilder builder = new CompatBufferBuilder(256);
        CapturingRenderType renderType = new CapturingRenderType(true);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(0.0, 0.0, 0.0);
        builder.vertex(1.0, 0.0, 0.0);
        builder.vertex(1.0, 1.0, 0.0);
        builder.vertex(0.0, 1.0, 0.0);

        try {
            CompatBufferBuilder.drawWithRenderType(renderType, builder, VertexSorting.DISTANCE_TO_ORIGIN);

            assertNotNull(renderType.drawn, "RenderType.draw must receive the built mesh");
            assertNotNull(renderType.drawn.indexBuffer(), "translucent quad indices must be sorted before draw");
        } finally {
            if (renderType.drawn != null) renderType.drawn.close();
            builder.discard();
        }
    }

    @Test
    void nonSortingRenderTypeDrawsWithoutCreatingSortedIndices() {
        CompatBufferBuilder builder = new CompatBufferBuilder(256);
        CapturingRenderType renderType = new CapturingRenderType(false);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(0.0, 0.0, 0.0);
        builder.vertex(1.0, 0.0, 0.0);
        builder.vertex(1.0, 1.0, 0.0);
        builder.vertex(0.0, 1.0, 0.0);

        try {
            CompatBufferBuilder.drawWithRenderType(renderType, builder, VertexSorting.DISTANCE_TO_ORIGIN);

            assertNotNull(renderType.drawn, "RenderType.draw must receive the built mesh");
            org.junit.jupiter.api.Assertions.assertNull(renderType.drawn.indexBuffer(),
                    "non-translucent layers should not allocate sorted quad indices");
        } finally {
            if (renderType.drawn != null) renderType.drawn.close();
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
