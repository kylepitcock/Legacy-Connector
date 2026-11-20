package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;

class CompatBufferBuilderTest {
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
