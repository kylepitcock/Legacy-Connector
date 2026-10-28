package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/**
 * A 1.12.2 explosion, carried out by a modern one: {@link #doExplosionA} finds the blocks and hurts entities, {@link #doExplosionB}
 * breaks the blocks, plays the sound and spawns the particles (and fire). In a generation region nothing explodes.
 */
public class Explosion {
    private final boolean causesFire;
    private final boolean damagesTerrain;
    private final World world;
    private final double x;
    private final double y;
    private final double z;
    private final Entity exploder;
    private final float size;
    private final List<BlockPos> affectedBlockPositions = new ArrayList<>();
    private final Map<EntityPlayer, Vec3d> playerKnockbackMap = new java.util.HashMap<>();
    private final Vec3d position;
    private net.minecraft.world.level.Explosion modern;

    public Explosion(World world, Entity entity, double x, double y, double z, float size, List<BlockPos> affectedPositions) {
        this(world, entity, x, y, z, size, false, true, affectedPositions);
    }

    public Explosion(World world, Entity entity, double x, double y, double z, float size, boolean causesFire, boolean damagesTerrain,
            List<BlockPos> affectedPositions) {
        this(world, entity, x, y, z, size, causesFire, damagesTerrain);
        if (affectedPositions != null) affectedBlockPositions.addAll(affectedPositions);
    }

    public Explosion(World world, Entity entity, double x, double y, double z, float size, boolean flaming, boolean damagesTerrain) {
        this.world = world;
        this.exploder = entity;
        this.size = size;
        this.x = x;
        this.y = y;
        this.z = z;
        this.causesFire = flaming;
        this.damagesTerrain = damagesTerrain;
        this.position = new Vec3d(x, y, z);
    }

    private Explosion(World world, Entity entity, net.minecraft.world.level.Explosion modern) {
        this(world, entity, modern.center().x, modern.center().y, modern.center().z, modern.radius(), false, modern.interactsWithBlocks());
        this.modern = modern;
        for (var p : modern.getToBlow()) affectedBlockPositions.add(BlockPos.mcc$of(p));
    }

    /** The legacy view of an explosion the modern level carried out. */
    public static Explosion mcc$of(World world, Entity entity, net.minecraft.world.level.Explosion modern) {
        return modern == null ? new Explosion(world, entity, 0, 0, 0, 0, false, false) : new Explosion(world, entity, modern);
    }

    private net.minecraft.world.level.Explosion modern() {
        if (modern == null) {
            net.minecraft.world.level.Level level = world == null ? null : world.mcc$liveLevel();
            if (level == null) return null;
            var source = exploder == null ? null : exploder.mcc$modern();
            var interaction = damagesTerrain ? net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                    : net.minecraft.world.level.Explosion.BlockInteraction.KEEP;
            List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
            for (BlockPos p : affectedBlockPositions) positions.add(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
            modern = new net.minecraft.world.level.Explosion(level, source, x, y, z, size, causesFire, interaction, positions);
        }
        return modern;
    }

    /** Finds the blocks to destroy and hurts and pushes the entities in range. */
    public void doExplosionA() {
        var m = modern();
        if (m == null) return;
        m.explode();
        affectedBlockPositions.clear();
        for (var p : m.getToBlow()) affectedBlockPositions.add(BlockPos.mcc$of(p));
        playerKnockbackMap.clear();
        for (var e : m.getHitPlayers().entrySet()) {
            var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e.getKey());
            if (view instanceof EntityPlayer p) playerKnockbackMap.put(p, Vec3d.mcc$of(e.getValue()));
        }
    }

    /** Breaks the affected blocks (and sets fire), with the sound and particles. */
    public void doExplosionB(boolean spawnParticles) {
        var m = modern();
        if (m == null) return;
        if (m.getToBlow().size() != affectedBlockPositions.size()) {
            m.clearToBlow();
            for (BlockPos p : affectedBlockPositions) m.getToBlow().add(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
        }
        m.finalizeExplosion(spawnParticles);
    }

    public Map<EntityPlayer, Vec3d> getPlayerKnockbackMap() {
        return playerKnockbackMap;
    }

    public EntityLivingBase getExplosivePlacedBy() {
        if (exploder instanceof EntityLivingBase living) return living;
        var m = modern;
        var indirect = m == null ? null : m.getIndirectSourceEntity();
        return indirect == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(indirect) instanceof EntityLivingBase l ? l : null;
    }

    public void clearAffectedBlockPositions() {
        affectedBlockPositions.clear();
        if (modern != null) modern.clearToBlow();
    }

    public List<BlockPos> getAffectedBlockPositions() {
        return affectedBlockPositions;
    }

    public Vec3d getPosition() {
        return position;
    }

    public float mcc$size() {
        return size;
    }

    public Entity mcc$exploder() {
        return exploder;
    }

    /** The explosion's random (1.12 used it for particles and block drops). */
    public final java.util.Random random = new java.util.Random();
}
