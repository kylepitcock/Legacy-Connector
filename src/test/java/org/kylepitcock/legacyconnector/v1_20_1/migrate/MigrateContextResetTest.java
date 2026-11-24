package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.WorldStateReset;

/** The registry access a bridge call remembered belongs to one world: after the world is left it must not be handed out again (#113's class). */
class MigrateContextResetTest {
    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void rememberedProviderIsForgottenWhenTheWorldIsLeft() {
        HolderLookup.Provider first = VanillaRegistries.createLookup();
        MigrateContext.remember(first);
        assertSame(first, MigrateContext.provider());
        WorldStateReset.serverStopped();
        assertNotSame(first, MigrateContext.provider());

        HolderLookup.Provider second = VanillaRegistries.createLookup();
        MigrateContext.remember(second);
        assertSame(second, MigrateContext.provider());
        WorldStateReset.clientLoggedOut();
        assertNotSame(second, MigrateContext.provider());
    }

    @Test
    void anotherThreadsRememberedProviderIsInvalidatedToo() throws Exception {
        HolderLookup.Provider p = VanillaRegistries.createLookup();
        var seen = new HolderLookup.Provider[2];
        var remembered = new java.util.concurrent.CountDownLatch(1);
        var reset = new java.util.concurrent.CountDownLatch(1);
        Thread t = new Thread(() -> {
            MigrateContext.remember(p);
            seen[0] = MigrateContext.provider();
            remembered.countDown();
            try {
                reset.await();
            } catch (InterruptedException e) {
                return;
            }
            seen[1] = MigrateContext.provider();
        });
        t.start();
        remembered.await();
        WorldStateReset.serverStopped();
        reset.countDown();
        t.join();
        assertSame(p, seen[0]);
        assertNotSame(p, seen[1]);
    }
}
