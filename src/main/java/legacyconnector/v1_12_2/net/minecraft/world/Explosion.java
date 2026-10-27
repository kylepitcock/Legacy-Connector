package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/**
 * 1.12.2 explosion over a modern {@link net.minecraft.world.level.Explosion}: {@code doExplosionA} finds the blocks and hurts
 * entities ({@code explode}), {@code doExplosionB} breaks the blocks, sets fires and, on the server, shows the explosion to
 * nearby players (1.12's {@code WorldServer.newExplosion} sent the same packet). The affected block list is live both ways: a
 * mod that edits it between the two steps changes what breaks.
 */
public class Explosion {
    public final boolean causesFire;
    public final boolean damagesTerrain;
    public final Random random = new Random();
    public final World world;
    public final double x;
    public final double y;
    public final double z;
    public final Entity exploder;
    public final float size;
    public final List<BlockPos> affectedBlockPositions = new ArrayList<>();
    public final Map<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer, Vec3d> playerKnockbackMap = new java.util.HashMap<>();
    private final Vec3d position;
    private net.minecraft.world.level.Explosion mcc$modern;

    public Explosion(World world, Entity entity, double x, double y, double z, float size, List<BlockPos> affectedPositions) {
        this(world, entity, x, y, z, size, false, true, affectedPositions);
    }

    public Explosion(World world, Entity entity, double x, double y, double z, float size, boolean flaming, boolean damagesTerrain, List<BlockPos> affectedPositions) {
        this(world, entity, x, y, z, size, flaming, damagesTerrain);
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

    /** A legacy view of an explosion the modern game (or {@code World.newExplosion}) made. */
    public static Explosion mcc$of(World world, net.minecraft.world.level.Explosion modern) {
        var c = modern.center();
        Entity source = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern.getDirectSourceEntity());
        Explosion e = new Explosion(world, source, c.x, c.y, c.z, modern.radius(), false,
                modern.getBlockInteraction() != net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
        e.mcc$modern = modern;
        e.mcc$pull();
        return e;
    }

    public net.minecraft.world.level.Explosion mcc$modern() {
        if (mcc$modern == null && world != null && world.mcc$level() != null) {
            var level = world.mcc$level();
            var interaction = !damagesTerrain ? net.minecraft.world.level.Explosion.BlockInteraction.KEEP
                    : level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_TNT_EXPLOSION_DROP_DECAY)
                            ? net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                            : net.minecraft.world.level.Explosion.BlockInteraction.DESTROY;
            List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
            for (BlockPos p : affectedBlockPositions) positions.add(p.mcc$modern());
            mcc$modern = new net.minecraft.world.level.Explosion(level, exploder == null ? null : exploder.mcc$entity, x, y, z, size, causesFire, interaction,
                    positions);
        }
        return mcc$modern;
    }

    /** Copies the modern explosion's blocks and knocked-back players into the legacy fields. */
    private void mcc$pull() {
        affectedBlockPositions.clear();
        playerKnockbackMap.clear();
        if (mcc$modern == null) return;
        for (var p : mcc$modern.getToBlow()) affectedBlockPositions.add(new BlockPos(p.getX(), p.getY(), p.getZ()));
        mcc$modern.getHitPlayers().forEach((player, v) -> {
            if (org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(player) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p) {
                playerKnockbackMap.put(p, new Vec3d(v.x, v.y, v.z));
            }
        });
    }

    /** Hands the (possibly edited) legacy block list to the modern explosion. */
    private void mcc$push() {
        if (mcc$modern == null) return;
        mcc$modern.clearToBlow();
        for (BlockPos p : affectedBlockPositions) mcc$modern.getToBlow().add(p.mcc$modern());
    }

    public void doExplosionA() {
        var modern = mcc$modern();
        if (modern == null) return;
        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(world.mcc$level(), modern)) return;
        modern.explode();
        mcc$pull();
    }

    public void doExplosionB(boolean spawnParticles) {
        var modern = mcc$modern();
        if (modern == null) return;
        mcc$push();
        var level = world.mcc$level();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            modern.finalizeExplosion(false);
            if (!modern.interactsWithBlocks()) modern.clearToBlow();
            if (spawnParticles) {
                for (var player : server.players()) {
                    if (player.distanceToSqr(x, y, z) < 4096.0D) {
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundExplodePacket(x, y, z, size, modern.getToBlow(),
                                modern.getHitPlayers().get(player), modern.getBlockInteraction(), modern.getSmallExplosionParticles(),
                                modern.getLargeExplosionParticles(), modern.getExplosionSound()));
                    }
                }
            }
        } else {
            modern.finalizeExplosion(spawnParticles);
        }
    }

    public Map<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer, Vec3d> getPlayerKnockbackMap() {
        return playerKnockbackMap;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase getExplosivePlacedBy() {
        if (exploder == null) return null;
        var modern = mcc$modern();
        if (modern != null && modern.getIndirectSourceEntity() != null) {
            var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern.getIndirectSourceEntity());
            if (view instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living) return living;
        }
        return exploder instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living ? living : null;
    }

    public void clearAffectedBlockPositions() {
        affectedBlockPositions.clear();
        if (mcc$modern != null) mcc$modern.clearToBlow();
    }

    public List<BlockPos> getAffectedBlockPositions() {
        return affectedBlockPositions;
    }

    public Vec3d getPosition() {
        return position;
    }
}
