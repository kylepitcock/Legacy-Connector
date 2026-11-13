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
    private static final String FAMILY = "b1.7.3-player-class";

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
            sendClass(actor, donor.getClass().getName());
            sendOverrides(actor);
        } else if (DONORS.remove(actor.getUUID()) != null) sendClass(actor, "");
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
        if (view != null) {
            // what the player's GUIs are built from: the very inventory, screen handlers and open screen of the native player's view
            donor.inventory = view.inventory;
            donor.playerScreenHandler = view.playerScreenHandler;
            donor.currentScreenHandler = view.currentScreenHandler;
            donor.mcc$inventoryScreen = view.mcc$inventoryScreen;
        }
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

    // A remote client simulates its own movement in its own process, which has the mod's classes too: it is told which class drives it and builds
    // its own instance. A single player's client shares this process (and this map), so it is told nothing.
    private static boolean remote(Player player) {
        return player instanceof ServerPlayer sp && sp.connection != null && !sp.server.isSingleplayerOwner(sp.getGameProfile())
                && sp.connection.hasChannel(org.kylepitcock.legacyconnector.core.network.LegacyPayload.TYPE);
    }

    private static void send(Player player, int kind, java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> body) {
        if (!remote(player)) return;
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            body.accept(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((ServerPlayer) player, new org.kylepitcock.legacyconnector.core.network.LegacyPayload(FAMILY, "state", kind, bytes));
        } finally { buf.release(); }
    }

    private static void sendClass(Player player, String className) { send(player, 0, buf -> buf.writeUtf(className, 512)); }

    /** A client that arrives (or is handed a player class) learns the texture overrides already in force. */
    private static void sendOverrides(Player player) {
        legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.overrides()
                .forEach((key, path) -> send(player, 1, buf -> { buf.writeUtf(key, 256); buf.writeUtf(path, 512); }));
    }

    /** Client: the server's player class becomes this player's own instance of that class. */
    private static void receiveClass(Player local, String className) {
        if (className.isEmpty()) { DONORS.remove(local.getUUID()); return; }
        for (var mod : ContentB173.loadedMods()) {
            try {
                Class<?> type = Class.forName(className, true, mod.getClass().getClassLoader());
                var ctor = type.getConstructor(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.class, World.class,
                        legacyconnector.vb1_7_3.net.minecraft.client.util.Session.class, int.class);
                var donor = (ClientPlayerEntity) ctor.newInstance(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance(), World.mcc$wrap(local.level()), null, 0);
                DONORS.put(local.getUUID(), donor);
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                LOG.warn("Original player class {} could not be made on this client: {}", className, error.toString());
                return;
            }
        }
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        org.kylepitcock.legacyconnector.core.network.LegacyNetwork.registerReceiver(FAMILY, (payload, context) -> {
            if (!context.flow().isClientbound() || payload.data().length > 4096) return;
            var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data()));
            try {
                if (payload.discriminator() == 0) {
                    String name = buf.readUtf(512);
                    context.enqueueWork(() -> receiveClass(context.player(), name));
                } else if (payload.discriminator() == 1) {
                    String key = buf.readUtf(256), path = buf.readUtf(512);
                    context.enqueueWork(() -> legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.mcc$remoteOverride(key, path));
                }
            } catch (RuntimeException ignored) { } finally { buf.release(); }
        });
        // a texture override the mod makes while its callback runs for a remote player belongs to that player's sky
        legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.LISTENER = (key, path) -> {
            Player actor = MenusB173.currentActor();
            if (actor != null) send(actor, 1, buf -> { buf.writeUtf(key, 256); buf.writeUtf(path == null ? "" : path, 512); });
        };
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
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> sendOverrides(event.getEntity()));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) -> DONORS.remove(event.getEntity().getUUID()));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> DONORS.clear());
    }
}
