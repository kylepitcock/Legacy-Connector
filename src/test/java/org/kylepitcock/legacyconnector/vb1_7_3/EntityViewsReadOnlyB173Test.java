package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class EntityViewsReadOnlyB173Test {
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();}
    private static <T> T blank(Class<T> type)throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    private static Map<?,?> pending()throws Exception {
        var field=EntityViewsB173.class.getDeclaredField("TOUCHED");field.setAccessible(true);
        return (Map<?,?>)((ThreadLocal<?>)field.get(null)).get();
    }
    @Test void nestedReadOnlyCollisionQueriesPreservePendingPlayerWritesAndDiscardOnlyNewEntries()throws Exception {
        // Native construction/sync would need a live level. Querying an already-pending player
        // must never reach that sync path, which also overwrites original velocity/fall writes.
        var nativeOwner=blank(net.minecraft.server.level.ServerPlayer.class);
        var owner=blank(PlayerViewB173.class);owner.mcc$modern=nativeOwner;owner.velocityY=-1.3;owner.fallDistance=9;
        var nativeOther=blank(net.minecraft.server.level.ServerPlayer.class);
        var other=blank(PlayerViewB173.class);other.mcc$modern=nativeOther;
        try(var isolation=EntityViewsB173.readOnly()){
            EntityViewsB173.spawned(nativeOwner,owner);var outer=pending();var baseline=outer.get(nativeOwner);
            owner.velocityY=-.25;owner.fallDistance=0;
            try(var query=EntityViewsB173.readOnly()){
                assertNotSame(outer,pending());assertSame(baseline,pending().get(nativeOwner));
                assertSame(owner,EntityViewsB173.wrap(nativeOwner));
                EntityViewsB173.spawned(nativeOther,other);
                try(var nested=EntityViewsB173.readOnly()){
                    assertSame(owner,EntityViewsB173.wrap(nativeOwner));assertSame(other,EntityViewsB173.wrap(nativeOther));
                }
                assertEquals(-.25,owner.velocityY);assertEquals(0,owner.fallDistance);
            }
            assertSame(outer,pending());assertSame(baseline,pending().get(nativeOwner));
            assertFalse(pending().containsKey(nativeOther));assertEquals(1,pending().size());
            assertSame(owner,EntityViewsB173.wrap(nativeOwner));assertEquals(-.25,owner.velocityY);assertEquals(0,owner.fallDistance);
        }
    }
}
