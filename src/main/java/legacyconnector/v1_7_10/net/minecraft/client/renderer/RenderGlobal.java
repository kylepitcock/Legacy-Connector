package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/** The world renderer; mods only ask it to redraw blocks after their tile entities changed. */
public class RenderGlobal {
    /**
     * Block-breaking progress by breaker id: a live view of the modern renderer's (mods find the 1.7.10 field by reflection and
     * read the cracking blocks from it).
     */
    private final java.util.Map damagedBlocks = new DamagedBlocks();
    /** Tile entities 1.7.10 rendered this frame (its chunk renderers filled it); the modern renderer keeps its own. */
    public java.util.List tileEntities = new java.util.ArrayList();
    /** 1.7.10's chunk section renderers: the modern renderer has its own, so this is empty. */
    private WorldRenderer[] worldRenderers = new WorldRenderer[0];
    private java.util.List worldRenderersToUpdate = new java.util.ArrayList();
    public void markBlockForUpdate(int x, int y, int z) {
        markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    public void markBlockForRenderUpdate(int x, int y, int z) {
        markBlockForUpdate(x, y, z);
    }

    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        markBlocksForUpdate(x1 - 1, y1 - 1, z1 - 1, x2 + 1, y2 + 1, z2 + 1);
    }

    /** Client side of World.markBlockForUpdate / markBlockRangeForRenderUpdate. */
    public static void mcc$dirty(int x1, int y1, int z1, int x2, int y2, int z2) {
        var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
        if (renderer != null) renderer.setBlocksDirty(x1, y1, z1, x2, y2, z2);
    }

    public void markBlocksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
        if (renderer != null) renderer.setBlocksDirty(x1, y1, z1, x2, y2, z2);
    }

    /** Rebuilds every chunk's geometry (mods call it after changing how their blocks draw, e.g. a config toggle). */
    public void loadRenderers() {
        var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
        if (renderer != null) renderer.allChanged();
    }

    public legacyconnector.v1_7_10.net.minecraft.client.particle.EntityFX doSpawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        return null;
    }

    public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        doSpawnParticle(name, x, y, z, vx, vy, vz);
    }

    /** Shows (stage 0-9) or clears (any other stage) the cracks of a block being broken by {@code breakerId}. */
    public void destroyBlockPartially(int breakerId, int x, int y, int z, int stage) {
        var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
        if (renderer != null) renderer.destroyBlockProgress(breakerId, new net.minecraft.core.BlockPos(x, y, z), stage);
    }

    /** The modern renderer tracks block entities and entities itself. */
    public void onStaticEntitiesChanged() {
    }

    /** Chunk sections are rebuilt by the modern renderer; nothing is left waiting here. */
    public boolean updateRenderers(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase viewer, boolean force) {
        return true;
    }

    /**
     * Draws the vanilla sky (mods' sky renderers call it before or after their own layers): the modern renderer draws it when no
     * legacy sky replaces it, so there is nothing to add here.
     */
    public void renderSky(float partialTicks) {
    }

    /** Draws the 12 edges of a box with the Tessellator ({@code color} RGB, or -1 to keep the current colour). */
    public static void drawOutlinedBoundingBox(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box, int color) {
        Tessellator t = Tessellator.instance;
        t.startDrawing(3);
        if (color != -1) t.setColorOpaque_I(color);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.draw();
        t.startDrawing(3);
        if (color != -1) t.setColorOpaque_I(color);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.draw();
        t.startDrawing(1);
        if (color != -1) t.setColorOpaque_I(color);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.draw();
    }

    /** The outline of the block the player looks at (pass 0 only), in translucent black, relative to the player's eye position. */
    public void drawSelectionBox(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition hit, int pass, float partialTicks) {
        if (pass != 0 || hit == null || hit.typeOfHit != legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) return;
        var world = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$theWorld(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft());
        if (world == null) return;
        var block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
        if (block.getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.air) return;
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        block.setBlockBoundsBasedOnState(world, hit.blockX, hit.blockY, hit.blockZ);
        var e = (legacyconnector.v1_7_10.net.minecraft.entity.Entity) player;
        double x = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$lastTickPosX(e)
                + (legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(e) - legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$lastTickPosX(e)) * partialTicks;
        double y = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$lastTickPosY(e)
                + (legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(e) - legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$lastTickPosY(e)) * partialTicks;
        double z = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$lastTickPosZ(e)
                + (legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(e) - legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$lastTickPosZ(e)) * partialTicks;
        var box = block.getSelectedBoundingBoxFromPool(world, hit.blockX, hit.blockY, hit.blockZ);
        if (box != null) drawOutlinedBoundingBox(box.expand(0.002D, 0.002D, 0.002D).getOffsetBoundingBox(-x, -y, -z), -1);
    }

    /** {@link #damagedBlocks}: the modern renderer's blocks being broken, as 1.7.10 progress entries (read only). */
    private static final class DamagedBlocks extends java.util.AbstractMap<Integer, DestroyBlockProgress> {
        private static java.lang.reflect.Field field;
        private static boolean failed;

        @SuppressWarnings("unchecked")
        private static it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraft.server.level.BlockDestructionProgress> modern() {
            var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
            if (renderer == null || failed) return null;
            try {
                if (field == null) {
                    field = net.minecraft.client.renderer.LevelRenderer.class.getDeclaredField("destroyingBlocks");
                    field.setAccessible(true);
                }
                return (it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraft.server.level.BlockDestructionProgress>) field.get(renderer);
            } catch (ReflectiveOperationException | RuntimeException e) {
                failed = true;
                return null;
            }
        }

        private static DestroyBlockProgress legacy(net.minecraft.server.level.BlockDestructionProgress p) {
            var pos = p.getPos();
            DestroyBlockProgress out = new DestroyBlockProgress(p.getId(), pos.getX(), pos.getY(), pos.getZ());
            out.setPartialBlockDamage(p.getProgress());
            out.setCloudUpdateTick(p.getUpdatedRenderTick());
            return out;
        }

        @Override
        public java.util.Set<Entry<Integer, DestroyBlockProgress>> entrySet() {
            var modern = modern();
            java.util.Map<Integer, DestroyBlockProgress> out = new java.util.LinkedHashMap<>();
            if (modern != null) {
                for (var p : new java.util.ArrayList<>(modern.values())) out.put(p.getId(), legacy(p));
            }
            return out.entrySet();
        }

        @Override
        public DestroyBlockProgress get(Object key) {
            var modern = modern();
            if (modern == null || !(key instanceof Integer id)) return null;
            var p = modern.get(id.intValue());
            return p == null ? null : legacy(p);
        }

        @Override
        public DestroyBlockProgress put(Integer key, DestroyBlockProgress value) {
            if (value != null) {
                var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
                if (renderer != null) {
                    renderer.destroyBlockProgress(key, new net.minecraft.core.BlockPos(value.getPartialBlockX(), value.getPartialBlockY(), value.getPartialBlockZ()),
                            value.getPartialBlockDamage());
                }
            }
            return null;
        }

        @Override
        public DestroyBlockProgress remove(Object key) {
            var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
            if (renderer != null && key instanceof Integer id) renderer.destroyBlockProgress(id, net.minecraft.core.BlockPos.ZERO, -1);
            return null;
        }
    }
}
