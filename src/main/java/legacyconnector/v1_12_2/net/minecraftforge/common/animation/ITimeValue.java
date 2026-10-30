package legacyconnector.v1_12_2.net.minecraftforge.common.animation;

/** Forge 1.12.2 animation parameter: a value as a function of time. */
public interface ITimeValue {
    float apply(float input);
}
