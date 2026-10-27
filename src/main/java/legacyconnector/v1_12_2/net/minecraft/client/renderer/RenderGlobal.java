package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexBuffer;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122;

/**
 * 1.12.2 {@code Minecraft.renderGlobal}. The world, sky and chunks are the modern level renderer's: block updates mark its sections
 * dirty, and the box helpers mods use for outlines draw lines through the emulated GL. Sky and chunk internals (display lists, VBOs)
 * have no modern counterpart and stay empty.
 */
public class RenderGlobal implements legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener {
    private static RenderGlobal instance;
    private final Minecraft mc;
    public int starGLCallList = -1;
    public int glSkyList = -1;
    public int glSkyList2 = -1;
    public VertexBuffer starVBO;
    public VertexBuffer skyVBO;
    public VertexBuffer sky2VBO;
    public final Set<Object> chunksToUpdate = new java.util.LinkedHashSet<>();
    public final List<Object> renderInfos = new java.util.ArrayList<>();
    public final Map<BlockPos, Object> mapSoundPositions = new java.util.HashMap<>();

    public RenderGlobal(Minecraft mcIn) {
        this.mc = mcIn;
    }

    /** The game's ({@code Minecraft.renderGlobal}). */
    public static synchronized RenderGlobal mcc$instance() {
        if (instance == null) instance = new RenderGlobal(Minecraft.getMinecraft());
        return instance;
    }

    @Override
    public void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager) {
    }

    /** Rebuilds every chunk (the modern renderer's allChanged). */
    public void loadRenderers() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();
    }

    public void setDisplayListEntitiesDirty() {
    }

    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        WorldClient1122.markRenderRange(x1 - 1, y1 - 1, z1 - 1, x2 + 1, y2 + 1, z2 + 1);
    }

    public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        WorldClient1122.markRenderRange(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    public void notifyLightSet(BlockPos pos) {
        notifyBlockUpdate(null, pos, null, null, 0);
    }

    /** Tile entities that render outside their chunk: the modern renderer tracks block entities itself. */
    public void updateTileEntities(Collection<?> tileEntitiesToRemove, Collection<?> tileEntitiesToAdd) {
    }

    public void spawnParticle(EnumParticleTypes particleIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        if (particleIn != null) Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(particleIn.getParticleID(), x, y, z, xSpeed, ySpeed, zSpeed, parameters);
    }

    public String getDebugInfoRenders() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.levelRenderer == null ? "" : mc.levelRenderer.getSectionStatistics();
    }

    public String getDebugInfoEntities() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.levelRenderer == null ? "" : mc.levelRenderer.getEntityStatistics();
    }

    /** The outline of the block under the crosshair, as 1.12 drew it (grown by 0.002, black at 40%). */
    public void drawSelectionBox(EntityPlayer player, RayTraceResult movingObjectPositionIn, int execute, float partialTicks) {
        if (execute != 0 || movingObjectPositionIn == null || movingObjectPositionIn.typeOfHit != RayTraceResult.Type.BLOCK) return;
        BlockPos pos = movingObjectPositionIn.getBlockPos();
        World world = Minecraft.mcc$get$world(Minecraft.getMinecraft());
        if (world == null || pos == null) return;
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR) return;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        double x = Entity.mcc$get$lastTickPosX(player) + (Entity.mcc$get$posX(player) - Entity.mcc$get$lastTickPosX(player)) * (double) partialTicks;
        double y = Entity.mcc$get$lastTickPosY(player) + (Entity.mcc$get$posY(player) - Entity.mcc$get$lastTickPosY(player)) * (double) partialTicks;
        double z = Entity.mcc$get$lastTickPosZ(player) + (Entity.mcc$get$posZ(player) - Entity.mcc$get$lastTickPosZ(player)) * (double) partialTicks;
        // the block's outline shape (its selected bounding box) from the modern level
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        var mp = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        var shape = level.getBlockState(mp).getShape(level, mp);
        if (shape.isEmpty()) return;
        var b = shape.bounds().move(mp);
        AxisAlignedBB box = new AxisAlignedBB(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
        drawSelectionBoundingBox(box.grow(0.0020000000949949026D).offset(-x, -y, -z), 0.0F, 0.0F, 0.0F, 0.4F);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawSelectionBoundingBox(AxisAlignedBB box, float red, float green, float blue, float alpha) {
        drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
    }

    public static void drawBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        // 1.12 drew a line strip; separate segments draw the same twelve edges through the line pipeline
        buffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        drawBoundingBox(buffer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        tessellator.draw();
    }

    /** Adds the twelve edges of the box to a buffer begun in GL_LINES mode. */
    public static void drawBoundingBox(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue,
            float alpha) {
        double[][] corners = {{minX, minY, minZ}, {maxX, minY, minZ}, {maxX, minY, maxZ}, {minX, minY, maxZ}, {minX, maxY, minZ}, {maxX, maxY, minZ}, {maxX, maxY, maxZ},
            {minX, maxY, maxZ}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            double[] a = corners[e[0]], b = corners[e[1]];
            buffer.pos(a[0], a[1], a[2]).color(red, green, blue, alpha).endVertex();
            buffer.pos(b[0], b[1], b[2]).color(red, green, blue, alpha).endVertex();
        }
    }

    /** A solid box (six quads) in one colour. */
    public static void renderFilledBox(AxisAlignedBB box, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        double[][] faces = {
            {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
            {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
            {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
        for (double[] f : faces) {
            for (int i = 0; i < 12; i += 3) buffer.pos(f[i], f[i + 1], f[i + 2]).color(red, green, blue, alpha).endVertex();
        }
        tessellator.draw();
    }
}
