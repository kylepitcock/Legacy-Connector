package legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Minecraft 1.4.7 vanilla animated tile (TextureCompassFX): the game animates its own tiles, so the tile itself only exists for mods that construct or subclass it and
 * hand it to {@code registerTextureFX}. The needle state is real: {@code instance} is the compass tile (the last one made), {@code field_76868_i} its angle and
 * {@code field_76866_j} the angular velocity, which {@code func_82390_a} advances the way the game did (mods read them to draw their own needles, e.g. a locator item).
 */
public class TextureCompassFX extends TextureFX {
    /** {@code instance}: the compass tile. */
    public static TextureCompassFX instance;
    /** The needle angle (1.4.7 {@code currentAngle}). */
    public double field_76868_i;
    /** The needle's angular velocity (1.4.7 {@code angleDelta}). */
    public double field_76866_j;

    public TextureCompassFX() {
        super(54);
        instance = this;
    }

    /**
     * 1.4.7 {@code func_82390_a(x, z, yaw, noWorld, instant)} (static; the updateCompass of {@link #instance}): turns the needle towards the world spawn as seen from (x, z) facing
     * {@code yaw}; {@code noWorld} points it at 0 and {@code instant} skips the swing; a world without a surface spins it.
     */
    public static void func_82390_a(double x, double z, double yaw, boolean noWorld, boolean instant) {
        TextureCompassFX self = instance != null ? instance : new TextureCompassFX();
        double target = 0.0D;
        World world = Minecraft.mcc$get$theWorld(Minecraft.getMinecraft());
        if (world != null && !noWorld) {
            var spawn = world.getSpawnPoint();
            double dx = spawn.posX - x;
            double dz = spawn.posZ - z;
            yaw %= 360.0D;
            target = -((yaw - 90.0D) * Math.PI / 180.0D - Math.atan2(dz, dx));
            if (!World.mcc$get$provider(world).isSurfaceWorld()) target = Math.random() * Math.PI * 2.0D;
        }
        if (instant) {
            self.field_76868_i = target;
            return;
        }
        double d = target - self.field_76868_i;
        while (d < -Math.PI) d += Math.PI * 2.0D;
        while (d >= Math.PI) d -= Math.PI * 2.0D;
        if (d < -1.0D) d = -1.0D;
        if (d > 1.0D) d = 1.0D;
        self.field_76866_j += d * 0.1D;
        self.field_76866_j *= 0.8D;
        self.field_76868_i += self.field_76866_j;
    }
}
