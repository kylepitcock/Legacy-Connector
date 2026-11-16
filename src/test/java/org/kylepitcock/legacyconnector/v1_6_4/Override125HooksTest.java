package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * The two hooks the loader emits for 1.2.5 overrides of unjoined members: {@code mcc125$makeChild(EntityAnimal)EntityAnimal} and
 * {@code mcc125$updateMetadataOnPlaced(World,int,int,int,int)V} (fifth int = side), with a synthetic mod subclass allocated without a game.
 */
class Override125HooksTest {
    public abstract static class Mod extends EntityAnimal {
        Mod() {
            super((World) null);
        }
    }

    public static class Cow125 extends Mod {
        static EntityAnimal child;
        EntityAnimal seenMate;

        @Override
        public EntityAnimal mcc125$makeChild(EntityAnimal mate) {
            seenMate = mate;
            return child;
        }
    }

    public static class Torch125 extends Block {
        Torch125() {
            super((legacyconnector.v1_7_10.net.minecraft.block.material.Material) null);
        }

        int[] seen;

        @Override
        public void mcc125$updateMetadataOnPlaced(World world, int x, int y, int z, int side) {
            seen = new int[] {x, y, z, side};
        }
    }

    public static class Forcer125 extends legacyconnector.v1_7_10.net.minecraft.world.Teleporter {
        java.util.List<String> calls;

        Forcer125() {
            super(null);
        }

        @Override
        public boolean mcc125$findNetherPortal(World world, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
            calls.add("find");
            return calls.size() > 2;
        }

        @Override
        public boolean mcc125$generateNetherPortal(World world, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
            calls.add("generate");
            return true;
        }
    }

    @Test
    void dimensionTravelReachesAModTeleportersHooks() {
        Forcer125 f = alloc(Forcer125.class);
        f.calls = new java.util.ArrayList<>();
        f.placeInPortal(null, 0, 0, 0, 0F);
        assertEquals(java.util.List.of("find", "generate", "find"), f.calls);
    }

    private static <T> T alloc(Class<T> type) {
        try {
            Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method m = unsafe.getClass().getMethod("allocateInstance", Class.class);
            return type.cast(m.invoke(unsafe, type));
        } catch (Throwable t) {
            Assumptions.abort("shim classes cannot be allocated offline: " + t);
            return null;
        }
    }

    @Test
    void createChildCallsTheMods12_5MakeChild() {
        Cow125 cow = alloc(Cow125.class);
        Cow125 mate = alloc(Cow125.class);
        Cow125.child = mate;
        assertSame(mate, cow.createChild(mate));
        assertSame(mate, cow.seenMate);
    }

    @Test
    void updateMetadataOnPlacedGetsTheSide() {
        Torch125 torch = alloc(Torch125.class);
        torch.mcc125$updateMetadataOnPlaced(null, 1, 2, 3, 5);
        assertEquals(5, torch.seen[3]);
    }
}
