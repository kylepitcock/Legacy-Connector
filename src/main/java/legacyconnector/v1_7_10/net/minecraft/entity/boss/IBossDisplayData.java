package legacyconnector.v1_7_10.net.minecraft.entity.boss;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** An entity whose health the boss bar shows. */
public interface IBossDisplayData {
    float getMaxHealth();

    float getHealth();

    IChatComponent func_145748_c_();
}
