package legacyconnector.v1_6_4.facade.net.minecraft.util;

/** Facade of the 1.7.10 shim's {@code DamageSource} for 1.5.2 mods: the death message as a string. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class DamageSource {
    private DamageSource() {}

    /** {@code getDeathMessage(victim)}: the (translated) text of the death message; 1.7.10 returns a chat component. */
    public static String getDeathMessage(legacyconnector.v1_7_10.net.minecraft.util.DamageSource self,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase victim) {
        return self.getDeathMessage(victim).getUnformattedText();
    }
}
