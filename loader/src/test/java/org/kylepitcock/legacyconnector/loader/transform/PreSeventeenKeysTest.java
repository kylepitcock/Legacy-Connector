package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PreSeventeenKeysTest {
    private static final String IN_BED = "net/minecraft/entity/player/EntityPlayer.func_71065_l()Z#after:aload 0";

    @Test
    void appliesToThePre17VersionsOnly() {
        assertTrue(PreSeventeenKeys.applies("1.6.4"));
        assertTrue(PreSeventeenKeys.applies("1.5.2"));
        assertTrue(PreSeventeenKeys.applies("1.4.7"));
        assertFalse(PreSeventeenKeys.applies("1.7.10"));
        assertFalse(PreSeventeenKeys.applies("1.12.2"));
        assertFalse(PreSeventeenKeys.applies("nonsense"));
    }

    @Test
    void keysOfUnchangedClassesStayTheSame() {
        assertEquals(IN_BED, PreSeventeenKeys.toSeventeen("1.6.4", IN_BED));
        assertEquals(IN_BED, PreSeventeenKeys.toSeventeen("1.7.10", IN_BED));
        assertEquals(IN_BED, PreSeventeenKeys.toSeventeen("1.5.2", IN_BED));
    }

    @Test
    void flatNamesOfOneFourSevenBecomeThePackagedOnes() {
        String flat = "net/minecraft/src/EntityPlayer.func_71065_l()Z#after:aload 0";
        assertEquals(IN_BED, PreSeventeenKeys.toSeventeen("1.4.7", flat));
        assertEquals("(Lnet/minecraft/block/Block;Lnet/minecraft/entity/player/EntityPlayer;III)V",
                PreSeventeenKeys.toSeventeen("1.4.7", "(Lnet/minecraft/src/Block;Lnet/minecraft/src/EntityPlayer;III)V"));
    }

    @Test
    void classesInsideAnAnchorAreMappedToo() {
        assertEquals("net/minecraft/block/Block.func_149695_a(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V#after:invokevirtual net/minecraft/world/World.func_147439_a(III)Lnet/minecraft/block/Block;",
                PreSeventeenKeys.toSeventeen("1.4.7", "net/minecraft/src/Block.func_149695_a(Lnet/minecraft/src/World;IIILnet/minecraft/src/Block;)V#after:invokevirtual net/minecraft/src/World.func_147439_a(III)Lnet/minecraft/src/Block;"));
    }
}
