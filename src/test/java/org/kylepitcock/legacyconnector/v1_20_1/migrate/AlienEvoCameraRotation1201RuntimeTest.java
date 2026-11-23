package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

/** Regression tests for the first-person named-bone key convention used by AlienEvo. */
class AlienEvoCameraRotation1201RuntimeTest {
    @Test
    void firstPersonArmBoneKeysMatchAlienEvosArmKeyConvention() {
        assertEquals("beamRightRIGHT",
                AlienEvoCameraRotation1201Runtime.firstPersonArmBoneKey("beamRight", HumanoidArm.RIGHT));
        assertEquals("beamLeftLEFT",
                AlienEvoCameraRotation1201Runtime.firstPersonArmBoneKey("beamLeft", HumanoidArm.LEFT));
    }

    @Test
    void nullBoneOrArmDoesNotCreateAnInvalidKey() {
        assertNull(AlienEvoCameraRotation1201Runtime.firstPersonArmBoneKey(null, HumanoidArm.RIGHT));
        assertEquals("beamRight",
                AlienEvoCameraRotation1201Runtime.firstPersonArmBoneKey("beamRight", null));
    }
}
