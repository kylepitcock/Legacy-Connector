package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import org.junit.jupiter.api.Test;

class TextureIdentityB173Test {
    @Test void decodedTextureKeepsOriginalLiteralIdentity() {
        var original = new LivingEntity(null) {};
        var decoded = new String("/mob/fixture.png");
        assertNotSame("/mob/fixture.png", decoded);
        original.mcc$setTexture(decoded);
        assertSame("/mob/fixture.png", original.getTexture());
        original.mcc$setTexture(null);
        assertNull(original.getTexture());
    }
}
