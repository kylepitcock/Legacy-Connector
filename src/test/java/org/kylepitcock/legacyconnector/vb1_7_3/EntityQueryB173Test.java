package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class EntityQueryB173Test {
    private static final class EmptyWorld extends World {}
    @Test void untypedQueriesReuseMutableWorldBufferWhileTypedQueriesOwnTheirLists(){
        var world=new EmptyWorld();var wide=world.getEntities(null,null);wide.add(null);
        var typed=world.collectEntitiesByClass(Entity.class,null);assertNotSame(wide,typed);assertEquals(1,wide.size());
        typed.add(null);var narrow=world.getEntities(null,null);assertSame(wide,narrow);assertTrue(wide.isEmpty());assertEquals(1,typed.size());
        assertNotSame(world.getEntities(null,null),new EmptyWorld().getEntities(null,null));
    }
}
