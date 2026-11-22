package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;

/**
 * GeckoLib-compatible stale-buffer predicate for AlienEvo's Palladium-backed renderer.
 * Reacquisition is deliberately left to the owning Palladium RenderTypeFunction.
 */
public final class AlienEvoBufferRefresh1201 {
    private AlienEvoBufferRefresh1201() {}

    /** Recognizes only the buffer wrappers GeckoLib 4.9.1 explicitly refreshes. */
    public static boolean needsRefresh(VertexConsumer consumer) {
        if (consumer instanceof BufferBuilder builder) return !builder.building;
        if (consumer instanceof OutlineBufferSource.EntityOutlineGenerator outlines) {
            return needsRefresh(outlines.delegate());
        }
        if (consumer instanceof VertexMultiConsumer.Double pair) {
            return needsRefresh(pair.first) || needsRefresh(pair.second);
        }
        return false;
    }
}
