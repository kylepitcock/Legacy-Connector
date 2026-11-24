package legacyconnector.vb1_7_3.net.minecraft.client.input;

/** Beta's {@code MovementInput}: the player's movement controls for this tick. */
public class Input {
    public float movementSideways, movementForward;
    public boolean jumping, sneaking;
    /** Reads the controls into the fields (a subclass reads the keys, a mod's input may read anything). */
    public void update(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {}
    /** Beta's {@code resetKeyState}: nothing is held. */
    public void reset() {
        movementSideways = 0.0F;
        movementForward = 0.0F;
        jumping = false;
        sneaking = false;
    }
    /** A key went down or up. */
    public void updateKey(int code, boolean pressed) {}
}
