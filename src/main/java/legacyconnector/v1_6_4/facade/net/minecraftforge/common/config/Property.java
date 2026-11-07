package legacyconnector.v1_6_4.facade.net.minecraftforge.common.config;

/**
 * Facade of the 1.7.10 shim's {@code Property} for the older versions: {@code property.value}, the public string field every Forge up to 1.6
 * read and wrote directly, is the value the shim keeps privately. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class Property {
    private Property() {}

    /** Property.value */
    public static String mcc$get$value(legacyconnector.v1_7_10.net.minecraftforge.common.config.Property self) {
        return self.getString();
    }

    public static void mcc$set$value(legacyconnector.v1_7_10.net.minecraftforge.common.config.Property self, String value) {
        self.set(value);
    }
}
