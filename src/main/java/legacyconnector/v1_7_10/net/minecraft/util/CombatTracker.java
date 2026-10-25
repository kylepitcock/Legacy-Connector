package legacyconnector.v1_7_10.net.minecraft.util;

/** Remembers who damaged an entity recently (for death messages). The modern game keeps its own record; this one accepts the calls. */
public class CombatTracker {
    public void func_94547_a(DamageSource source, float health, float damage) {
    }

    public void func_94545_a() {
    }

    public IChatComponent func_151521_b() {
        return new ChatComponentText("");
    }
}
