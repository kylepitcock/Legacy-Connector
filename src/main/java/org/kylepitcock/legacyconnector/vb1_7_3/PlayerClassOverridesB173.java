package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A mod's own subclass of the Beta client player ({@code class EntityPlayerMoon extends EntityPlayerSP}), handed over through
 * {@code Minecraft.changeWorld(world, message, player)}. The layer's player is the native one, so the subclass is kept as the player's
 * <em>behaviour</em>: its overridden callbacks run where Beta ran them, on the state of the native player, and what they change goes back.
 * <ul>
 * <li>{@code moveEntityWithHeading} runs in place of the native travel of the local player (the client simulates its own movement);</li>
 * <li>{@code jump} runs on the native jump event of the local player;</li>
 * <li>{@code fall} runs for the native fall event of the player on the server, which owns the damage;</li>
 * <li>{@code onLivingUpdate} runs every tick of the server player (suit and air logic, damage).</li>
 * </ul>
 * The player the mod's own code reads ({@code mc.thePlayer}) is the subclass, so its {@code instanceof} checks see it.
 */
public final class PlayerClassOverridesB173 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final Map<UUID, ClientPlayerEntity> DONORS = new ConcurrentHashMap<>();
    private static final java.util.Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static boolean installed;

    private PlayerClassOverridesB173() {}

    private static boolean subclass(ClientPlayerEntity player) {
        return player != null && !(player instanceof PlayerViewB173) && player.getClass() != ClientPlayerEntity.class;
    }

    /** {@code Minecraft.changeWorld}: the actor of the running callback gets (or loses) its player subclass. */
    public static void hand(PlayerEntity handed) {
        Player actor = MenusB173.currentActor();
        if (actor == null && legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext() instanceof PlayerViewB173 view) actor = view.mcc$modern instanceof Player p ? p : null;
        if (actor == null) actor = MenusB173.localPlayer();
        if (actor == null) return;
        if (handed instanceof ClientPlayerEntity donor && subclass(donor)) {
            DONORS.put(actor.getUUID(), donor);
            donor.inventory = PlayerViewB173.of(actor).inventory;
            LOG.info("Original player class {} now drives {}", donor.getClass().getName(), actor.getGameProfile().getName());
        } else DONORS.remove(actor.getUUID());
    }

    /** What the mod's own code sees as the player: its subclass while one is handed over. */
    public static ClientPlayerEntity present(ClientPlayerEntity player) {
        if (DONORS.isEmpty() || !(player instanceof PlayerViewB173) || !(player.mcc$modern instanceof Player modern)) return player;
        var donor = DONORS.get(modern.getUUID());
        if (donor == null) return player;
        synchronized (donor) { bind(donor, modern, (PlayerViewB173) player); }
        return donor;
    }

    /** True for the handed-over player subclass object itself. */
    public static boolean drives(ClientPlayerEntity player) { return !DONORS.isEmpty() && DONORS.containsValue(player); }

    public static boolean active(Player player) { return !DONORS.isEmpty() && DONORS.containsKey(player.getUUID()); }

    /** The donor's view of the native player: where it is, how it moves, what it carries. */
    private static void bind(ClientPlayerEntity donor, Player modern, PlayerViewB173 view) {
        donor.mcc$modern = modern;
        donor.world = World.mcc$wrap(modern.level());
        donor.dimensionId = view == null ? donor.dimensionId : view.dimensionId;
        donor.name = modern.getGameProfile().getName();
        donor.standingEyeHeight = view == null ? donor.standingEyeHeight : view.standingEyeHeight;
        donor.setPosition(modern.getX(), modern.getY() + donor.standingEyeHeight, modern.getZ());
        donor.prevX = donor.x; donor.prevY = donor.y; donor.prevZ = donor.z;
        donor.yaw = modern.getYRot(); donor.pitch = modern.getXRot();
        var motion = modern.getDeltaMovement();
        donor.velocityX = motion.x; donor.velocityY = motion.y; donor.velocityZ = motion.z;
        donor.onGround = modern.onGround();
        donor.mcc$fallDistance(modern.fallDistance);
        donor.health = Math.round(modern.getHealth());
        donor.air = modern.getAirSupply();
        donor.fireTicks = modern.getRemainingFireTicks();
        donor.dead = !modern.isAlive();
        if (view != null) donor.inventory = view.inventory;
    }

    private static void run(Player modern, String hook, java.util.function.Consumer<ClientPlayerEntity> action, boolean writeBackMotion, boolean writeBackState) {
        var donor = DONORS.get(modern.getUUID());
        if (donor == null) return;
        synchronized (donor) {
            var previous = donor.mcc$modern;
            PlayerViewB173 view = writeBackState || modern instanceof ServerPlayer ? PlayerViewB173.of(modern) : null;
            try (var context = legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(modern.level()), donor)) {
                bind(donor, modern, view);
                action.accept(donor);
                if (writeBackMotion) modern.setDeltaMovement(donor.velocityX, donor.velocityY, donor.velocityZ);
                if (writeBackState) {
                    // the subclass's own changes (damage through its callbacks, air, fall distance) belong to the native player
                    if (donor.mcc$fallDistance() != modern.fallDistance) modern.fallDistance = donor.mcc$fallDistance();
                    if (donor.air != modern.getAirSupply()) modern.setAirSupply(donor.air);
                    if (view != null) view.writeBack();
                }
            } catch (RuntimeException | LinkageError error) {
                if (REPORTED.add(donor.getClass().getName() + "#" + hook))
                    LOG.error("Original player class {} failed in {}; it keeps running without it", donor.getClass().getName(), hook, error);
            } finally {
                donor.mcc$modern = previous;
            }
        }
    }

    /** The mixin of {@code LivingEntity.travel}: true when the subclass's move-with-heading ran instead of the native one. */
    public static boolean travel(Player player, Vec3 input) {
        if (DONORS.isEmpty() || !player.level().isClientSide || !DONORS.containsKey(player.getUUID())) return false;
        boolean[] ran = {false};
        run(player, "moveEntityWithHeading", donor -> { donor.travel((float) input.x, (float) input.z); ran[0] = true; }, true, false);
        return ran[0];
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) -> {
            if (event.getEntity() instanceof Player player && player.level().isClientSide && active(player))
                run(player, "jump", donor -> donor.mcc$jump(), true, false);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingFallEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || !active(player)) return;
            event.setCanceled(true);
            float distance = event.getDistance();
            run(player, "fall", donor -> donor.mcc$onLanding(distance), false, true);
        });
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player && active(player)) run(player, "livingUpdate", donor -> donor.tickMovement(), false, true);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) -> DONORS.remove(event.getEntity().getUUID()));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> DONORS.clear());
    }
}
