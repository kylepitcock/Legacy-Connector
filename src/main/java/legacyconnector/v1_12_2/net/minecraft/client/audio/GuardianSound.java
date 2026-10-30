package legacyconnector.v1_12_2.net.minecraft.client.audio;

import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityGuardian;
import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;

/** 1.12.2's guardian beam sound: follows the guardian while it targets something, louder and higher as the attack charges. */
public class GuardianSound extends MovingSound {
    private final EntityGuardian guardian;

    public GuardianSound(EntityGuardian guardian) {
        super(SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.HOSTILE);
        this.guardian = guardian;
        this.attenuationType = ISound.AttenuationType.NONE;
        this.repeat = true;
        this.repeatDelay = 0;
    }

    @Override
    public void update() {
        var modern = guardian == null ? null : guardian.mcc$entity;
        if (modern instanceof net.minecraft.world.entity.monster.Guardian g && g.isAlive() && g.hasActiveAttackTarget()) {
            this.xPosF = (float) g.getX();
            this.yPosF = (float) g.getY();
            this.zPosF = (float) g.getZ();
            float f = g.getAttackAnimationScale(0.0F);
            this.volume = 0.0F + 1.0F * f * f;
            this.pitch = 0.7F + 0.5F * f;
        } else {
            this.donePlaying = true;
        }
    }
}
