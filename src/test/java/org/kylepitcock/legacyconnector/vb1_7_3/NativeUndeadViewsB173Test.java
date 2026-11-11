package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.mob.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeUndeadViewsB173Test {
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();}
    private static net.minecraft.world.entity.Entity nativeEntity(Class<? extends net.minecraft.world.entity.Entity> type)throws Exception {
        // Only native class identity and unchanged scalar state are needed. Constructing a native
        // mob additionally requires a live level, registries and AI, outside this view-only seam.
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var entity=(net.minecraft.world.entity.Entity)((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
        entity.tickCount=41;entity.setDeltaMovement(.1,-.2,.3);entity.setXRot(19);return entity;
    }
    private static Entity view(net.minecraft.world.entity.Entity nativeEntity)throws Exception {
        var method=EntityViewsB173.class.getDeclaredMethod("create",net.minecraft.world.entity.Entity.class);method.setAccessible(true);
        return (Entity)method.invoke(null,nativeEntity);
    }
    private static boolean undead(Entity entity){
        return java.util.List.of(ZombieEntity.class,SkeletonEntity.class,PigZombieEntity.class).stream().anyMatch(type->type.isAssignableFrom(entity.getClass()));
    }
    @Test void nativeUndeadFamiliesExposeOriginalClassIdentityWithoutChangingNativeMotionOrTicks()throws Exception {
        for(var pair:java.util.Map.<Class<? extends net.minecraft.world.entity.Entity>,Class<?>>of(
                net.minecraft.world.entity.monster.Zombie.class,ZombieEntity.class,
                net.minecraft.world.entity.monster.Skeleton.class,SkeletonEntity.class,
                net.minecraft.world.entity.monster.ZombifiedPiglin.class,PigZombieEntity.class).entrySet()){
            var nativeEntity=nativeEntity(pair.getKey());var motion=nativeEntity.getDeltaMovement();
            var view=view(nativeEntity);assertEquals(pair.getValue(),view.getClass());assertTrue(undead(view));
            assertEquals(41,nativeEntity.tickCount);assertSame(motion,nativeEntity.getDeltaMovement());assertEquals(19,nativeEntity.getXRot());
        }
    }
    @Test void pigZombieSpecificityAndNativeFamilySubtypesRemainRecognizable()throws Exception {
        var pig=view(nativeEntity(net.minecraft.world.entity.monster.ZombifiedPiglin.class));
        assertEquals(PigZombieEntity.class,pig.getClass());assertInstanceOf(ZombieEntity.class,pig);
        assertEquals(ZombieEntity.class,view(nativeEntity(net.minecraft.world.entity.monster.Husk.class)).getClass());
        assertEquals(SkeletonEntity.class,view(nativeEntity(net.minecraft.world.entity.monster.Stray.class)).getClass());
        assertFalse(ZombieEntity.class.isAssignableFrom(SkeletonEntity.class));assertTrue(ZombieEntity.class.isAssignableFrom(PigZombieEntity.class));
    }
    @Test void nonUndeadNativeMonstersKeepTheirExistingViewsAndConstructorDefaultsMatchBeta()throws Exception {
        var spider=view(nativeEntity(net.minecraft.world.entity.monster.Spider.class));assertInstanceOf(MonsterEntity.class,spider);assertFalse(undead(spider));
        assertEquals(CreeperEntity.class,view(nativeEntity(net.minecraft.world.entity.monster.Creeper.class)).getClass());
        var zombie=new ZombieEntity(null);var skeleton=new SkeletonEntity(null);var pig=new PigZombieEntity(null);
        assertEquals("/mob/zombie.png",zombie.mcc$texture());assertEquals(.5F,zombie.mcc$movementSpeed());
        assertEquals("/mob/skeleton.png",skeleton.mcc$texture());assertEquals("/mob/pigzombie.png",pig.mcc$texture());assertTrue(pig.mcc$fireImmune());
    }
}
