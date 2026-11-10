package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CollisionProjectionB173Test {
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();}
    private static net.minecraft.world.entity.monster.Zombie nativeBody()throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return (net.minecraft.world.entity.monster.Zombie)((sun.misc.Unsafe)field.get(null)).allocateInstance(net.minecraft.world.entity.monster.Zombie.class);
    }
    @Test void collisionSnapshotCoversEveryNativeCombinationAndClearsStaleFlagsWithoutOtherWrites()throws Exception {
        var nativeBody=nativeBody();nativeBody.setDeltaMovement(.1,-.2,.3);nativeBody.hurtTime=6;nativeBody.tickCount=41;
        var ground=net.minecraft.world.entity.Entity.class.getDeclaredField("onGround");ground.setAccessible(true);ground.setBoolean(nativeBody,true);
        var view=new legacyconnector.vb1_7_3.net.minecraft.entity.mob.ZombieEntity(null);
        view.velocityX=8;view.velocityY=-.25;view.velocityZ=9;view.onGround=false;view.health=7;view.mcc$fallDistance(13);
        view.x=14;view.y=15;view.z=16;view.noClip=true;var box=view.boundingBox;
        var nativeMotion=nativeBody.getDeltaMovement();
        // End with false/false after true/true so previously reported collisions must clear.
        for(int bits:new int[]{0,1,2,3,0}){
            nativeBody.horizontalCollision=(bits&1)!=0;nativeBody.verticalCollision=(bits&2)!=0;
            EntityViewsB173.syncCollisions(view,nativeBody);
            assertEquals((bits&1)!=0,view.horizontalCollision);assertEquals((bits&2)!=0,view.verticalCollision);
            assertEquals(bits!=0,view.hasCollided);
            assertEquals((bits&1)!=0,nativeBody.horizontalCollision);assertEquals((bits&2)!=0,nativeBody.verticalCollision);
            assertSame(nativeMotion,nativeBody.getDeltaMovement());assertTrue(nativeBody.onGround());
            assertEquals(6,nativeBody.hurtTime);assertEquals(41,nativeBody.tickCount);
            assertEquals(8,view.velocityX);assertEquals(-.25,view.velocityY);assertEquals(9,view.velocityZ);
            assertFalse(view.onGround);assertEquals(7,view.health);assertEquals(13,view.mcc$fallDistance());
            assertEquals(14,view.x);assertEquals(15,view.y);assertEquals(16,view.z);assertTrue(view.noClip);assertSame(box,view.boundingBox);
        }
    }
}
