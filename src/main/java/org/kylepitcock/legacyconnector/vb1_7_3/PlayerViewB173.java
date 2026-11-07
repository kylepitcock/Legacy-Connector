package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * A modern player as a b1.7.3 {@code EntityPlayer} for block callbacks: before a callback {@link #sync} copies position, rotation, sneaking,
 * health and the 36 main inventory slots (as legacy stacks); afterwards {@link #writeBack} gives the modern player whatever the mod did to
 * those slots (a used-up item, a filled bucket). One view per player.
 */
public final class PlayerViewB173 extends PlayerEntity {
    /**
     * Keyed by identity: {@code Entity.equals} compares entity ids, and in single player the client's and the server's player have the same
     * id (a WeakHashMap gave the server the client player's view, so OpenGUI saw a client player and no menu opened).
     */
    private static final Map<Player, PlayerViewB173> VIEWS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private final Player player;
    private final ItemStack[] before = new ItemStack[36];

    private PlayerViewB173(Player player) {
        super(World.mcc$wrap(player.level()));
        this.player = player;
        this.mcc$modern = player;
    }

    public static PlayerViewB173 of(Player player) {
        PlayerViewB173 view;
        synchronized (VIEWS) {
            view = VIEWS.computeIfAbsent(player, PlayerViewB173::new);
        }
        view.sync();
        return view;
    }

    private void sync() {
        world = World.mcc$wrap(player.level());
        prevX = x = player.getX();
        y = player.getY() + standingEyeHeight;
        prevY = y;
        prevZ = z = player.getZ();
        yaw = prevYaw = player.getYRot();
        pitch = prevPitch = player.getXRot();
        onGround = player.onGround();
        health = Math.round(player.getHealth());
        name = player.getGameProfile().getName();
        dead = !player.isAlive();
        inventory.selectedSlot = player.getInventory().selected;
        for (int i = 0; i < 36; i++) {
            ItemStack legacy = StacksB173.toLegacy(player.getInventory().items.get(i));
            inventory.main[i] = legacy;
            before[i] = legacy == null ? null : legacy.copy();
        }
    }

    /** Applies the mod's changes of the main inventory slots to the modern player. */
    public void writeBack() {
        for (int i = 0; i < 36; i++) {
            ItemStack now = inventory.main[i];
            ItemStack was = before[i];
            if (same(now, was)) continue;
            player.getInventory().items.set(i, now == null || now.count <= 0 ? net.minecraft.world.item.ItemStack.EMPTY : StacksB173.toModern(now));
            before[i] = now == null ? null : now.copy();
        }
    }

    private static boolean same(ItemStack a, ItemStack b) {
        if (a == null || a.count <= 0) return b == null || b.count <= 0;
        return b != null && a.itemId == b.itemId && a.count == b.count && a.getDamage() == b.getDamage();
    }

    @Override
    public boolean isSneaking() {
        return player.isShiftKeyDown();
    }

    @Override
    public void sendMessage(String message) {
        if (!player.level().isClientSide) player.sendSystemMessage(Component.literal(message));
    }
}
