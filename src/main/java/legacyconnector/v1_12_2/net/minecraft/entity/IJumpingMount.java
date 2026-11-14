package legacyconnector.v1_12_2.net.minecraft.entity;

/** A mount the rider makes jump with the jump key (horses, and the mods' own rideable mobs): the jump bar fills while the key is held. */
public interface IJumpingMount {
    void setJumpPower(int jumpPowerIn);

    boolean canJump();

    void handleStartJump(int p_184775_1_);

    void handleStopJump();
}
