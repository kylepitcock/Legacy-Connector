package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 camera facts for the frame: the camera's offset from the viewing entity and the five rotation values particles and
 * billboards use, computed from the modern camera when asked.
 */
public class ActiveRenderInfo {
    /** Phantom-like: kept in step by {@link #getCameraPosition()}. */
    public static Vec3d position = new Vec3d(0.0D, 0.0D, 0.0D);

    private static net.minecraft.client.Camera camera() {
        return net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    /** The camera relative to the viewing entity's interpolated position (third person moves it back). */
    public static Vec3d getCameraPosition() {
        var camera = camera();
        var entity = camera.getEntity();
        if (entity == null) return position;
        float partial = net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        var at = entity.getPosition(partial);
        var cam = camera.getPosition();
        position = new Vec3d(cam.x - at.x, cam.y - at.y, cam.z - at.z);
        return position;
    }

    public static Vec3d projectViewFromEntity(Entity entityIn, double partialTicks) {
        double x = Entity.mcc$get$prevPosX(entityIn) + (Entity.mcc$get$posX(entityIn) - Entity.mcc$get$prevPosX(entityIn)) * partialTicks;
        double y = Entity.mcc$get$prevPosY(entityIn) + (Entity.mcc$get$posY(entityIn) - Entity.mcc$get$prevPosY(entityIn)) * partialTicks;
        double z = Entity.mcc$get$prevPosZ(entityIn) + (Entity.mcc$get$posZ(entityIn) - Entity.mcc$get$prevPosZ(entityIn)) * partialTicks;
        Vec3d p = getCameraPosition();
        return new Vec3d(x + p.x, y + p.y, z + p.z);
    }

    public static IBlockState getBlockStateAtEntityViewpoint(World worldIn, Entity entityIn, float partialTicks) {
        Vec3d view = projectViewFromEntity(entityIn, partialTicks);
        return worldIn.getBlockState(new BlockPos(view));
    }

    public static float getRotationX() {
        return (float) Math.cos(Math.toRadians(camera().getYRot()));
    }

    public static float getRotationZ() {
        return (float) Math.sin(Math.toRadians(camera().getYRot()));
    }

    public static float getRotationYZ() {
        return (float) (-getRotationZ() * Math.sin(Math.toRadians(camera().getXRot())));
    }

    public static float getRotationXY() {
        return (float) (getRotationX() * Math.sin(Math.toRadians(camera().getXRot())));
    }

    public static float getRotationXZ() {
        return (float) Math.cos(Math.toRadians(camera().getXRot()));
    }

    /**
     * 1.12 recomputed the camera facts from the player here each frame. The rotation values above are read from the modern camera when
     * asked, so this only refreshes {@link #position}.
     */
    public static void updateRenderInfo(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer entityplayerIn, boolean p_74583_1_) {
        if (net.minecraft.client.Minecraft.getInstance().gameRenderer != null) getCameraPosition();
    }
}
