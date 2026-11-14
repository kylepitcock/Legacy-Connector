package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Stands in for 1.20.1's {@code QuadBakingVertexConsumer(Consumer<BakedQuad>)}: it baked a quad and handed it to the consumer as soon as the fourth vertex
 * ended. 1.21.1's class has no constructor argument and waits for {@code bakeQuad()}. The legacy {@code endVertex()} of translated code reaches
 * {@link VertexCompat#endVertex}, which calls {@link #endLegacyVertex()} here.
 */
public class LegacyQuadBakingVertexConsumer extends QuadBakingVertexConsumer {
    private final Consumer<BakedQuad> quadConsumer;
    private int vertices;

    public LegacyQuadBakingVertexConsumer(Consumer<BakedQuad> quadConsumer) {
        this.quadConsumer = quadConsumer;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        vertices++;
        return super.addVertex(x, y, z);
    }

    /** A vertex of the quad is complete; after the fourth the quad goes to the consumer. */
    public void endLegacyVertex() {
        if (vertices >= 4) {
            vertices = 0;
            quadConsumer.accept(bakeQuad());
        }
    }

    /** 1.20.1's {@code QuadBakingVertexConsumer.Buffered}: keeps the last baked quad for {@link #getQuad()}. */
    public static class Buffered extends LegacyQuadBakingVertexConsumer {
        private final BakedQuad[] last;

        public Buffered() {
            this(new BakedQuad[1]);
        }

        private Buffered(BakedQuad[] last) {
            super(q -> last[0] = q);
            this.last = last;
        }

        public BakedQuad getQuad() {
            return last[0];
        }
    }
}
