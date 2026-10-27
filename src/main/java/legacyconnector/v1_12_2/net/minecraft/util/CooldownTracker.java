package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.item.Item;

/**
 * A player's item cooldowns: a view over the modern player's cooldowns (each legacy item cools down as the modern items it
 * became), or a standalone table for players without one.
 */
public class CooldownTracker {
    private final net.minecraft.world.item.ItemCooldowns modern;
    private final java.util.Map<Item, int[]> cooldowns = new java.util.HashMap<>();
    private int ticks;

    public CooldownTracker() {
        this(null);
    }

    protected CooldownTracker(net.minecraft.world.item.ItemCooldowns modern) {
        this.modern = modern;
    }

    /** The legacy view of a modern player's cooldowns. */
    public static CooldownTracker mcc$of(net.minecraft.world.item.ItemCooldowns modern) {
        return new CooldownTracker(modern);
    }

    private static net.minecraft.world.item.Item modernItem(Item item) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, 0);
    }

    public boolean hasCooldown(Item itemIn) {
        return getCooldown(itemIn, 0.0F) > 0.0F;
    }

    public float getCooldown(Item itemIn, float partialTicks) {
        if (modern != null) {
            var m = modernItem(itemIn);
            return m == null ? 0.0F : modern.getCooldownPercent(m, partialTicks);
        }
        int[] c = cooldowns.get(itemIn);
        if (c == null) return 0.0F;
        float total = (float) (c[1] - c[0]);
        float left = (float) c[1] - ((float) ticks + partialTicks);
        return Math.max(0.0F, Math.min(1.0F, left / total));
    }

    public void tick() {
        if (modern != null) return; // the modern player ticks its own
        ticks++;
        cooldowns.values().removeIf(c -> c[1] <= ticks);
    }

    public void setCooldown(Item itemIn, int ticksIn) {
        if (modern != null) {
            var m = modernItem(itemIn);
            if (m != null) modern.addCooldown(m, ticksIn);
            return;
        }
        cooldowns.put(itemIn, new int[] {ticks, ticks + ticksIn});
        notifyOnSet(itemIn, ticksIn);
    }

    public void removeCooldown(Item itemIn) {
        if (modern != null) {
            var m = modernItem(itemIn);
            if (m != null) modern.removeCooldown(m);
            return;
        }
        cooldowns.remove(itemIn);
        notifyOnRemove(itemIn);
    }

    protected void notifyOnSet(Item itemIn, int ticksIn) {
    }

    protected void notifyOnRemove(Item itemIn) {
    }
}
