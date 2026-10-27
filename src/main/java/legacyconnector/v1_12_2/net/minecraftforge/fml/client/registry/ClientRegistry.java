package legacyconnector.v1_12_2.net.minecraftforge.fml.client.registry;

/** FML's client registrations that are not rendering: key bindings (they appear in the controls screen). */
public class ClientRegistry {
    public static void registerKeyBinding(legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding key) {
        if (key != null) legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding.mcc$register(key);
    }
}
