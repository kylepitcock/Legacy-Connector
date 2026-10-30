package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * 1.12.2 fluid mesher: writes a liquid block's surface and sides into a legacy buffer (mods mesh fluids of multiblock previews and
 * fake worlds this way). The geometry is the modern liquid renderer's, run over the legacy block access, at the block's absolute
 * position as 1.12 wrote it.
 */
public class BlockFluidRenderer {
    private final BlockColors blockColors;

    public BlockFluidRenderer(BlockColors blockColorsIn) {
        this.blockColors = blockColorsIn;
    }

    /** Meshes the fluid of the state at {@code pos}; false when the state holds no fluid or nothing was written. */
    public boolean renderFluid(IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn) {
        if (blockStateIn == null || blockPosIn == null || bufferBuilderIn == null) return false;
        net.minecraft.world.level.block.state.BlockState modern = StatesBridge1122.toModern(blockStateIn);
        if (modern == null || modern.getFluidState().isEmpty()) return false;
        var pos = new net.minecraft.core.BlockPos(blockPosIn.getX(), blockPosIn.getY(), blockPosIn.getZ());
        Consumer consumer = new Consumer(bufferBuilderIn, pos.getX() & ~15, pos.getY() & ~15, pos.getZ() & ~15);
        try {
            net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderLiquid(pos, new org.kylepitcock.legacyconnector.v1_12_2.client.LegacyBlockGetter1122(blockAccess), consumer, modern, modern.getFluidState());
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("meshing a fluid", e);
        } finally {
            consumer.flush();
        }
        return consumer.written > 0;
    }

    /** Collects each modern vertex and writes it to the legacy buffer in its format's element order, offset to absolute coordinates. */
    private static final class Consumer implements com.mojang.blaze3d.vertex.VertexConsumer {
        private final BufferBuilder buffer;
        private final double offX, offY, offZ;
        private boolean pending;
        private float x, y, z, u, v, nx, ny = 1.0F, nz;
        private int r = 255, g = 255, b = 255, a = 255, light;
        int written;

        Consumer(BufferBuilder buffer, double offX, double offY, double offZ) {
            this.buffer = buffer;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
        }

        void flush() {
            if (!pending) return;
            pending = false;
            VertexFormat format = buffer.getVertexFormat();
            for (int i = 0; i < format.getElementCount(); i++) {
                VertexFormatElement e = format.getElement(i);
                switch (e.getUsage()) {
                    case POSITION -> buffer.pos(x + offX, y + offY, z + offZ);
                    case COLOR -> buffer.color(r, g, b, a);
                    case UV -> {
                        if (e.getIndex() == 0) buffer.tex(u, v);
                        else if (e.getIndex() == 1) buffer.lightmap(light >> 16 & 65535, light & 65535);
                    }
                    case NORMAL -> buffer.normal(nx, ny, nz);
                    default -> {
                    }
                }
            }
            buffer.endVertex();
            written++;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float xIn, float yIn, float zIn) {
            flush();
            pending = true;
            x = xIn;
            y = yIn;
            z = zIn;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int red, int green, int blue, int alpha) {
            r = red;
            g = green;
            b = blue;
            a = alpha;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float uIn, float vIn) {
            u = uIn;
            v = vIn;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int uIn, int vIn) {
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int uIn, int vIn) {
            light = vIn << 16 | uIn & 65535;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            return this;
        }
    }
}
