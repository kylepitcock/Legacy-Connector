package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * The local player's movement input ({@code player.movementInput}, {@code InputUpdateEvent}). The player's own is a live view of the game's
 * input (handlers that change it steer the player); one a mod makes keeps its values itself. The fields are phantoms.
 * (Created for area R2b's player and input event entries; area F owns the class.)
 */
public class MovementInput {
    /** The game's input this one views, or null for a mod's own. */
    private final net.minecraft.client.player.Input modern;
    private float localStrafe;
    private float localForward;
    private boolean localForwardKey;
    private boolean localBackKey;
    private boolean localLeftKey;
    private boolean localRightKey;
    private boolean localJump;
    private boolean localSneak;

    @Deprecated
    public float moveStrafe;
    @Deprecated
    public float moveForward;
    @Deprecated
    public boolean forwardKeyDown;
    @Deprecated
    public boolean backKeyDown;
    @Deprecated
    public boolean leftKeyDown;
    @Deprecated
    public boolean rightKeyDown;
    @Deprecated
    public boolean jump;
    @Deprecated
    public boolean sneak;

    public MovementInput() {
        this.modern = null;
    }

    protected MovementInput(net.minecraft.client.player.Input modern) {
        this.modern = modern;
    }

    /** The view of the game's input (not a 1.12 method). */
    public static MovementInput mcc$of(net.minecraft.client.player.Input modern) {
        return new MovementInput(modern);
    }

    /** Reads the keys (the game's input reads them itself). */
    public void updatePlayerMoveState() {
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec2f getMoveVector() {
        return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec2f(mcc$get$moveStrafe(this), mcc$get$moveForward(this));
    }

    public static float mcc$get$moveStrafe(MovementInput i) {
        return i.modern != null ? i.modern.leftImpulse : i.localStrafe;
    }

    public static void mcc$set$moveStrafe(MovementInput i, float v) {
        if (i.modern != null) i.modern.leftImpulse = v;
        else i.localStrafe = v;
    }

    public static float mcc$get$moveForward(MovementInput i) {
        return i.modern != null ? i.modern.forwardImpulse : i.localForward;
    }

    public static void mcc$set$moveForward(MovementInput i, float v) {
        if (i.modern != null) i.modern.forwardImpulse = v;
        else i.localForward = v;
    }

    public static boolean mcc$get$forwardKeyDown(MovementInput i) {
        return i.modern != null ? i.modern.up : i.localForwardKey;
    }

    public static void mcc$set$forwardKeyDown(MovementInput i, boolean v) {
        if (i.modern != null) i.modern.up = v;
        else i.localForwardKey = v;
    }

    public static boolean mcc$get$backKeyDown(MovementInput i) {
        return i.modern != null ? i.modern.down : i.localBackKey;
    }

    public static void mcc$set$backKeyDown(MovementInput i, boolean v) {
        if (i.modern != null) i.modern.down = v;
        else i.localBackKey = v;
    }

    public static boolean mcc$get$leftKeyDown(MovementInput i) {
        return i.modern != null ? i.modern.left : i.localLeftKey;
    }

    public static void mcc$set$leftKeyDown(MovementInput i, boolean v) {
        if (i.modern != null) i.modern.left = v;
        else i.localLeftKey = v;
    }

    public static boolean mcc$get$rightKeyDown(MovementInput i) {
        return i.modern != null ? i.modern.right : i.localRightKey;
    }

    public static void mcc$set$rightKeyDown(MovementInput i, boolean v) {
        if (i.modern != null) i.modern.right = v;
        else i.localRightKey = v;
    }

    public static boolean mcc$get$jump(MovementInput i) {
        return i.modern != null ? i.modern.jumping : i.localJump;
    }

    public static void mcc$set$jump(MovementInput i, boolean v) {
        if (i.modern != null) i.modern.jumping = v;
        else i.localJump = v;
    }

    public static boolean mcc$get$sneak(MovementInput i) {
        return i.modern != null ? i.modern.shiftKeyDown : i.localSneak;
    }

    public static void mcc$set$sneak(MovementInput i, boolean v) {
        if (i.modern != null) i.modern.shiftKeyDown = v;
        else i.localSneak = v;
    }
}
