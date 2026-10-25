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
        return 0.12F;
    }

    public void closeScreen() {
        mcc$player().closeContainer();
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
