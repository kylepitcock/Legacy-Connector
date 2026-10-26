package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockAnvil;
import legacyconnector.v1_7_10.net.minecraft.block.BlockBeacon;
import legacyconnector.v1_7_10.net.minecraft.block.BlockBed;
import legacyconnector.v1_7_10.net.minecraft.block.BlockBrewingStand;
import legacyconnector.v1_7_10.net.minecraft.block.BlockCauldron;
import legacyconnector.v1_7_10.net.minecraft.block.BlockCocoa;
import legacyconnector.v1_7_10.net.minecraft.block.BlockDirectional;
import legacyconnector.v1_7_10.net.minecraft.block.BlockDoublePlant;
import legacyconnector.v1_7_10.net.minecraft.block.BlockDragonEgg;
import legacyconnector.v1_7_10.net.minecraft.block.BlockEndPortalFrame;
import legacyconnector.v1_7_10.net.minecraft.block.BlockFence;
import legacyconnector.v1_7_10.net.minecraft.block.BlockFenceGate;
import legacyconnector.v1_7_10.net.minecraft.block.BlockFire;
import legacyconnector.v1_7_10.net.minecraft.block.BlockFlowerPot;
import legacyconnector.v1_7_10.net.minecraft.block.BlockHopper;
import legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid;
import legacyconnector.v1_7_10.net.minecraft.block.BlockPane;
import legacyconnector.v1_7_10.net.minecraft.block.BlockPistonBase;
import legacyconnector.v1_7_10.net.minecraft.block.BlockPistonExtension;
import legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase;
import legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneComparator;
import legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneDiode;
import legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneRepeater;
import legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneWire;
import legacyconnector.v1_7_10.net.minecraft.block.BlockStainedGlassPane;
import legacyconnector.v1_7_10.net.minecraft.block.BlockStairs;
import legacyconnector.v1_7_10.net.minecraft.block.BlockStem;
import legacyconnector.v1_7_10.net.minecraft.block.BlockTripWire;
import legacyconnector.v1_7_10.net.minecraft.block.BlockWall;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFlowerPot;
import legacyconnector.v1_7_10.net.minecraft.util.Direction;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10's block renderer: bounds, the six face renderers (with 1.7.10's UV rotations), standard blocks and every vanilla
 * render type, drawn with the Tessellator. Vanilla's own world rendering is done by modern baked models; mods' ISBRH renderers
 * (and the vanilla shapes they reuse: stairs, fences, panes, liquids, torches...) run through this class while their geometry
 * is captured into baked models (see IsbrhBlockModel). Standard blocks leave untinted faces uncoloured so the modern pipeline
 * shades them with its own ambient occlusion; the special shapes set 1.7.10's flat face colours themselves.
 */
public class RenderBlocks {
    public IBlockAccess blockAccess;
    public IIcon overrideBlockTexture;
    public boolean flipTexture;
    /** Mirrors the U of north and east faces (1.7.10 sets it while drawing ambient-occluded faces of some blocks). */
    public boolean field_152631_f;
    public boolean renderAllFaces;
    public static boolean fancyGrass = true;
    public boolean useInventoryTint = true;
    public boolean renderFromInside;
    public double renderMinX, renderMinY, renderMinZ, renderMaxX = 1.0, renderMaxY = 1.0, renderMaxZ = 1.0;
    public boolean lockBlockBounds;
    public boolean partialRenderBounds;
    /** The client (null on a dedicated server: mods build renderers there too, e.g. GuiScreen's static RenderItem). */
    public Minecraft minecraftRB = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? Minecraft.getMinecraft() : null;
    public int uvRotateEast, uvRotateWest, uvRotateSouth, uvRotateNorth, uvRotateTop, uvRotateBottom;
    public boolean enableAO;
    public float aoLightValueScratchXYZNNN, aoLightValueScratchXYNN, aoLightValueScratchXYZNNP, aoLightValueScratchYZNN, aoLightValueScratchYZNP,
            aoLightValueScratchXYZPNN, aoLightValueScratchXYPN, aoLightValueScratchXYZPNP, aoLightValueScratchXYZNPN, aoLightValueScratchXYNP,
            aoLightValueScratchXYZNPP, aoLightValueScratchYZPN, aoLightValueScratchXYZPPN, aoLightValueScratchXYPP, aoLightValueScratchYZPP,
            aoLightValueScratchXYZPPP, aoLightValueScratchXZNN, aoLightValueScratchXZPN, aoLightValueScratchXZNP, aoLightValueScratchXZPP;
    public int aoBrightnessXYZNNN, aoBrightnessXYNN, aoBrightnessXYZNNP, aoBrightnessYZNN, aoBrightnessYZNP, aoBrightnessXYZPNN, aoBrightnessXYPN,
            aoBrightnessXYZPNP, aoBrightnessXYZNPN, aoBrightnessXYNP, aoBrightnessXYZNPP, aoBrightnessYZPN, aoBrightnessXYZPPN, aoBrightnessXYPP,
            aoBrightnessYZPP, aoBrightnessXYZPPP, aoBrightnessXZNN, aoBrightnessXZPN, aoBrightnessXZNP, aoBrightnessXZPP;
    /** Per-corner light and colour of the face being drawn while {@link #enableAO} is set. */
    public int brightnessTopLeft, brightnessBottomLeft, brightnessBottomRight, brightnessTopRight;
    public float colorRedTopLeft, colorRedBottomLeft, colorRedBottomRight, colorRedTopRight;
    public float colorGreenTopLeft, colorGreenBottomLeft, colorGreenBottomRight, colorGreenTopRight;
    public float colorBlueTopLeft, colorBlueBottomLeft, colorBlueBottomRight, colorBlueTopRight;

    public RenderBlocks() {
    }

    public RenderBlocks(IBlockAccess access) {
        this.blockAccess = access;
    }

    private static RenderBlocks mcc$instance;

    public static synchronized RenderBlocks getInstance() {
        if (mcc$instance == null) mcc$instance = new RenderBlocks();
        return mcc$instance;
    }

    /** Whether a block with this render type is drawn as a 3D block in inventories (vanilla's list). */
    public static boolean renderItemIn3d(int renderType) {
        return switch (renderType) {
            case 0, 31, 39, 13, 10, 11, 27, 22, 21, 16, 26, 32, 34, 35 -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ state

    public void setOverrideBlockTexture(IIcon icon) {
        this.overrideBlockTexture = icon;
    }

    public void clearOverrideBlockTexture() {
        this.overrideBlockTexture = null;
    }

    public boolean hasOverrideBlockTexture() {
        return this.overrideBlockTexture != null;
    }

    public void setRenderFromInside(boolean inside) {
        this.renderFromInside = inside;
    }

    public void setRenderAllFaces(boolean all) {
        this.renderAllFaces = all;
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

    // ------------------------------------------------------------------ icons

    /** The icon of a placed block's side; a block without a world icon falls back to its metadata icon, then to "missingno". */
    public IIcon getBlockIcon(Block block, IBlockAccess access, int x, int y, int z, int side) {
        if (overrideBlockTexture != null) return overrideBlockTexture;
        IIcon icon = block.getIcon(access, x, y, z, side);
        if (icon == null && access != null) icon = block.getIcon(side, access.getBlockMetadata(x, y, z));
        return getIconSafe(icon);
    }

    public IIcon getBlockIconFromSideAndMetadata(Block block, int side, int meta) {
        return getIconSafe(block.getIcon(side, meta));
    }

    public IIcon getBlockIconFromSide(Block block, int side) {
        return getIconSafe(block.getBlockTextureFromSide(side));
    }

    public IIcon getBlockIcon(Block block) {
        return getIconSafe(block.getBlockTextureFromSide(1));
    }

    /** The icon, or the block atlas's missing-texture sprite for null (1.7.10 drew "missingno" rather than failing). */
    public IIcon getIconSafe(IIcon icon) {
        if (icon != null) return icon;
        IIcon missing = mcc$missing;
        if (missing == null) {
            try {
                mcc$missing = missing = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno");
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }
        return missing;
    }

    private static volatile IIcon mcc$missing;

    // ------------------------------------------------------------------ blocks

    public void renderBlockUsingTexture(Block block, int x, int y, int z, IIcon icon) {
        setOverrideBlockTexture(icon);
        try {
            renderBlockByRenderType(block, x, y, z);
        } finally {
            clearOverrideBlockTexture();
        }
    }

    public void renderBlockAllFaces(Block block, int x, int y, int z) {
        renderAllFaces = true;
        try {
            renderBlockByRenderType(block, x, y, z);
        } finally {
            renderAllFaces = false;
        }
    }

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
        enableAO = false;
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

    /** The six faces of the current bounds at the origin, with the block's icons for metadata {@code meta}. */
    public void renderBlockAsItem(Block block, int meta, float brightness) {
        Tessellator t = Tessellator.instance;
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

    /** Draws {@code icon} as two crossing upright squares centred on the block (plants, crops, tall grass). */
    public void drawCrossedSquares(IIcon icon, double x, double y, double z, float scale) {
        Tessellator t = Tessellator.instance;
        if (overrideBlockTexture != null) icon = overrideBlockTexture;
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

    // ------------------------------------------------------------------ faces
    // u0/u1/v0/v1 are the face's texture span; c0..c3 the copies 1.7.10 used for the other two corners, which its UV rotations
    // swap around (rotation "A" turns the texture a quarter one way, "B" the other way, 3 turns it half way).

    private static final int ROT_NONE = 0, ROT_A = 1, ROT_B = 2;

    /** Corner UVs after 1.7.10's copy-and-swap: [u0, u1, v0, v1, c18, c20, c22, c24] as named after its local variable slots. */
    private static double[] corners(double u0, double u1, double v0, double v1, int rot) {
        double c18 = u1, c20 = u0, c22 = v0, c24 = v1;
        if (rot == ROT_A) {
            c18 = u0;
            c20 = u1;
            v0 = v1;
            v1 = c22;
        } else if (rot == ROT_B) {
            u0 = c18;
            u1 = c20;
            c22 = v1;
            c24 = v0;
        }
        return new double[] {u0, u1, v0, v1, c18, c20, c22, c24};
    }

    /** One corner: with {@link #enableAO} the corner's own colour and light (0 top left, 1 bottom left, 2 bottom right, 3 top right). */
    private void corner(Tessellator t, int corner, double x, double y, double z, double u, double v) {
        if (enableAO) {
            switch (corner) {
                case 0 -> {
                    t.setColorOpaque_F(colorRedTopLeft, colorGreenTopLeft, colorBlueTopLeft);
                    t.setBrightness(brightnessTopLeft);
                }
                case 1 -> {
                    t.setColorOpaque_F(colorRedBottomLeft, colorGreenBottomLeft, colorBlueBottomLeft);
                    t.setBrightness(brightnessBottomLeft);
                }
                case 2 -> {
                    t.setColorOpaque_F(colorRedBottomRight, colorGreenBottomRight, colorBlueBottomRight);
                    t.setBrightness(brightnessBottomRight);
                }
                default -> {
                    t.setColorOpaque_F(colorRedTopRight, colorGreenTopRight, colorBlueTopRight);
                    t.setBrightness(brightnessTopRight);
                }
            }
        }
        t.addVertexWithUV(x, y, z, u, v);
    }

    public void renderFaceYNeg(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        if (hasOverrideBlockTexture()) icon = overrideBlockTexture;
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
        int rot = ROT_NONE;
        if (uvRotateBottom == 2) {
            u0 = icon.getInterpolatedU(renderMinZ * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMaxX * 16.0D);
            u1 = icon.getInterpolatedU(renderMaxZ * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMinX * 16.0D);
            rot = ROT_A;
        } else if (uvRotateBottom == 1) {
            u0 = icon.getInterpolatedU(16.0D - renderMaxZ * 16.0D);
            v0 = icon.getInterpolatedV(renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMinZ * 16.0D);
            v1 = icon.getInterpolatedV(renderMaxX * 16.0D);
            rot = ROT_B;
        } else if (uvRotateBottom == 3) {
            u0 = icon.getInterpolatedU(16.0D - renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMaxX * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMinZ * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMaxZ * 16.0D);
        }
        double[] c = corners(u0, u1, v0, v1, rot);
        double x1 = x + renderMinX, x2 = x + renderMaxX, yy = y + renderMinY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        if (renderFromInside) {
            x1 = x + renderMaxX;
            x2 = x + renderMinX;
        }
        corner(t, 0, x1, yy, z2, c[5], c[7]);
        corner(t, 1, x1, yy, z1, c[0], c[2]);
        corner(t, 2, x2, yy, z1, c[4], c[6]);
        corner(t, 3, x2, yy, z2, c[1], c[3]);
    }

    public void renderFaceYPos(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        if (hasOverrideBlockTexture()) icon = overrideBlockTexture;
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
        int rot = ROT_NONE;
        if (uvRotateTop == 1) {
            u0 = icon.getInterpolatedU(renderMinZ * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMaxX * 16.0D);
            u1 = icon.getInterpolatedU(renderMaxZ * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMinX * 16.0D);
            rot = ROT_A;
        } else if (uvRotateTop == 2) {
            u0 = icon.getInterpolatedU(16.0D - renderMaxZ * 16.0D);
            v0 = icon.getInterpolatedV(renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMinZ * 16.0D);
            v1 = icon.getInterpolatedV(renderMaxX * 16.0D);
            rot = ROT_B;
        } else if (uvRotateTop == 3) {
            u0 = icon.getInterpolatedU(16.0D - renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMaxX * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMinZ * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMaxZ * 16.0D);
        }
        double[] c = corners(u0, u1, v0, v1, rot);
        double x1 = x + renderMinX, x2 = x + renderMaxX, yy = y + renderMaxY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        if (renderFromInside) {
            x1 = x + renderMaxX;
            x2 = x + renderMinX;
        }
        corner(t, 0, x2, yy, z2, c[1], c[3]);
        corner(t, 1, x2, yy, z1, c[4], c[6]);
        corner(t, 2, x1, yy, z1, c[0], c[2]);
        corner(t, 3, x1, yy, z2, c[5], c[7]);
    }

    public void renderFaceZNeg(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        if (hasOverrideBlockTexture()) icon = overrideBlockTexture;
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinX * 16.0D), u1 = icon.getInterpolatedU(renderMaxX * 16.0D);
        if (field_152631_f) {
            u1 = icon.getInterpolatedU((1.0D - renderMinX) * 16.0D);
            u0 = icon.getInterpolatedU((1.0D - renderMaxX) * 16.0D);
        }
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
        int rot = ROT_NONE;
        if (uvRotateEast == 2) {
            u0 = icon.getInterpolatedU(renderMinY * 16.0D);
            u1 = icon.getInterpolatedU(renderMaxY * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMinX * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMaxX * 16.0D);
            rot = ROT_A;
        } else if (uvRotateEast == 1) {
            u0 = icon.getInterpolatedU(16.0D - renderMaxY * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMinY * 16.0D);
            v0 = icon.getInterpolatedV(renderMaxX * 16.0D);
            v1 = icon.getInterpolatedV(renderMinX * 16.0D);
            rot = ROT_B;
        } else if (uvRotateEast == 3) {
            u0 = icon.getInterpolatedU(16.0D - renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMaxX * 16.0D);
            v0 = icon.getInterpolatedV(renderMaxY * 16.0D);
            v1 = icon.getInterpolatedV(renderMinY * 16.0D);
        }
        double[] c = corners(u0, u1, v0, v1, rot);
        double x1 = x + renderMinX, x2 = x + renderMaxX, y1 = y + renderMinY, y2 = y + renderMaxY, zz = z + renderMinZ;
        if (renderFromInside) {
            x1 = x + renderMaxX;
            x2 = x + renderMinX;
        }
        corner(t, 0, x1, y2, zz, c[4], c[6]);
        corner(t, 1, x2, y2, zz, c[0], c[2]);
        corner(t, 2, x2, y1, zz, c[5], c[7]);
        corner(t, 3, x1, y1, zz, c[1], c[3]);
    }

    public void renderFaceZPos(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        if (hasOverrideBlockTexture()) icon = overrideBlockTexture;
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
        int rot = ROT_NONE;
        if (uvRotateWest == 1) {
            u0 = icon.getInterpolatedU(renderMinY * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(renderMaxY * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMaxX * 16.0D);
            rot = ROT_A;
        } else if (uvRotateWest == 2) {
            u0 = icon.getInterpolatedU(16.0D - renderMaxY * 16.0D);
            v0 = icon.getInterpolatedV(renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMinY * 16.0D);
            v1 = icon.getInterpolatedV(renderMaxX * 16.0D);
            rot = ROT_B;
        } else if (uvRotateWest == 3) {
            u0 = icon.getInterpolatedU(16.0D - renderMinX * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMaxX * 16.0D);
            v0 = icon.getInterpolatedV(renderMaxY * 16.0D);
            v1 = icon.getInterpolatedV(renderMinY * 16.0D);
        }
        double[] c = corners(u0, u1, v0, v1, rot);
        double x1 = x + renderMinX, x2 = x + renderMaxX, y1 = y + renderMinY, y2 = y + renderMaxY, zz = z + renderMaxZ;
        if (renderFromInside) {
            x1 = x + renderMaxX;
            x2 = x + renderMinX;
        }
        corner(t, 0, x1, y2, zz, c[0], c[2]);
        corner(t, 1, x1, y1, zz, c[5], c[7]);
        corner(t, 2, x2, y1, zz, c[1], c[3]);
        corner(t, 3, x2, y2, zz, c[4], c[6]);
    }

    public void renderFaceXNeg(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        if (hasOverrideBlockTexture()) icon = overrideBlockTexture;
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
        int rot = ROT_NONE;
        if (uvRotateNorth == 1) {
            u0 = icon.getInterpolatedU(renderMinY * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMaxZ * 16.0D);
            u1 = icon.getInterpolatedU(renderMaxY * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMinZ * 16.0D);
            rot = ROT_A;
        } else if (uvRotateNorth == 2) {
            u0 = icon.getInterpolatedU(16.0D - renderMaxY * 16.0D);
            v0 = icon.getInterpolatedV(renderMinZ * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMinY * 16.0D);
            v1 = icon.getInterpolatedV(renderMaxZ * 16.0D);
            rot = ROT_B;
        } else if (uvRotateNorth == 3) {
            u0 = icon.getInterpolatedU(16.0D - renderMinZ * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMaxZ * 16.0D);
            v0 = icon.getInterpolatedV(renderMaxY * 16.0D);
            v1 = icon.getInterpolatedV(renderMinY * 16.0D);
        }
        double[] c = corners(u0, u1, v0, v1, rot);
        double xx = x + renderMinX, y1 = y + renderMinY, y2 = y + renderMaxY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        if (renderFromInside) {
            z1 = z + renderMaxZ;
            z2 = z + renderMinZ;
        }
        corner(t, 0, xx, y2, z2, c[4], c[6]);
        corner(t, 1, xx, y2, z1, c[0], c[2]);
        corner(t, 2, xx, y1, z1, c[5], c[7]);
        corner(t, 3, xx, y1, z2, c[1], c[3]);
    }

    public void renderFaceXPos(Block block, double x, double y, double z, IIcon icon) {
        Tessellator t = Tessellator.instance;
        if (hasOverrideBlockTexture()) icon = overrideBlockTexture;
        if (icon == null) return;
        double u0 = icon.getInterpolatedU(renderMinZ * 16.0D), u1 = icon.getInterpolatedU(renderMaxZ * 16.0D);
        if (field_152631_f) {
            u1 = icon.getInterpolatedU((1.0D - renderMinZ) * 16.0D);
            u0 = icon.getInterpolatedU((1.0D - renderMaxZ) * 16.0D);
        }
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
        int rot = ROT_NONE;
        if (uvRotateSouth == 2) {
            u0 = icon.getInterpolatedU(renderMinY * 16.0D);
            v0 = icon.getInterpolatedV(16.0D - renderMinZ * 16.0D);
            u1 = icon.getInterpolatedU(renderMaxY * 16.0D);
            v1 = icon.getInterpolatedV(16.0D - renderMaxZ * 16.0D);
            rot = ROT_A;
        } else if (uvRotateSouth == 1) {
            u0 = icon.getInterpolatedU(16.0D - renderMaxY * 16.0D);
            v0 = icon.getInterpolatedV(renderMaxZ * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMinY * 16.0D);
            v1 = icon.getInterpolatedV(renderMinZ * 16.0D);
            rot = ROT_B;
        } else if (uvRotateSouth == 3) {
            u0 = icon.getInterpolatedU(16.0D - renderMinZ * 16.0D);
            u1 = icon.getInterpolatedU(16.0D - renderMaxZ * 16.0D);
            v0 = icon.getInterpolatedV(renderMaxY * 16.0D);
            v1 = icon.getInterpolatedV(renderMinY * 16.0D);
        }
        double[] c = corners(u0, u1, v0, v1, rot);
        double xx = x + renderMaxX, y1 = y + renderMinY, y2 = y + renderMaxY, z1 = z + renderMinZ, z2 = z + renderMaxZ;
        if (renderFromInside) {
            z1 = z + renderMaxZ;
            z2 = z + renderMinZ;
        }
        corner(t, 0, xx, y1, z2, c[5], c[7]);
        corner(t, 1, xx, y1, z1, c[1], c[3]);
        corner(t, 2, xx, y2, z1, c[4], c[6]);
        corner(t, 3, xx, y2, z2, c[0], c[2]);
    }

    // ------------------------------------------------------------------ vanilla render types (1.7.10's code, Forge's world-render hook)

    /**
     * Draws the block the way its render type asks: the vanilla shapes, or (Forge) the ISBRH registered for a mod's id. A block
     * whose class does not fit the vanilla type it claims (a mod block reusing a vanilla render id) is drawn as a cube.
     */
    public boolean renderBlockByRenderType(Block block, int x, int y, int z) {
        try {
            return mcc$renderByType(block, x, y, z);
        } catch (ClassCastException e) {
            return renderStandardBlock(block, x, y, z);
        }
    }

    private boolean mcc$renderByType(Block block, int i, int j, int k) {
        int l = block.getRenderType();
        if (l == -1) {
            return false;
        } else {
            block.setBlockBoundsBasedOnState(this.blockAccess, i, j, k);
            this.setRenderBoundsFromBlock(block);
            if (l == 0) {
                return this.renderStandardBlock(block, i, j, k);
            } else if (l == 4) {
                return this.renderBlockLiquid(block, i, j, k);
            } else if (l == 31) {
                return this.renderBlockLog(block, i, j, k);
            } else if (l == 1) {
                return this.renderCrossedSquares(block, i, j, k);
            } else if (l == 40) {
                return this.renderBlockDoublePlant((BlockDoublePlant)block, i, j, k);
            } else if (l == 2) {
                return this.renderBlockTorch(block, i, j, k);
            } else if (l == 20) {
                return this.renderBlockVine(block, i, j, k);
            } else if (l == 11) {
                return this.renderBlockFence((BlockFence)block, i, j, k);
            } else if (l == 39) {
                return this.renderBlockQuartz(block, i, j, k);
            } else if (l == 5) {
                return this.renderBlockRedstoneWire(block, i, j, k);
            } else if (l == 13) {
                return this.renderBlockCactus(block, i, j, k);
            } else if (l == 9) {
                return this.renderBlockMinecartTrack((BlockRailBase)block, i, j, k);
            } else if (l == 19) {
                return this.renderBlockStem(block, i, j, k);
            } else if (l == 23) {
                return this.renderBlockLilyPad(block, i, j, k);
            } else if (l == 6) {
                return this.renderBlockCrops(block, i, j, k);
            } else if (l == 3) {
                return this.renderBlockFire((BlockFire)block, i, j, k);
            } else if (l == 8) {
                return this.renderBlockLadder(block, i, j, k);
            } else if (l == 7) {
                return this.renderBlockDoor(block, i, j, k);
            } else if (l == 10) {
                return this.renderBlockStairs((BlockStairs)block, i, j, k);
            } else if (l == 27) {
                return this.renderBlockDragonEgg((BlockDragonEgg)block, i, j, k);
            } else if (l == 32) {
                return this.renderBlockWall((BlockWall)block, i, j, k);
            } else if (l == 12) {
                return this.renderBlockLever(block, i, j, k);
            } else if (l == 29) {
                return this.renderBlockTripWireSource(block, i, j, k);
            } else if (l == 30) {
                return this.renderBlockTripWire(block, i, j, k);
            } else if (l == 14) {
                return this.renderBlockBed(block, i, j, k);
            } else if (l == 15) {
                return this.renderBlockRepeater((BlockRedstoneRepeater)block, i, j, k);
            } else if (l == 36) {
                return this.renderBlockRedstoneDiode((BlockRedstoneDiode)block, i, j, k);
            } else if (l == 37) {
                return this.renderBlockRedstoneComparator((BlockRedstoneComparator)block, i, j, k);
            } else if (l == 16) {
                return this.renderPistonBase(block, i, j, k, false);
            } else if (l == 17) {
                return this.renderPistonExtension(block, i, j, k, true);
            } else if (l == 18) {
                return this.renderBlockPane((BlockPane)block, i, j, k);
            } else if (l == 41) {
                return this.renderBlockStainedGlassPane(block, i, j, k);
            } else if (l == 21) {
                return this.renderBlockFenceGate((BlockFenceGate)block, i, j, k);
            } else if (l == 24) {
                return this.renderBlockCauldron((BlockCauldron)block, i, j, k);
            } else if (l == 33) {
                return this.renderBlockFlowerpot((BlockFlowerPot)block, i, j, k);
            } else if (l == 35) {
                return this.renderBlockAnvil((BlockAnvil)block, i, j, k);
            } else if (l == 25) {
                return this.renderBlockBrewingStand((BlockBrewingStand)block, i, j, k);
            } else if (l == 26) {
                return this.renderBlockEndPortalFrame((BlockEndPortalFrame)block, i, j, k);
            } else if (l == 28) {
                return this.renderBlockCocoa((BlockCocoa)block, i, j, k);
            } else if (l == 34) {
                return this.renderBlockBeacon((BlockBeacon)block, i, j, k);
            } else {
                return l == 38 ? this.renderBlockHopper((BlockHopper)block, i, j, k) : this.mcc$renderModBlock(block, i, j, k, l);
            }
        }
    }

    public boolean renderBlockEndPortalFrame(BlockEndPortalFrame blockendportalframe, int i, int j, int k) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = l & 3;
        if (i1 == 0) {
            this.uvRotateTop = 3;
        } else if (i1 == 3) {
            this.uvRotateTop = 1;
        } else if (i1 == 1) {
            this.uvRotateTop = 2;
        }

        if (!BlockEndPortalFrame.isEnderEyeInserted(l)) {
            this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 0.8125, 1.0);
            this.renderStandardBlock(blockendportalframe, i, j, k);
            this.uvRotateTop = 0;
            return true;
        } else {
            this.renderAllFaces = true;
            this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 0.8125, 1.0);
            this.renderStandardBlock(blockendportalframe, i, j, k);
            this.setOverrideBlockTexture(blockendportalframe.getIconEndPortalFrameEye());
            this.setRenderBounds(0.25, 0.8125, 0.25, 0.75, 1.0, 0.75);
            this.renderStandardBlock(blockendportalframe, i, j, k);
            this.renderAllFaces = false;
            this.clearOverrideBlockTexture();
            this.uvRotateTop = 0;
            return true;
        }
    }

    public boolean renderBlockBed(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        // Forge: the bed's own direction and half
        int i1 = block.getBedDirection(this.blockAccess, i, j, k);
        boolean flag = block.isBedFoot(this.blockAccess, i, j, k);
        float f = 0.5F;
        float f1 = 1.0F;
        float f2 = 0.8F;
        float f3 = 0.6F;
        int j1 = block.getMixedBrightnessForBlock(this.blockAccess, i, j, k);
        tessellator.setBrightness(j1);
        tessellator.setColorOpaque_F(f, f, f);
        IIcon iicon = this.getBlockIcon(block, this.blockAccess, i, j, k, 0);
        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMaxU();
        double d2 = (double)iicon.getMinV();
        double d3 = (double)iicon.getMaxV();
        double d4 = (double)i + this.renderMinX;
        double d5 = (double)i + this.renderMaxX;
        double d6 = (double)j + this.renderMinY + 0.1875;
        double d7 = (double)k + this.renderMinZ;
        double d8 = (double)k + this.renderMaxZ;
        tessellator.addVertexWithUV(d4, d6, d8, d0, d3);
        tessellator.addVertexWithUV(d4, d6, d7, d0, d2);
        tessellator.addVertexWithUV(d5, d6, d7, d1, d2);
        tessellator.addVertexWithUV(d5, d6, d8, d1, d3);
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j + 1, k));
        tessellator.setColorOpaque_F(f1, f1, f1);
        iicon = this.getBlockIcon(block, this.blockAccess, i, j, k, 1);
        d0 = (double)iicon.getMinU();
        d1 = (double)iicon.getMaxU();
        d2 = (double)iicon.getMinV();
        d3 = (double)iicon.getMaxV();
        d4 = d0;
        d5 = d1;
        d6 = d2;
        d7 = d2;
        d8 = d0;
        double d9 = d1;
        double d10 = d3;
        double d11 = d3;
        if (i1 == 0) {
            d5 = d0;
            d6 = d3;
            d8 = d1;
            d11 = d2;
        } else if (i1 == 2) {
            d4 = d1;
            d7 = d3;
            d9 = d0;
            d10 = d2;
        } else if (i1 == 3) {
            d4 = d1;
            d7 = d3;
            d9 = d0;
            d10 = d2;
            d5 = d0;
            d6 = d3;
            d8 = d1;
            d11 = d2;
        }

        double d12 = (double)i + this.renderMinX;
        double d13 = (double)i + this.renderMaxX;
        double d14 = (double)j + this.renderMaxY;
        double d15 = (double)k + this.renderMinZ;
        double d16 = (double)k + this.renderMaxZ;
        tessellator.addVertexWithUV(d13, d14, d16, d8, d10);
        tessellator.addVertexWithUV(d13, d14, d15, d4, d6);
        tessellator.addVertexWithUV(d12, d14, d15, d5, d7);
        tessellator.addVertexWithUV(d12, d14, d16, d9, d11);
        int k1 = Direction.directionToFacing[i1];
        if (flag) {
            k1 = Direction.directionToFacing[Direction.rotateOpposite[i1]];
        }

        byte b0 = 4;
        switch (i1) {
            case 0:
                b0 = 5;
                break;
            case 1:
                b0 = 3;
            case 2:
            default:
                break;
            case 3:
                b0 = 2;
        }

        if (k1 != 2 && (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i, j, k - 1, 2))) {
            tessellator.setBrightness(this.renderMinZ > 0.0 ? j1 : block.getMixedBrightnessForBlock(this.blockAccess, i, j, k - 1));
            tessellator.setColorOpaque_F(f2, f2, f2);
            this.flipTexture = b0 == 2;
            this.renderFaceZNeg(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 2));
        }

