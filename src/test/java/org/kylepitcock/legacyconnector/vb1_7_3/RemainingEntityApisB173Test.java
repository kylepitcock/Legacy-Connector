package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity;
import org.junit.jupiter.api.Test;

class RemainingEntityApisB173Test {
    static class Probe extends LivingEntity {
        int lastDamage;float landed;
        Probe(){super(null);}
        @Override public boolean damage(Entity attacker,int amount){lastDamage=amount;return true;}
        @Override public void onLanding(float distance){landed=distance;}
        void fire(boolean immune){fireImmune=immune;setOnFire();}
        void contactDamage(int amount){damage(amount);}
        void markVelocity(){scheduleVelocityUpdate();}
        void rawDamage(int amount){applyDamage(amount);}
        void lookTarget(Entity entity){lookTarget=entity;}
    }
    @Test void originalFireContactAndRawDamageSemanticsRemainDistinct() {
        var entity=new Probe();entity.fire(false);assertEquals(4,entity.lastDamage);assertEquals(600,entity.fireTicks);
        entity.fireTicks=0;entity.lastDamage=0;entity.fire(true);entity.contactDamage(7);assertEquals(0,entity.lastDamage);assertEquals(0,entity.fireTicks);
        entity.health=10;entity.rawDamage(3);assertEquals(7,entity.health);assertEquals(0,entity.lastDamage);
        entity.markVelocity();assertTrue(entity.velocityModified);
    }
    @Test void originalFallAccumulationAndLookIdentityArePreserved() {
        var entity=new Probe();entity.mcc$fall(-2,false);entity.mcc$fall(-1,false);assertEquals(3,entity.mcc$fallDistance());
        entity.mcc$fall(0,true);assertEquals(3,entity.landed);assertEquals(0,entity.mcc$fallDistance());
        assertFalse(entity.hasLookTarget());var target=new Probe();entity.lookTarget(target);assertTrue(entity.hasLookTarget());assertSame(target,entity.getLookTarget());
        entity.setPosition(1,2,3);target.setPosition(4,6,3);assertEquals(25,entity.getSquaredDistance(target));
    }
    @Test void vanillaArrowOwnerConstructorAndMappedVelocityAliasSetPoseAndMotion() {
        var owner=new Probe();owner.setPositionAndAngles(2,5,3,0,0);
        var arrow=new ArrowEntity(null,owner);assertSame(owner,arrow.owner);assertEquals(1.84,arrow.x,.00001);assertEquals(5+owner.getEyeHeight()-.1,arrow.y,.00001);
        arrow.setVelocity(0,0,1,2,0);assertEquals(0,arrow.velocityX);assertEquals(0,arrow.velocityY);assertEquals(2,arrow.velocityZ);
    }
}
