package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldStorageB173Test {
    static class UnboundWorld extends World {UnboundWorld(){super();}}
    @Test void nativeSaveRootTakesPriorityWithoutConsultingClientFallback(@TempDir Path root){
        var called=new AtomicBoolean();
        assertEquals(root,WorldStorageB173.choose(root,()->{called.set(true);throw new AssertionError("Client resolver consulted for native save");}));
        assertFalse(called.get());
        assertEquals(root,WorldStorageB173.boundClientRoot(true,root,null,null));
    }
    @Test void realBoundClientSelectionUsesOnlyItsActiveInstanceAndServerIdentity(@TempDir Path root)throws Exception{
        var first=WorldStorageB173.boundClientRoot(true,null,root,"example.test:25565");
        assertTrue(first.startsWith(root.resolve(".legacyconnector/client-worlds")));
        assertEquals(first,WorldStorageB173.boundClientRoot(true,null,root,"example.test:25565"));
        assertNotEquals(first,WorldStorageB173.boundClientRoot(true,null,root,"other.test:25565"));
        assertNotEquals(first,WorldStorageB173.boundClientRoot(true,null,root,"example.test:25566"));
        assertNotEquals(first,WorldStorageB173.boundClientRoot(true,null,root.resolve("other-instance"),"example.test:25565"));
        Files.createDirectories(first);Files.writeString(first.resolve("stopevil.txt"),"fixture");
        assertTrue(Files.exists(WorldStorageB173.boundClientRoot(true,null,root,"example.test:25565").resolve("stopevil.txt")));
        assertFalse(Files.exists(WorldStorageB173.boundClientRoot(true,null,root,"other.test:25565").resolve("stopevil.txt")));
        assertNotEquals(PlayerApiB173.playerStorageDirectory(first,UUID.randomUUID()),PlayerApiB173.playerStorageDirectory(first,UUID.randomUUID()));
    }
    @Test void missingWorldConnectionIdentityAndRelativeFallbackFailClosed(@TempDir Path root){
        assertThrows(IllegalStateException.class,()->WorldStorageB173.root(null));
        assertThrows(IllegalStateException.class,()->((legacyconnector.vb1_7_3.net.minecraft.world.storage.AlphaWorldStorage)new UnboundWorld().storage).getDirectory());
        assertThrows(IllegalStateException.class,()->WorldStorageB173.boundClientRoot(false,root,root,"server"));
        assertThrows(IllegalStateException.class,()->WorldStorageB173.boundClientRoot(true,null,root,null));
        assertThrows(IllegalStateException.class,()->WorldStorageB173.boundClientRoot(true,null,root," "));
        assertThrows(IllegalStateException.class,()->WorldStorageB173.boundClientRoot(true,null,Path.of("relative"),"server"));
        assertThrows(IllegalStateException.class,()->WorldStorageB173.choose(null,()->null));
    }
}
