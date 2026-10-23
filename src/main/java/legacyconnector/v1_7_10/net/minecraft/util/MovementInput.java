package legacyconnector.v1_7_10.net.minecraft.util;

/** The local player's movement keys as 1.7.10 code reads them; refreshed from the modern input every client tick. */
public class MovementInput {
    public float moveStrafe;
    public float moveForward;
    public boolean jump;
    public boolean sneak;

    public void updatePlayerMoveState() {
    }
}
