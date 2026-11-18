package legacyconnector.v1_12_2.net.minecraft.util;

/** 1.12.2's mouse smoothing: eases the cursor's movement by carrying the part of it not yet applied into the next frames. */
public class MouseFilter {
    private float targetValue;
    private float remainingValue;
    private float lastAmount;

    /** Smooths a movement by the given factor and returns the part to apply this frame. */
    public float smooth(float movement, float factor) {
        this.targetValue += movement;
        movement = (this.targetValue - this.remainingValue) * factor;
        this.lastAmount += (movement - this.lastAmount) * 0.5F;
        if (movement > 0.0F && movement > this.lastAmount || movement < 0.0F && movement < this.lastAmount) movement = this.lastAmount;
        this.remainingValue += movement;
        return movement;
    }

    public void reset() {
        this.targetValue = 0.0F;
        this.remainingValue = 0.0F;
        this.lastAmount = 0.0F;
    }
}
