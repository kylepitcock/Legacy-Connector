package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10's compass sprite: a frame per needle angle, the needle swinging towards the world spawn with damping. Mods extend it
 * for locator items (pointing at an entity or a structure instead) and drive it themselves ({@link #updateCompass}).
 * A new frame is copied into the sprite's place in the modern block atlas.
 */
public class TextureCompass extends TextureAtlasSprite {
    public double currentAngle;
    public double angleDelta;

    public TextureCompass(String name) {
        super(name);
    }

    @Override
    public void updateAnimation() {
        var mc = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
        var world = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$theWorld(mc);
        var player = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$thePlayer(mc);
        if (world != null && player != null && player.mcc$entity != null) {
            updateCompass(world, player.mcc$entity.getX(), player.mcc$entity.getZ(), player.mcc$entity.getYRot(), false, false);
        } else {
            updateCompass(null, 0.0D, 0.0D, 0.0D, true, false);
        }
    }

    /**
     * Turns the needle towards the spawn as seen from (x, z) facing {@code yaw}; spins randomly outside surface worlds.
     * {@code noWorld} points it at angle 0, {@code instant} skips the swing.
     */
    public void updateCompass(World world, double x, double z, double yaw, boolean noWorld, boolean instant) {
        if (framesTextureData.isEmpty()) return;
        double target = 0.0D;
        if (world != null && !noWorld) {
            var spawn = world.getSpawnPoint();
            double dx = spawn.posX - x;
            double dz = spawn.posZ - z;
            yaw %= 360.0D;
            target = -((yaw - 90.0D) * Math.PI / 180.0D - Math.atan2(dz, dx));
            if (!World.mcc$get$provider(world).isSurfaceWorld()) target = Math.random() * Math.PI * 2.0D;
        }
        if (instant) {
            currentAngle = target;
        } else {
            double d = target - currentAngle;
            while (d < -Math.PI) d += Math.PI * 2.0D;
            while (d >= Math.PI) d -= Math.PI * 2.0D;
            if (d < -1.0D) d = -1.0D;
            if (d > 1.0D) d = 1.0D;
            angleDelta += d * 0.1D;
            angleDelta *= 0.8D;
            currentAngle += angleDelta;
        }
        int frames = framesTextureData.size();
        int frame = (int) ((currentAngle / (Math.PI * 2.0D) + 1.0D) * frames) % frames;
        while (frame < 0) frame = (frame + frames) % frames;
        if (frame != frameCounter) {
            frameCounter = frame;
            mcc$upload((int[][]) framesTextureData.get(frameCounter));
        }
    }

    /** Copies a frame into this sprite's area of the modern block atlas (its top mip level). */
    protected void mcc$upload(int[][] frame) {
        if (frame == null || frame.length == 0 || frame[0] == null || !com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return;
        try {
            var rl = net.minecraft.resources.ResourceLocation.tryParse(iconName);
            if (rl == null) return;
            var atlas = net.minecraft.client.Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
            var sprite = atlas.getSprite(rl);
            var contents = sprite.contents();
            if (contents.width() != width || contents.height() != height || frame[0].length < width * height) return;
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(atlas.getId());
            TextureUtil.uploadTextureMipmap(new int[][] {frame[0]}, width, height, sprite.getX(), sprite.getY(), false, false);
        } catch (RuntimeException e) {
            // the atlas is being rebuilt: the next frame change uploads again
        }
    }
}
