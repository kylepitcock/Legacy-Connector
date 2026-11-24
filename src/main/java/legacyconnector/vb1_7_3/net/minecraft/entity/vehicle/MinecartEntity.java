package legacyconnector.vb1_7_3.net.minecraft.entity.vehicle;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta's minecart. The game's own carts are shown to mods as this type ({@code type}: 0 empty, 1 chest, 2 furnace; the wobble fields as
 * of the moment the view was made). A plain {@code new MinecartEntity(world, x, y, z, type)} spawned into a world becomes a native cart; a mod's
 * own subclass is hosted as a neutral entity running its callbacks.
 */
public class MinecartEntity extends Entity {
    public int type;
    public int damageWobbleTicks, damageWobbleStrength, damageWobbleSide = 1;

    /** Beta's {@code field_855_j}: the two end points of each rail shape, as offsets from the block centre. */
    private static final int[][][] RAIL_ENDS = {
        {{0, 0, -1}, {0, 0, 1}}, {{-1, 0, 0}, {1, 0, 0}}, {{-1, -1, 0}, {1, 0, 0}}, {{-1, 0, 0}, {1, -1, 0}}, {{0, 0, -1}, {0, -1, 1}},
        {{0, -1, -1}, {0, 0, 1}}, {{0, 0, 1}, {1, 0, 0}}, {{0, 0, 1}, {-1, 0, 0}}, {{0, 0, -1}, {-1, 0, 0}}, {{0, 0, -1}, {1, 0, 0}}};

    public MinecartEntity(World world) {
        super(world);
        setBoundingBoxSpacing(0.98F, 0.7F);
        standingEyeHeight = height / 2.0F;
    }

    public MinecartEntity(World world, double x, double y, double z, int type) {
        this(world);
        setPosition(x, y + standingEyeHeight, z);
        prevX = x;
        prevY = y;
        prevZ = z;
        this.type = type;
    }

    private static boolean isRail(int id) { return id == 66 || id == 27 || id == 28; }

    /** Beta's {@code func_514_g}: the point on the rail under (x, y, z) closest to it, or null when no rail is there. */
    public Vec3d snapPositionToRail(double x, double y, double z) {
        int i = (int) Math.floor(x), j = (int) Math.floor(y), k = (int) Math.floor(z);
        if (isRail(world.getBlockId(i, j - 1, k))) j--;
        if (!isRail(world.getBlockId(i, j, k))) return null;
        int shape = world.getBlockMeta(i, j, k) & (world.getBlockId(i, j, k) == 66 ? 15 : 7);
        if (shape < 0 || shape >= RAIL_ENDS.length) return null;
        int[][] ends = RAIL_ENDS[shape];
        double d4 = i + 0.5 + ends[0][0] * 0.5, d5 = j + 0.5 + ends[0][1] * 0.5, d6 = k + 0.5 + ends[0][2] * 0.5;
        double d7 = i + 0.5 + ends[1][0] * 0.5, d8 = j + 0.5 + ends[1][1] * 0.5, d9 = k + 0.5 + ends[1][2] * 0.5;
        double dx = d7 - d4, dy = (d8 - d5) * 2.0, dz = d9 - d6, along;
        if (dx == 0.0) { x = i + 0.5; along = z - k; }
        else if (dz == 0.0) { z = k + 0.5; along = x - i; }
        else { along = ((x - d4) * dx + (z - d6) * dz) * 2.0; }
        x = d4 + dx * along;
        y = d5 + dy * along;
        z = d6 + dz * along;
        if (dy < 0.0) y += 1.0;
        if (dy > 0.0) y += 0.5;
        return Vec3d.create(x, y, z);
    }
}
