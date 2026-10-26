package legacyconnector.v1_7_10.net.minecraft.client.entity;

import legacyconnector.v1_7_10.net.minecraft.util.MovementInput;
import net.minecraft.world.entity.player.Player;

public class EntityPlayerSP extends AbstractClientPlayer {
    public MovementInput movementInput = new MovementInput();

    public EntityPlayerSP(Player modern) {
        super(modern);
        // 1.7.10's client player stood at eye level: posY = feet + yOffset, and its eye height above posY was 0.12
        this.yOffset = 1.62F;
        this.mcc$posYShift = 1.62;
    }

    @Override
    public float getEyeHeight() {
        return Float.isNaN(mcc$eyeHeightOverride) ? 0.12F : mcc$eyeHeightOverride;
    }

    // 1.7.10's first-person arm sway (the lagging copy of the view angles the held item swings by), read from the modern
    // player's xBob/yBob: Thaumcraft's Thaumometer undoes the sway with them, and with zeros it tilted by a tenth of the
    // player's pitch and yaw
    public static float mcc$get$renderArmPitch(EntityPlayerSP p) {
        return p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l ? l.xBob : 0.0F;
    }

    public static void mcc$set$renderArmPitch(EntityPlayerSP p, float v) {
        if (p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l) l.xBob = v;
    }

    public static float mcc$get$prevRenderArmPitch(EntityPlayerSP p) {
        return p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l ? l.xBobO : 0.0F;
    }

    public static void mcc$set$prevRenderArmPitch(EntityPlayerSP p, float v) {
        if (p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l) l.xBobO = v;
    }

    public static float mcc$get$renderArmYaw(EntityPlayerSP p) {
        return p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l ? l.yBob : 0.0F;
    }

    public static void mcc$set$renderArmYaw(EntityPlayerSP p, float v) {
        if (p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l) l.yBob = v;
    }

    public static float mcc$get$prevRenderArmYaw(EntityPlayerSP p) {
        return p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l ? l.yBobO : 0.0F;
    }

    public static void mcc$set$prevRenderArmYaw(EntityPlayerSP p, float v) {
        if (p.mcc$player() instanceof net.minecraft.client.player.LocalPlayer l) l.yBobO = v;
    }

    public void closeScreen() {
        mcc$player().closeContainer();
    }

    /**
     * 1.7.10 turned the keyboard input into the living entity's movement here every tick. The modern player already moves
     * by its input; a mod calling it (a player subclass chaining to super) gets the input copied over again.
     */
    public void updateEntityActionState() {
        mcc$syncInput();
        mcc$set$moveStrafing(this, movementInput.moveStrafe);
        mcc$set$moveForward(this, movementInput.moveForward);
    }

    /** Copies the modern keyboard input into {@link #movementInput}, which legacy code reads as plain fields. */
    public void mcc$syncInput() {
        if (mcc$player() instanceof net.minecraft.client.player.LocalPlayer local) {
            var in = local.input;
            movementInput.moveForward = in.forwardImpulse;
            movementInput.moveStrafe = in.leftImpulse;
            movementInput.jump = in.jumping;
            movementInput.sneak = in.shiftKeyDown;
        }
    }
}