        if (k1 != 3 && (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i, j, k + 1, 3))) {
            tessellator.setBrightness(this.renderMaxZ < 1.0 ? j1 : block.getMixedBrightnessForBlock(this.blockAccess, i, j, k + 1));
            tessellator.setColorOpaque_F(f2, f2, f2);
            this.flipTexture = b0 == 3;
            this.renderFaceZPos(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 3));
        }

        if (k1 != 4 && (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i - 1, j, k, 4))) {
            tessellator.setBrightness(this.renderMinZ > 0.0 ? j1 : block.getMixedBrightnessForBlock(this.blockAccess, i - 1, j, k));
            tessellator.setColorOpaque_F(f3, f3, f3);
            this.flipTexture = b0 == 4;
            this.renderFaceXNeg(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 4));
        }

        if (k1 != 5 && (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i + 1, j, k, 5))) {
            tessellator.setBrightness(this.renderMaxZ < 1.0 ? j1 : block.getMixedBrightnessForBlock(this.blockAccess, i + 1, j, k));
            tessellator.setColorOpaque_F(f3, f3, f3);
            this.flipTexture = b0 == 5;
            this.renderFaceXPos(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 5));
        }

        this.flipTexture = false;
        return true;
    }

    public boolean renderBlockBrewingStand(BlockBrewingStand blockbrewingstand, int i, int j, int k) {
        this.setRenderBounds(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625);
        this.renderStandardBlock(blockbrewingstand, i, j, k);
        this.setOverrideBlockTexture(blockbrewingstand.getIconBrewingStandBase());
        this.renderAllFaces = true;
        this.setRenderBounds(0.5625, 0.0, 0.3125, 0.9375, 0.125, 0.6875);
        this.renderStandardBlock(blockbrewingstand, i, j, k);
        this.setRenderBounds(0.125, 0.0, 0.0625, 0.5, 0.125, 0.4375);
        this.renderStandardBlock(blockbrewingstand, i, j, k);
        this.setRenderBounds(0.125, 0.0, 0.5625, 0.5, 0.125, 0.9375);
        this.renderStandardBlock(blockbrewingstand, i, j, k);
        this.renderAllFaces = false;
        this.clearOverrideBlockTexture();
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockbrewingstand.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = blockbrewingstand.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        IIcon iicon = this.getBlockIconFromSideAndMetadata(blockbrewingstand, 0, 0);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d8 = (double)iicon.getMinV();
        double d0 = (double)iicon.getMaxV();
        int i1 = this.blockAccess.getBlockMetadata(i, j, k);

        for (int j1 = 0; j1 < 3; j1++) {
            double d1 = (double)j1 * Math.PI * 2.0 / 3.0 + (Math.PI / 2);
            double d2 = (double)iicon.getInterpolatedU(8.0);
            double d3 = (double)iicon.getMaxU();
            if ((i1 & 1 << j1) != 0) {
                d3 = (double)iicon.getMinU();
            }

            double d4 = (double)i + 0.5;
            double d5 = (double)i + 0.5 + Math.sin(d1) * 8.0 / 16.0;
            double d6 = (double)k + 0.5;
            double d7 = (double)k + 0.5 + Math.cos(d1) * 8.0 / 16.0;
            tessellator.addVertexWithUV(d4, (double)(j + 1), d6, d2, d8);
            tessellator.addVertexWithUV(d4, (double)(j + 0), d6, d2, d0);
            tessellator.addVertexWithUV(d5, (double)(j + 0), d7, d3, d0);
            tessellator.addVertexWithUV(d5, (double)(j + 1), d7, d3, d8);
            tessellator.addVertexWithUV(d5, (double)(j + 1), d7, d3, d8);
            tessellator.addVertexWithUV(d5, (double)(j + 0), d7, d3, d0);
            tessellator.addVertexWithUV(d4, (double)(j + 0), d6, d2, d0);
            tessellator.addVertexWithUV(d4, (double)(j + 1), d6, d2, d8);
        }

        blockbrewingstand.setBlockBoundsForItemRender();
        return true;
    }

    public boolean renderBlockCauldron(BlockCauldron blockcauldron, int i, int j, int k) {
        this.renderStandardBlock(blockcauldron, i, j, k);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockcauldron.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = blockcauldron.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        IIcon iicon1 = blockcauldron.getBlockTextureFromSide(2);
        float f6 = 0.125F;
        this.renderFaceXPos(blockcauldron, (double)((float)i - 1.0F + f6), (double)j, (double)k, iicon1);
        this.renderFaceXNeg(blockcauldron, (double)((float)i + 1.0F - f6), (double)j, (double)k, iicon1);
        this.renderFaceZPos(blockcauldron, (double)i, (double)j, (double)((float)k - 1.0F + f6), iicon1);
        this.renderFaceZNeg(blockcauldron, (double)i, (double)j, (double)((float)k + 1.0F - f6), iicon1);
        IIcon iicon2 = BlockCauldron.getCauldronIcon("inner");
        this.renderFaceYPos(blockcauldron, (double)i, (double)((float)j - 1.0F + 0.25F), (double)k, iicon2);
        this.renderFaceYNeg(blockcauldron, (double)i, (double)((float)j + 1.0F - 0.75F), (double)k, iicon2);
        int i1 = this.blockAccess.getBlockMetadata(i, j, k);
        if (i1 > 0) {
            IIcon iicon = BlockLiquid.getLiquidIcon("water_still");
            this.renderFaceYPos(blockcauldron, (double)i, (double)((float)j - 1.0F + BlockCauldron.getRenderLiquidLevel(i1)), (double)k, iicon);
        }

        return true;
    }

    public boolean renderBlockFlowerpot(BlockFlowerPot blockflowerpot, int i, int j, int k) {
        this.renderStandardBlock(blockflowerpot, i, j, k);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockflowerpot.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = blockflowerpot.colorMultiplier(this.blockAccess, i, j, k);
        IIcon iicon = this.getBlockIconFromSide(blockflowerpot, 0);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        float f10 = 0.1865F;
        this.renderFaceXPos(blockflowerpot, (double)((float)i - 0.5F + f10), (double)j, (double)k, iicon);
        this.renderFaceXNeg(blockflowerpot, (double)((float)i + 0.5F - f10), (double)j, (double)k, iicon);
        this.renderFaceZPos(blockflowerpot, (double)i, (double)j, (double)((float)k - 0.5F + f10), iicon);
        this.renderFaceZNeg(blockflowerpot, (double)i, (double)j, (double)((float)k + 0.5F - f10), iicon);
        this.renderFaceYPos(blockflowerpot, (double)i, (double)((float)j - 0.5F + f10 + 0.1875F), (double)k, this.getBlockIcon(Blocks.dirt));
        TileEntity tileentity = this.blockAccess.getTileEntity(i, j, k);
        if (tileentity != null && tileentity instanceof TileEntityFlowerPot) {
            Item item = ((TileEntityFlowerPot)tileentity).getFlowerPotItem();
            int i1 = ((TileEntityFlowerPot)tileentity).getFlowerPotData();
            if (item instanceof ItemBlock) {
                Block block = Block.getBlockFromItem(item);
                int j1 = block.getRenderType();
                float f6 = 0.0F;
                float f7 = 4.0F;
                float f8 = 0.0F;
                tessellator.addTranslation(f6 / 16.0F, f7 / 16.0F, f8 / 16.0F);
                l = block.colorMultiplier(this.blockAccess, i, j, k);
                if (l != 16777215) {
                    f = (float)(l >> 16 & 0xFF) / 255.0F;
                    f1 = (float)(l >> 8 & 0xFF) / 255.0F;
                    f2 = (float)(l & 0xFF) / 255.0F;
                    tessellator.setColorOpaque_F(f, f1, f2);
                }

                if (j1 == 1) {
                    this.drawCrossedSquares(this.getBlockIconFromSideAndMetadata(block, 0, i1), (double)i, (double)j, (double)k, 0.75F);
                } else if (j1 == 13) {
                    this.renderAllFaces = true;
                    float f9 = 0.125F;
                    this.setRenderBounds((double)(0.5F - f9), 0.0, (double)(0.5F - f9), (double)(0.5F + f9), 0.25, (double)(0.5F + f9));
                    this.renderStandardBlock(block, i, j, k);
                    this.setRenderBounds((double)(0.5F - f9), 0.25, (double)(0.5F - f9), (double)(0.5F + f9), 0.5, (double)(0.5F + f9));
                    this.renderStandardBlock(block, i, j, k);
                    this.setRenderBounds((double)(0.5F - f9), 0.5, (double)(0.5F - f9), (double)(0.5F + f9), 0.75, (double)(0.5F + f9));
                    this.renderStandardBlock(block, i, j, k);
                    this.renderAllFaces = false;
                    this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
                }

                tessellator.addTranslation(-f6 / 16.0F, -f7 / 16.0F, -f8 / 16.0F);
            }
        }

        return true;
    }

    public boolean renderBlockAnvil(BlockAnvil blockanvil, int i, int j, int k) {
        return this.renderBlockAnvilMetadata(blockanvil, i, j, k, this.blockAccess.getBlockMetadata(i, j, k));
    }

    public boolean renderBlockAnvilMetadata(BlockAnvil blockanvil, int i, int j, int k, int l) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockanvil.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int i1 = blockanvil.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(i1 >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(i1 >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(i1 & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        return this.renderBlockAnvilOrient(blockanvil, i, j, k, l, false);
    }

    public boolean renderBlockAnvilOrient(BlockAnvil blockanvil, int i, int j, int k, int l, boolean flag) {
        int i1 = flag ? 0 : l & 3;
        boolean flag1 = false;
        float f = 0.0F;
        switch (i1) {
            case 0:
                this.uvRotateSouth = 2;
                this.uvRotateNorth = 1;
                this.uvRotateTop = 3;
                this.uvRotateBottom = 3;
                break;
            case 1:
                this.uvRotateEast = 1;
                this.uvRotateWest = 2;
                this.uvRotateTop = 2;
                this.uvRotateBottom = 1;
                flag1 = true;
                break;
            case 2:
                this.uvRotateSouth = 1;
                this.uvRotateNorth = 2;
                break;
            case 3:
                this.uvRotateEast = 2;
                this.uvRotateWest = 1;
                this.uvRotateTop = 1;
                this.uvRotateBottom = 2;
                flag1 = true;
        }

        f = this.renderBlockAnvilRotate(blockanvil, i, j, k, 0, f, 0.75F, 0.25F, 0.75F, flag1, flag, l);
        f = this.renderBlockAnvilRotate(blockanvil, i, j, k, 1, f, 0.5F, 0.0625F, 0.625F, flag1, flag, l);
        f = this.renderBlockAnvilRotate(blockanvil, i, j, k, 2, f, 0.25F, 0.3125F, 0.5F, flag1, flag, l);
        f = this.renderBlockAnvilRotate(blockanvil, i, j, k, 3, f, 0.625F, 0.375F, 1.0F, flag1, flag, l);
        this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateSouth = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        return true;
    }

    public float renderBlockAnvilRotate(
        BlockAnvil blockanvil, int i, int j, int k, int l, float f, float f1, float f2, float f3, boolean flag, boolean flag1, int i1
    ) {
        if (flag) {
            float f4 = f1;
            f1 = f3;
            f3 = f4;
        }

        f1 /= 2.0F;
        f3 /= 2.0F;
        blockanvil.anvilRenderSide = l;
        this.setRenderBounds((double)(0.5F - f1), (double)f, (double)(0.5F - f3), (double)(0.5F + f1), (double)(f + f2), (double)(0.5F + f3));
        if (flag1) {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, -1.0F, 0.0F);
            this.renderFaceYNeg(blockanvil, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockanvil, 0, i1));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            this.renderFaceYPos(blockanvil, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockanvil, 1, i1));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            this.renderFaceZNeg(blockanvil, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockanvil, 2, i1));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            this.renderFaceZPos(blockanvil, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockanvil, 3, i1));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            this.renderFaceXNeg(blockanvil, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockanvil, 4, i1));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            this.renderFaceXPos(blockanvil, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockanvil, 5, i1));
            tessellator.draw();
        } else {
            this.renderStandardBlock(blockanvil, i, j, k);
        }

        return f + f2;
    }

    public boolean renderBlockTorch(Block block, int i, int j, int k) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double d0 = 0.4F;
        double d1 = 0.5 - d0;
        double d2 = 0.2F;
        if (l == 1) {
            this.renderTorchAtAngle(block, (double)i - d1, (double)j + d2, (double)k, -d0, 0.0, 0);
        } else if (l == 2) {
            this.renderTorchAtAngle(block, (double)i + d1, (double)j + d2, (double)k, d0, 0.0, 0);
        } else if (l == 3) {
            this.renderTorchAtAngle(block, (double)i, (double)j + d2, (double)k - d1, 0.0, -d0, 0);
        } else if (l == 4) {
            this.renderTorchAtAngle(block, (double)i, (double)j + d2, (double)k + d1, 0.0, d0, 0);
        } else {
            this.renderTorchAtAngle(block, (double)i, (double)j, (double)k, 0.0, 0.0, 0);
        }

        return true;
    }

    public boolean renderBlockRepeater(BlockRedstoneRepeater blockredstonerepeater, int i, int j, int k) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = l & 3;
        int j1 = (l & 12) >> 2;
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockredstonerepeater.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double d0 = -0.1875;
        boolean flag = blockredstonerepeater.func_149910_g(this.blockAccess, i, j, k, l);
        double d1 = 0.0;
        double d2 = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;
        switch (i1) {
            case 0:
                d4 = -0.3125;
                d2 = BlockRedstoneRepeater.repeaterTorchOffset[j1];
                break;
            case 1:
                d3 = 0.3125;
                d1 = -BlockRedstoneRepeater.repeaterTorchOffset[j1];
                break;
            case 2:
                d4 = 0.3125;
                d2 = -BlockRedstoneRepeater.repeaterTorchOffset[j1];
                break;
            case 3:
                d3 = -0.3125;
                d1 = BlockRedstoneRepeater.repeaterTorchOffset[j1];
        }

        if (!flag) {
            this.renderTorchAtAngle(blockredstonerepeater, (double)i + d1, (double)j + d0, (double)k + d2, 0.0, 0.0, 0);
        } else {
            IIcon iicon = this.getBlockIcon(Blocks.bedrock);
            this.setOverrideBlockTexture(iicon);
            float f = 2.0F;
            float f1 = 14.0F;
            float f2 = 7.0F;
            float f3 = 9.0F;
            switch (i1) {
                case 1:
                case 3:
                    f = 7.0F;
                    f1 = 9.0F;
                    f2 = 2.0F;
                    f3 = 14.0F;
                case 0:
                case 2:
                default:
                    this.setRenderBounds(
                        (double)(f / 16.0F + (float)d1),
                        0.125,
                        (double)(f2 / 16.0F + (float)d2),
                        (double)(f1 / 16.0F + (float)d1),
                        0.25,
                        (double)(f3 / 16.0F + (float)d2)
                    );
                    double d5 = (double)iicon.getInterpolatedU((double)f);
                    double d6 = (double)iicon.getInterpolatedV((double)f2);
                    double d7 = (double)iicon.getInterpolatedU((double)f1);
                    double d8 = (double)iicon.getInterpolatedV((double)f3);
                    tessellator.addVertexWithUV((double)((float)i + f / 16.0F) + d1, (double)((float)j + 0.25F), (double)((float)k + f2 / 16.0F) + d2, d5, d6);
                    tessellator.addVertexWithUV((double)((float)i + f / 16.0F) + d1, (double)((float)j + 0.25F), (double)((float)k + f3 / 16.0F) + d2, d5, d8);
                    tessellator.addVertexWithUV((double)((float)i + f1 / 16.0F) + d1, (double)((float)j + 0.25F), (double)((float)k + f3 / 16.0F) + d2, d7, d8);
                    tessellator.addVertexWithUV((double)((float)i + f1 / 16.0F) + d1, (double)((float)j + 0.25F), (double)((float)k + f2 / 16.0F) + d2, d7, d6);
                    this.renderStandardBlock(blockredstonerepeater, i, j, k);
                    this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
                    this.clearOverrideBlockTexture();
            }
        }

        tessellator.setBrightness(blockredstonerepeater.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        this.renderTorchAtAngle(blockredstonerepeater, (double)i + d3, (double)j + d0, (double)k + d4, 0.0, 0.0, 0);
        this.renderBlockRedstoneDiode(blockredstonerepeater, i, j, k);
        return true;
    }

    public boolean renderBlockRedstoneComparator(BlockRedstoneComparator blockredstonecomparator, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockredstonecomparator.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = l & 3;
        double d0 = 0.0;
        double d1 = -0.1875;
        double d2 = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;
        IIcon iicon;
        if (blockredstonecomparator.func_149969_d(l)) {
            iicon = Blocks.redstone_torch.getBlockTextureFromSide(0);
        } else {
            d1 -= 0.1875;
            iicon = Blocks.unlit_redstone_torch.getBlockTextureFromSide(0);
        }

        switch (i1) {
            case 0:
                d2 = -0.3125;
                d4 = 1.0;
                break;
            case 1:
                d0 = 0.3125;
                d3 = -1.0;
                break;
            case 2:
                d2 = 0.3125;
                d4 = -1.0;
                break;
            case 3:
                d0 = -0.3125;
                d3 = 1.0;
        }

        this.renderTorchAtAngle(
            blockredstonecomparator, (double)i + 0.25 * d3 + 0.1875 * d4, (double)((float)j - 0.1875F), (double)k + 0.25 * d4 + 0.1875 * d3, 0.0, 0.0, l
        );
        this.renderTorchAtAngle(
            blockredstonecomparator, (double)i + 0.25 * d3 + -0.1875 * d4, (double)((float)j - 0.1875F), (double)k + 0.25 * d4 + -0.1875 * d3, 0.0, 0.0, l
        );
        this.setOverrideBlockTexture(iicon);
        this.renderTorchAtAngle(blockredstonecomparator, (double)i + d0, (double)j + d1, (double)k + d2, 0.0, 0.0, l);
        this.clearOverrideBlockTexture();
        this.renderBlockRedstoneDiodeMetadata(blockredstonecomparator, i, j, k, i1);
        return true;
    }

    public boolean renderBlockRedstoneDiode(BlockRedstoneDiode blockredstonediode, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        this.renderBlockRedstoneDiodeMetadata(blockredstonediode, i, j, k, this.blockAccess.getBlockMetadata(i, j, k) & 3);
        return true;
    }

    public void renderBlockRedstoneDiodeMetadata(BlockRedstoneDiode blockredstonediode, int i, int j, int k, int l) {
        this.renderStandardBlock(blockredstonediode, i, j, k);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockredstonediode.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        int i1 = this.blockAccess.getBlockMetadata(i, j, k);
        IIcon iicon = this.getBlockIconFromSideAndMetadata(blockredstonediode, 1, i1);
        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMaxU();
        double d2 = (double)iicon.getMinV();
        double d3 = (double)iicon.getMaxV();
        double d4 = 0.125;
        double d5 = (double)(i + 1);
        double d6 = (double)(i + 1);
        double d7 = (double)(i + 0);
        double d8 = (double)(i + 0);
        double d9 = (double)(k + 0);
        double d10 = (double)(k + 1);
        double d11 = (double)(k + 1);
        double d12 = (double)(k + 0);
        double d13 = (double)j + d4;
        if (l == 2) {
            d5 = d6 = (double)(i + 0);
            d7 = d8 = (double)(i + 1);
            d9 = d12 = (double)(k + 1);
            d10 = d11 = (double)(k + 0);
        } else if (l == 3) {
            d5 = d8 = (double)(i + 0);
            d6 = d7 = (double)(i + 1);
            d9 = d10 = (double)(k + 0);
            d11 = d12 = (double)(k + 1);
        } else if (l == 1) {
            d5 = d8 = (double)(i + 1);
            d6 = d7 = (double)(i + 0);
            d9 = d10 = (double)(k + 1);
            d11 = d12 = (double)(k + 0);
        }

        tessellator.addVertexWithUV(d8, d13, d12, d0, d2);
        tessellator.addVertexWithUV(d7, d13, d11, d0, d3);
        tessellator.addVertexWithUV(d6, d13, d10, d1, d3);
        tessellator.addVertexWithUV(d5, d13, d9, d1, d2);
    }

    public void renderPistonBaseAllFaces(Block block, int i, int j, int k) {
        this.renderAllFaces = true;
        this.renderPistonBase(block, i, j, k, true);
        this.renderAllFaces = false;
    }

    public boolean renderPistonBase(Block block, int i, int j, int k, boolean flag) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        boolean flag1 = flag || (l & 8) != 0;
        int i1 = BlockPistonBase.getPistonOrientation(l);
        float f = 0.25F;
        if (flag1) {
            switch (i1) {
                case 0:
                    this.uvRotateEast = 3;
                    this.uvRotateWest = 3;
                    this.uvRotateSouth = 3;
                    this.uvRotateNorth = 3;
                    this.setRenderBounds(0.0, 0.25, 0.0, 1.0, 1.0, 1.0);
                    break;
                case 1:
                    this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);
                    break;
                case 2:
                    this.uvRotateSouth = 1;
                    this.uvRotateNorth = 2;
                    this.setRenderBounds(0.0, 0.0, 0.25, 1.0, 1.0, 1.0);
                    break;
                case 3:
                    this.uvRotateSouth = 2;
                    this.uvRotateNorth = 1;
                    this.uvRotateTop = 3;
                    this.uvRotateBottom = 3;
                    this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 0.75);
                    break;
                case 4:
                    this.uvRotateEast = 1;
                    this.uvRotateWest = 2;
                    this.uvRotateTop = 2;
                    this.uvRotateBottom = 1;
                    this.setRenderBounds(0.25, 0.0, 0.0, 1.0, 1.0, 1.0);
                    break;
                case 5:
                    this.uvRotateEast = 2;
                    this.uvRotateWest = 1;
                    this.uvRotateTop = 1;
                    this.uvRotateBottom = 2;
                    this.setRenderBounds(0.0, 0.0, 0.0, 0.75, 1.0, 1.0);
            }

            ((BlockPistonBase)block)
                .func_150070_b(
                    (float)this.renderMinX, (float)this.renderMinY, (float)this.renderMinZ, (float)this.renderMaxX, (float)this.renderMaxY, (float)this.renderMaxZ
                );
            this.renderStandardBlock(block, i, j, k);
            this.uvRotateEast = 0;
            this.uvRotateWest = 0;
            this.uvRotateSouth = 0;
            this.uvRotateNorth = 0;
            this.uvRotateTop = 0;
            this.uvRotateBottom = 0;
            this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            ((BlockPistonBase)block)
                .func_150070_b(
                    (float)this.renderMinX, (float)this.renderMinY, (float)this.renderMinZ, (float)this.renderMaxX, (float)this.renderMaxY, (float)this.renderMaxZ
                );
        } else {
            switch (i1) {
                case 0:
                    this.uvRotateEast = 3;
                    this.uvRotateWest = 3;
                    this.uvRotateSouth = 3;
                    this.uvRotateNorth = 3;
                case 1:
                default:
                    break;
                case 2:
                    this.uvRotateSouth = 1;
                    this.uvRotateNorth = 2;
                    break;
                case 3:
                    this.uvRotateSouth = 2;
                    this.uvRotateNorth = 1;
                    this.uvRotateTop = 3;
                    this.uvRotateBottom = 3;
                    break;
                case 4:
                    this.uvRotateEast = 1;
                    this.uvRotateWest = 2;
                    this.uvRotateTop = 2;
                    this.uvRotateBottom = 1;
                    break;
                case 5:
                    this.uvRotateEast = 2;
                    this.uvRotateWest = 1;
                    this.uvRotateTop = 1;
                    this.uvRotateBottom = 2;
            }

            this.renderStandardBlock(block, i, j, k);
            this.uvRotateEast = 0;
            this.uvRotateWest = 0;
            this.uvRotateSouth = 0;
            this.uvRotateNorth = 0;
            this.uvRotateTop = 0;
            this.uvRotateBottom = 0;
        }

        return true;
    }

    public void renderPistonRodUD(double d0, double d1, double d2, double d3, double d4, double d5, float f, double d6) {
        IIcon iicon = BlockPistonBase.getPistonBaseIcon("piston_side");
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        Tessellator tessellator = Tessellator.instance;
        double d7 = (double)iicon.getMinU();
        double d8 = (double)iicon.getMinV();
        double d9 = (double)iicon.getInterpolatedU(d6);
        double d10 = (double)iicon.getInterpolatedV(4.0);
        tessellator.setColorOpaque_F(f, f, f);
        tessellator.addVertexWithUV(d0, d3, d4, d9, d8);
        tessellator.addVertexWithUV(d0, d2, d4, d7, d8);
        tessellator.addVertexWithUV(d1, d2, d5, d7, d10);
        tessellator.addVertexWithUV(d1, d3, d5, d9, d10);
    }

    public void renderPistonRodSN(double d0, double d1, double d2, double d3, double d4, double d5, float f, double d6) {
        IIcon iicon = BlockPistonBase.getPistonBaseIcon("piston_side");
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        Tessellator tessellator = Tessellator.instance;
        double d7 = (double)iicon.getMinU();
        double d8 = (double)iicon.getMinV();
        double d9 = (double)iicon.getInterpolatedU(d6);
        double d10 = (double)iicon.getInterpolatedV(4.0);
        tessellator.setColorOpaque_F(f, f, f);
        tessellator.addVertexWithUV(d0, d2, d5, d9, d8);
        tessellator.addVertexWithUV(d0, d2, d4, d7, d8);
        tessellator.addVertexWithUV(d1, d3, d4, d7, d10);
        tessellator.addVertexWithUV(d1, d3, d5, d9, d10);
    }

    public void renderPistonRodEW(double d0, double d1, double d2, double d3, double d4, double d5, float f, double d6) {
        IIcon iicon = BlockPistonBase.getPistonBaseIcon("piston_side");
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        Tessellator tessellator = Tessellator.instance;
        double d7 = (double)iicon.getMinU();
        double d8 = (double)iicon.getMinV();
        double d9 = (double)iicon.getInterpolatedU(d6);
        double d10 = (double)iicon.getInterpolatedV(4.0);
        tessellator.setColorOpaque_F(f, f, f);
        tessellator.addVertexWithUV(d1, d2, d4, d9, d8);
        tessellator.addVertexWithUV(d0, d2, d4, d7, d8);
        tessellator.addVertexWithUV(d0, d3, d5, d7, d10);
        tessellator.addVertexWithUV(d1, d3, d5, d9, d10);
    }

    public void renderPistonExtensionAllFaces(Block block, int i, int j, int k, boolean flag) {
        this.renderAllFaces = true;
        this.renderPistonExtension(block, i, j, k, flag);
        this.renderAllFaces = false;
    }

    public boolean renderPistonExtension(Block block, int i, int j, int k, boolean flag) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = BlockPistonExtension.getDirectionMeta(l);
        float f = 0.25F;
        float f1 = 0.375F;
        float f2 = 0.625F;
        float f3 = flag ? 1.0F : 0.5F;
        double d0 = flag ? 16.0 : 8.0;
        switch (i1) {
            case 0:
                this.uvRotateEast = 3;
                this.uvRotateWest = 3;
                this.uvRotateSouth = 3;
                this.uvRotateNorth = 3;
                this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 0.25, 1.0);
                this.renderStandardBlock(block, i, j, k);
                this.renderPistonRodUD(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.625F),
                    (double)((float)j + 0.25F),
                    (double)((float)j + 0.25F + f3),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.625F),
                    0.8F,
                    d0
                );
                this.renderPistonRodUD(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.375F),
                    (double)((float)j + 0.25F),
                    (double)((float)j + 0.25F + f3),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.375F),
                    0.8F,
                    d0
                );
                this.renderPistonRodUD(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.375F),
                    (double)((float)j + 0.25F),
                    (double)((float)j + 0.25F + f3),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.625F),
                    0.6F,
                    d0
                );
                this.renderPistonRodUD(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.625F),
                    (double)((float)j + 0.25F),
                    (double)((float)j + 0.25F + f3),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.375F),
                    0.6F,
                    d0
                );
                break;
            case 1:
                this.setRenderBounds(0.0, 0.75, 0.0, 1.0, 1.0, 1.0);
                this.renderStandardBlock(block, i, j, k);
                this.renderPistonRodUD(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.625F),
                    (double)((float)j - 0.25F + 1.0F - f3),
                    (double)((float)j - 0.25F + 1.0F),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.625F),
                    0.8F,
                    d0
                );
                this.renderPistonRodUD(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.375F),
                    (double)((float)j - 0.25F + 1.0F - f3),
                    (double)((float)j - 0.25F + 1.0F),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.375F),
                    0.8F,
                    d0
                );
                this.renderPistonRodUD(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.375F),
                    (double)((float)j - 0.25F + 1.0F - f3),
                    (double)((float)j - 0.25F + 1.0F),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.625F),
                    0.6F,
                    d0
                );
                this.renderPistonRodUD(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.625F),
                    (double)((float)j - 0.25F + 1.0F - f3),
                    (double)((float)j - 0.25F + 1.0F),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.375F),
                    0.6F,
                    d0
                );
                break;
            case 2:
                this.uvRotateSouth = 1;
                this.uvRotateNorth = 2;
                this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 0.25);
                this.renderStandardBlock(block, i, j, k);
                this.renderPistonRodSN(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)k + 0.25F),
                    (double)((float)k + 0.25F + f3),
                    0.6F,
                    d0
                );
                this.renderPistonRodSN(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)k + 0.25F),
                    (double)((float)k + 0.25F + f3),
                    0.6F,
                    d0
                );
                this.renderPistonRodSN(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.375F),
                    (double)((float)k + 0.25F),
                    (double)((float)k + 0.25F + f3),
                    0.5F,
                    d0
                );
                this.renderPistonRodSN(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.625F),
                    (double)((float)k + 0.25F),
                    (double)((float)k + 0.25F + f3),
                    1.0F,
                    d0
                );
                break;
            case 3:
                this.uvRotateSouth = 2;
                this.uvRotateNorth = 1;
                this.uvRotateTop = 3;
                this.uvRotateBottom = 3;
                this.setRenderBounds(0.0, 0.0, 0.75, 1.0, 1.0, 1.0);
                this.renderStandardBlock(block, i, j, k);
                this.renderPistonRodSN(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)k - 0.25F + 1.0F - f3),
                    (double)((float)k - 0.25F + 1.0F),
                    0.6F,
                    d0
                );
                this.renderPistonRodSN(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)k - 0.25F + 1.0F - f3),
                    (double)((float)k - 0.25F + 1.0F),
                    0.6F,
                    d0
                );
                this.renderPistonRodSN(
                    (double)((float)i + 0.375F),
                    (double)((float)i + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.375F),
                    (double)((float)k - 0.25F + 1.0F - f3),
                    (double)((float)k - 0.25F + 1.0F),
                    0.5F,
                    d0
                );
                this.renderPistonRodSN(
                    (double)((float)i + 0.625F),
                    (double)((float)i + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.625F),
                    (double)((float)k - 0.25F + 1.0F - f3),
                    (double)((float)k - 0.25F + 1.0F),
                    1.0F,
                    d0
                );
                break;
            case 4:
                this.uvRotateEast = 1;
                this.uvRotateWest = 2;
                this.uvRotateTop = 2;
                this.uvRotateBottom = 1;
                this.setRenderBounds(0.0, 0.0, 0.0, 0.25, 1.0, 1.0);
                this.renderStandardBlock(block, i, j, k);
                this.renderPistonRodEW(
                    (double)((float)i + 0.25F),
                    (double)((float)i + 0.25F + f3),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.375F),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.375F),
                    0.5F,
                    d0
                );
                this.renderPistonRodEW(
                    (double)((float)i + 0.25F),
                    (double)((float)i + 0.25F + f3),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.625F),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.625F),
                    1.0F,
                    d0
                );
                this.renderPistonRodEW(
                    (double)((float)i + 0.25F),
                    (double)((float)i + 0.25F + f3),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.375F),
                    0.6F,
                    d0
                );
                this.renderPistonRodEW(
                    (double)((float)i + 0.25F),
                    (double)((float)i + 0.25F + f3),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.625F),
                    0.6F,
                    d0
                );
                break;
            case 5:
                this.uvRotateEast = 2;
                this.uvRotateWest = 1;
                this.uvRotateTop = 1;
                this.uvRotateBottom = 2;
                this.setRenderBounds(0.75, 0.0, 0.0, 1.0, 1.0, 1.0);
                this.renderStandardBlock(block, i, j, k);
                this.renderPistonRodEW(
                    (double)((float)i - 0.25F + 1.0F - f3),
                    (double)((float)i - 0.25F + 1.0F),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.375F),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.375F),
                    0.5F,
                    d0
                );
                this.renderPistonRodEW(
                    (double)((float)i - 0.25F + 1.0F - f3),
                    (double)((float)i - 0.25F + 1.0F),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.625F),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.625F),
                    1.0F,
                    d0
                );
                this.renderPistonRodEW(
                    (double)((float)i - 0.25F + 1.0F - f3),
                    (double)((float)i - 0.25F + 1.0F),
                    (double)((float)j + 0.375F),
                    (double)((float)j + 0.625F),
                    (double)((float)k + 0.375F),
                    (double)((float)k + 0.375F),
                    0.6F,
                    d0
                );
                this.renderPistonRodEW(
                    (double)((float)i - 0.25F + 1.0F - f3),
                    (double)((float)i - 0.25F + 1.0F),
                    (double)((float)j + 0.625F),
                    (double)((float)j + 0.375F),
                    (double)((float)k + 0.625F),
                    (double)((float)k + 0.625F),
                    0.6F,
                    d0
                );
        }

        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateSouth = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        return true;
    }

    public boolean renderBlockLever(Block block, int i, int j, int k) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = l & 7;
        boolean flag = (l & 8) > 0;
        Tessellator tessellator = Tessellator.instance;
        boolean flag1 = this.hasOverrideBlockTexture();
        if (!flag1) {
            this.setOverrideBlockTexture(this.getBlockIcon(Blocks.cobblestone));
        }

        float f = 0.25F;
        float f1 = 0.1875F;
        float f2 = 0.1875F;
        if (i1 == 5) {
            this.setRenderBounds((double)(0.5F - f1), 0.0, (double)(0.5F - f), (double)(0.5F + f1), (double)f2, (double)(0.5F + f));
        } else if (i1 == 6) {
            this.setRenderBounds((double)(0.5F - f), 0.0, (double)(0.5F - f1), (double)(0.5F + f), (double)f2, (double)(0.5F + f1));
        } else if (i1 == 4) {
            this.setRenderBounds((double)(0.5F - f1), (double)(0.5F - f), (double)(1.0F - f2), (double)(0.5F + f1), (double)(0.5F + f), 1.0);
        } else if (i1 == 3) {
            this.setRenderBounds((double)(0.5F - f1), (double)(0.5F - f), 0.0, (double)(0.5F + f1), (double)(0.5F + f), (double)f2);
        } else if (i1 == 2) {
            this.setRenderBounds((double)(1.0F - f2), (double)(0.5F - f), (double)(0.5F - f1), 1.0, (double)(0.5F + f), (double)(0.5F + f1));
        } else if (i1 == 1) {
            this.setRenderBounds(0.0, (double)(0.5F - f), (double)(0.5F - f1), (double)f2, (double)(0.5F + f), (double)(0.5F + f1));
        } else if (i1 == 0) {
            this.setRenderBounds((double)(0.5F - f), (double)(1.0F - f2), (double)(0.5F - f1), (double)(0.5F + f), 1.0, (double)(0.5F + f1));
        } else if (i1 == 7) {
            this.setRenderBounds((double)(0.5F - f1), (double)(1.0F - f2), (double)(0.5F - f), (double)(0.5F + f1), 1.0, (double)(0.5F + f));
        }

        this.renderStandardBlock(block, i, j, k);
        if (!flag1) {
            this.clearOverrideBlockTexture();
        }

        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        IIcon iicon = this.getBlockIconFromSide(block, 0);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMinV();
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getMaxV();
        Vec3[] avec3 = new Vec3[8];
        float f3 = 0.0625F;
        float f4 = 0.0625F;
        float f5 = 0.625F;
        avec3[0] = Vec3.createVectorHelper((double)(-f3), 0.0, (double)(-f4));
        avec3[1] = Vec3.createVectorHelper((double)f3, 0.0, (double)(-f4));
        avec3[2] = Vec3.createVectorHelper((double)f3, 0.0, (double)f4);
        avec3[3] = Vec3.createVectorHelper((double)(-f3), 0.0, (double)f4);
        avec3[4] = Vec3.createVectorHelper((double)(-f3), (double)f5, (double)(-f4));
        avec3[5] = Vec3.createVectorHelper((double)f3, (double)f5, (double)(-f4));
        avec3[6] = Vec3.createVectorHelper((double)f3, (double)f5, (double)f4);
        avec3[7] = Vec3.createVectorHelper((double)(-f3), (double)f5, (double)f4);

        for (int j1 = 0; j1 < 8; j1++) {
            if (flag) {
                avec3[j1].zCoord -= 0.0625;
                avec3[j1].rotateAroundX((float) Math.PI * 2.0F / 9.0F);
            } else {
                avec3[j1].zCoord += 0.0625;
                avec3[j1].rotateAroundX((float) -Math.PI * 2.0F / 9.0F);
            }

            if (i1 == 0 || i1 == 7) {
                avec3[j1].rotateAroundZ((float) Math.PI);
            }

            if (i1 == 6 || i1 == 0) {
                avec3[j1].rotateAroundY((float) (Math.PI / 2));
            }

            if (i1 > 0 && i1 < 5) {
                avec3[j1].yCoord -= 0.375;
                avec3[j1].rotateAroundX((float) (Math.PI / 2));
                if (i1 == 4) {
                    avec3[j1].rotateAroundY(0.0F);
                }

                if (i1 == 3) {
                    avec3[j1].rotateAroundY((float) Math.PI);
                }

                if (i1 == 2) {
                    avec3[j1].rotateAroundY((float) (Math.PI / 2));
                }

                if (i1 == 1) {
                    avec3[j1].rotateAroundY((float) (-Math.PI / 2));
                }

                avec3[j1].xCoord += (double)i + 0.5;
                avec3[j1].yCoord += (double)((float)j + 0.5F);
                avec3[j1].zCoord += (double)k + 0.5;
            } else if (i1 != 0 && i1 != 7) {
                avec3[j1].xCoord += (double)i + 0.5;
                avec3[j1].yCoord += (double)((float)j + 0.125F);
                avec3[j1].zCoord += (double)k + 0.5;
            } else {
                avec3[j1].xCoord += (double)i + 0.5;
                avec3[j1].yCoord += (double)((float)j + 0.875F);
                avec3[j1].zCoord += (double)k + 0.5;
            }
        }

        Vec3 vec33 = null;
        Vec3 vec3 = null;
        Vec3 vec31 = null;
        Vec3 vec32 = null;

        for (int k1 = 0; k1 < 6; k1++) {
            if (k1 == 0) {
                d0 = (double)iicon.getInterpolatedU(7.0);
                d1 = (double)iicon.getInterpolatedV(6.0);
                d2 = (double)iicon.getInterpolatedU(9.0);
                d3 = (double)iicon.getInterpolatedV(8.0);
            } else if (k1 == 2) {
                d0 = (double)iicon.getInterpolatedU(7.0);
                d1 = (double)iicon.getInterpolatedV(6.0);
                d2 = (double)iicon.getInterpolatedU(9.0);
                d3 = (double)iicon.getMaxV();
            }

            if (k1 == 0) {
                vec33 = avec3[0];
                vec3 = avec3[1];
                vec31 = avec3[2];
                vec32 = avec3[3];
            } else if (k1 == 1) {
                vec33 = avec3[7];
                vec3 = avec3[6];
                vec31 = avec3[5];
                vec32 = avec3[4];
            } else if (k1 == 2) {
                vec33 = avec3[1];
                vec3 = avec3[0];
                vec31 = avec3[4];
                vec32 = avec3[5];
            } else if (k1 == 3) {
                vec33 = avec3[2];
                vec3 = avec3[1];
                vec31 = avec3[5];
                vec32 = avec3[6];
            } else if (k1 == 4) {
                vec33 = avec3[3];
                vec3 = avec3[2];
                vec31 = avec3[6];
                vec32 = avec3[7];
            } else if (k1 == 5) {
                vec33 = avec3[0];
                vec3 = avec3[3];
                vec31 = avec3[7];
                vec32 = avec3[4];
            }

            tessellator.addVertexWithUV(vec33.xCoord, vec33.yCoord, vec33.zCoord, d0, d3);
            tessellator.addVertexWithUV(vec3.xCoord, vec3.yCoord, vec3.zCoord, d2, d3);
            tessellator.addVertexWithUV(vec31.xCoord, vec31.yCoord, vec31.zCoord, d2, d1);
            tessellator.addVertexWithUV(vec32.xCoord, vec32.yCoord, vec32.zCoord, d0, d1);
        }

        return true;
    }

    public boolean renderBlockTripWireSource(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = l & 3;
        boolean flag = (l & 4) == 4;
        boolean flag1 = (l & 8) == 8;
        boolean flag2 = !World.doesBlockHaveSolidTopSurface(this.blockAccess, i, j - 1, k);
        boolean flag3 = this.hasOverrideBlockTexture();
        if (!flag3) {
            this.setOverrideBlockTexture(this.getBlockIcon(Blocks.planks));
        }

        float f = 0.25F;
        float f1 = 0.125F;
        float f2 = 0.125F;
        float f3 = 0.3F - f;
        float f4 = 0.3F + f;
        if (i1 == 2) {
            this.setRenderBounds((double)(0.5F - f1), (double)f3, (double)(1.0F - f2), (double)(0.5F + f1), (double)f4, 1.0);
        } else if (i1 == 0) {
            this.setRenderBounds((double)(0.5F - f1), (double)f3, 0.0, (double)(0.5F + f1), (double)f4, (double)f2);
        } else if (i1 == 1) {
            this.setRenderBounds((double)(1.0F - f2), (double)f3, (double)(0.5F - f1), 1.0, (double)f4, (double)(0.5F + f1));
        } else if (i1 == 3) {
            this.setRenderBounds(0.0, (double)f3, (double)(0.5F - f1), (double)f2, (double)f4, (double)(0.5F + f1));
        }

        this.renderStandardBlock(block, i, j, k);
        if (!flag3) {
            this.clearOverrideBlockTexture();
        }

        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        IIcon iicon = this.getBlockIconFromSide(block, 0);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMinV();
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getMaxV();
        Vec3[] avec3 = new Vec3[8];
        float f5 = 0.046875F;
        float f6 = 0.046875F;
        float f7 = 0.3125F;
        avec3[0] = Vec3.createVectorHelper((double)(-f5), 0.0, (double)(-f6));
        avec3[1] = Vec3.createVectorHelper((double)f5, 0.0, (double)(-f6));
        avec3[2] = Vec3.createVectorHelper((double)f5, 0.0, (double)f6);
        avec3[3] = Vec3.createVectorHelper((double)(-f5), 0.0, (double)f6);
        avec3[4] = Vec3.createVectorHelper((double)(-f5), (double)f7, (double)(-f6));
        avec3[5] = Vec3.createVectorHelper((double)f5, (double)f7, (double)(-f6));
        avec3[6] = Vec3.createVectorHelper((double)f5, (double)f7, (double)f6);
        avec3[7] = Vec3.createVectorHelper((double)(-f5), (double)f7, (double)f6);

        for (int j1 = 0; j1 < 8; j1++) {
            avec3[j1].zCoord += 0.0625;
            if (flag1) {
                avec3[j1].rotateAroundX((float) (Math.PI / 6));
                avec3[j1].yCoord -= 0.4375;
            } else if (flag) {
                avec3[j1].rotateAroundX(0.08726647F);
                avec3[j1].yCoord -= 0.4375;
            } else {
                avec3[j1].rotateAroundX((float) -Math.PI * 2.0F / 9.0F);
                avec3[j1].yCoord -= 0.375;
            }

            avec3[j1].rotateAroundX((float) (Math.PI / 2));
            if (i1 == 2) {
                avec3[j1].rotateAroundY(0.0F);
            }

            if (i1 == 0) {
                avec3[j1].rotateAroundY((float) Math.PI);
            }

            if (i1 == 1) {
                avec3[j1].rotateAroundY((float) (Math.PI / 2));
            }

            if (i1 == 3) {
                avec3[j1].rotateAroundY((float) (-Math.PI / 2));
            }

            avec3[j1].xCoord += (double)i + 0.5;
            avec3[j1].yCoord += (double)((float)j + 0.3125F);
            avec3[j1].zCoord += (double)k + 0.5;
        }

        Vec3 vec33 = null;
        Vec3 vec3 = null;
        Vec3 vec31 = null;
        Vec3 vec32 = null;
        byte b0 = 7;
        byte b1 = 9;
        byte b2 = 9;
        byte b3 = 16;

        for (int k1 = 0; k1 < 6; k1++) {
            if (k1 == 0) {
                vec33 = avec3[0];
                vec3 = avec3[1];
                vec31 = avec3[2];
                vec32 = avec3[3];
                d0 = (double)iicon.getInterpolatedU((double)b0);
                d1 = (double)iicon.getInterpolatedV((double)b2);
                d2 = (double)iicon.getInterpolatedU((double)b1);
                d3 = (double)iicon.getInterpolatedV((double)(b2 + 2));
            } else if (k1 == 1) {
                vec33 = avec3[7];
                vec3 = avec3[6];
                vec31 = avec3[5];
                vec32 = avec3[4];
            } else if (k1 == 2) {
                vec33 = avec3[1];
                vec3 = avec3[0];
                vec31 = avec3[4];
                vec32 = avec3[5];
                d0 = (double)iicon.getInterpolatedU((double)b0);
                d1 = (double)iicon.getInterpolatedV((double)b2);
                d2 = (double)iicon.getInterpolatedU((double)b1);
                d3 = (double)iicon.getInterpolatedV((double)b3);
            } else if (k1 == 3) {
                vec33 = avec3[2];
                vec3 = avec3[1];
                vec31 = avec3[5];
                vec32 = avec3[6];
            } else if (k1 == 4) {
                vec33 = avec3[3];
                vec3 = avec3[2];
                vec31 = avec3[6];
                vec32 = avec3[7];
            } else if (k1 == 5) {
                vec33 = avec3[0];
                vec3 = avec3[3];
                vec31 = avec3[7];
                vec32 = avec3[4];
            }

            tessellator.addVertexWithUV(vec33.xCoord, vec33.yCoord, vec33.zCoord, d0, d3);
            tessellator.addVertexWithUV(vec3.xCoord, vec3.yCoord, vec3.zCoord, d2, d3);
            tessellator.addVertexWithUV(vec31.xCoord, vec31.yCoord, vec31.zCoord, d2, d1);
            tessellator.addVertexWithUV(vec32.xCoord, vec32.yCoord, vec32.zCoord, d0, d1);
        }

        float f13 = 0.09375F;
        float f8 = 0.09375F;
        float f9 = 0.03125F;
        avec3[0] = Vec3.createVectorHelper((double)(-f13), 0.0, (double)(-f8));
        avec3[1] = Vec3.createVectorHelper((double)f13, 0.0, (double)(-f8));
        avec3[2] = Vec3.createVectorHelper((double)f13, 0.0, (double)f8);
        avec3[3] = Vec3.createVectorHelper((double)(-f13), 0.0, (double)f8);
        avec3[4] = Vec3.createVectorHelper((double)(-f13), (double)f9, (double)(-f8));
        avec3[5] = Vec3.createVectorHelper((double)f13, (double)f9, (double)(-f8));
        avec3[6] = Vec3.createVectorHelper((double)f13, (double)f9, (double)f8);
        avec3[7] = Vec3.createVectorHelper((double)(-f13), (double)f9, (double)f8);

        for (int l1 = 0; l1 < 8; l1++) {
            avec3[l1].zCoord += 0.21875;
            if (flag1) {
                avec3[l1].yCoord -= 0.09375;
                avec3[l1].zCoord -= 0.1625;
                avec3[l1].rotateAroundX(0.0F);
            } else if (flag) {
                avec3[l1].yCoord += 0.015625;
                avec3[l1].zCoord -= 0.171875;
                avec3[l1].rotateAroundX(0.17453294F);
            } else {
                avec3[l1].rotateAroundX(0.87266463F);
            }

            if (i1 == 2) {
                avec3[l1].rotateAroundY(0.0F);
            }

            if (i1 == 0) {
                avec3[l1].rotateAroundY((float) Math.PI);
            }

            if (i1 == 1) {
                avec3[l1].rotateAroundY((float) (Math.PI / 2));
            }

            if (i1 == 3) {
                avec3[l1].rotateAroundY((float) (-Math.PI / 2));
            }

            avec3[l1].xCoord += (double)i + 0.5;
            avec3[l1].yCoord += (double)((float)j + 0.3125F);
            avec3[l1].zCoord += (double)k + 0.5;
        }

        byte b7 = 5;
        byte b4 = 11;
        byte b5 = 3;
        byte b6 = 9;

        for (int i2 = 0; i2 < 6; i2++) {
            if (i2 == 0) {
                vec33 = avec3[0];
                vec3 = avec3[1];
                vec31 = avec3[2];
                vec32 = avec3[3];
                d0 = (double)iicon.getInterpolatedU((double)b7);
                d1 = (double)iicon.getInterpolatedV((double)b5);
                d2 = (double)iicon.getInterpolatedU((double)b4);
                d3 = (double)iicon.getInterpolatedV((double)b6);
            } else if (i2 == 1) {
                vec33 = avec3[7];
                vec3 = avec3[6];
                vec31 = avec3[5];
                vec32 = avec3[4];
            } else if (i2 == 2) {
                vec33 = avec3[1];
                vec3 = avec3[0];
                vec31 = avec3[4];
                vec32 = avec3[5];
                d0 = (double)iicon.getInterpolatedU((double)b7);
                d1 = (double)iicon.getInterpolatedV((double)b5);
                d2 = (double)iicon.getInterpolatedU((double)b4);
                d3 = (double)iicon.getInterpolatedV((double)(b5 + 2));
            } else if (i2 == 3) {
                vec33 = avec3[2];
                vec3 = avec3[1];
                vec31 = avec3[5];
                vec32 = avec3[6];
            } else if (i2 == 4) {
                vec33 = avec3[3];
                vec3 = avec3[2];
                vec31 = avec3[6];
                vec32 = avec3[7];
            } else if (i2 == 5) {
                vec33 = avec3[0];
                vec3 = avec3[3];
                vec31 = avec3[7];
                vec32 = avec3[4];
            }

            tessellator.addVertexWithUV(vec33.xCoord, vec33.yCoord, vec33.zCoord, d0, d3);
            tessellator.addVertexWithUV(vec3.xCoord, vec3.yCoord, vec3.zCoord, d2, d3);
            tessellator.addVertexWithUV(vec31.xCoord, vec31.yCoord, vec31.zCoord, d2, d1);
            tessellator.addVertexWithUV(vec32.xCoord, vec32.yCoord, vec32.zCoord, d0, d1);
        }

        if (flag) {
            double d9 = avec3[0].yCoord;
            float f10 = 0.03125F;
            float f11 = 0.5F - f10 / 2.0F;
            float f12 = f11 + f10;
            double d4 = (double)iicon.getMinU();
            double d5 = (double)iicon.getInterpolatedV(flag ? 2.0 : 0.0);
            double d6 = (double)iicon.getMaxU();
            double d7 = (double)iicon.getInterpolatedV(flag ? 4.0 : 2.0);
            double d8 = (double)(flag2 ? 3.5F : 1.5F) / 16.0;
            tessellator.setColorOpaque_F(0.75F, 0.75F, 0.75F);
            if (i1 == 2) {
                tessellator.addVertexWithUV((double)((float)i + f11), (double)j + d8, (double)k + 0.25, d4, d5);
                tessellator.addVertexWithUV((double)((float)i + f12), (double)j + d8, (double)k + 0.25, d4, d7);
                tessellator.addVertexWithUV((double)((float)i + f12), (double)j + d8, (double)k, d6, d7);
                tessellator.addVertexWithUV((double)((float)i + f11), (double)j + d8, (double)k, d6, d5);
                tessellator.addVertexWithUV((double)((float)i + f11), d9, (double)k + 0.5, d4, d5);
                tessellator.addVertexWithUV((double)((float)i + f12), d9, (double)k + 0.5, d4, d7);
                tessellator.addVertexWithUV((double)((float)i + f12), (double)j + d8, (double)k + 0.25, d6, d7);
                tessellator.addVertexWithUV((double)((float)i + f11), (double)j + d8, (double)k + 0.25, d6, d5);
            } else if (i1 == 0) {
                tessellator.addVertexWithUV((double)((float)i + f11), (double)j + d8, (double)k + 0.75, d4, d5);
                tessellator.addVertexWithUV((double)((float)i + f12), (double)j + d8, (double)k + 0.75, d4, d7);
                tessellator.addVertexWithUV((double)((float)i + f12), d9, (double)k + 0.5, d6, d7);
                tessellator.addVertexWithUV((double)((float)i + f11), d9, (double)k + 0.5, d6, d5);
                tessellator.addVertexWithUV((double)((float)i + f11), (double)j + d8, (double)(k + 1), d4, d5);
                tessellator.addVertexWithUV((double)((float)i + f12), (double)j + d8, (double)(k + 1), d4, d7);
                tessellator.addVertexWithUV((double)((float)i + f12), (double)j + d8, (double)k + 0.75, d6, d7);
                tessellator.addVertexWithUV((double)((float)i + f11), (double)j + d8, (double)k + 0.75, d6, d5);
            } else if (i1 == 1) {
                tessellator.addVertexWithUV((double)i, (double)j + d8, (double)((float)k + f12), d4, d7);
                tessellator.addVertexWithUV((double)i + 0.25, (double)j + d8, (double)((float)k + f12), d6, d7);
                tessellator.addVertexWithUV((double)i + 0.25, (double)j + d8, (double)((float)k + f11), d6, d5);
                tessellator.addVertexWithUV((double)i, (double)j + d8, (double)((float)k + f11), d4, d5);
                tessellator.addVertexWithUV((double)i + 0.25, (double)j + d8, (double)((float)k + f12), d4, d7);
                tessellator.addVertexWithUV((double)i + 0.5, d9, (double)((float)k + f12), d6, d7);
                tessellator.addVertexWithUV((double)i + 0.5, d9, (double)((float)k + f11), d6, d5);
                tessellator.addVertexWithUV((double)i + 0.25, (double)j + d8, (double)((float)k + f11), d4, d5);
            } else {
                tessellator.addVertexWithUV((double)i + 0.5, d9, (double)((float)k + f12), d4, d7);
                tessellator.addVertexWithUV((double)i + 0.75, (double)j + d8, (double)((float)k + f12), d6, d7);
                tessellator.addVertexWithUV((double)i + 0.75, (double)j + d8, (double)((float)k + f11), d6, d5);
                tessellator.addVertexWithUV((double)i + 0.5, d9, (double)((float)k + f11), d4, d5);
                tessellator.addVertexWithUV((double)i + 0.75, (double)j + d8, (double)((float)k + f12), d4, d7);
                tessellator.addVertexWithUV((double)(i + 1), (double)j + d8, (double)((float)k + f12), d6, d7);
                tessellator.addVertexWithUV((double)(i + 1), (double)j + d8, (double)((float)k + f11), d6, d5);
                tessellator.addVertexWithUV((double)i + 0.75, (double)j + d8, (double)((float)k + f11), d4, d5);
            }
        }

        return true;
    }

    public boolean renderBlockTripWire(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSide(block, 0);
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        boolean flag = (l & 4) == 4;
        boolean flag1 = (l & 2) == 2;
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getInterpolatedV(flag ? 2.0 : 0.0);
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getInterpolatedV(flag ? 4.0 : 2.0);
        double d4 = (double)(flag1 ? 3.5F : 1.5F) / 16.0;
        boolean flag2 = BlockTripWire.func_150139_a(this.blockAccess, i, j, k, l, 1);
        boolean flag3 = BlockTripWire.func_150139_a(this.blockAccess, i, j, k, l, 3);
        boolean flag4 = BlockTripWire.func_150139_a(this.blockAccess, i, j, k, l, 2);
        boolean flag5 = BlockTripWire.func_150139_a(this.blockAccess, i, j, k, l, 0);
        float f = 0.03125F;
        float f1 = 0.5F - f / 2.0F;
        float f2 = f1 + f;
        if (!flag4 && !flag3 && !flag5 && !flag2) {
            flag4 = true;
            flag5 = true;
        }

        if (flag4) {
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.25, d0, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.25, d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.25, d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.25, d0, d1);
        }

        if (flag4 || flag5 && !flag3 && !flag2) {
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.5, d0, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.5, d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.25, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.25, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.25, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.25, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.5, d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.5, d0, d1);
        }

        if (flag5 || flag4 && !flag3 && !flag2) {
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.75, d0, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.75, d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.5, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.5, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.5, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.5, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.75, d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.75, d0, d1);
        }

        if (flag5) {
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)(k + 1), d0, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)(k + 1), d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.75, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.75, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)k + 0.75, d2, d1);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)k + 0.75, d2, d3);
            tessellator.addVertexWithUV((double)((float)i + f2), (double)j + d4, (double)(k + 1), d0, d3);
            tessellator.addVertexWithUV((double)((float)i + f1), (double)j + d4, (double)(k + 1), d0, d1);
        }

        if (flag2) {
            tessellator.addVertexWithUV((double)i, (double)j + d4, (double)((float)k + f2), d0, d3);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i, (double)j + d4, (double)((float)k + f2), d0, d3);
        }

        if (flag2 || flag3 && !flag4 && !flag5) {
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f2), d0, d3);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i + 0.25, (double)j + d4, (double)((float)k + f2), d0, d3);
        }

        if (flag3 || flag2 && !flag4 && !flag5) {
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f2), d0, d3);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i + 0.5, (double)j + d4, (double)((float)k + f2), d0, d3);
        }

        if (flag3) {
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f2), d0, d3);
            tessellator.addVertexWithUV((double)(i + 1), (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)(i + 1), (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f1), d0, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)j + d4, (double)((float)k + f1), d2, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)j + d4, (double)((float)k + f2), d2, d3);
            tessellator.addVertexWithUV((double)i + 0.75, (double)j + d4, (double)((float)k + f2), d0, d3);
        }

        return true;
    }

    public boolean renderBlockFire(BlockFire blockfire, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = blockfire.getFireIcon(0);
        IIcon iicon1 = blockfire.getFireIcon(1);
        IIcon iicon2 = iicon;
        if (this.hasOverrideBlockTexture()) {
            iicon2 = this.overrideBlockTexture;
        }

        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        tessellator.setBrightness(blockfire.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        double d0 = (double)iicon2.getMinU();
        double d1 = (double)iicon2.getMinV();
        double d2 = (double)iicon2.getMaxU();
        double d3 = (double)iicon2.getMaxV();
        float f = 1.4F;
        if (!World.doesBlockHaveSolidTopSurface(this.blockAccess, i, j - 1, k) && !Blocks.fire.canBlockCatchFire(this.blockAccess, i, j - 1, k)) {
            float f2 = 0.2F;
            float f1 = 0.0625F;
            if ((i + j + k & 1) == 1) {
                d0 = (double)iicon1.getMinU();
                d1 = (double)iicon1.getMinV();
                d2 = (double)iicon1.getMaxU();
                d3 = (double)iicon1.getMaxV();
            }

            if ((i / 2 + j / 2 + k / 2 & 1) == 1) {
                double d13 = d2;
                d2 = d0;
                d0 = d13;
            }

            if (Blocks.fire.canBlockCatchFire(this.blockAccess, i - 1, j, k)) {
                tessellator.addVertexWithUV((double)((float)i + f2), (double)((float)j + f + f1), (double)(k + 1), d2, d1);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 1), d2, d3);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 0), d0, d3);
                tessellator.addVertexWithUV((double)((float)i + f2), (double)((float)j + f + f1), (double)(k + 0), d0, d1);
                tessellator.addVertexWithUV((double)((float)i + f2), (double)((float)j + f + f1), (double)(k + 0), d0, d1);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 0), d0, d3);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 1), d2, d3);
                tessellator.addVertexWithUV((double)((float)i + f2), (double)((float)j + f + f1), (double)(k + 1), d2, d1);
            }

            if (Blocks.fire.canBlockCatchFire(this.blockAccess, i + 1, j, k)) {
                tessellator.addVertexWithUV((double)((float)(i + 1) - f2), (double)((float)j + f + f1), (double)(k + 0), d0, d1);
                tessellator.addVertexWithUV((double)(i + 1 - 0), (double)((float)(j + 0) + f1), (double)(k + 0), d0, d3);
                tessellator.addVertexWithUV((double)(i + 1 - 0), (double)((float)(j + 0) + f1), (double)(k + 1), d2, d3);
                tessellator.addVertexWithUV((double)((float)(i + 1) - f2), (double)((float)j + f + f1), (double)(k + 1), d2, d1);
                tessellator.addVertexWithUV((double)((float)(i + 1) - f2), (double)((float)j + f + f1), (double)(k + 1), d2, d1);
                tessellator.addVertexWithUV((double)(i + 1 - 0), (double)((float)(j + 0) + f1), (double)(k + 1), d2, d3);
                tessellator.addVertexWithUV((double)(i + 1 - 0), (double)((float)(j + 0) + f1), (double)(k + 0), d0, d3);
                tessellator.addVertexWithUV((double)((float)(i + 1) - f2), (double)((float)j + f + f1), (double)(k + 0), d0, d1);
            }

            if (Blocks.fire.canBlockCatchFire(this.blockAccess, i, j, k - 1)) {
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f + f1), (double)((float)k + f2), d2, d1);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 0), d2, d3);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)(j + 0) + f1), (double)(k + 0), d0, d3);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f + f1), (double)((float)k + f2), d0, d1);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f + f1), (double)((float)k + f2), d0, d1);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)(j + 0) + f1), (double)(k + 0), d0, d3);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 0), d2, d3);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f + f1), (double)((float)k + f2), d2, d1);
            }

            if (Blocks.fire.canBlockCatchFire(this.blockAccess, i, j, k + 1)) {
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f + f1), (double)((float)(k + 1) - f2), d0, d1);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)(j + 0) + f1), (double)(k + 1 - 0), d0, d3);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 1 - 0), d2, d3);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f + f1), (double)((float)(k + 1) - f2), d2, d1);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f + f1), (double)((float)(k + 1) - f2), d2, d1);
                tessellator.addVertexWithUV((double)(i + 0), (double)((float)(j + 0) + f1), (double)(k + 1 - 0), d2, d3);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)(j + 0) + f1), (double)(k + 1 - 0), d0, d3);
                tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f + f1), (double)((float)(k + 1) - f2), d0, d1);
            }

            if (Blocks.fire.canBlockCatchFire(this.blockAccess, i, j + 1, k)) {
                double d14 = (double)i + 0.5 + 0.5;
                double d15 = (double)i + 0.5 - 0.5;
                double d16 = (double)k + 0.5 + 0.5;
                double d17 = (double)k + 0.5 - 0.5;
                double d18 = (double)i + 0.5 - 0.5;
                double d19 = (double)i + 0.5 + 0.5;
                double d20 = (double)k + 0.5 - 0.5;
                double d12 = (double)k + 0.5 + 0.5;
                d0 = (double)iicon.getMinU();
                d1 = (double)iicon.getMinV();
                d2 = (double)iicon.getMaxU();
                d3 = (double)iicon.getMaxV();
                j++;
                f = -0.2F;
                if ((i + j + k & 1) == 0) {
                    tessellator.addVertexWithUV(d18, (double)((float)j + f), (double)(k + 0), d2, d1);
                    tessellator.addVertexWithUV(d14, (double)(j + 0), (double)(k + 0), d2, d3);
                    tessellator.addVertexWithUV(d14, (double)(j + 0), (double)(k + 1), d0, d3);
                    tessellator.addVertexWithUV(d18, (double)((float)j + f), (double)(k + 1), d0, d1);
                    d0 = (double)iicon1.getMinU();
                    d1 = (double)iicon1.getMinV();
                    d2 = (double)iicon1.getMaxU();
                    d3 = (double)iicon1.getMaxV();
                    tessellator.addVertexWithUV(d19, (double)((float)j + f), (double)(k + 1), d2, d1);
                    tessellator.addVertexWithUV(d15, (double)(j + 0), (double)(k + 1), d2, d3);
                    tessellator.addVertexWithUV(d15, (double)(j + 0), (double)(k + 0), d0, d3);
                    tessellator.addVertexWithUV(d19, (double)((float)j + f), (double)(k + 0), d0, d1);
                } else {
                    tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f), d12, d2, d1);
                    tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), d17, d2, d3);
                    tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), d17, d0, d3);
                    tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f), d12, d0, d1);
                    d0 = (double)iicon1.getMinU();
                    d1 = (double)iicon1.getMinV();
                    d2 = (double)iicon1.getMaxU();
                    d3 = (double)iicon1.getMaxV();
                    tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f), d20, d2, d1);
                    tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), d16, d2, d3);
                    tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), d16, d0, d3);
                    tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f), d20, d0, d1);
                }
            }
        } else {
            double d4 = (double)i + 0.5 + 0.2;
            double d5 = (double)i + 0.5 - 0.2;
            double d6 = (double)k + 0.5 + 0.2;
            double d7 = (double)k + 0.5 - 0.2;
            double d8 = (double)i + 0.5 - 0.3;
            double d9 = (double)i + 0.5 + 0.3;
            double d10 = (double)k + 0.5 - 0.3;
            double d11 = (double)k + 0.5 + 0.3;
            tessellator.addVertexWithUV(d8, (double)((float)j + f), (double)(k + 1), d2, d1);
            tessellator.addVertexWithUV(d4, (double)(j + 0), (double)(k + 1), d2, d3);
            tessellator.addVertexWithUV(d4, (double)(j + 0), (double)(k + 0), d0, d3);
            tessellator.addVertexWithUV(d8, (double)((float)j + f), (double)(k + 0), d0, d1);
            tessellator.addVertexWithUV(d9, (double)((float)j + f), (double)(k + 0), d2, d1);
            tessellator.addVertexWithUV(d5, (double)(j + 0), (double)(k + 0), d2, d3);
            tessellator.addVertexWithUV(d5, (double)(j + 0), (double)(k + 1), d0, d3);
            tessellator.addVertexWithUV(d9, (double)((float)j + f), (double)(k + 1), d0, d1);
            d0 = (double)iicon1.getMinU();
            d1 = (double)iicon1.getMinV();
            d2 = (double)iicon1.getMaxU();
            d3 = (double)iicon1.getMaxV();
            tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f), d11, d2, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), d7, d2, d3);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), d7, d0, d3);
            tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f), d11, d0, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f), d10, d2, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), d6, d2, d3);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), d6, d0, d3);
            tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f), d10, d0, d1);
            d4 = (double)i + 0.5 - 0.5;
            d5 = (double)i + 0.5 + 0.5;
            d6 = (double)k + 0.5 - 0.5;
            d7 = (double)k + 0.5 + 0.5;
            d8 = (double)i + 0.5 - 0.4;
            d9 = (double)i + 0.5 + 0.4;
            d10 = (double)k + 0.5 - 0.4;
            d11 = (double)k + 0.5 + 0.4;
            tessellator.addVertexWithUV(d8, (double)((float)j + f), (double)(k + 0), d0, d1);
            tessellator.addVertexWithUV(d4, (double)(j + 0), (double)(k + 0), d0, d3);
            tessellator.addVertexWithUV(d4, (double)(j + 0), (double)(k + 1), d2, d3);
            tessellator.addVertexWithUV(d8, (double)((float)j + f), (double)(k + 1), d2, d1);
            tessellator.addVertexWithUV(d9, (double)((float)j + f), (double)(k + 1), d0, d1);
            tessellator.addVertexWithUV(d5, (double)(j + 0), (double)(k + 1), d0, d3);
            tessellator.addVertexWithUV(d5, (double)(j + 0), (double)(k + 0), d2, d3);
            tessellator.addVertexWithUV(d9, (double)((float)j + f), (double)(k + 0), d2, d1);
            d0 = (double)iicon.getMinU();
            d1 = (double)iicon.getMinV();
            d2 = (double)iicon.getMaxU();
            d3 = (double)iicon.getMaxV();
            tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f), d11, d0, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), d7, d0, d3);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), d7, d2, d3);
            tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f), d11, d2, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)((float)j + f), d10, d0, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), d6, d0, d3);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), d6, d2, d3);
            tessellator.addVertexWithUV((double)(i + 0), (double)((float)j + f), d10, d2, d1);
        }

        return true;
    }

    public boolean renderBlockRedstoneWire(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        IIcon iicon = BlockRedstoneWire.getRedstoneWireIcon("cross");
        IIcon iicon1 = BlockRedstoneWire.getRedstoneWireIcon("line");
        IIcon iicon2 = BlockRedstoneWire.getRedstoneWireIcon("cross_overlay");
        IIcon iicon3 = BlockRedstoneWire.getRedstoneWireIcon("line_overlay");
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        float f = (float)l / 15.0F;
        float f1 = f * 0.6F + 0.4F;
        if (l == 0) {
            f1 = 0.3F;
        }

        float f2 = f * f * 0.7F - 0.5F;
        float f3 = f * f * 0.6F - 0.7F;
        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        if (f3 < 0.0F) {
            f3 = 0.0F;
        }

        tessellator.setColorOpaque_F(f1, f2, f3);
        double d0 = 0.015625;
        double d1 = 0.015625;
        boolean flag = BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i - 1, j, k, 1)
            || !this.blockAccess.getBlock(i - 1, j, k).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i - 1, j - 1, k, -1);
        boolean flag1 = BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i + 1, j, k, 3)
            || !this.blockAccess.getBlock(i + 1, j, k).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i + 1, j - 1, k, -1);
        boolean flag2 = BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i, j, k - 1, 2)
            || !this.blockAccess.getBlock(i, j, k - 1).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i, j - 1, k - 1, -1);
        boolean flag3 = BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i, j, k + 1, 0)
            || !this.blockAccess.getBlock(i, j, k + 1).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i, j - 1, k + 1, -1);
        if (!this.blockAccess.getBlock(i, j + 1, k).isBlockNormalCube()) {
            if (this.blockAccess.getBlock(i - 1, j, k).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i - 1, j + 1, k, -1)) {
                flag = true;
            }

            if (this.blockAccess.getBlock(i + 1, j, k).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i + 1, j + 1, k, -1)) {
                flag1 = true;
            }

            if (this.blockAccess.getBlock(i, j, k - 1).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i, j + 1, k - 1, -1)) {
                flag2 = true;
            }

            if (this.blockAccess.getBlock(i, j, k + 1).isBlockNormalCube() && BlockRedstoneWire.isPowerProviderOrWire(this.blockAccess, i, j + 1, k + 1, -1)) {
                flag3 = true;
            }
        }

        float f4 = (float)(i + 0);
        float f5 = (float)(i + 1);
        float f6 = (float)(k + 0);
        float f7 = (float)(k + 1);
        byte b0 = 0;
        if ((flag || flag1) && !flag2 && !flag3) {
            b0 = 1;
        }

        if ((flag2 || flag3) && !flag1 && !flag) {
            b0 = 2;
        }

        if (b0 == 0) {
            byte b1 = 0;
            byte b2 = 0;
            byte b3 = 16;
            byte b4 = 16;
            byte b5 = 5;
            if (!flag) {
                f4 += 0.3125F;
            }

            if (!flag) {
                b1 += 5;
            }

            if (!flag1) {
                f5 -= 0.3125F;
            }

            if (!flag1) {
                b3 -= 5;
            }

            if (!flag2) {
                f6 += 0.3125F;
            }

            if (!flag2) {
                b2 += 5;
            }

            if (!flag3) {
                f7 -= 0.3125F;
            }

            if (!flag3) {
                b4 -= 5;
            }

            tessellator.addVertexWithUV(
                (double)f5, (double)j + 0.015625, (double)f7, (double)iicon.getInterpolatedU((double)b3), (double)iicon.getInterpolatedV((double)b4)
            );
            tessellator.addVertexWithUV(
                (double)f5, (double)j + 0.015625, (double)f6, (double)iicon.getInterpolatedU((double)b3), (double)iicon.getInterpolatedV((double)b2)
            );
            tessellator.addVertexWithUV(
                (double)f4, (double)j + 0.015625, (double)f6, (double)iicon.getInterpolatedU((double)b1), (double)iicon.getInterpolatedV((double)b2)
            );
            tessellator.addVertexWithUV(
                (double)f4, (double)j + 0.015625, (double)f7, (double)iicon.getInterpolatedU((double)b1), (double)iicon.getInterpolatedV((double)b4)
            );
            tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            tessellator.addVertexWithUV(
                (double)f5, (double)j + 0.015625, (double)f7, (double)iicon2.getInterpolatedU((double)b3), (double)iicon2.getInterpolatedV((double)b4)
            );
            tessellator.addVertexWithUV(
                (double)f5, (double)j + 0.015625, (double)f6, (double)iicon2.getInterpolatedU((double)b3), (double)iicon2.getInterpolatedV((double)b2)
            );
            tessellator.addVertexWithUV(
                (double)f4, (double)j + 0.015625, (double)f6, (double)iicon2.getInterpolatedU((double)b1), (double)iicon2.getInterpolatedV((double)b2)
            );
            tessellator.addVertexWithUV(
                (double)f4, (double)j + 0.015625, (double)f7, (double)iicon2.getInterpolatedU((double)b1), (double)iicon2.getInterpolatedV((double)b4)
            );
        } else if (b0 == 1) {
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f7, (double)iicon1.getMaxU(), (double)iicon1.getMaxV());
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f6, (double)iicon1.getMaxU(), (double)iicon1.getMinV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f6, (double)iicon1.getMinU(), (double)iicon1.getMinV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f7, (double)iicon1.getMinU(), (double)iicon1.getMaxV());
            tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f7, (double)iicon3.getMaxU(), (double)iicon3.getMaxV());
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f6, (double)iicon3.getMaxU(), (double)iicon3.getMinV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f6, (double)iicon3.getMinU(), (double)iicon3.getMinV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f7, (double)iicon3.getMinU(), (double)iicon3.getMaxV());
        } else {
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f7, (double)iicon1.getMaxU(), (double)iicon1.getMaxV());
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f6, (double)iicon1.getMinU(), (double)iicon1.getMaxV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f6, (double)iicon1.getMinU(), (double)iicon1.getMinV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f7, (double)iicon1.getMaxU(), (double)iicon1.getMinV());
            tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f7, (double)iicon3.getMaxU(), (double)iicon3.getMaxV());
            tessellator.addVertexWithUV((double)f5, (double)j + 0.015625, (double)f6, (double)iicon3.getMinU(), (double)iicon3.getMaxV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f6, (double)iicon3.getMinU(), (double)iicon3.getMinV());
            tessellator.addVertexWithUV((double)f4, (double)j + 0.015625, (double)f7, (double)iicon3.getMaxU(), (double)iicon3.getMinV());
        }

        if (!this.blockAccess.getBlock(i, j + 1, k).isBlockNormalCube()) {
            float f8 = 0.021875F;
            if (this.blockAccess.getBlock(i - 1, j, k).isBlockNormalCube() && this.blockAccess.getBlock(i - 1, j + 1, k) == Blocks.redstone_wire) {
                tessellator.setColorOpaque_F(f1, f2, f3);
                tessellator.addVertexWithUV(
                    (double)i + 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 1), (double)iicon1.getMaxU(), (double)iicon1.getMinV()
                );
                tessellator.addVertexWithUV((double)i + 0.015625, (double)(j + 0), (double)(k + 1), (double)iicon1.getMinU(), (double)iicon1.getMinV());
                tessellator.addVertexWithUV((double)i + 0.015625, (double)(j + 0), (double)(k + 0), (double)iicon1.getMinU(), (double)iicon1.getMaxV());
                tessellator.addVertexWithUV(
                    (double)i + 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 0), (double)iicon1.getMaxU(), (double)iicon1.getMaxV()
                );
                tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
                tessellator.addVertexWithUV(
                    (double)i + 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 1), (double)iicon3.getMaxU(), (double)iicon3.getMinV()
                );
                tessellator.addVertexWithUV((double)i + 0.015625, (double)(j + 0), (double)(k + 1), (double)iicon3.getMinU(), (double)iicon3.getMinV());
                tessellator.addVertexWithUV((double)i + 0.015625, (double)(j + 0), (double)(k + 0), (double)iicon3.getMinU(), (double)iicon3.getMaxV());
                tessellator.addVertexWithUV(
                    (double)i + 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 0), (double)iicon3.getMaxU(), (double)iicon3.getMaxV()
                );
            }

            if (this.blockAccess.getBlock(i + 1, j, k).isBlockNormalCube() && this.blockAccess.getBlock(i + 1, j + 1, k) == Blocks.redstone_wire) {
                tessellator.setColorOpaque_F(f1, f2, f3);
                tessellator.addVertexWithUV((double)(i + 1) - 0.015625, (double)(j + 0), (double)(k + 1), (double)iicon1.getMinU(), (double)iicon1.getMaxV());
                tessellator.addVertexWithUV(
                    (double)(i + 1) - 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 1), (double)iicon1.getMaxU(), (double)iicon1.getMaxV()
                );
                tessellator.addVertexWithUV(
                    (double)(i + 1) - 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 0), (double)iicon1.getMaxU(), (double)iicon1.getMinV()
                );
                tessellator.addVertexWithUV((double)(i + 1) - 0.015625, (double)(j + 0), (double)(k + 0), (double)iicon1.getMinU(), (double)iicon1.getMinV());
                tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
                tessellator.addVertexWithUV((double)(i + 1) - 0.015625, (double)(j + 0), (double)(k + 1), (double)iicon3.getMinU(), (double)iicon3.getMaxV());
                tessellator.addVertexWithUV(
                    (double)(i + 1) - 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 1), (double)iicon3.getMaxU(), (double)iicon3.getMaxV()
                );
                tessellator.addVertexWithUV(
                    (double)(i + 1) - 0.015625, (double)((float)(j + 1) + 0.021875F), (double)(k + 0), (double)iicon3.getMaxU(), (double)iicon3.getMinV()
                );
                tessellator.addVertexWithUV((double)(i + 1) - 0.015625, (double)(j + 0), (double)(k + 0), (double)iicon3.getMinU(), (double)iicon3.getMinV());
            }

            if (this.blockAccess.getBlock(i, j, k - 1).isBlockNormalCube() && this.blockAccess.getBlock(i, j + 1, k - 1) == Blocks.redstone_wire) {
                tessellator.setColorOpaque_F(f1, f2, f3);
                tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)k + 0.015625, (double)iicon1.getMinU(), (double)iicon1.getMaxV());
                tessellator.addVertexWithUV(
                    (double)(i + 1), (double)((float)(j + 1) + 0.021875F), (double)k + 0.015625, (double)iicon1.getMaxU(), (double)iicon1.getMaxV()
                );
                tessellator.addVertexWithUV(
                    (double)(i + 0), (double)((float)(j + 1) + 0.021875F), (double)k + 0.015625, (double)iicon1.getMaxU(), (double)iicon1.getMinV()
                );
                tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)k + 0.015625, (double)iicon1.getMinU(), (double)iicon1.getMinV());
                tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
                tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)k + 0.015625, (double)iicon3.getMinU(), (double)iicon3.getMaxV());
                tessellator.addVertexWithUV(
                    (double)(i + 1), (double)((float)(j + 1) + 0.021875F), (double)k + 0.015625, (double)iicon3.getMaxU(), (double)iicon3.getMaxV()
                );
                tessellator.addVertexWithUV(
                    (double)(i + 0), (double)((float)(j + 1) + 0.021875F), (double)k + 0.015625, (double)iicon3.getMaxU(), (double)iicon3.getMinV()
                );
                tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)k + 0.015625, (double)iicon3.getMinU(), (double)iicon3.getMinV());
            }

            if (this.blockAccess.getBlock(i, j, k + 1).isBlockNormalCube() && this.blockAccess.getBlock(i, j + 1, k + 1) == Blocks.redstone_wire) {
                tessellator.setColorOpaque_F(f1, f2, f3);
                tessellator.addVertexWithUV(
                    (double)(i + 1), (double)((float)(j + 1) + 0.021875F), (double)(k + 1) - 0.015625, (double)iicon1.getMaxU(), (double)iicon1.getMinV()
                );
                tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)(k + 1) - 0.015625, (double)iicon1.getMinU(), (double)iicon1.getMinV());
                tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)(k + 1) - 0.015625, (double)iicon1.getMinU(), (double)iicon1.getMaxV());
                tessellator.addVertexWithUV(
                    (double)(i + 0), (double)((float)(j + 1) + 0.021875F), (double)(k + 1) - 0.015625, (double)iicon1.getMaxU(), (double)iicon1.getMaxV()
                );
                tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
                tessellator.addVertexWithUV(
                    (double)(i + 1), (double)((float)(j + 1) + 0.021875F), (double)(k + 1) - 0.015625, (double)iicon3.getMaxU(), (double)iicon3.getMinV()
                );
                tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)(k + 1) - 0.015625, (double)iicon3.getMinU(), (double)iicon3.getMinV());
                tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)(k + 1) - 0.015625, (double)iicon3.getMinU(), (double)iicon3.getMaxV());
                tessellator.addVertexWithUV(
                    (double)(i + 0), (double)((float)(j + 1) + 0.021875F), (double)(k + 1) - 0.015625, (double)iicon3.getMaxU(), (double)iicon3.getMaxV()
                );
            }
        }

        return true;
    }

    public boolean renderBlockMinecartTrack(BlockRailBase blockrailbase, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        IIcon iicon = this.getBlockIconFromSideAndMetadata(blockrailbase, 0, l);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        if (blockrailbase.isPowered()) {
            l &= 7;
        }

        tessellator.setBrightness(blockrailbase.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMinV();
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getMaxV();
        double d4 = 0.0625;
        double d5 = (double)(i + 1);
        double d6 = (double)(i + 1);
        double d7 = (double)(i + 0);
        double d8 = (double)(i + 0);
        double d9 = (double)(k + 0);
        double d10 = (double)(k + 1);
        double d11 = (double)(k + 1);
        double d12 = (double)(k + 0);
        double d13 = (double)j + d4;
        double d14 = (double)j + d4;
        double d15 = (double)j + d4;
        double d16 = (double)j + d4;
        if (l == 1 || l == 2 || l == 3 || l == 7) {
            d5 = d8 = (double)(i + 1);
            d6 = d7 = (double)(i + 0);
            d9 = d10 = (double)(k + 1);
            d11 = d12 = (double)(k + 0);
        } else if (l == 8) {
            d5 = d6 = (double)(i + 0);
            d7 = d8 = (double)(i + 1);
            d9 = d12 = (double)(k + 1);
            d10 = d11 = (double)(k + 0);
        } else if (l == 9) {
            d5 = d8 = (double)(i + 0);
            d6 = d7 = (double)(i + 1);
            d9 = d10 = (double)(k + 0);
            d11 = d12 = (double)(k + 1);
        }

        if (l == 2 || l == 4) {
            d13++;
            d16++;
        } else if (l == 3 || l == 5) {
            d14++;
            d15++;
        }

        tessellator.addVertexWithUV(d5, d13, d9, d2, d1);
        tessellator.addVertexWithUV(d6, d14, d10, d2, d3);
        tessellator.addVertexWithUV(d7, d15, d11, d0, d3);
        tessellator.addVertexWithUV(d8, d16, d12, d0, d1);
        tessellator.addVertexWithUV(d8, d16, d12, d0, d1);
        tessellator.addVertexWithUV(d7, d15, d11, d0, d3);
        tessellator.addVertexWithUV(d6, d14, d10, d2, d3);
        tessellator.addVertexWithUV(d5, d13, d9, d2, d1);
        return true;
    }

    public boolean renderBlockLadder(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSide(block, 0);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMinV();
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getMaxV();
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        double d4 = 0.0;
        double d5 = 0.05F;
        if (l == 5) {
            tessellator.addVertexWithUV((double)i + d5, (double)(j + 1) + d4, (double)(k + 1) + d4, d0, d1);
            tessellator.addVertexWithUV((double)i + d5, (double)(j + 0) - d4, (double)(k + 1) + d4, d0, d3);
            tessellator.addVertexWithUV((double)i + d5, (double)(j + 0) - d4, (double)(k + 0) - d4, d2, d3);
            tessellator.addVertexWithUV((double)i + d5, (double)(j + 1) + d4, (double)(k + 0) - d4, d2, d1);
        }

        if (l == 4) {
            tessellator.addVertexWithUV((double)(i + 1) - d5, (double)(j + 0) - d4, (double)(k + 1) + d4, d2, d3);
            tessellator.addVertexWithUV((double)(i + 1) - d5, (double)(j + 1) + d4, (double)(k + 1) + d4, d2, d1);
            tessellator.addVertexWithUV((double)(i + 1) - d5, (double)(j + 1) + d4, (double)(k + 0) - d4, d0, d1);
            tessellator.addVertexWithUV((double)(i + 1) - d5, (double)(j + 0) - d4, (double)(k + 0) - d4, d0, d3);
        }

        if (l == 3) {
            tessellator.addVertexWithUV((double)(i + 1) + d4, (double)(j + 0) - d4, (double)k + d5, d2, d3);
            tessellator.addVertexWithUV((double)(i + 1) + d4, (double)(j + 1) + d4, (double)k + d5, d2, d1);
            tessellator.addVertexWithUV((double)(i + 0) - d4, (double)(j + 1) + d4, (double)k + d5, d0, d1);
            tessellator.addVertexWithUV((double)(i + 0) - d4, (double)(j + 0) - d4, (double)k + d5, d0, d3);
        }

        if (l == 2) {
            tessellator.addVertexWithUV((double)(i + 1) + d4, (double)(j + 1) + d4, (double)(k + 1) - d5, d0, d1);
            tessellator.addVertexWithUV((double)(i + 1) + d4, (double)(j + 0) - d4, (double)(k + 1) - d5, d0, d3);
            tessellator.addVertexWithUV((double)(i + 0) - d4, (double)(j + 0) - d4, (double)(k + 1) - d5, d2, d3);
            tessellator.addVertexWithUV((double)(i + 0) - d4, (double)(j + 1) + d4, (double)(k + 1) - d5, d2, d1);
        }

        return true;
    }

    public boolean renderBlockVine(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSide(block, 0);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = block.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        tessellator.setColorOpaque_F(f, f1, f2);
        double d3 = (double)iicon.getMinU();
        double d4 = (double)iicon.getMinV();
        double d0 = (double)iicon.getMaxU();
        double d1 = (double)iicon.getMaxV();
        double d2 = 0.05F;
        int i1 = this.blockAccess.getBlockMetadata(i, j, k);
        if ((i1 & 2) != 0) {
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 1), (double)(k + 1), d3, d4);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 0), (double)(k + 1), d3, d1);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 0), (double)(k + 0), d0, d1);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 1), (double)(k + 0), d0, d4);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 1), (double)(k + 0), d0, d4);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 0), (double)(k + 0), d0, d1);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 0), (double)(k + 1), d3, d1);
            tessellator.addVertexWithUV((double)i + d2, (double)(j + 1), (double)(k + 1), d3, d4);
        }

        if ((i1 & 8) != 0) {
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 0), (double)(k + 1), d0, d1);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 1), (double)(k + 1), d0, d4);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 1), (double)(k + 0), d3, d4);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 0), (double)(k + 0), d3, d1);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 0), (double)(k + 0), d3, d1);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 1), (double)(k + 0), d3, d4);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 1), (double)(k + 1), d0, d4);
            tessellator.addVertexWithUV((double)(i + 1) - d2, (double)(j + 0), (double)(k + 1), d0, d1);
        }

        if ((i1 & 4) != 0) {
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)k + d2, d0, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 1), (double)k + d2, d0, d4);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 1), (double)k + d2, d3, d4);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)k + d2, d3, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)k + d2, d3, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 1), (double)k + d2, d3, d4);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 1), (double)k + d2, d0, d4);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)k + d2, d0, d1);
        }

        if ((i1 & 1) != 0) {
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 1), (double)(k + 1) - d2, d3, d4);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)(k + 1) - d2, d3, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)(k + 1) - d2, d0, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 1), (double)(k + 1) - d2, d0, d4);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 1), (double)(k + 1) - d2, d0, d4);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 0), (double)(k + 1) - d2, d0, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 0), (double)(k + 1) - d2, d3, d1);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 1), (double)(k + 1) - d2, d3, d4);
        }

        if (this.blockAccess.getBlock(i, j + 1, k).isBlockNormalCube()) {
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 1) - d2, (double)(k + 0), d3, d4);
            tessellator.addVertexWithUV((double)(i + 1), (double)(j + 1) - d2, (double)(k + 1), d3, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 1) - d2, (double)(k + 1), d0, d1);
            tessellator.addVertexWithUV((double)(i + 0), (double)(j + 1) - d2, (double)(k + 0), d0, d4);
        }

        return true;
    }

    public boolean renderBlockStainedGlassPane(Block block, int i, int j, int k) {
        int l = this.blockAccess.getHeight();
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int i1 = block.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(i1 >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(i1 >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(i1 & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        boolean flag5 = block instanceof BlockStainedGlassPane;
        IIcon iicon;
        IIcon iicon1;
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
            iicon1 = this.overrideBlockTexture;
        } else {
            int j1 = this.blockAccess.getBlockMetadata(i, j, k);
            iicon = this.getBlockIconFromSideAndMetadata(block, 0, j1);
            iicon1 = flag5 ? ((BlockStainedGlassPane)block).func_150104_b(j1) : ((BlockPane)block).func_150097_e();
        }

        double d22 = (double)iicon.getMinU();
        double d0 = (double)iicon.getInterpolatedU(7.0);
        double d1 = (double)iicon.getInterpolatedU(9.0);
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getMinV();
        double d4 = (double)iicon.getMaxV();
        double d5 = (double)iicon1.getInterpolatedU(7.0);
        double d6 = (double)iicon1.getInterpolatedU(9.0);
        double d7 = (double)iicon1.getMinV();
        double d8 = (double)iicon1.getMaxV();
        double d9 = (double)iicon1.getInterpolatedV(7.0);
        double d10 = (double)iicon1.getInterpolatedV(9.0);
        double d11 = (double)i;
        double d12 = (double)(i + 1);
        double d13 = (double)k;
        double d14 = (double)(k + 1);
        double d15 = (double)i + 0.5 - 0.0625;
        double d16 = (double)i + 0.5 + 0.0625;
        double d17 = (double)k + 0.5 - 0.0625;
        double d18 = (double)k + 0.5 + 0.0625;
        boolean flag = flag5
            ? ((BlockStainedGlassPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i, j, k - 1))
            : ((BlockPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i, j, k - 1));
        boolean flag1 = flag5
            ? ((BlockStainedGlassPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i, j, k + 1))
            : ((BlockPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i, j, k + 1));
        boolean flag2 = flag5
            ? ((BlockStainedGlassPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i - 1, j, k))
            : ((BlockPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i - 1, j, k));
        boolean flag3 = flag5
            ? ((BlockStainedGlassPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i + 1, j, k))
            : ((BlockPane)block).canPaneConnectToBlock(this.blockAccess.getBlock(i + 1, j, k));
        double d19 = 0.001;
        double d20 = 0.999;
        double d21 = 0.001;
        boolean flag4 = !flag && !flag1 && !flag2 && !flag3;
        if (!flag2 && !flag4) {
            if (!flag && !flag1) {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d1, d3);
            }
        } else if (flag2 && flag3) {
            if (!flag) {
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d2, d3);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d2, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d22, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d22, d3);
            } else {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d22, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d22, d3);
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d2, d3);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d2, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d1, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d1, d3);
            }

            if (!flag1) {
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d22, d3);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d22, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d2, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d2, d3);
            } else {
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d22, d3);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d22, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d0, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d2, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d2, d3);
            }

            tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d6, d7);
            tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d6, d8);
            tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d5, d8);
            tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d5, d7);
            tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d5, d8);
            tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d5, d7);
            tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d6, d7);
            tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d6, d8);
        } else {
            if (!flag && !flag4) {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d1, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d1, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d22, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d22, d3);
            } else {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d22, d4);
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d22, d3);
            }

            if (!flag1 && !flag4) {
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d22, d3);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d22, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
            } else {
                tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d22, d3);
                tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d22, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d0, d3);
            }

            tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d6, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d6, d9);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d5, d9);
            tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d5, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d5, d9);
            tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d5, d7);
            tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d6, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d6, d9);
        }

        if ((flag3 || flag4) && !flag2) {
            if (!flag1 && !flag4) {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d0, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d0, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d2, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d2, d3);
            } else {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d2, d4);
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d2, d3);
            }

            if (!flag && !flag4) {
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d2, d3);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d2, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
            } else {
                tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d2, d3);
                tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d2, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d1, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d1, d3);
            }

            tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d6, d10);
            tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d6, d7);
            tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d5, d7);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d5, d10);
            tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d5, d8);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d5, d10);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d6, d10);
            tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d6, d8);
        } else if (!flag3 && !flag && !flag1) {
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d0, d3);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d0, d4);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d1, d4);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d1, d3);
        }

        if (!flag && !flag4) {
            if (!flag3 && !flag2) {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d1, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d1, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
            }
        } else if (flag && flag1) {
            if (!flag2) {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d22, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d2, d3);
            } else {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d22, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d1, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d2, d3);
            }

            if (!flag3) {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d2, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d22, d3);
            } else {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d22, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d2, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
            }

            tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d6, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d5, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d5, d8);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d6, d8);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d5, d7);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d6, d7);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d6, d8);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d5, d8);
        } else {
            if (!flag2 && !flag4) {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d22, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d1, d3);
            } else {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d22, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
            }

            if (!flag3 && !flag4) {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d22, d3);
            } else {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d22, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d22, d3);
            }

            tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d6, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d5, d7);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d5, d9);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d6, d9);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d5, d7);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d6, d7);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d6, d9);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d5, d9);
        }

        if ((flag1 || flag4) && !flag) {
            if (!flag2 && !flag4) {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d0, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d2, d3);
            } else {
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d1, d3);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d2, d3);
            }

            if (!flag3 && !flag4) {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d2, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d0, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d0, d3);
            } else {
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d2, d3);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d2, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
                tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
            }

            tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d6, d10);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d5, d10);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d5, d8);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d6, d8);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d5, d10);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d6, d10);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d6, d8);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d5, d8);
        } else if (!flag1 && !flag3 && !flag2) {
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d0, d3);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d0, d4);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d1, d4);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d1, d3);
        }

        tessellator.addVertexWithUV(d16, (double)j + 0.999, d17, d6, d9);
        tessellator.addVertexWithUV(d15, (double)j + 0.999, d17, d5, d9);
        tessellator.addVertexWithUV(d15, (double)j + 0.999, d18, d5, d10);
        tessellator.addVertexWithUV(d16, (double)j + 0.999, d18, d6, d10);
        tessellator.addVertexWithUV(d15, (double)j + 0.001, d17, d5, d9);
        tessellator.addVertexWithUV(d16, (double)j + 0.001, d17, d6, d9);
        tessellator.addVertexWithUV(d16, (double)j + 0.001, d18, d6, d10);
        tessellator.addVertexWithUV(d15, (double)j + 0.001, d18, d5, d10);
        if (flag4) {
            tessellator.addVertexWithUV(d11, (double)j + 0.999, d17, d0, d3);
            tessellator.addVertexWithUV(d11, (double)j + 0.001, d17, d0, d4);
            tessellator.addVertexWithUV(d11, (double)j + 0.001, d18, d1, d4);
            tessellator.addVertexWithUV(d11, (double)j + 0.999, d18, d1, d3);
            tessellator.addVertexWithUV(d12, (double)j + 0.999, d18, d0, d3);
            tessellator.addVertexWithUV(d12, (double)j + 0.001, d18, d0, d4);
            tessellator.addVertexWithUV(d12, (double)j + 0.001, d17, d1, d4);
            tessellator.addVertexWithUV(d12, (double)j + 0.999, d17, d1, d3);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d13, d1, d3);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d13, d1, d4);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d13, d0, d4);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d13, d0, d3);
            tessellator.addVertexWithUV(d15, (double)j + 0.999, d14, d0, d3);
            tessellator.addVertexWithUV(d15, (double)j + 0.001, d14, d0, d4);
            tessellator.addVertexWithUV(d16, (double)j + 0.001, d14, d1, d4);
            tessellator.addVertexWithUV(d16, (double)j + 0.999, d14, d1, d3);
        }

        return true;
    }

    public boolean renderBlockPane(BlockPane blockpane, int i, int j, int k) {
        int l = this.blockAccess.getHeight();
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockpane.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int i1 = blockpane.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(i1 >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(i1 >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(i1 & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        IIcon iicon;
        IIcon iicon1;
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
            iicon1 = this.overrideBlockTexture;
        } else {
            int j1 = this.blockAccess.getBlockMetadata(i, j, k);
            iicon = this.getBlockIconFromSideAndMetadata(blockpane, 0, j1);
            iicon1 = blockpane.func_150097_e();
        }

        double d21 = (double)iicon.getMinU();
        double d0 = (double)iicon.getInterpolatedU(8.0);
        double d1 = (double)iicon.getMaxU();
        double d2 = (double)iicon.getMinV();
        double d3 = (double)iicon.getMaxV();
        double d4 = (double)iicon1.getInterpolatedU(7.0);
        double d5 = (double)iicon1.getInterpolatedU(9.0);
        double d6 = (double)iicon1.getMinV();
        double d7 = (double)iicon1.getInterpolatedV(8.0);
        double d8 = (double)iicon1.getMaxV();
        double d9 = (double)i;
        double d10 = (double)i + 0.5;
        double d11 = (double)(i + 1);
        double d12 = (double)k;
        double d13 = (double)k + 0.5;
        double d14 = (double)(k + 1);
        double d15 = (double)i + 0.5 - 0.0625;
        double d16 = (double)i + 0.5 + 0.0625;
        double d17 = (double)k + 0.5 - 0.0625;
        double d18 = (double)k + 0.5 + 0.0625;
        boolean flag = blockpane.canPaneConnectToBlock(this.blockAccess.getBlock(i, j, k - 1));
        boolean flag1 = blockpane.canPaneConnectToBlock(this.blockAccess.getBlock(i, j, k + 1));
        boolean flag2 = blockpane.canPaneConnectToBlock(this.blockAccess.getBlock(i - 1, j, k));
        boolean flag3 = blockpane.canPaneConnectToBlock(this.blockAccess.getBlock(i + 1, j, k));
        boolean flag4 = blockpane.shouldSideBeRendered(this.blockAccess, i, j + 1, k, 1);
        boolean flag5 = blockpane.shouldSideBeRendered(this.blockAccess, i, j - 1, k, 0);
        double d19 = 0.01;
        double d20 = 0.005;
        if ((!flag2 || !flag3) && (flag2 || flag3 || flag || flag1)) {
            if (flag2 && !flag3) {
                tessellator.addVertexWithUV(d9, (double)(j + 1), d13, d21, d2);
                tessellator.addVertexWithUV(d9, (double)(j + 0), d13, d21, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d0, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d0, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d21, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d21, d3);
                tessellator.addVertexWithUV(d9, (double)(j + 0), d13, d0, d3);
                tessellator.addVertexWithUV(d9, (double)(j + 1), d13, d0, d2);
                if (!flag1 && !flag) {
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d18, d4, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d18, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d17, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d17, d5, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d17, d4, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d17, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d18, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d18, d5, d6);
                }

                if (flag4 || j < l - 1 && this.blockAccess.isAirBlock(i - 1, j + 1, k)) {
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d7);
                }

                if (flag5 || j > 1 && this.blockAccess.isAirBlock(i - 1, j - 1, k)) {
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d7);
                }
            } else if (!flag2 && flag3) {
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d0, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d0, d3);
                tessellator.addVertexWithUV(d11, (double)(j + 0), d13, d1, d3);
                tessellator.addVertexWithUV(d11, (double)(j + 1), d13, d1, d2);
                tessellator.addVertexWithUV(d11, (double)(j + 1), d13, d0, d2);
                tessellator.addVertexWithUV(d11, (double)(j + 0), d13, d0, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d1, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d1, d2);
                if (!flag1 && !flag) {
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d17, d4, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d17, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d18, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d18, d5, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d18, d4, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d18, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 0), d17, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1), d17, d5, d6);
                }

                if (flag4 || j < l - 1 && this.blockAccess.isAirBlock(i + 1, j + 1, k)) {
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d6);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d17, d4, d6);
                }

                if (flag5 || j > 1 && this.blockAccess.isAirBlock(i + 1, j - 1, k)) {
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d6);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d17, d4, d6);
                }
            }
        } else {
            tessellator.addVertexWithUV(d9, (double)(j + 1), d13, d21, d2);
            tessellator.addVertexWithUV(d9, (double)(j + 0), d13, d21, d3);
            tessellator.addVertexWithUV(d11, (double)(j + 0), d13, d1, d3);
            tessellator.addVertexWithUV(d11, (double)(j + 1), d13, d1, d2);
            tessellator.addVertexWithUV(d11, (double)(j + 1), d13, d21, d2);
            tessellator.addVertexWithUV(d11, (double)(j + 0), d13, d21, d3);
            tessellator.addVertexWithUV(d9, (double)(j + 0), d13, d1, d3);
            tessellator.addVertexWithUV(d9, (double)(j + 1), d13, d1, d2);
            if (flag4) {
                tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d18, d5, d8);
                tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d18, d5, d6);
                tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d17, d4, d6);
                tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d17, d4, d8);
                tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d18, d5, d8);
                tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d18, d5, d6);
                tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d17, d4, d6);
                tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d17, d4, d8);
            } else {
                if (j < l - 1 && this.blockAccess.isAirBlock(i - 1, j + 1, k)) {
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d9, (double)(j + 1) + 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d7);
                }

                if (j < l - 1 && this.blockAccess.isAirBlock(i + 1, j + 1, k)) {
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d6);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)(j + 1) + 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d11, (double)(j + 1) + 0.01, d17, d4, d6);
                }
            }

            if (flag5) {
                tessellator.addVertexWithUV(d9, (double)j - 0.01, d18, d5, d8);
                tessellator.addVertexWithUV(d11, (double)j - 0.01, d18, d5, d6);
                tessellator.addVertexWithUV(d11, (double)j - 0.01, d17, d4, d6);
                tessellator.addVertexWithUV(d9, (double)j - 0.01, d17, d4, d8);
                tessellator.addVertexWithUV(d11, (double)j - 0.01, d18, d5, d8);
                tessellator.addVertexWithUV(d9, (double)j - 0.01, d18, d5, d6);
                tessellator.addVertexWithUV(d9, (double)j - 0.01, d17, d4, d6);
                tessellator.addVertexWithUV(d11, (double)j - 0.01, d17, d4, d8);
            } else {
                if (j > 1 && this.blockAccess.isAirBlock(i - 1, j - 1, k)) {
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d18, d5, d8);
                    tessellator.addVertexWithUV(d9, (double)j - 0.01, d17, d4, d8);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d7);
                }

                if (j > 1 && this.blockAccess.isAirBlock(i + 1, j - 1, k)) {
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d6);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d18, d5, d6);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d18, d5, d7);
                    tessellator.addVertexWithUV(d10, (double)j - 0.01, d17, d4, d7);
                    tessellator.addVertexWithUV(d11, (double)j - 0.01, d17, d4, d6);
                }
            }
        }

        if ((!flag || !flag1) && (flag2 || flag3 || flag || flag1)) {
            if (flag && !flag1) {
                tessellator.addVertexWithUV(d10, (double)(j + 1), d12, d21, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d12, d21, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d0, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d0, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d21, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d21, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d12, d0, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d12, d0, d2);
                if (!flag3 && !flag2) {
                    tessellator.addVertexWithUV(d15, (double)(j + 1), d13, d4, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 0), d13, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 0), d13, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1), d13, d5, d6);
                    tessellator.addVertexWithUV(d16, (double)(j + 1), d13, d4, d6);
                    tessellator.addVertexWithUV(d16, (double)(j + 0), d13, d4, d8);
                    tessellator.addVertexWithUV(d15, (double)(j + 0), d13, d5, d8);
                    tessellator.addVertexWithUV(d15, (double)(j + 1), d13, d5, d6);
                }

                if (flag4 || j < l - 1 && this.blockAccess.isAirBlock(i, j + 1, k - 1)) {
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d12, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d12, d4, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d12, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d12, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d4, d6);
                }

                if (flag5 || j > 1 && this.blockAccess.isAirBlock(i, j - 1, k - 1)) {
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d12, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d12, d4, d6);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d12, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d12, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d4, d6);
                }
            } else if (!flag && flag1) {
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d0, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d0, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d14, d1, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d14, d1, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d14, d0, d2);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d14, d0, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 0), d13, d1, d3);
                tessellator.addVertexWithUV(d10, (double)(j + 1), d13, d1, d2);
                if (!flag3 && !flag2) {
                    tessellator.addVertexWithUV(d16, (double)(j + 1), d13, d4, d6);
                    tessellator.addVertexWithUV(d16, (double)(j + 0), d13, d4, d8);
                    tessellator.addVertexWithUV(d15, (double)(j + 0), d13, d5, d8);
                    tessellator.addVertexWithUV(d15, (double)(j + 1), d13, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1), d13, d4, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 0), d13, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 0), d13, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1), d13, d5, d6);
                }

                if (flag4 || j < l - 1 && this.blockAccess.isAirBlock(i, j + 1, k + 1)) {
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d14, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d14, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d14, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d14, d5, d7);
                }

                if (flag5 || j > 1 && this.blockAccess.isAirBlock(i, j - 1, k + 1)) {
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d14, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d14, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d14, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d14, d5, d7);
                }
            }
        } else {
            tessellator.addVertexWithUV(d10, (double)(j + 1), d14, d21, d2);
            tessellator.addVertexWithUV(d10, (double)(j + 0), d14, d21, d3);
            tessellator.addVertexWithUV(d10, (double)(j + 0), d12, d1, d3);
            tessellator.addVertexWithUV(d10, (double)(j + 1), d12, d1, d2);
            tessellator.addVertexWithUV(d10, (double)(j + 1), d12, d21, d2);
            tessellator.addVertexWithUV(d10, (double)(j + 0), d12, d21, d3);
            tessellator.addVertexWithUV(d10, (double)(j + 0), d14, d1, d3);
            tessellator.addVertexWithUV(d10, (double)(j + 1), d14, d1, d2);
            if (flag4) {
                tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d14, d5, d8);
                tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d12, d5, d6);
                tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d12, d4, d6);
                tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d14, d4, d8);
                tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d12, d5, d8);
                tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d14, d5, d6);
                tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d14, d4, d6);
                tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d12, d4, d8);
            } else {
                if (j < l - 1 && this.blockAccess.isAirBlock(i, j + 1, k - 1)) {
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d12, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d12, d4, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d12, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d12, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d4, d6);
                }

                if (j < l - 1 && this.blockAccess.isAirBlock(i, j + 1, k + 1)) {
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d14, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d14, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d14, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)(j + 1) + 0.005, d13, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d13, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)(j + 1) + 0.005, d14, d5, d7);
                }
            }

            if (flag5) {
                tessellator.addVertexWithUV(d16, (double)j - 0.005, d14, d5, d8);
                tessellator.addVertexWithUV(d16, (double)j - 0.005, d12, d5, d6);
                tessellator.addVertexWithUV(d15, (double)j - 0.005, d12, d4, d6);
                tessellator.addVertexWithUV(d15, (double)j - 0.005, d14, d4, d8);
                tessellator.addVertexWithUV(d16, (double)j - 0.005, d12, d5, d8);
                tessellator.addVertexWithUV(d16, (double)j - 0.005, d14, d5, d6);
                tessellator.addVertexWithUV(d15, (double)j - 0.005, d14, d4, d6);
                tessellator.addVertexWithUV(d15, (double)j - 0.005, d12, d4, d8);
            } else {
                if (j > 1 && this.blockAccess.isAirBlock(i, j - 1, k - 1)) {
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d12, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d12, d4, d6);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d5, d6);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d12, d5, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d12, d4, d7);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d4, d6);
                }

                if (j > 1 && this.blockAccess.isAirBlock(i, j - 1, k + 1)) {
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d14, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d14, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d5, d7);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d14, d4, d7);
                    tessellator.addVertexWithUV(d15, (double)j - 0.005, d13, d4, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d13, d5, d8);
                    tessellator.addVertexWithUV(d16, (double)j - 0.005, d14, d5, d7);
                }
            }
        }

        return true;
    }

    public boolean renderCrossedSquares(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = block.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        double d1 = (double)i;
        double d2 = (double)j;
        double d0 = (double)k;
        if (block == Blocks.tallgrass) {
            long i1 = (long)(i * 3129871) ^ (long)k * 116129781L ^ (long)j;
            i1 = i1 * i1 * 42317861L + i1 * 11L;
            d1 += ((double)((float)(i1 >> 16 & 15L) / 15.0F) - 0.5) * 0.5;
            d2 += ((double)((float)(i1 >> 20 & 15L) / 15.0F) - 1.0) * 0.2;
            d0 += ((double)((float)(i1 >> 24 & 15L) / 15.0F) - 0.5) * 0.5;
        } else if (block == Blocks.red_flower || block == Blocks.yellow_flower) {
            long j1 = (long)(i * 3129871) ^ (long)k * 116129781L ^ (long)j;
            j1 = j1 * j1 * 42317861L + j1 * 11L;
            d1 += ((double)((float)(j1 >> 16 & 15L) / 15.0F) - 0.5) * 0.3;
            d0 += ((double)((float)(j1 >> 24 & 15L) / 15.0F) - 0.5) * 0.3;
        }

        IIcon iicon = this.getBlockIconFromSideAndMetadata(block, 0, this.blockAccess.getBlockMetadata(i, j, k));
        this.drawCrossedSquares(iicon, d1, d2, d0, 1.0F);
        return true;
    }

    public boolean renderBlockDoublePlant(BlockDoublePlant blockdoubleplant, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockdoubleplant.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = blockdoubleplant.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        long k1 = (long)(i * 3129871) ^ (long)k * 116129781L;
        k1 = k1 * k1 * 42317861L + k1 * 11L;
        double d19 = (double)i;
        double d0 = (double)j;
        double d1 = (double)k;
        d19 += ((double)((float)(k1 >> 16 & 15L) / 15.0F) - 0.5) * 0.3;
        d1 += ((double)((float)(k1 >> 24 & 15L) / 15.0F) - 0.5) * 0.3;
        int i1 = this.blockAccess.getBlockMetadata(i, j, k);
        int j1 = 0;
        boolean flag = BlockDoublePlant.func_149887_c(i1);
        if (flag) {
            if (this.blockAccess.getBlock(i, j - 1, k) != blockdoubleplant) {
                return false;
            }

            j1 = BlockDoublePlant.func_149890_d(this.blockAccess.getBlockMetadata(i, j - 1, k));
        } else {
            j1 = BlockDoublePlant.func_149890_d(i1);
        }

        IIcon iicon = blockdoubleplant.func_149888_a(flag, j1);
        this.drawCrossedSquares(iicon, d19, d0, d1, 1.0F);
        if (flag && j1 == 0) {
            IIcon iicon1 = blockdoubleplant.mcc$sunflowerIcon(0);
            double d2 = Math.cos((double)k1 * 0.8) * Math.PI * 0.1;
            double d3 = Math.cos(d2);
            double d4 = Math.sin(d2);
            double d5 = (double)iicon1.getMinU();
            double d6 = (double)iicon1.getMinV();
            double d7 = (double)iicon1.getMaxU();
            double d8 = (double)iicon1.getMaxV();
            double d9 = 0.3;
            double d10 = -0.05;
            double d11 = 0.5 + 0.3 * d3 - 0.5 * d4;
            double d12 = 0.5 + 0.5 * d3 + 0.3 * d4;
            double d13 = 0.5 + 0.3 * d3 + 0.5 * d4;
            double d14 = 0.5 + -0.5 * d3 + 0.3 * d4;
            double d15 = 0.5 + -0.05 * d3 + 0.5 * d4;
            double d16 = 0.5 + -0.5 * d3 + -0.05 * d4;
            double d17 = 0.5 + -0.05 * d3 - 0.5 * d4;
            double d18 = 0.5 + 0.5 * d3 + -0.05 * d4;
            tessellator.addVertexWithUV(d19 + d15, d0 + 1.0, d1 + d16, d5, d8);
            tessellator.addVertexWithUV(d19 + d17, d0 + 1.0, d1 + d18, d7, d8);
            tessellator.addVertexWithUV(d19 + d11, d0 + 0.0, d1 + d12, d7, d6);
            tessellator.addVertexWithUV(d19 + d13, d0 + 0.0, d1 + d14, d5, d6);
            IIcon iicon2 = blockdoubleplant.mcc$sunflowerIcon(1);
            d5 = (double)iicon2.getMinU();
            d6 = (double)iicon2.getMinV();
            d7 = (double)iicon2.getMaxU();
            d8 = (double)iicon2.getMaxV();
            tessellator.addVertexWithUV(d19 + d17, d0 + 1.0, d1 + d18, d5, d8);
            tessellator.addVertexWithUV(d19 + d15, d0 + 1.0, d1 + d16, d7, d8);
            tessellator.addVertexWithUV(d19 + d13, d0 + 0.0, d1 + d14, d7, d6);
            tessellator.addVertexWithUV(d19 + d11, d0 + 0.0, d1 + d12, d5, d6);
        }

        return true;
    }

    public boolean renderBlockStem(Block block, int i, int j, int k) {
        BlockStem blockstem = (BlockStem)block;
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockstem.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = blockstem.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        blockstem.setBlockBoundsBasedOnState(this.blockAccess, i, j, k);
        int i1 = blockstem.getState(this.blockAccess, i, j, k);
        if (i1 < 0) {
            this.renderBlockStemSmall(blockstem, this.blockAccess.getBlockMetadata(i, j, k), this.renderMaxY, (double)i, (double)((float)j - 0.0625F), (double)k);
        } else {
            this.renderBlockStemSmall(blockstem, this.blockAccess.getBlockMetadata(i, j, k), 0.5, (double)i, (double)((float)j - 0.0625F), (double)k);
            this.renderBlockStemBig(blockstem, this.blockAccess.getBlockMetadata(i, j, k), i1, this.renderMaxY, (double)i, (double)((float)j - 0.0625F), (double)k);
        }

        return true;
    }

    public boolean renderBlockCrops(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        this.renderBlockCropsImpl(block, this.blockAccess.getBlockMetadata(i, j, k), (double)i, (double)((float)j - 0.0625F), (double)k);
        return true;
    }

    public void renderTorchAtAngle(Block block, double d0, double d1, double d2, double d3, double d4, int i) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSideAndMetadata(block, 0, i);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d5 = (double)iicon.getMinU();
        double d6 = (double)iicon.getMinV();
        double d7 = (double)iicon.getMaxU();
        double d8 = (double)iicon.getMaxV();
        double d9 = (double)iicon.getInterpolatedU(7.0);
        double d10 = (double)iicon.getInterpolatedV(6.0);
        double d11 = (double)iicon.getInterpolatedU(9.0);
        double d12 = (double)iicon.getInterpolatedV(8.0);
        double d13 = (double)iicon.getInterpolatedU(7.0);
        double d14 = (double)iicon.getInterpolatedV(13.0);
        double d15 = (double)iicon.getInterpolatedU(9.0);
        double d16 = (double)iicon.getInterpolatedV(15.0);
        d0 += 0.5;
        d2 += 0.5;
        double d17 = d0 - 0.5;
        double d18 = d0 + 0.5;
        double d19 = d2 - 0.5;
        double d20 = d2 + 0.5;
        double d21 = 0.0625;
        double d22 = 0.625;
        tessellator.addVertexWithUV(d0 + d3 * (1.0 - d22) - d21, d1 + d22, d2 + d4 * (1.0 - d22) - d21, d9, d10);
        tessellator.addVertexWithUV(d0 + d3 * (1.0 - d22) - d21, d1 + d22, d2 + d4 * (1.0 - d22) + d21, d9, d12);
        tessellator.addVertexWithUV(d0 + d3 * (1.0 - d22) + d21, d1 + d22, d2 + d4 * (1.0 - d22) + d21, d11, d12);
        tessellator.addVertexWithUV(d0 + d3 * (1.0 - d22) + d21, d1 + d22, d2 + d4 * (1.0 - d22) - d21, d11, d10);
        tessellator.addVertexWithUV(d0 + d21 + d3, d1, d2 - d21 + d4, d15, d14);
        tessellator.addVertexWithUV(d0 + d21 + d3, d1, d2 + d21 + d4, d15, d16);
        tessellator.addVertexWithUV(d0 - d21 + d3, d1, d2 + d21 + d4, d13, d16);
        tessellator.addVertexWithUV(d0 - d21 + d3, d1, d2 - d21 + d4, d13, d14);
        tessellator.addVertexWithUV(d0 - d21, d1 + 1.0, d19, d5, d6);
        tessellator.addVertexWithUV(d0 - d21 + d3, d1 + 0.0, d19 + d4, d5, d8);
        tessellator.addVertexWithUV(d0 - d21 + d3, d1 + 0.0, d20 + d4, d7, d8);
        tessellator.addVertexWithUV(d0 - d21, d1 + 1.0, d20, d7, d6);
        tessellator.addVertexWithUV(d0 + d21, d1 + 1.0, d20, d5, d6);
        tessellator.addVertexWithUV(d0 + d3 + d21, d1 + 0.0, d20 + d4, d5, d8);
        tessellator.addVertexWithUV(d0 + d3 + d21, d1 + 0.0, d19 + d4, d7, d8);
        tessellator.addVertexWithUV(d0 + d21, d1 + 1.0, d19, d7, d6);
        tessellator.addVertexWithUV(d17, d1 + 1.0, d2 + d21, d5, d6);
        tessellator.addVertexWithUV(d17 + d3, d1 + 0.0, d2 + d21 + d4, d5, d8);
        tessellator.addVertexWithUV(d18 + d3, d1 + 0.0, d2 + d21 + d4, d7, d8);
        tessellator.addVertexWithUV(d18, d1 + 1.0, d2 + d21, d7, d6);
        tessellator.addVertexWithUV(d18, d1 + 1.0, d2 - d21, d5, d6);
        tessellator.addVertexWithUV(d18 + d3, d1 + 0.0, d2 - d21 + d4, d5, d8);
        tessellator.addVertexWithUV(d17 + d3, d1 + 0.0, d2 - d21 + d4, d7, d8);
        tessellator.addVertexWithUV(d17, d1 + 1.0, d2 - d21, d7, d6);
    }

    public void renderBlockStemSmall(Block block, int i, double d0, double d1, double d2, double d3) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSideAndMetadata(block, 0, i);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d4 = (double)iicon.getMinU();
        double d5 = (double)iicon.getMinV();
        double d6 = (double)iicon.getMaxU();
        double d7 = (double)iicon.getInterpolatedV(d0 * 16.0);
        double d8 = d1 + 0.5 - 0.45F;
        double d9 = d1 + 0.5 + 0.45F;
        double d10 = d3 + 0.5 - 0.45F;
        double d11 = d3 + 0.5 + 0.45F;
        tessellator.addVertexWithUV(d8, d2 + d0, d10, d4, d5);
        tessellator.addVertexWithUV(d8, d2 + 0.0, d10, d4, d7);
        tessellator.addVertexWithUV(d9, d2 + 0.0, d11, d6, d7);
        tessellator.addVertexWithUV(d9, d2 + d0, d11, d6, d5);
        tessellator.addVertexWithUV(d9, d2 + d0, d11, d6, d5);
        tessellator.addVertexWithUV(d9, d2 + 0.0, d11, d6, d7);
        tessellator.addVertexWithUV(d8, d2 + 0.0, d10, d4, d7);
        tessellator.addVertexWithUV(d8, d2 + d0, d10, d4, d5);
        tessellator.addVertexWithUV(d8, d2 + d0, d11, d4, d5);
        tessellator.addVertexWithUV(d8, d2 + 0.0, d11, d4, d7);
        tessellator.addVertexWithUV(d9, d2 + 0.0, d10, d6, d7);
        tessellator.addVertexWithUV(d9, d2 + d0, d10, d6, d5);
        tessellator.addVertexWithUV(d9, d2 + d0, d10, d6, d5);
        tessellator.addVertexWithUV(d9, d2 + 0.0, d10, d6, d7);
        tessellator.addVertexWithUV(d8, d2 + 0.0, d11, d4, d7);
        tessellator.addVertexWithUV(d8, d2 + d0, d11, d4, d5);
    }

    public boolean renderBlockLilyPad(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSide(block, 1);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        float f = 0.015625F;
        double d0 = (double)iicon.getMinU();
        double d1 = (double)iicon.getMinV();
        double d2 = (double)iicon.getMaxU();
        double d3 = (double)iicon.getMaxV();
        long l = (long)(i * 3129871) ^ (long)k * 116129781L ^ (long)j;
        l = l * l * 42317861L + l * 11L;
        int i1 = (int)(l >> 16 & 3L);
        tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        float f1 = (float)i + 0.5F;
        float f2 = (float)k + 0.5F;
        float f3 = (float)(i1 & 1) * 0.5F * (float)(1 - i1 / 2 % 2 * 2);
        float f4 = (float)(i1 + 1 & 1) * 0.5F * (float)(1 - (i1 + 1) / 2 % 2 * 2);
        tessellator.setColorOpaque_I(block.getBlockColor());
        tessellator.addVertexWithUV((double)(f1 + f3 - f4), (double)((float)j + f), (double)(f2 + f3 + f4), d0, d1);
        tessellator.addVertexWithUV((double)(f1 + f3 + f4), (double)((float)j + f), (double)(f2 - f3 + f4), d2, d1);
        tessellator.addVertexWithUV((double)(f1 - f3 + f4), (double)((float)j + f), (double)(f2 - f3 - f4), d2, d3);
        tessellator.addVertexWithUV((double)(f1 - f3 - f4), (double)((float)j + f), (double)(f2 + f3 - f4), d0, d3);
        tessellator.setColorOpaque_I((block.getBlockColor() & 16711422) >> 1);
        tessellator.addVertexWithUV((double)(f1 - f3 - f4), (double)((float)j + f), (double)(f2 + f3 - f4), d0, d3);
        tessellator.addVertexWithUV((double)(f1 - f3 + f4), (double)((float)j + f), (double)(f2 - f3 - f4), d2, d3);
        tessellator.addVertexWithUV((double)(f1 + f3 + f4), (double)((float)j + f), (double)(f2 - f3 + f4), d2, d1);
        tessellator.addVertexWithUV((double)(f1 + f3 - f4), (double)((float)j + f), (double)(f2 + f3 + f4), d0, d1);
        return true;
    }

    public void renderBlockStemBig(BlockStem blockstem, int j, int i, double d0, double d1, double d2, double d3) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = blockstem.getStemIcon();
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d4 = (double)iicon.getMinU();
        double d5 = (double)iicon.getMinV();
        double d6 = (double)iicon.getMaxU();
        double d7 = (double)iicon.getMaxV();
        double d8 = d1 + 0.5 - 0.5;
        double d9 = d1 + 0.5 + 0.5;
        double d10 = d3 + 0.5 - 0.5;
        double d11 = d3 + 0.5 + 0.5;
        double d12 = d1 + 0.5;
        double d13 = d3 + 0.5;
        if ((i + 1) / 2 % 2 == 1) {
            double d14 = d6;
            d6 = d4;
            d4 = d14;
        }

        if (i < 2) {
            tessellator.addVertexWithUV(d8, d2 + d0, d13, d4, d5);
            tessellator.addVertexWithUV(d8, d2 + 0.0, d13, d4, d7);
            tessellator.addVertexWithUV(d9, d2 + 0.0, d13, d6, d7);
            tessellator.addVertexWithUV(d9, d2 + d0, d13, d6, d5);
            tessellator.addVertexWithUV(d9, d2 + d0, d13, d6, d5);
            tessellator.addVertexWithUV(d9, d2 + 0.0, d13, d6, d7);
            tessellator.addVertexWithUV(d8, d2 + 0.0, d13, d4, d7);
            tessellator.addVertexWithUV(d8, d2 + d0, d13, d4, d5);
        } else {
            tessellator.addVertexWithUV(d12, d2 + d0, d11, d4, d5);
            tessellator.addVertexWithUV(d12, d2 + 0.0, d11, d4, d7);
            tessellator.addVertexWithUV(d12, d2 + 0.0, d10, d6, d7);
            tessellator.addVertexWithUV(d12, d2 + d0, d10, d6, d5);
            tessellator.addVertexWithUV(d12, d2 + d0, d10, d6, d5);
            tessellator.addVertexWithUV(d12, d2 + 0.0, d10, d6, d7);
            tessellator.addVertexWithUV(d12, d2 + 0.0, d11, d4, d7);
            tessellator.addVertexWithUV(d12, d2 + d0, d11, d4, d5);
        }
    }

    public void renderBlockCropsImpl(Block block, int i, double d0, double d1, double d2) {
        Tessellator tessellator = Tessellator.instance;
        IIcon iicon = this.getBlockIconFromSideAndMetadata(block, 0, i);
        if (this.hasOverrideBlockTexture()) {
            iicon = this.overrideBlockTexture;
        }

        double d3 = (double)iicon.getMinU();
        double d4 = (double)iicon.getMinV();
        double d5 = (double)iicon.getMaxU();
        double d6 = (double)iicon.getMaxV();
        double d7 = d0 + 0.5 - 0.25;
        double d8 = d0 + 0.5 + 0.25;
        double d9 = d2 + 0.5 - 0.5;
        double d10 = d2 + 0.5 + 0.5;
        tessellator.addVertexWithUV(d7, d1 + 1.0, d9, d3, d4);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d9, d3, d6);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d10, d5, d6);
        tessellator.addVertexWithUV(d7, d1 + 1.0, d10, d5, d4);
        tessellator.addVertexWithUV(d7, d1 + 1.0, d10, d3, d4);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d10, d3, d6);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d9, d5, d6);
        tessellator.addVertexWithUV(d7, d1 + 1.0, d9, d5, d4);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d10, d3, d4);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d10, d3, d6);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d9, d5, d6);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d9, d5, d4);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d9, d3, d4);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d9, d3, d6);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d10, d5, d6);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d10, d5, d4);
        d7 = d0 + 0.5 - 0.5;
        d8 = d0 + 0.5 + 0.5;
        d9 = d2 + 0.5 - 0.25;
        d10 = d2 + 0.5 + 0.25;
        tessellator.addVertexWithUV(d7, d1 + 1.0, d9, d3, d4);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d9, d3, d6);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d9, d5, d6);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d9, d5, d4);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d9, d3, d4);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d9, d3, d6);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d9, d5, d6);
        tessellator.addVertexWithUV(d7, d1 + 1.0, d9, d5, d4);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d10, d3, d4);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d10, d3, d6);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d10, d5, d6);
        tessellator.addVertexWithUV(d7, d1 + 1.0, d10, d5, d4);
        tessellator.addVertexWithUV(d7, d1 + 1.0, d10, d3, d4);
        tessellator.addVertexWithUV(d7, d1 + 0.0, d10, d3, d6);
        tessellator.addVertexWithUV(d8, d1 + 0.0, d10, d5, d6);
        tessellator.addVertexWithUV(d8, d1 + 1.0, d10, d5, d4);
    }

    public boolean renderBlockLiquid(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        int l = block.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        boolean flag = block.shouldSideBeRendered(this.blockAccess, i, j + 1, k, 1);
        boolean flag1 = block.shouldSideBeRendered(this.blockAccess, i, j - 1, k, 0);
        boolean[] aboolean = new boolean[]{
            block.shouldSideBeRendered(this.blockAccess, i, j, k - 1, 2),
            block.shouldSideBeRendered(this.blockAccess, i, j, k + 1, 3),
            block.shouldSideBeRendered(this.blockAccess, i - 1, j, k, 4),
            block.shouldSideBeRendered(this.blockAccess, i + 1, j, k, 5)
        };
        if (!flag && !flag1 && !aboolean[0] && !aboolean[1] && !aboolean[2] && !aboolean[3]) {
            return false;
        } else {
            boolean flag2 = false;
            float f3 = 0.5F;
            float f4 = 1.0F;
            float f5 = 0.8F;
            float f6 = 0.6F;
            double d0 = 0.0;
            double d1 = 1.0;
            Material material = block.getMaterial();
            int i1 = this.blockAccess.getBlockMetadata(i, j, k);
            double d2 = (double)this.getLiquidHeight(i, j, k, material);
            double d3 = (double)this.getLiquidHeight(i, j, k + 1, material);
            double d4 = (double)this.getLiquidHeight(i + 1, j, k + 1, material);
            double d5 = (double)this.getLiquidHeight(i + 1, j, k, material);
            double d6 = 0.001F;
            if (this.renderAllFaces || flag) {
                flag2 = true;
                IIcon iicon = this.getBlockIconFromSideAndMetadata(block, 1, i1);
                float f7 = (float)BlockLiquid.getFlowDirection(this.blockAccess, i, j, k, material);
                if (f7 > -999.0F) {
                    iicon = this.getBlockIconFromSideAndMetadata(block, 2, i1);
                }

                d2 -= d6;
                d3 -= d6;
                d4 -= d6;
                d5 -= d6;
                double d7;
                double d8;
                double d10;
                double d12;
                double d14;
                double d16;
                double d18;
                double d20;
                if (f7 < -999.0F) {
                    d7 = (double)iicon.getInterpolatedU(0.0);
                    d14 = (double)iicon.getInterpolatedV(0.0);
                    d8 = d7;
                    d16 = (double)iicon.getInterpolatedV(16.0);
                    d10 = (double)iicon.getInterpolatedU(16.0);
                    d18 = d16;
                    d12 = d10;
                    d20 = d14;
                } else {
                    float f9 = MathHelper.sin(f7) * 0.25F;
                    float f10 = MathHelper.cos(f7) * 0.25F;
                    float f11 = 8.0F;
                    d7 = (double)iicon.getInterpolatedU((double)(8.0F + (-f10 - f9) * 16.0F));
                    d14 = (double)iicon.getInterpolatedV((double)(8.0F + (-f10 + f9) * 16.0F));
                    d8 = (double)iicon.getInterpolatedU((double)(8.0F + (-f10 + f9) * 16.0F));
                    d16 = (double)iicon.getInterpolatedV((double)(8.0F + (f10 + f9) * 16.0F));
                    d10 = (double)iicon.getInterpolatedU((double)(8.0F + (f10 + f9) * 16.0F));
                    d18 = (double)iicon.getInterpolatedV((double)(8.0F + (f10 - f9) * 16.0F));
                    d12 = (double)iicon.getInterpolatedU((double)(8.0F + (f10 - f9) * 16.0F));
                    d20 = (double)iicon.getInterpolatedV((double)(8.0F + (-f10 - f9) * 16.0F));
                }

                tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
                tessellator.setColorOpaque_F(f4 * f, f4 * f1, f4 * f2);
                tessellator.addVertexWithUV((double)(i + 0), (double)j + d2, (double)(k + 0), d7, d14);
                tessellator.addVertexWithUV((double)(i + 0), (double)j + d3, (double)(k + 1), d8, d16);
                tessellator.addVertexWithUV((double)(i + 1), (double)j + d4, (double)(k + 1), d10, d18);
                tessellator.addVertexWithUV((double)(i + 1), (double)j + d5, (double)(k + 0), d12, d20);
                tessellator.addVertexWithUV((double)(i + 0), (double)j + d2, (double)(k + 0), d7, d14);
                tessellator.addVertexWithUV((double)(i + 1), (double)j + d5, (double)(k + 0), d12, d20);
                tessellator.addVertexWithUV((double)(i + 1), (double)j + d4, (double)(k + 1), d10, d18);
                tessellator.addVertexWithUV((double)(i + 0), (double)j + d3, (double)(k + 1), d8, d16);
            }

            if (this.renderAllFaces || flag1) {
                tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, i, j - 1, k));
                tessellator.setColorOpaque_F(f3, f3, f3);
                this.renderFaceYNeg(block, (double)i, (double)j + d6, (double)k, this.getBlockIconFromSide(block, 0));
                flag2 = true;
            }

            for (int k1 = 0; k1 < 4; k1++) {
                int l1 = i;
                int j1 = k;
                if (k1 == 0) {
                    j1 = k - 1;
                }

                if (k1 == 1) {
                    j1++;
                }

                if (k1 == 2) {
                    l1 = i - 1;
                }

                if (k1 == 3) {
                    l1++;
                }

                IIcon iicon1 = this.getBlockIconFromSideAndMetadata(block, k1 + 2, i1);
                if (this.renderAllFaces || aboolean[k1]) {
                    double d9;
                    double d11;
                    double d13;
                    double d15;
                    double d17;
                    double d19;
                    if (k1 == 0) {
                        d9 = d2;
                        d11 = d5;
                        d13 = (double)i;
                        d17 = (double)(i + 1);
                        d15 = (double)k + d6;
                        d19 = (double)k + d6;
                    } else if (k1 == 1) {
                        d9 = d4;
                        d11 = d3;
                        d13 = (double)(i + 1);
                        d17 = (double)i;
                        d15 = (double)(k + 1) - d6;
                        d19 = (double)(k + 1) - d6;
                    } else if (k1 == 2) {
                        d9 = d3;
                        d11 = d2;
                        d13 = (double)i + d6;
                        d17 = (double)i + d6;
                        d15 = (double)(k + 1);
                        d19 = (double)k;
                    } else {
                        d9 = d5;
                        d11 = d4;
                        d13 = (double)(i + 1) - d6;
                        d17 = (double)(i + 1) - d6;
                        d15 = (double)k;
                        d19 = (double)(k + 1);
                    }

                    flag2 = true;
                    float f8 = iicon1.getInterpolatedU(0.0);
                    float f14 = iicon1.getInterpolatedU(8.0);
                    float f15 = iicon1.getInterpolatedV((1.0 - d9) * 16.0 * 0.5);
                    float f16 = iicon1.getInterpolatedV((1.0 - d11) * 16.0 * 0.5);
                    float f12 = iicon1.getInterpolatedV(8.0);
                    tessellator.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, l1, j, j1));
                    float f13 = 1.0F;
                    f13 *= k1 < 2 ? f5 : f6;
                    tessellator.setColorOpaque_F(f4 * f13 * f, f4 * f13 * f1, f4 * f13 * f2);
                    tessellator.addVertexWithUV(d13, (double)j + d9, d15, (double)f8, (double)f15);
                    tessellator.addVertexWithUV(d17, (double)j + d11, d19, (double)f14, (double)f16);
                    tessellator.addVertexWithUV(d17, (double)(j + 0), d19, (double)f14, (double)f12);
                    tessellator.addVertexWithUV(d13, (double)(j + 0), d15, (double)f8, (double)f12);
                    tessellator.addVertexWithUV(d13, (double)(j + 0), d15, (double)f8, (double)f12);
                    tessellator.addVertexWithUV(d17, (double)(j + 0), d19, (double)f14, (double)f12);
                    tessellator.addVertexWithUV(d17, (double)j + d11, d19, (double)f14, (double)f16);
                    tessellator.addVertexWithUV(d13, (double)j + d9, d15, (double)f8, (double)f15);
                }
            }

            this.renderMinY = d0;
            this.renderMaxY = d1;
            return flag2;
        }
    }

    public float getLiquidHeight(int i, int j, int k, Material material) {
        int l = 0;
        float f = 0.0F;

        for (int i1 = 0; i1 < 4; i1++) {
            int j1 = i - (i1 & 1);
            int k1 = k - (i1 >> 1 & 1);
            if (this.blockAccess.getBlock(j1, j + 1, k1).getMaterial() == material) {
                return 1.0F;
            }

            Material material1 = this.blockAccess.getBlock(j1, j, k1).getMaterial();
            if (material1 == material) {
                int l1 = this.blockAccess.getBlockMetadata(j1, j, k1);
                if (l1 >= 8 || l1 == 0) {
                    f += BlockLiquid.getLiquidHeightPercent(l1) * 10.0F;
                    l += 10;
                }

                f += BlockLiquid.getLiquidHeightPercent(l1);
                l++;
            } else if (!material1.isSolid()) {
                f++;
                l++;
            }
        }

        return 1.0F - f / (float)l;
    }

    public void renderBlockSandFalling(Block block, World world, int i, int j, int k, int l) {
        float f = 0.5F;
        float f1 = 1.0F;
        float f2 = 0.8F;
        float f3 = 0.6F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, i, j, k));
        tessellator.setColorOpaque_F(f, f, f);
        this.renderFaceYNeg(block, -0.5, -0.5, -0.5, this.getBlockIconFromSideAndMetadata(block, 0, l));
        tessellator.setColorOpaque_F(f1, f1, f1);
        this.renderFaceYPos(block, -0.5, -0.5, -0.5, this.getBlockIconFromSideAndMetadata(block, 1, l));
        tessellator.setColorOpaque_F(f2, f2, f2);
        this.renderFaceZNeg(block, -0.5, -0.5, -0.5, this.getBlockIconFromSideAndMetadata(block, 2, l));
        tessellator.setColorOpaque_F(f2, f2, f2);
        this.renderFaceZPos(block, -0.5, -0.5, -0.5, this.getBlockIconFromSideAndMetadata(block, 3, l));
        tessellator.setColorOpaque_F(f3, f3, f3);
        this.renderFaceXNeg(block, -0.5, -0.5, -0.5, this.getBlockIconFromSideAndMetadata(block, 4, l));
        tessellator.setColorOpaque_F(f3, f3, f3);
        this.renderFaceXPos(block, -0.5, -0.5, -0.5, this.getBlockIconFromSideAndMetadata(block, 5, l));
        tessellator.draw();
    }

    public boolean renderBlockLog(Block block, int i, int j, int k) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = l & 12;
        if (i1 == 4) {
            this.uvRotateEast = 1;
            this.uvRotateWest = 1;
            this.uvRotateTop = 1;
            this.uvRotateBottom = 1;
        } else if (i1 == 8) {
            this.uvRotateSouth = 1;
            this.uvRotateNorth = 1;
        }

        boolean flag = this.renderStandardBlock(block, i, j, k);
        this.uvRotateSouth = 0;
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        return flag;
    }

    public boolean renderBlockQuartz(Block block, int i, int j, int k) {
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        if (l == 3) {
            this.uvRotateEast = 1;
            this.uvRotateWest = 1;
            this.uvRotateTop = 1;
            this.uvRotateBottom = 1;
        } else if (l == 4) {
            this.uvRotateSouth = 1;
            this.uvRotateNorth = 1;
        }

        boolean flag = this.renderStandardBlock(block, i, j, k);
        this.uvRotateSouth = 0;
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        return flag;
    }

    public int getAoBrightness(int i, int j, int k, int l) {
        if (i == 0) {
            i = l;
        }

        if (j == 0) {
            j = l;
        }

        if (k == 0) {
            k = l;
        }

        return i + j + k + l >> 2 & 16711935;
    }

    public int mixAoBrightness(int i, int j, int k, int l, double d0, double d1, double d2, double d3) {
        int i1 = (int)((double)(i >> 16 & 0xFF) * d0 + (double)(j >> 16 & 0xFF) * d1 + (double)(k >> 16 & 0xFF) * d2 + (double)(l >> 16 & 0xFF) * d3) & 0xFF;
        int j1 = (int)((double)(i & 0xFF) * d0 + (double)(j & 0xFF) * d1 + (double)(k & 0xFF) * d2 + (double)(l & 0xFF) * d3) & 0xFF;
        return i1 << 16 | j1;
    }

    public boolean renderBlockCocoa(BlockCocoa blockcocoa, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockcocoa.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        int i1 = BlockDirectional.getDirection(l);
        int j1 = BlockCocoa.func_149987_c(l);
        IIcon iicon = blockcocoa.getCocoaIcon(j1);
        int k1 = 4 + j1 * 2;
        int l1 = 5 + j1 * 2;
        double d0 = 15.0 - (double)k1;
        double d1 = 15.0;
        double d2 = 4.0;
        double d3 = 4.0 + (double)l1;
        double d4 = (double)iicon.getInterpolatedU(d0);
        double d5 = (double)iicon.getInterpolatedU(d1);
        double d6 = (double)iicon.getInterpolatedV(d2);
        double d7 = (double)iicon.getInterpolatedV(d3);
        double d8 = 0.0;
        double d9 = 0.0;
        switch (i1) {
            case 0:
                d8 = 8.0 - (double)(k1 / 2);
                d9 = 15.0 - (double)k1;
                break;
            case 1:
                d8 = 1.0;
                d9 = 8.0 - (double)(k1 / 2);
                break;
            case 2:
                d8 = 8.0 - (double)(k1 / 2);
                d9 = 1.0;
                break;
            case 3:
                d8 = 15.0 - (double)k1;
                d9 = 8.0 - (double)(k1 / 2);
        }

        double d10 = (double)i + d8 / 16.0;
        double d11 = (double)i + (d8 + (double)k1) / 16.0;
        double d12 = (double)j + (12.0 - (double)l1) / 16.0;
        double d13 = (double)j + 0.75;
        double d14 = (double)k + d9 / 16.0;
        double d15 = (double)k + (d9 + (double)k1) / 16.0;
        tessellator.addVertexWithUV(d10, d12, d14, d4, d7);
        tessellator.addVertexWithUV(d10, d12, d15, d5, d7);
        tessellator.addVertexWithUV(d10, d13, d15, d5, d6);
        tessellator.addVertexWithUV(d10, d13, d14, d4, d6);
        tessellator.addVertexWithUV(d11, d12, d15, d4, d7);
        tessellator.addVertexWithUV(d11, d12, d14, d5, d7);
        tessellator.addVertexWithUV(d11, d13, d14, d5, d6);
        tessellator.addVertexWithUV(d11, d13, d15, d4, d6);
        tessellator.addVertexWithUV(d11, d12, d14, d4, d7);
        tessellator.addVertexWithUV(d10, d12, d14, d5, d7);
        tessellator.addVertexWithUV(d10, d13, d14, d5, d6);
        tessellator.addVertexWithUV(d11, d13, d14, d4, d6);
        tessellator.addVertexWithUV(d10, d12, d15, d4, d7);
        tessellator.addVertexWithUV(d11, d12, d15, d5, d7);
        tessellator.addVertexWithUV(d11, d13, d15, d5, d6);
        tessellator.addVertexWithUV(d10, d13, d15, d4, d6);
        int i2 = k1;
        if (j1 >= 2) {
            i2 = k1 - 1;
        }

        d4 = (double)iicon.getMinU();
        d5 = (double)iicon.getInterpolatedU((double)i2);
        d6 = (double)iicon.getMinV();
        d7 = (double)iicon.getInterpolatedV((double)i2);
        tessellator.addVertexWithUV(d10, d13, d15, d4, d7);
        tessellator.addVertexWithUV(d11, d13, d15, d5, d7);
        tessellator.addVertexWithUV(d11, d13, d14, d5, d6);
        tessellator.addVertexWithUV(d10, d13, d14, d4, d6);
        tessellator.addVertexWithUV(d10, d12, d14, d4, d6);
        tessellator.addVertexWithUV(d11, d12, d14, d5, d6);
        tessellator.addVertexWithUV(d11, d12, d15, d5, d7);
        tessellator.addVertexWithUV(d10, d12, d15, d4, d7);
        d4 = (double)iicon.getInterpolatedU(12.0);
        d5 = (double)iicon.getMaxU();
        d6 = (double)iicon.getMinV();
        d7 = (double)iicon.getInterpolatedV(4.0);
        d8 = 8.0;
        d9 = 0.0;
        switch (i1) {
            case 0:
                d8 = 8.0;
                d9 = 12.0;
                double d17 = d4;
                d4 = d5;
                d5 = d17;
                break;
            case 1:
                d8 = 0.0;
                d9 = 8.0;
                break;
            case 2:
                d8 = 8.0;
                d9 = 0.0;
                break;
            case 3:
                d8 = 12.0;
                d9 = 8.0;
                double d16 = d4;
                d4 = d5;
                d5 = d16;
        }

        d10 = (double)i + d8 / 16.0;
        d11 = (double)i + (d8 + 4.0) / 16.0;
        d12 = (double)j + 0.75;
        d13 = (double)j + 1.0;
        d14 = (double)k + d9 / 16.0;
        d15 = (double)k + (d9 + 4.0) / 16.0;
        if (i1 == 2 || i1 == 0) {
            tessellator.addVertexWithUV(d10, d12, d14, d5, d7);
            tessellator.addVertexWithUV(d10, d12, d15, d4, d7);
            tessellator.addVertexWithUV(d10, d13, d15, d4, d6);
            tessellator.addVertexWithUV(d10, d13, d14, d5, d6);
            tessellator.addVertexWithUV(d10, d12, d15, d4, d7);
            tessellator.addVertexWithUV(d10, d12, d14, d5, d7);
            tessellator.addVertexWithUV(d10, d13, d14, d5, d6);
            tessellator.addVertexWithUV(d10, d13, d15, d4, d6);
        } else if (i1 == 1 || i1 == 3) {
            tessellator.addVertexWithUV(d11, d12, d14, d4, d7);
            tessellator.addVertexWithUV(d10, d12, d14, d5, d7);
            tessellator.addVertexWithUV(d10, d13, d14, d5, d6);
            tessellator.addVertexWithUV(d11, d13, d14, d4, d6);
            tessellator.addVertexWithUV(d10, d12, d14, d5, d7);
            tessellator.addVertexWithUV(d11, d12, d14, d4, d7);
            tessellator.addVertexWithUV(d11, d13, d14, d4, d6);
            tessellator.addVertexWithUV(d10, d13, d14, d5, d6);
        }

        return true;
    }

    public boolean renderBlockBeacon(BlockBeacon blockbeacon, int i, int j, int k) {
        float f = 0.1875F;
        this.setOverrideBlockTexture(this.getBlockIcon(Blocks.glass));
        this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        this.renderStandardBlock(blockbeacon, i, j, k);
        this.renderAllFaces = true;
        this.setOverrideBlockTexture(this.getBlockIcon(Blocks.obsidian));
        this.setRenderBounds(0.125, 0.00625F, 0.125, 0.875, (double)f, 0.875);
        this.renderStandardBlock(blockbeacon, i, j, k);
        this.setOverrideBlockTexture(this.getBlockIcon(Blocks.beacon));
        this.setRenderBounds(0.1875, (double)f, 0.1875, 0.8125, 0.875, 0.8125);
        this.renderStandardBlock(blockbeacon, i, j, k);
        this.renderAllFaces = false;
        this.clearOverrideBlockTexture();
        return true;
    }

    public boolean renderBlockCactus(Block block, int i, int j, int k) {
        int l = block.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        return this.renderBlockCactusImpl(block, i, j, k, f, f1, f2);
    }

    public boolean renderBlockCactusImpl(Block block, int i, int j, int k, float f, float f1, float f2) {
        Tessellator tessellator = Tessellator.instance;
        boolean flag = false;
        float f3 = 0.5F;
        float f4 = 1.0F;
        float f5 = 0.8F;
        float f6 = 0.6F;
        float f7 = f3 * f;
        float f8 = f4 * f;
        float f9 = f5 * f;
        float f10 = f6 * f;
        float f11 = f3 * f1;
        float f12 = f4 * f1;
        float f13 = f5 * f1;
        float f14 = f6 * f1;
        float f15 = f3 * f2;
        float f16 = f4 * f2;
        float f17 = f5 * f2;
        float f18 = f6 * f2;
        float f19 = 0.0625F;
        int l = block.getMixedBrightnessForBlock(this.blockAccess, i, j, k);
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i, j - 1, k, 0)) {
            tessellator.setBrightness(this.renderMinY > 0.0 ? l : block.getMixedBrightnessForBlock(this.blockAccess, i, j - 1, k));
            tessellator.setColorOpaque_F(f7, f11, f15);
            this.renderFaceYNeg(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 0));
        }

        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i, j + 1, k, 1)) {
            tessellator.setBrightness(this.renderMaxY < 1.0 ? l : block.getMixedBrightnessForBlock(this.blockAccess, i, j + 1, k));
            tessellator.setColorOpaque_F(f8, f12, f16);
            this.renderFaceYPos(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 1));
        }

        tessellator.setBrightness(l);
        tessellator.setColorOpaque_F(f9, f13, f17);
        tessellator.addTranslation(0.0F, 0.0F, f19);
        this.renderFaceZNeg(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 2));
        tessellator.addTranslation(0.0F, 0.0F, -f19);
        tessellator.addTranslation(0.0F, 0.0F, -f19);
        this.renderFaceZPos(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 3));
        tessellator.addTranslation(0.0F, 0.0F, f19);
        tessellator.setColorOpaque_F(f10, f14, f18);
        tessellator.addTranslation(f19, 0.0F, 0.0F);
        this.renderFaceXNeg(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 4));
        tessellator.addTranslation(-f19, 0.0F, 0.0F);
        tessellator.addTranslation(-f19, 0.0F, 0.0F);
        this.renderFaceXPos(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 5));
        tessellator.addTranslation(f19, 0.0F, 0.0F);
        return true;
    }

    public boolean renderBlockFence(BlockFence blockfence, int i, int j, int k) {
        boolean flag = false;
        float f = 0.375F;
        float f1 = 0.625F;
        this.setRenderBounds((double)f, 0.0, (double)f, (double)f1, 1.0, (double)f1);
        this.renderStandardBlock(blockfence, i, j, k);
        flag = true;
        boolean flag1 = false;
        boolean flag2 = false;
        if (blockfence.canConnectFenceTo(this.blockAccess, i - 1, j, k) || blockfence.canConnectFenceTo(this.blockAccess, i + 1, j, k)) {
            flag1 = true;
        }

        if (blockfence.canConnectFenceTo(this.blockAccess, i, j, k - 1) || blockfence.canConnectFenceTo(this.blockAccess, i, j, k + 1)) {
            flag2 = true;
        }

        boolean flag3 = blockfence.canConnectFenceTo(this.blockAccess, i - 1, j, k);
        boolean flag4 = blockfence.canConnectFenceTo(this.blockAccess, i + 1, j, k);
        boolean flag5 = blockfence.canConnectFenceTo(this.blockAccess, i, j, k - 1);
        boolean flag6 = blockfence.canConnectFenceTo(this.blockAccess, i, j, k + 1);
        if (!flag1 && !flag2) {
            flag1 = true;
        }

        f = 0.4375F;
        f1 = 0.5625F;
        float f2 = 0.75F;
        float f3 = 0.9375F;
        float f4 = flag3 ? 0.0F : f;
        float f5 = flag4 ? 1.0F : f1;
        float f6 = flag5 ? 0.0F : f;
        float f7 = flag6 ? 1.0F : f1;
        this.field_152631_f = true;
        if (flag1) {
            this.setRenderBounds((double)f4, (double)f2, (double)f, (double)f5, (double)f3, (double)f1);
            this.renderStandardBlock(blockfence, i, j, k);
            flag = true;
        }

        if (flag2) {
            this.setRenderBounds((double)f, (double)f2, (double)f6, (double)f1, (double)f3, (double)f7);
            this.renderStandardBlock(blockfence, i, j, k);
            flag = true;
        }

        f2 = 0.375F;
        f3 = 0.5625F;
        if (flag1) {
            this.setRenderBounds((double)f4, (double)f2, (double)f, (double)f5, (double)f3, (double)f1);
            this.renderStandardBlock(blockfence, i, j, k);
            flag = true;
        }

        if (flag2) {
            this.setRenderBounds((double)f, (double)f2, (double)f6, (double)f1, (double)f3, (double)f7);
            this.renderStandardBlock(blockfence, i, j, k);
            flag = true;
        }

        this.field_152631_f = false;
        blockfence.setBlockBoundsBasedOnState(this.blockAccess, i, j, k);
        return flag;
    }

    public boolean renderBlockWall(BlockWall blockwall, int i, int j, int k) {
        boolean flag = blockwall.canConnectWallTo(this.blockAccess, i - 1, j, k);
        boolean flag1 = blockwall.canConnectWallTo(this.blockAccess, i + 1, j, k);
        boolean flag2 = blockwall.canConnectWallTo(this.blockAccess, i, j, k - 1);
        boolean flag3 = blockwall.canConnectWallTo(this.blockAccess, i, j, k + 1);
        boolean flag4 = flag2 && flag3 && !flag && !flag1;
        boolean flag5 = !flag2 && !flag3 && flag && flag1;
        boolean flag6 = this.blockAccess.isAirBlock(i, j + 1, k);
        if ((flag4 || flag5) && flag6) {
            if (flag4) {
                this.setRenderBounds(0.3125, 0.0, 0.0, 0.6875, 0.8125, 1.0);
                this.renderStandardBlock(blockwall, i, j, k);
            } else {
                this.setRenderBounds(0.0, 0.0, 0.3125, 1.0, 0.8125, 0.6875);
                this.renderStandardBlock(blockwall, i, j, k);
            }
        } else {
            this.setRenderBounds(0.25, 0.0, 0.25, 0.75, 1.0, 0.75);
            this.renderStandardBlock(blockwall, i, j, k);
            if (flag) {
                this.setRenderBounds(0.0, 0.0, 0.3125, 0.25, 0.8125, 0.6875);
                this.renderStandardBlock(blockwall, i, j, k);
            }

            if (flag1) {
                this.setRenderBounds(0.75, 0.0, 0.3125, 1.0, 0.8125, 0.6875);
                this.renderStandardBlock(blockwall, i, j, k);
            }

            if (flag2) {
                this.setRenderBounds(0.3125, 0.0, 0.0, 0.6875, 0.8125, 0.25);
                this.renderStandardBlock(blockwall, i, j, k);
            }

            if (flag3) {
                this.setRenderBounds(0.3125, 0.0, 0.75, 0.6875, 0.8125, 1.0);
                this.renderStandardBlock(blockwall, i, j, k);
            }
        }

        blockwall.setBlockBoundsBasedOnState(this.blockAccess, i, j, k);
        return true;
    }

    public boolean renderBlockDragonEgg(BlockDragonEgg blockdragonegg, int i, int j, int k) {
        boolean flag = false;
        byte b0 = 0;

        for (int l = 0; l < 8; l++) {
            byte b1 = 0;
            byte b2 = 1;
            if (l == 0) {
                b1 = 2;
            }

            if (l == 1) {
                b1 = 3;
            }

            if (l == 2) {
                b1 = 4;
            }

            if (l == 3) {
                b1 = 5;
                b2 = 2;
            }

            if (l == 4) {
                b1 = 6;
                b2 = 3;
            }

            if (l == 5) {
                b1 = 7;
                b2 = 5;
            }

            if (l == 6) {
                b1 = 6;
                b2 = 2;
            }

            if (l == 7) {
                b1 = 3;
            }

            float f = (float)b1 / 16.0F;
            float f1 = 1.0F - (float)b0 / 16.0F;
            float f2 = 1.0F - (float)(b0 + b2) / 16.0F;
            b0 += b2;
            this.setRenderBounds((double)(0.5F - f), (double)f2, (double)(0.5F - f), (double)(0.5F + f), (double)f1, (double)(0.5F + f));
            this.renderStandardBlock(blockdragonegg, i, j, k);
        }

        flag = true;
        this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        return flag;
    }

    public boolean renderBlockFenceGate(BlockFenceGate blockfencegate, int i, int j, int k) {
        boolean flag = true;
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        boolean flag1 = BlockFenceGate.isFenceGateOpen(l);
        int i1 = BlockDirectional.getDirection(l);
        float f = 0.375F;
        float f1 = 0.5625F;
        float f2 = 0.75F;
        float f3 = 0.9375F;
        float f4 = 0.3125F;
        float f5 = 1.0F;
        if ((i1 == 2 || i1 == 0)
                && this.blockAccess.getBlock(i - 1, j, k) == Blocks.cobblestone_wall
                && this.blockAccess.getBlock(i + 1, j, k) == Blocks.cobblestone_wall
            || (i1 == 3 || i1 == 1)
                && this.blockAccess.getBlock(i, j, k - 1) == Blocks.cobblestone_wall
                && this.blockAccess.getBlock(i, j, k + 1) == Blocks.cobblestone_wall) {
            f -= 0.1875F;
            f1 -= 0.1875F;
            f2 -= 0.1875F;
            f3 -= 0.1875F;
            f4 -= 0.1875F;
            f5 -= 0.1875F;
        }

        this.renderAllFaces = true;
        if (i1 != 3 && i1 != 1) {
            float f13 = 0.0F;
            float f20 = 0.125F;
            float f27 = 0.4375F;
            float f34 = 0.5625F;
            this.setRenderBounds((double)f13, (double)f4, (double)f27, (double)f20, (double)f5, (double)f34);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f13 = 0.875F;
            f20 = 1.0F;
            this.setRenderBounds((double)f13, (double)f4, (double)f27, (double)f20, (double)f5, (double)f34);
            this.renderStandardBlock(blockfencegate, i, j, k);
        } else {
            this.uvRotateTop = 1;
            float f6 = 0.4375F;
            float f7 = 0.5625F;
            float f8 = 0.0F;
            float f9 = 0.125F;
            this.setRenderBounds((double)f6, (double)f4, (double)f8, (double)f7, (double)f5, (double)f9);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f8 = 0.875F;
            f9 = 1.0F;
            this.setRenderBounds((double)f6, (double)f4, (double)f8, (double)f7, (double)f5, (double)f9);
            this.renderStandardBlock(blockfencegate, i, j, k);
            this.uvRotateTop = 0;
        }

        if (flag1) {
            if (i1 == 2 || i1 == 0) {
                this.uvRotateTop = 1;
            }

            if (i1 == 3) {
                float f14 = 0.0F;
                float f21 = 0.125F;
                float f28 = 0.875F;
                float f35 = 1.0F;
                float f10 = 0.5625F;
                float f11 = 0.8125F;
                float f12 = 0.9375F;
                this.setRenderBounds(0.8125, (double)f, 0.0, 0.9375, (double)f3, 0.125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.8125, (double)f, 0.875, 0.9375, (double)f3, 1.0);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.5625, (double)f, 0.0, 0.8125, (double)f1, 0.125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.5625, (double)f, 0.875, 0.8125, (double)f1, 1.0);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.5625, (double)f2, 0.0, 0.8125, (double)f3, 0.125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.5625, (double)f2, 0.875, 0.8125, (double)f3, 1.0);
                this.renderStandardBlock(blockfencegate, i, j, k);
            } else if (i1 == 1) {
                float f15 = 0.0F;
                float f22 = 0.125F;
                float f29 = 0.875F;
                float f36 = 1.0F;
                float f41 = 0.0625F;
                float f44 = 0.1875F;
                float f47 = 0.4375F;
                this.setRenderBounds(0.0625, (double)f, 0.0, 0.1875, (double)f3, 0.125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.0625, (double)f, 0.875, 0.1875, (double)f3, 1.0);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.1875, (double)f, 0.0, 0.4375, (double)f1, 0.125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.1875, (double)f, 0.875, 0.4375, (double)f1, 1.0);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.1875, (double)f2, 0.0, 0.4375, (double)f3, 0.125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.1875, (double)f2, 0.875, 0.4375, (double)f3, 1.0);
                this.renderStandardBlock(blockfencegate, i, j, k);
            } else if (i1 == 0) {
                float f16 = 0.0F;
                float f23 = 0.125F;
                float f30 = 0.875F;
                float f37 = 1.0F;
                float f42 = 0.5625F;
                float f45 = 0.8125F;
                float f48 = 0.9375F;
                this.setRenderBounds(0.0, (double)f, 0.8125, 0.125, (double)f3, 0.9375);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.875, (double)f, 0.8125, 1.0, (double)f3, 0.9375);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.0, (double)f, 0.5625, 0.125, (double)f1, 0.8125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.875, (double)f, 0.5625, 1.0, (double)f1, 0.8125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.0, (double)f2, 0.5625, 0.125, (double)f3, 0.8125);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.875, (double)f2, 0.5625, 1.0, (double)f3, 0.8125);
                this.renderStandardBlock(blockfencegate, i, j, k);
            } else if (i1 == 2) {
                float f17 = 0.0F;
                float f24 = 0.125F;
                float f31 = 0.875F;
                float f38 = 1.0F;
                float f43 = 0.0625F;
                float f46 = 0.1875F;
                float f49 = 0.4375F;
                this.setRenderBounds(0.0, (double)f, 0.0625, 0.125, (double)f3, 0.1875);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.875, (double)f, 0.0625, 1.0, (double)f3, 0.1875);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.0, (double)f, 0.1875, 0.125, (double)f1, 0.4375);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.875, (double)f, 0.1875, 1.0, (double)f1, 0.4375);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.0, (double)f2, 0.1875, 0.125, (double)f3, 0.4375);
                this.renderStandardBlock(blockfencegate, i, j, k);
                this.setRenderBounds(0.875, (double)f2, 0.1875, 1.0, (double)f3, 0.4375);
                this.renderStandardBlock(blockfencegate, i, j, k);
            }
        } else if (i1 != 3 && i1 != 1) {
            float f19 = 0.375F;
            float f26 = 0.5F;
            float f33 = 0.4375F;
            float f40 = 0.5625F;
            this.setRenderBounds((double)f19, (double)f, (double)f33, (double)f26, (double)f3, (double)f40);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f19 = 0.5F;
            f26 = 0.625F;
            this.setRenderBounds((double)f19, (double)f, (double)f33, (double)f26, (double)f3, (double)f40);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f19 = 0.625F;
            f26 = 0.875F;
            this.setRenderBounds((double)f19, (double)f, (double)f33, (double)f26, (double)f1, (double)f40);
            this.renderStandardBlock(blockfencegate, i, j, k);
            this.setRenderBounds((double)f19, (double)f2, (double)f33, (double)f26, (double)f3, (double)f40);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f19 = 0.125F;
            f26 = 0.375F;
            this.setRenderBounds((double)f19, (double)f, (double)f33, (double)f26, (double)f1, (double)f40);
            this.renderStandardBlock(blockfencegate, i, j, k);
            this.setRenderBounds((double)f19, (double)f2, (double)f33, (double)f26, (double)f3, (double)f40);
            this.renderStandardBlock(blockfencegate, i, j, k);
        } else {
            this.uvRotateTop = 1;
            float f18 = 0.4375F;
            float f25 = 0.5625F;
            float f32 = 0.375F;
            float f39 = 0.5F;
            this.setRenderBounds((double)f18, (double)f, (double)f32, (double)f25, (double)f3, (double)f39);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f32 = 0.5F;
            f39 = 0.625F;
            this.setRenderBounds((double)f18, (double)f, (double)f32, (double)f25, (double)f3, (double)f39);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f32 = 0.625F;
            f39 = 0.875F;
            this.setRenderBounds((double)f18, (double)f, (double)f32, (double)f25, (double)f1, (double)f39);
            this.renderStandardBlock(blockfencegate, i, j, k);
            this.setRenderBounds((double)f18, (double)f2, (double)f32, (double)f25, (double)f3, (double)f39);
            this.renderStandardBlock(blockfencegate, i, j, k);
            f32 = 0.125F;
            f39 = 0.375F;
            this.setRenderBounds((double)f18, (double)f, (double)f32, (double)f25, (double)f1, (double)f39);
            this.renderStandardBlock(blockfencegate, i, j, k);
            this.setRenderBounds((double)f18, (double)f2, (double)f32, (double)f25, (double)f3, (double)f39);
            this.renderStandardBlock(blockfencegate, i, j, k);
        }

        this.renderAllFaces = false;
        this.uvRotateTop = 0;
        this.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        return flag;
    }

    public boolean renderBlockHopper(BlockHopper blockhopper, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(blockhopper.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
        int l = blockhopper.colorMultiplier(this.blockAccess, i, j, k);
        float f = (float)(l >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(l >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(l & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        tessellator.setColorOpaque_F(f, f1, f2);
        return this.renderBlockHopperMetadata(blockhopper, i, j, k, this.blockAccess.getBlockMetadata(i, j, k), false);
    }

    public boolean renderBlockHopperMetadata(BlockHopper blockhopper, int i, int j, int k, int l, boolean flag) {
        Tessellator tessellator = Tessellator.instance;
        int i1 = BlockHopper.getDirectionFromMetadata(l);
        double d0 = 0.625;
        this.setRenderBounds(0.0, d0, 0.0, 1.0, 1.0, 1.0);
        if (flag) {
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, -1.0F, 0.0F);
            this.renderFaceYNeg(blockhopper, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockhopper, 0, l));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            this.renderFaceYPos(blockhopper, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockhopper, 1, l));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            this.renderFaceZNeg(blockhopper, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockhopper, 2, l));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            this.renderFaceZPos(blockhopper, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockhopper, 3, l));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            this.renderFaceXNeg(blockhopper, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockhopper, 4, l));
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            this.renderFaceXPos(blockhopper, 0.0, 0.0, 0.0, this.getBlockIconFromSideAndMetadata(blockhopper, 5, l));
            tessellator.draw();
        } else {
            this.renderStandardBlock(blockhopper, i, j, k);
        }

        if (!flag) {
            tessellator.setBrightness(blockhopper.getMixedBrightnessForBlock(this.blockAccess, i, j, k));
            int j1 = blockhopper.colorMultiplier(this.blockAccess, i, j, k);
            float f = (float)(j1 >> 16 & 0xFF) / 255.0F;
            float f1 = (float)(j1 >> 8 & 0xFF) / 255.0F;
            float f2 = (float)(j1 & 0xFF) / 255.0F;
            if (EntityRenderer.anaglyphEnable) {
                float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
                float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
                float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
                f = f3;
                f1 = f4;
                f2 = f5;
            }

            tessellator.setColorOpaque_F(f, f1, f2);
        }

        IIcon iicon = BlockHopper.getHopperIcon("hopper_outside");
        IIcon iicon1 = BlockHopper.getHopperIcon("hopper_inside");
        float f6 = 0.125F;
        if (flag) {
            tessellator.startDrawingQuads();
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            this.renderFaceXPos(blockhopper, (double)(-1.0F + f6), 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            this.renderFaceXNeg(blockhopper, (double)(1.0F - f6), 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            this.renderFaceZPos(blockhopper, 0.0, 0.0, (double)(-1.0F + f6), iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            this.renderFaceZNeg(blockhopper, 0.0, 0.0, (double)(1.0F - f6), iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            this.renderFaceYPos(blockhopper, 0.0, -1.0 + d0, 0.0, iicon1);
            tessellator.draw();
        } else {
            this.renderFaceXPos(blockhopper, (double)((float)i - 1.0F + f6), (double)j, (double)k, iicon);
            this.renderFaceXNeg(blockhopper, (double)((float)i + 1.0F - f6), (double)j, (double)k, iicon);
            this.renderFaceZPos(blockhopper, (double)i, (double)j, (double)((float)k - 1.0F + f6), iicon);
            this.renderFaceZNeg(blockhopper, (double)i, (double)j, (double)((float)k + 1.0F - f6), iicon);
            this.renderFaceYPos(blockhopper, (double)i, (double)((float)j - 1.0F) + d0, (double)k, iicon1);
        }

        this.setOverrideBlockTexture(iicon);
        double d3 = 0.25;
        double d4 = 0.25;
        this.setRenderBounds(d3, d4, d3, 1.0 - d3, d0 - 0.002, 1.0 - d3);
        if (flag) {
            tessellator.startDrawingQuads();
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            this.renderFaceXPos(blockhopper, 0.0, 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            this.renderFaceXNeg(blockhopper, 0.0, 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            this.renderFaceZPos(blockhopper, 0.0, 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            this.renderFaceZNeg(blockhopper, 0.0, 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            this.renderFaceYPos(blockhopper, 0.0, 0.0, 0.0, iicon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, -1.0F, 0.0F);
            this.renderFaceYNeg(blockhopper, 0.0, 0.0, 0.0, iicon);
            tessellator.draw();
        } else {
            this.renderStandardBlock(blockhopper, i, j, k);
        }

        if (!flag) {
            double d1 = 0.375;
            double d2 = 0.25;
            this.setOverrideBlockTexture(iicon);
            if (i1 == 0) {
                this.setRenderBounds(d1, 0.0, d1, 1.0 - d1, 0.25, 1.0 - d1);
                this.renderStandardBlock(blockhopper, i, j, k);
            }

            if (i1 == 2) {
                this.setRenderBounds(d1, d4, 0.0, 1.0 - d1, d4 + d2, d3);
                this.renderStandardBlock(blockhopper, i, j, k);
            }

            if (i1 == 3) {
                this.setRenderBounds(d1, d4, 1.0 - d3, 1.0 - d1, d4 + d2, 1.0);
                this.renderStandardBlock(blockhopper, i, j, k);
            }

            if (i1 == 4) {
                this.setRenderBounds(0.0, d4, d1, d3, d4 + d2, 1.0 - d1);
                this.renderStandardBlock(blockhopper, i, j, k);
            }

            if (i1 == 5) {
                this.setRenderBounds(1.0 - d3, d4, d1, 1.0, d4 + d2, 1.0 - d1);
                this.renderStandardBlock(blockhopper, i, j, k);
            }
        }

        this.clearOverrideBlockTexture();
        return true;
    }

    public boolean renderBlockStairs(BlockStairs blockstairs, int i, int j, int k) {
        blockstairs.setBaseBounds(this.blockAccess, i, j, k);
        this.setRenderBoundsFromBlock(blockstairs);
        this.renderStandardBlock(blockstairs, i, j, k);
        this.field_152631_f = true;
        boolean flag = blockstairs.func_150145_f(this.blockAccess, i, j, k);
        this.setRenderBoundsFromBlock(blockstairs);
        this.renderStandardBlock(blockstairs, i, j, k);
        if (flag && blockstairs.func_150144_g(this.blockAccess, i, j, k)) {
            this.setRenderBoundsFromBlock(blockstairs);
            this.renderStandardBlock(blockstairs, i, j, k);
        }

        this.field_152631_f = false;
        return true;
    }

    public boolean renderBlockDoor(Block block, int i, int j, int k) {
        Tessellator tessellator = Tessellator.instance;
        int l = this.blockAccess.getBlockMetadata(i, j, k);
        if ((l & 8) != 0) {
            if (this.blockAccess.getBlock(i, j - 1, k) != block) {
                return false;
            }
        } else if (this.blockAccess.getBlock(i, j + 1, k) != block) {
            return false;
        }

        boolean flag = false;
        float f = 0.5F;
        float f1 = 1.0F;
        float f2 = 0.8F;
        float f3 = 0.6F;
        int i1 = block.getMixedBrightnessForBlock(this.blockAccess, i, j, k);
        tessellator.setBrightness(this.renderMinY > 0.0 ? i1 : block.getMixedBrightnessForBlock(this.blockAccess, i, j - 1, k));
        tessellator.setColorOpaque_F(f, f, f);
        this.renderFaceYNeg(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 0));
        flag = true;
        tessellator.setBrightness(this.renderMaxY < 1.0 ? i1 : block.getMixedBrightnessForBlock(this.blockAccess, i, j + 1, k));
        tessellator.setColorOpaque_F(f1, f1, f1);
        this.renderFaceYPos(block, (double)i, (double)j, (double)k, this.getBlockIcon(block, this.blockAccess, i, j, k, 1));
        flag = true;
        tessellator.setBrightness(this.renderMinZ > 0.0 ? i1 : block.getMixedBrightnessForBlock(this.blockAccess, i, j, k - 1));
        tessellator.setColorOpaque_F(f2, f2, f2);
        IIcon iicon = this.getBlockIcon(block, this.blockAccess, i, j, k, 2);
        this.renderFaceZNeg(block, (double)i, (double)j, (double)k, iicon);
        flag = true;
        this.flipTexture = false;
        tessellator.setBrightness(this.renderMaxZ < 1.0 ? i1 : block.getMixedBrightnessForBlock(this.blockAccess, i, j, k + 1));
        tessellator.setColorOpaque_F(f2, f2, f2);
        iicon = this.getBlockIcon(block, this.blockAccess, i, j, k, 3);
        this.renderFaceZPos(block, (double)i, (double)j, (double)k, iicon);
        flag = true;
        this.flipTexture = false;
        tessellator.setBrightness(this.renderMinX > 0.0 ? i1 : block.getMixedBrightnessForBlock(this.blockAccess, i - 1, j, k));
        tessellator.setColorOpaque_F(f3, f3, f3);
        iicon = this.getBlockIcon(block, this.blockAccess, i, j, k, 4);
        this.renderFaceXNeg(block, (double)i, (double)j, (double)k, iicon);
        flag = true;
        this.flipTexture = false;
        tessellator.setBrightness(this.renderMaxX < 1.0 ? i1 : block.getMixedBrightnessForBlock(this.blockAccess, i + 1, j, k));
        tessellator.setColorOpaque_F(f3, f3, f3);
        iicon = this.getBlockIcon(block, this.blockAccess, i, j, k, 5);
        this.renderFaceXPos(block, (double)i, (double)j, (double)k, iicon);
        flag = true;
        this.flipTexture = false;
        return flag;
    }

    /** Forge's hook for render types no vanilla renderer handles: the ISBRH registered for the id draws the block. */
    private boolean mcc$renderModBlock(Block block, int x, int y, int z, int renderType) {
        var handler = legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(renderType);
        return handler != null && handler.renderWorldBlock(this.blockAccess, x, y, z, block, renderType, this);
    }
}
