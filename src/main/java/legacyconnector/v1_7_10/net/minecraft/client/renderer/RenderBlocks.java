package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/**
 * 1.7.10's block renderer as legacy renderers use it: bounds, the six face renderers and standard blocks, drawn with
 * the Tessellator. Vanilla's own world rendering is done by modern baked models; mods' ISBRH renderers run through
 * this class while their geometry is captured into baked models (see IsbrhBlockModel). No ambient occlusion or
 * per-vertex shading: the baked quads are lit by the modern renderer.
 */
public class RenderBlocks {
    /** Draws {@code icon} as two crossing upright squares centred on the block (plants, crops, tall grass). */
    public void drawCrossedSquares(legacyconnector.v1_7_10.net.minecraft.util.IIcon icon, double x, double y, double z, float scale) {
        Tessellator t = Tessellator.instance;
        double u1 = icon.getMinU();
        double u2 = icon.getMaxU();
        double v1 = icon.getMinV();
        double v2 = icon.getMaxV();
        double r = 0.45D * scale;
        double x1 = x + 0.5D - r;
        double x2 = x + 0.5D + r;
        double z1 = z + 0.5D - r;
        double z2 = z + 0.5D + r;
        double top = y + scale;
        t.addVertexWithUV(x1, top, z1, u1, v1);
        t.addVertexWithUV(x1, y, z1, u1, v2);
        t.addVertexWithUV(x2, y, z2, u2, v2);
        t.addVertexWithUV(x2, top, z2, u2, v1);
        t.addVertexWithUV(x2, top, z2, u1, v1);
        t.addVertexWithUV(x2, y, z2, u1, v2);
        t.addVertexWithUV(x1, y, z1, u2, v2);
        t.addVertexWithUV(x1, top, z1, u2, v1);
        t.addVertexWithUV(x1, top, z2, u1, v1);
        t.addVertexWithUV(x1, y, z2, u1, v2);
        t.addVertexWithUV(x2, y, z1, u2, v2);
        t.addVertexWithUV(x2, top, z1, u2, v1);
        t.addVertexWithUV(x2, top, z1, u1, v1);
        t.addVertexWithUV(x2, y, z1, u1, v2);
        t.addVertexWithUV(x1, y, z2, u2, v2);
        t.addVertexWithUV(x1, top, z2, u2, v1);
    }

    public IBlockAccess blockAccess;
    public IIcon overrideBlockTexture;
    public boolean flipTexture;
    public boolean renderAllFaces;
    public boolean useInventoryTint = true;
    public boolean renderFromInside;
    public double renderMinX, renderMinY, renderMinZ, renderMaxX = 1.0, renderMaxY = 1.0, renderMaxZ = 1.0;
    public boolean lockBlockBounds;
    public boolean partialRenderBounds;
    public boolean enableAO;
    public int uvRotateEast, uvRotateWest, uvRotateSouth, uvRotateNorth, uvRotateTop, uvRotateBottom;

    public RenderBlocks() {
    }

    public RenderBlocks(IBlockAccess access) {
        this.blockAccess = access;
    }

    /** Whether a block with this render type is drawn as a 3D block in inventories (vanilla's list). */
    public static boolean renderItemIn3d(int renderType) {
        return switch (renderType) {
            case 0, 31, 39, 13, 10, 11, 27, 22, 21, 16, 26, 32, 34, 35 -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ bounds

    public void setRenderBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
        if (!lockBlockBounds) {
            renderMinX = x1;
            renderMaxX = x2;
            renderMinY = y1;
            renderMaxY = y2;
            renderMinZ = z1;
            renderMaxZ = z2;
            partialRenderBounds = renderMinX > 0.0D || renderMaxX < 1.0D || renderMinY > 0.0D || renderMaxY < 1.0D
                    || renderMinZ > 0.0D || renderMaxZ < 1.0D;
        }
    }

    public void setRenderBoundsFromBlock(Block block) {
        if (!lockBlockBounds) {
            renderMinX = block.getBlockBoundsMinX();
            renderMaxX = block.getBlockBoundsMaxX();
            renderMinY = block.getBlockBoundsMinY();
            renderMaxY = block.getBlockBoundsMaxY();
            renderMinZ = block.getBlockBoundsMinZ();
            renderMaxZ = block.getBlockBoundsMaxZ();
            partialRenderBounds = renderMinX > 0.0D || renderMaxX < 1.0D || renderMinY > 0.0D || renderMaxY < 1.0D
                    || renderMinZ > 0.0D || renderMaxZ < 1.0D;
        }
    }

    public void overrideBlockBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
        renderMinX = x1;
        renderMaxX = x2;
        renderMinY = y1;
        renderMaxY = y2;
        renderMinZ = z1;
        renderMaxZ = z2;
        lockBlockBounds = true;
        partialRenderBounds = renderMinX > 0.0D || renderMaxX < 1.0D || renderMinY > 0.0D || renderMaxY < 1.0D
                || renderMinZ > 0.0D || renderMaxZ < 1.0D;
    }

