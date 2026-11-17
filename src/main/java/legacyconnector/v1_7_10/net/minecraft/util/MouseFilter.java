package legacyconnector.v1_7_10.net.minecraft.util;

/** The smoothing filter of the mouse (smooth camera): eases the input towards what was asked over the last updates. */
public class MouseFilter {
    private float field_76336_a;
    private float field_76334_b;
    private float field_76335_c;

    public MouseFilter() {
    }

    /** Smooths one axis of the mouse movement: {@code input} is the new movement, {@code factor} the smoothing step. */
    public float smooth(float input, float factor) {
        field_76336_a += input;
        input = (field_76336_a - field_76334_b) * factor;
        field_76335_c += (input - field_76335_c) * 0.5F;
        if (input > 0.0F && input > field_76335_c || input < 0.0F && input < field_76335_c) input = field_76335_c;
        field_76334_b += input;
        return input;
    }

    /** Forgets the smoothing state (a new camera movement starts from rest). */
    public void func_180179_a() {
        field_76336_a = 0.0F;
        field_76334_b = 0.0F;
        field_76335_c = 0.0F;
    }
}
