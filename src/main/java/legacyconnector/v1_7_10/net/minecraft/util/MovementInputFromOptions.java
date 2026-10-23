package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings;

/** Keyboard movement input; the state itself is mirrored from the modern player (see EntityPlayerSP#mcc$syncInput). */
public class MovementInputFromOptions extends MovementInput {
    public MovementInputFromOptions(GameSettings settings) {
    }
}
