package legacyconnector.vb1_7_3.net.minecraft.client.input;

import legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions;
import legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding;

/** Beta's {@code MovementInputFromOptions}: movement from the forward/back/left/right/jump/sneak keys. */
public class KeyboardInput extends Input {
    private final boolean[] held = new boolean[6];
    private final GameOptions options;

    public KeyboardInput(GameOptions options) { this.options = options; }

    private KeyBinding[] keys() { return new KeyBinding[]{options.forwardKey, options.backKey, options.leftKey, options.rightKey, options.jumpKey, options.sneakKey}; }

    @Override public void updateKey(int code, boolean pressed) {
        KeyBinding[] keys = keys();
        for (int i = 0; i < keys.length; i++) if (KeyBinding.mcc$get$code(keys[i]) == code) held[i] = pressed;
    }

    @Override public void update(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {
        KeyBinding[] keys = keys();
        for (int i = 0; i < keys.length; i++) held[i] = KeyBinding.mcc$get$pressed(keys[i]);
        movementSideways = 0.0F;
        movementForward = 0.0F;
        if (held[0]) movementForward += 1.0F;
        if (held[1]) movementForward -= 1.0F;
        if (held[2]) movementSideways += 1.0F;
        if (held[3]) movementSideways -= 1.0F;
        jumping = held[4];
        sneaking = held[5];
        if (sneaking) {
            movementSideways *= 0.3F;
            movementForward *= 0.3F;
        }
    }

    @Override public void reset() {
        super.reset();
        java.util.Arrays.fill(held, false);
    }
}
