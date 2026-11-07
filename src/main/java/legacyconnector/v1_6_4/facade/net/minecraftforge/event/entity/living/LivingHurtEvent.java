package legacyconnector.v1_6_4.facade.net.minecraftforge.event.entity.living;

/** Facade of the 1.7.10 shim's {@code LivingHurtEvent} for the int-damage era (1.5.2 and 1.4.7, and 1.6.4 mods built on them). Never instantiated. */
public final class LivingHurtEvent {
    private LivingHurtEvent() {}

    /** {@code event.ammount} as whole damage points (a half heart is 1). */
    public static int mcc$get$ammount(legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingHurtEvent self) {
        return (int) self.ammount;
    }

    public static void mcc$set$ammount(legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingHurtEvent self, int value) {
        self.ammount = value;
    }
}
