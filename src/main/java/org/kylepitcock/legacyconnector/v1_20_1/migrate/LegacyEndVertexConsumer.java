package org.kylepitcock.legacyconnector.v1_20_1.migrate;

/**
 * Marker for a compatibility vertex consumer that still buffers an old-style
 * vertex until its legacy endVertex() callback arrives.
 */
public interface LegacyEndVertexConsumer {
    void legacyEndVertex();
}
