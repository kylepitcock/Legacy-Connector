package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockPane;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * 1.7.10's own renderers for vanilla render types that mod blocks use but no static model can show, as built-in block
 * rendering handlers: {@link IsbrhBlockModel} runs them while a chunk is meshed, like a mod's ISBRH. A mod block with such a
 * render type and no handler of its own would otherwise be drawn as a plain cube.
 */
public final class VanillaBlockRenderers {
    private VanillaBlockRenderers() {}

    /** The built-in handler of a vanilla render type, or null. */
    public static ISimpleBlockRenderingHandler forRenderType(int renderType) {
        return renderType == 18 || renderType == 41 ? PANE : null;
    }

    /** The built-in handler for a block: its render type's, or the standard renderer for cubes whose icons depend on the world. */
    public static ISimpleBlockRenderingHandler forBlock(Block block, int renderType) {
        if (renderType == 0 && worldIcons(block)) return STANDARD;
        return forRenderType(renderType);
    }

    /**
     * Whether a mod class overrides {@code getIcon(IBlockAccess, x, y, z, side)}: the faces then depend on the neighbours
     * (Thaumcraft's obsidian totem shows a face on its top block, connected textures), which no static model shows.
     */
    static boolean worldIcons(Block block) {
        for (Class<?> c = block.getClass(); c != null && c != Block.class; c = c.getSuperclass()) {
            if (c.getName().startsWith("legacyconnector.")) continue;
            try {
                c.getDeclaredMethod("getIcon", IBlockAccess.class, int.class, int.class, int.class, int.class);
                return true;
            } catch (NoSuchMethodException ignored) {
            } catch (LinkageError e) {
                return false;
            }
        }
        return false;
    }

    /** 1.7.10's standard cube (render type 0), drawn per position so world-dependent icons show. */
    private static final ISimpleBlockRenderingHandler STANDARD = new ISimpleBlockRenderingHandler() {
        @Override
        public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        }

        @Override
        public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
            return renderer.renderStandardBlock(block, x, y, z);
        }

        @Override
        public boolean shouldRender3DInInventory(int modelId) {
            return true;
        }

        @Override
        public int getRenderId() {
            return 0;
        }
    };

    /**
     * Panes and bars (render types 18 and 41, RenderBlocks.renderBlockPane/renderBlockStainedGlassPane): a 2/16 thick arm
     * towards every connected side, both arms when nothing connects; the face texture on the broad sides, the edge texture
     * ({@code BlockPane.func_150097_e}) on top, bottom and the free ends.
     */
    private static final ISimpleBlockRenderingHandler PANE = new ISimpleBlockRenderingHandler() {
        @Override
        public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        }

        @Override
        public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
            int meta = world.getBlockMetadata(x, y, z);
            IIcon face = renderer.getBlockIconFromSideAndMetadata(block, 0, meta);
            IIcon edge = block instanceof BlockPane pane ? pane.func_150097_e() : face;
            if (edge == null) edge = face;
            if (face == null) return false;
            boolean north = connects(block, world, x, y, z - 1, ForgeDirection.NORTH);
            boolean south = connects(block, world, x, y, z + 1, ForgeDirection.SOUTH);
            boolean west = connects(block, world, x - 1, y, z, ForgeDirection.WEST);
            boolean east = connects(block, world, x + 1, y, z, ForgeDirection.EAST);
            if (!north && !south && !west && !east) north = south = west = east = true;
            boolean top = block.shouldSideBeRendered(world, x, y + 1, z, 1);
            boolean bottom = block.shouldSideBeRendered(world, x, y - 1, z, 0);
            Tessellator t = Tessellator.instance;
            long saved = t.mcc$colorState();
            t.mcc$setColorState(-1L);
            final double lo = 7.0D / 16.0D, hi = 9.0D / 16.0D;
            try {
                if (north || south) {
                    double z1 = north ? 0.0D : lo, z2 = south ? 1.0D : hi;
                    renderer.setRenderBounds(lo, 0.0D, z1, hi, 1.0D, z2);
                    renderer.renderFaceXNeg(block, x, y, z, face);
                    renderer.renderFaceXPos(block, x, y, z, face);
                    if (!north) renderer.renderFaceZNeg(block, x, y, z, edge);
                    if (!south) renderer.renderFaceZPos(block, x, y, z, edge);
                    if (top) renderer.renderFaceYPos(block, x, y, z, edge);
                    if (bottom) renderer.renderFaceYNeg(block, x, y, z, edge);
                }
                if (west || east) {
                    double x1 = west ? 0.0D : lo, x2 = east ? 1.0D : hi;
                    renderer.setRenderBounds(x1, 0.0D, lo, x2, 1.0D, hi);
                    renderer.renderFaceZNeg(block, x, y, z, face);
                    renderer.renderFaceZPos(block, x, y, z, face);
                    if (!west) renderer.renderFaceXNeg(block, x, y, z, edge);
                    if (!east) renderer.renderFaceXPos(block, x, y, z, edge);
                    // the edge texture's stripe runs along v: an east-west arm's top and bottom take it turned by 90 degrees
                    if (top) horizontal(t, x, y + 1.0D, z, x1, x2, lo, hi, edge, true);
                    if (bottom) horizontal(t, x, y, z, x1, x2, lo, hi, edge, false);
                }
            } finally {
                renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                t.mcc$setColorState(saved);
            }
            return true;
        }

        @Override
        public boolean shouldRender3DInInventory(int modelId) {
            return false;
        }

        @Override
        public int getRenderId() {
            return 18;
        }
    };

    private static boolean connects(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        try {
            if (block instanceof BlockPane pane) return pane.canPaneConnectTo(world, x, y, z, direction);
            Block other = world.getBlock(x, y, z);
            return other == block || other != null && other.isOpaqueCube();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** A top (up) or bottom face over x1..x2, z1..z2 with u along z and v along x. */
    private static void horizontal(Tessellator t, double x, double y, double z, double x1, double x2, double z1, double z2, IIcon icon, boolean up) {
        double u1 = icon.getInterpolatedU(z1 * 16.0D), u2 = icon.getInterpolatedU(z2 * 16.0D);
        double v1 = icon.getInterpolatedV(x1 * 16.0D), v2 = icon.getInterpolatedV(x2 * 16.0D);
        double ax = x + x1, bx = x + x2, az = z + z1, bz = z + z2;
        if (up) {
            t.addVertexWithUV(bx, y, bz, u2, v2);
            t.addVertexWithUV(bx, y, az, u1, v2);
            t.addVertexWithUV(ax, y, az, u1, v1);
            t.addVertexWithUV(ax, y, bz, u2, v1);
        } else {
            t.addVertexWithUV(ax, y, bz, u2, v1);
            t.addVertexWithUV(ax, y, az, u1, v1);
            t.addVertexWithUV(bx, y, az, u1, v2);
            t.addVertexWithUV(bx, y, bz, u2, v2);
        }
    }
}