    public void unlockBlockBounds() {
        lockBlockBounds = false;
    }

    public void setOverrideBlockTexture(IIcon icon) {
        this.overrideBlockTexture = icon;
    }

    public void clearOverrideBlockTexture() {
        this.overrideBlockTexture = null;
    }

    public boolean hasOverrideBlockTexture() {
        return this.overrideBlockTexture != null;
    }

    // ------------------------------------------------------------------ icons

    public IIcon getBlockIcon(Block block, IBlockAccess access, int x, int y, int z, int side) {
        return getIconSafe(block, access, x, y, z, side);
    }

    public IIcon getBlockIconFromSideAndMetadata(Block block, int side, int meta) {
        return getIconOrNull(overrideBlockTexture, block.getIcon(side, meta));
    }

    public IIcon getBlockIconFromSide(Block block, int side) {
        return getIconOrNull(overrideBlockTexture, block.getIcon(side, 0));
    }

    private static IIcon getIconOrNull(IIcon override, IIcon icon) {
        return override != null ? override : icon;
    }

    private IIcon getIconSafe(Block block, IBlockAccess access, int x, int y, int z, int side) {
        if (overrideBlockTexture != null) return overrideBlockTexture;
        IIcon icon = block.getIcon(access, x, y, z, side);
        return icon != null ? icon : block.getIcon(side, access.getBlockMetadata(x, y, z));
    }

    // ------------------------------------------------------------------ blocks

