package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The camera's orientation as billboard rotation components (particles, labels and overlays that always face the viewer).
 * Updated once per frame from the modern camera (see RenderEvents1710).
 */
public class ActiveRenderInfo {
    /**
     * The camera's offset from the viewed entity's eye (1.7.10 unprojected the screen centre). Legacy code adds it to
     * {@code RenderManager.renderPosX..}, which here already is the modern camera position, so it stays 0.
     */
    public static float objectX;
    public static float objectY;
    public static float objectZ;
    public static float rotationX;
    public static float rotationXZ;
    public static float rotationZ;
    public static float rotationYZ;
    public static float rotationXY;
    /** 1.7.10's GL matrix read-back buffers (mods read them after updateRenderInfo; the emulation keeps them empty). */
    private static final java.nio.IntBuffer viewport = java.nio.ByteBuffer.allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
    private static final java.nio.FloatBuffer modelview = java.nio.ByteBuffer.allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private static final java.nio.FloatBuffer projection = java.nio.ByteBuffer.allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private static final java.nio.FloatBuffer objectCoords = java.nio.ByteBuffer.allocateDirect(3 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();

    public static void mcc$update(float yawDegrees, float pitchDegrees) {
        float yaw = yawDegrees * (float) Math.PI / 180.0F;
        float pitch = pitchDegrees * (float) Math.PI / 180.0F;
        rotationX = (float) Math.cos(yaw);
        rotationZ = (float) Math.sin(yaw);
        rotationYZ = -rotationZ * (float) Math.sin(pitch);
        rotationXY = rotationX * (float) Math.sin(pitch);
        rotationXZ = (float) Math.cos(pitch);
    }

    /**
     * Recomputes the billboard rotations from the player's look, mirrored for the front-facing third-person view
     * (as 1.7.10 did). The camera offset stays 0 (see {@link #objectX}).
     */
    public static void updateRenderInfo(EntityPlayer player, boolean mirror) {
        if (player == null || player.mcc$entity == null) return;
        int m = mirror ? 1 : 0;
        float yaw = player.mcc$entity.getYRot() * (float) Math.PI / 180.0F;
        float pitch = player.mcc$entity.getXRot() * (float) Math.PI / 180.0F;
        rotationX = (float) Math.cos(yaw) * (1 - m * 2);
        rotationZ = (float) Math.sin(yaw) * (1 - m * 2);
        rotationYZ = -rotationZ * (float) Math.sin(pitch) * (1 - m * 2);
        rotationXY = rotationX * (float) Math.sin(pitch) * (1 - m * 2);
        rotationXZ = (float) Math.cos(pitch);
    }

    /**
     * Where the entity's view starts: its interpolated eye position plus the camera offset. When the entity is the camera
     * entity, the modern camera's own position (third-person distance included).
     */
    public static Vec3 projectViewFromEntity(EntityLivingBase entity, double partialTicks) {
        var modern = entity == null ? null : entity.mcc$entity;
        if (modern == null) return Vec3.createVectorHelper(0, 0, 0);
        var mc = net.minecraft.client.Minecraft.getInstance();
        var camera = mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
        net.minecraft.world.phys.Vec3 eye = camera != null && camera.isInitialized() && camera.getEntity() == modern
                ? camera.getPosition()
                : modern.getEyePosition((float) partialTicks);
        return Vec3.createVectorHelper(eye.x + objectX, eye.y + objectY, eye.z + objectZ);
    }

    /**
     * The block the view starts in (fog handlers colour the fog by it: water, lava, mod fluids). Inside a fluid block above
     * its surface, the block above, as 1.7.10 did.
     */
    public static Block getBlockAtEntityViewpoint(World world, EntityLivingBase entity, float partialTicks) {
        Vec3 view = projectViewFromEntity(entity, partialTicks);
        int x = net.minecraft.util.Mth.floor(view.xCoord);
        int y = net.minecraft.util.Mth.floor(view.yCoord);
        int z = net.minecraft.util.Mth.floor(view.zCoord);
        if (world == null) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        try {
            var level = world.mcc$level();
            if (level != null) {
                var pos = new net.minecraft.core.BlockPos(x, y, z);
                var fluid = level.getFluidState(pos);
                if (!fluid.isEmpty() && view.yCoord >= y + fluid.getHeight(level, pos)) return world.getBlock(x, y + 1, z);
            }
            Block block = world.getBlock(x, y, z);
            return block != null ? block : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        } catch (RuntimeException e) {
            return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        }
    }
}
