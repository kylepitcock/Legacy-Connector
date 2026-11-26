package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/** Regression coverage for the shader-state bridge used by AlienEvo's scan-band renderer. */
class AlienEvoRenderStateCompat1201Test {
    @Test
    void positionColorTextureShaderStateIsAvailable() {
        assertNotNull(AlienEvoRenderStateCompat1201.positionColorTexShader());
    }
}
