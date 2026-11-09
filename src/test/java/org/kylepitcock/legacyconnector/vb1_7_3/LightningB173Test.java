package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity;
import org.junit.jupiter.api.Test;

class LightningB173Test {
    static class Victim extends Entity {
        int damageCalls, amount;
        Entity attacker;
        Victim(boolean immune){super(null);fireImmune=immune;}
        @Override public boolean damage(Entity attacker,int amount){this.attacker=attacker;this.amount=amount;damageCalls++;return true;}
    }
    @Test void ordinaryStrikeUsesOriginalFireDamageAndNullAttacker(){
        var victim=new Victim(false);victim.fireTicks=-1;
        victim.onStruckByLightning(new LightningEntity(null,1,2,3));
        assertEquals(1,victim.damageCalls);assertEquals(5,victim.amount);assertNull(victim.attacker);
        assertEquals(300,victim.fireTicks);
    }
    @Test void fireImmuneVictimSkipsDamageButRetainsBetaIgnitionCounter(){
        var victim=new Victim(true);victim.fireTicks=-1;victim.onStruckByLightning(null);
        assertEquals(0,victim.damageCalls);assertEquals(300,victim.fireTicks);
    }
    @Test void existingFireCounterOnlyIncrementsRatherThanResetting(){
        var victim=new Victim(false);victim.fireTicks=20;victim.onStruckByLightning(null);
        assertEquals(21,victim.fireTicks);
        victim.fireTicks=-20;victim.onStruckByLightning(null);assertEquals(-19,victim.fireTicks);
    }
    @Test void originalLightningOverrideCanChooseWhetherToCallDefault(){
        var victim=new Victim(false){@Override public void onStruckByLightning(LightningEntity bolt){fireTicks=42;}};
        Entity callbackTarget=victim;callbackTarget.onStruckByLightning(null);
        assertEquals(0,victim.damageCalls);assertEquals(42,victim.fireTicks);
    }
}
