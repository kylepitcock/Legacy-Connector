package legacyconnector.v1_7_10.net.minecraft.entity.boss;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** An entity whose health the boss bar shows. */
public interface IBossDisplayData {
    float getMaxHealth();

    float getHealth();

    /** The boss bar's title (every entity has it). */
    IChatComponent getFormattedCommandSenderName();

    /** The SRG name of {@link #getFormattedCommandSenderName}. */
    default IChatComponent func_145748_c_() {
        return getFormattedCommandSenderName();
    }
}