    /**
     * Draws every face that is not hidden by an opaque neighbour (vanilla's standard block). An untinted block's faces carry no
     * colour, so the modern pipeline shades them (with AO); a tinted one (colorMultiplier) gets 1.7.10's colour per face, its
     * face shading included. Either way the colour a renderer set before is restored afterwards.
     */
    public boolean renderStandardBlock(Block block, int x, int y, int z) {
        int c = 0xFFFFFF;
        if (blockAccess != null) {
            try {
                c = block.colorMultiplier(blockAccess, x, y, z);
            } catch (RuntimeException e) {
                c = 0xFFFFFF;
            }
        }
        return renderStandardBlockWithColorMultiplier(block, x, y, z, (c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F);
    }

    public boolean renderStandardBlockWithColorMultiplier(Block block, int x, int y, int z, float r, float g, float b) {
        Tessellator t = Tessellator.instance;
        long saved = t.mcc$colorState();
        boolean tinted = r < 1.0F || g < 1.0F || b < 1.0F;
        boolean any = false;
        try {
            if (renderAllFaces || block.shouldSideBeRendered(blockAccess, x, y - 1, z, 0)) {
                faceColor(t, tinted, r, g, b, 0.5F);
                renderFaceYNeg(block, x, y, z, getBlockIcon(block, blockAccess, x, y, z, 0));
                any = true;
            }
            if (renderAllFaces || block.shouldSideBeRendered(blockAccess, x, y + 1, z, 1)) {
                faceColor(t, tinted, r, g, b, 1.0F);
                renderFaceYPos(block, x, y, z, getBlockIcon(block, blockAccess, x, y, z, 1));
                any = true;
            }
            if (renderAllFaces || block.shouldSideBeRendered(blockAccess, x, y, z - 1, 2)) {
                faceColor(t, tinted, r, g, b, 0.8F);
                renderFaceZNeg(block, x, y, z, getBlockIcon(block, blockAccess, x, y, z, 2));
                any = true;
            }
            if (renderAllFaces || block.shouldSideBeRendered(blockAccess, x, y, z + 1, 3)) {
                faceColor(t, tinted, r, g, b, 0.8F);
                renderFaceZPos(block, x, y, z, getBlockIcon(block, blockAccess, x, y, z, 3));
                any = true;
            }
            if (renderAllFaces || block.shouldSideBeRendered(blockAccess, x - 1, y, z, 4)) {
                faceColor(t, tinted, r, g, b, 0.6F);
                renderFaceXNeg(block, x, y, z, getBlockIcon(block, blockAccess, x, y, z, 4));
                any = true;
            }
            if (renderAllFaces || block.shouldSideBeRendered(blockAccess, x + 1, y, z, 5)) {
                faceColor(t, tinted, r, g, b, 0.6F);
                renderFaceXPos(block, x, y, z, getBlockIcon(block, blockAccess, x, y, z, 5));
                any = true;
            }
        } finally {
            t.mcc$setColorState(saved);
        }
        return any;
    }

    private static void faceColor(Tessellator t, boolean tinted, float r, float g, float b, float shade) {
        if (tinted) t.setColorOpaque_F(r * shade, g * shade, b * shade);
        else t.mcc$setColorState(-1L);
    }

    public boolean renderStandardBlockWithAmbientOcclusion(Block block, int x, int y, int z, float r, float g, float b) {
        return renderStandardBlockWithColorMultiplier(block, x, y, z, r, g, b);
    }

    public boolean renderStandardBlockWithAmbientOcclusionPartial(Block block, int x, int y, int z, float r, float g, float b) {
        return renderStandardBlockWithColorMultiplier(block, x, y, z, r, g, b);
    }

    public boolean renderBlockAllFaces(Block block, int x, int y, int z) {
        boolean old = renderAllFaces;
        renderAllFaces = true;
        try {
            return renderStandardBlock(block, x, y, z);
        } finally {
            renderAllFaces = old;
        }
    }

    public void renderBlockUsingTexture(Block block, int x, int y, int z, IIcon icon) {
        IIcon old = overrideBlockTexture;
        overrideBlockTexture = icon;
        try {
            renderStandardBlock(block, x, y, z);
        } finally {
            overrideBlockTexture = old;
        }
    }

    /** The six faces of the current bounds at the origin, with the block's icons for metadata {@code meta}. */
    public void renderBlockAsItem(Block block, int meta, float brightness) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator t = Tessellator.instance;
        block.setBlockBoundsForItemRender();
        setRenderBoundsFromBlock(block);
        for (int side = 0; side < 6; side++) {
            t.startDrawingQuads();
            switch (side) {
                case 0 -> {
                    t.setNormal(0.0F, -1.0F, 0.0F);
                    renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, getBlockIconFromSideAndMetadata(block, 0, meta));
                }
                case 1 -> {
                    t.setNormal(0.0F, 1.0F, 0.0F);
                    renderFaceYPos(block, 0.0D, 0.0D, 0.0D, getBlockIconFromSideAndMetadata(block, 1, meta));
                }
                case 2 -> {
                    t.setNormal(0.0F, 0.0F, -1.0F);
                    renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, getBlockIconFromSideAndMetadata(block, 2, meta));
                }
                case 3 -> {
                    t.setNormal(0.0F, 0.0F, 1.0F);
                    renderFaceZPos(block, 0.0D, 0.0D, 0.0D, getBlockIconFromSideAndMetadata(block, 3, meta));
                }
                case 4 -> {
                    t.setNormal(-1.0F, 0.0F, 0.0F);
                    renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, getBlockIconFromSideAndMetadata(block, 4, meta));
                }
                default -> {
                    t.setNormal(1.0F, 0.0F, 0.0F);
                    renderFaceXPos(block, 0.0D, 0.0D, 0.0D, getBlockIconFromSideAndMetadata(block, 5, meta));
                }
            }
            t.draw();
        }
    }

    /** Two crossed vertical planes (plants). */
    public boolean renderCrossedSquares(Block block, int x, int y, int z) {
        IIcon icon = getIconSafe(block, blockAccess, x, y, z, 0);
        if (icon == null) return false;
        Tessellator t = Tessellator.instance;
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        double a = x + 0.5D - 0.45D, b = x + 0.5D + 0.45D, c = z + 0.5D - 0.45D, d = z + 0.5D + 0.45D;
        t.addVertexWithUV(a, y + 1.0D, c, u0, v0);
        t.addVertexWithUV(a, y, c, u0, v1);
        t.addVertexWithUV(b, y, d, u1, v1);
        t.addVertexWithUV(b, y + 1.0D, d, u1, v0);
        t.addVertexWithUV(b, y + 1.0D, d, u0, v0);
        t.addVertexWithUV(b, y, d, u0, v1);
        t.addVertexWithUV(a, y, c, u1, v1);
        t.addVertexWithUV(a, y + 1.0D, c, u1, v0);
        t.addVertexWithUV(a, y + 1.0D, d, u0, v0);
        t.addVertexWithUV(a, y, d, u0, v1);
        t.addVertexWithUV(b, y, c, u1, v1);
        t.addVertexWithUV(b, y + 1.0D, c, u1, v0);
        t.addVertexWithUV(b, y + 1.0D, c, u0, v0);
        t.addVertexWithUV(b, y, c, u0, v1);
        t.addVertexWithUV(a, y, d, u1, v1);
        t.addVertexWithUV(a, y + 1.0D, d, u1, v0);
        return true;
    }

    // ------------------------------------------------------------------ faces

    private IIcon pick(IIcon icon) {
        return overrideBlockTexture != null ? overrideBlockTexture : icon;
    }

    public void renderFaceYNeg(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        icon = pick(icon);
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinX * 16.0D), u1 = icon.getInterpolatedU(renderMaxX * 16.0D);
        double v0 = icon.getInterpolatedV(renderMinZ * 16.0D), v1 = icon.getInterpolatedV(renderMaxZ * 16.0D);
        if (renderMinX < 0.0D || renderMaxX > 1.0D) {
            u0 = icon.getMinU();
            u1 = icon.getMaxU();
        }
        if (renderMinZ < 0.0D || renderMaxZ > 1.0D) {
            v0 = icon.getMinV();
            v1 = icon.getMaxV();
        }
        double x1 = x + renderMinX, x2 = x + renderMaxX, yy = y + renderMinY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        t.addVertexWithUV(x1, yy, z2, u0, v1);
        t.addVertexWithUV(x1, yy, z1, u0, v0);
        t.addVertexWithUV(x2, yy, z1, u1, v0);
        t.addVertexWithUV(x2, yy, z2, u1, v1);
    }

    public void renderFaceYPos(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        icon = pick(icon);
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinX * 16.0D), u1 = icon.getInterpolatedU(renderMaxX * 16.0D);
        double v0 = icon.getInterpolatedV(renderMinZ * 16.0D), v1 = icon.getInterpolatedV(renderMaxZ * 16.0D);
        if (renderMinX < 0.0D || renderMaxX > 1.0D) {
            u0 = icon.getMinU();
            u1 = icon.getMaxU();
        }
        if (renderMinZ < 0.0D || renderMaxZ > 1.0D) {
            v0 = icon.getMinV();
            v1 = icon.getMaxV();
        }
        double x1 = x + renderMinX, x2 = x + renderMaxX, yy = y + renderMaxY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        t.addVertexWithUV(x2, yy, z2, u1, v1);
        t.addVertexWithUV(x2, yy, z1, u1, v0);
        t.addVertexWithUV(x1, yy, z1, u0, v0);
        t.addVertexWithUV(x1, yy, z2, u0, v1);
    }

    public void renderFaceZNeg(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        icon = pick(icon);
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinX * 16.0D), u1 = icon.getInterpolatedU(renderMaxX * 16.0D);
        double v0 = icon.getInterpolatedV(16.0D - renderMaxY * 16.0D), v1 = icon.getInterpolatedV(16.0D - renderMinY * 16.0D);
        if (flipTexture) {
            double tmp = u0;
            u0 = u1;
            u1 = tmp;
        }
        if (renderMinX < 0.0D || renderMaxX > 1.0D) {
            u0 = icon.getMinU();
            u1 = icon.getMaxU();
        }
        if (renderMinY < 0.0D || renderMaxY > 1.0D) {
            v0 = icon.getMinV();
            v1 = icon.getMaxV();
        }
        double x1 = x + renderMinX, x2 = x + renderMaxX, y1 = y + renderMinY, y2 = y + renderMaxY, zz = z + renderMinZ;
        t.addVertexWithUV(x1, y2, zz, u1, v0);
        t.addVertexWithUV(x2, y2, zz, u0, v0);
        t.addVertexWithUV(x2, y1, zz, u0, v1);
        t.addVertexWithUV(x1, y1, zz, u1, v1);
    }

    public void renderFaceZPos(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        icon = pick(icon);
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinX * 16.0D), u1 = icon.getInterpolatedU(renderMaxX * 16.0D);
        double v0 = icon.getInterpolatedV(16.0D - renderMaxY * 16.0D), v1 = icon.getInterpolatedV(16.0D - renderMinY * 16.0D);
        if (flipTexture) {
            double tmp = u0;
            u0 = u1;
            u1 = tmp;
        }
        if (renderMinX < 0.0D || renderMaxX > 1.0D) {
            u0 = icon.getMinU();
            u1 = icon.getMaxU();
        }
        if (renderMinY < 0.0D || renderMaxY > 1.0D) {
            v0 = icon.getMinV();
            v1 = icon.getMaxV();
        }
        double x1 = x + renderMinX, x2 = x + renderMaxX, y1 = y + renderMinY, y2 = y + renderMaxY, zz = z + renderMaxZ;
        t.addVertexWithUV(x1, y2, zz, u0, v0);
        t.addVertexWithUV(x1, y1, zz, u0, v1);
        t.addVertexWithUV(x2, y1, zz, u1, v1);
        t.addVertexWithUV(x2, y2, zz, u1, v0);
    }

    public void renderFaceXNeg(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        icon = pick(icon);
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinZ * 16.0D), u1 = icon.getInterpolatedU(renderMaxZ * 16.0D);
        double v0 = icon.getInterpolatedV(16.0D - renderMaxY * 16.0D), v1 = icon.getInterpolatedV(16.0D - renderMinY * 16.0D);
        if (flipTexture) {
            double tmp = u0;
            u0 = u1;
            u1 = tmp;
        }
        if (renderMinZ < 0.0D || renderMaxZ > 1.0D) {
            u0 = icon.getMinU();
            u1 = icon.getMaxU();
        }
        if (renderMinY < 0.0D || renderMaxY > 1.0D) {
            v0 = icon.getMinV();
            v1 = icon.getMaxV();
        }
        double xx = x + renderMinX, y1 = y + renderMinY, y2 = y + renderMaxY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        t.addVertexWithUV(xx, y2, z2, u1, v0);
        t.addVertexWithUV(xx, y2, z1, u0, v0);
        t.addVertexWithUV(xx, y1, z1, u0, v1);
        t.addVertexWithUV(xx, y1, z2, u1, v1);
    }

    public void renderFaceXPos(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        icon = pick(icon);
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinZ * 16.0D), u1 = icon.getInterpolatedU(renderMaxZ * 16.0D);
        double v0 = icon.getInterpolatedV(16.0D - renderMaxY * 16.0D), v1 = icon.getInterpolatedV(16.0D - renderMinY * 16.0D);
        if (flipTexture) {
            double tmp = u0;
            u0 = u1;
            u1 = tmp;
        }
        if (renderMinZ < 0.0D || renderMaxZ > 1.0D) {
            u0 = icon.getMinU();
            u1 = icon.getMaxU();
        }
        if (renderMinY < 0.0D || renderMaxY > 1.0D) {
            v0 = icon.getMinV();
            v1 = icon.getMaxV();
        }
        double xx = x + renderMaxX, y1 = y + renderMinY, y2 = y + renderMaxY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        t.addVertexWithUV(xx, y1, z2, u0, v1);
        t.addVertexWithUV(xx, y1, z1, u1, v1);
        t.addVertexWithUV(xx, y2, z1, u1, v0);
        t.addVertexWithUV(xx, y2, z2, u0, v0);
    }

    private static RenderBlocks mcc$instance;

    public static synchronized RenderBlocks getInstance() {
        if (mcc$instance == null) mcc$instance = new RenderBlocks();
        return mcc$instance;
    }

    /** Draws the block at the position the way its render type asks (standard cube or crossed squares; other types go through their own renderer). */
    public boolean renderBlockByRenderType(Block block, int x, int y, int z) {
        return block.getRenderType() == 1 ? renderCrossedSquares(block, x, y, z) : renderStandardBlock(block, x, y, z);
    }
}
