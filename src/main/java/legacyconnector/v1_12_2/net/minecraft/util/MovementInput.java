package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * The client player's movement keys of this tick, as 1.12.2's plain holder ({@code MovementInputFromOptions} fills it from the
 * key bindings). The client player's live instance is the client bridge's to provide.
 */
public class MovementInput {
    public float moveStrafe;
    public float moveForward;
    public boolean forwardKeyDown;
    public boolean backKeyDown;
    public boolean leftKeyDown;
    public boolean rightKeyDown;
    public boolean jump;
    public boolean sneak;

    public void updatePlayerMoveState() {
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec2f getMoveVector() {
        return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec2f(moveStrafe, moveForward);
    }
}
