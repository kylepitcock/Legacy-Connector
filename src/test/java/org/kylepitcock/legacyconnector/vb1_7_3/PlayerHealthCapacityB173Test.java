package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerHealthCapacityB173Test {
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}
    private static AttributeInstance attribute(){return new AttributeInstance(Attributes.MAX_HEALTH,ignored->{});}
    private static AttributeModifier foreign(String name,double amount,AttributeModifier.Operation operation){
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("fixture",name),amount,operation);
    }
    @Test void explicitAboveCapWriteRaisesOnlyItsActorsCapacity(){
        var first=attribute();var second=attribute();var firstData=new CompoundTag();var secondData=new CompoundTag();
        PlayerHealthCapacityB173.commit(firstData,first,20,22,true);
        assertEquals(22,first.getValue());assertEquals(20,first.getBaseValue());assertEquals(22,PlayerHealthCapacityB173.floor(firstData));
        assertEquals(20,second.getValue());assertEquals(0,PlayerHealthCapacityB173.floor(secondData));
        PlayerHealthCapacityB173.commit(firstData,first,22,3,true);
        assertEquals(22,first.getValue(),"Damage does not infer an unobservable private capacity decrease");
    }
    @Test void unchangedAndDisabledWritesNeverCreateCapacity(){
        var attribute=attribute();var data=new CompoundTag();
        PlayerHealthCapacityB173.commit(data,attribute,22,22,true);
        PlayerHealthCapacityB173.commit(data,attribute,10,18,true);
        PlayerHealthCapacityB173.commit(data,attribute,20,30,false);
        assertEquals(20,attribute.getValue());assertTrue(attribute.getModifiers().isEmpty());assertEquals(0,PlayerHealthCapacityB173.floor(data));
    }
    @Test void foreignBasesAndAllModifierFamiliesSurviveReconciliationAndCleanup(){
        var attribute=attribute();attribute.setBaseValue(10);var data=new CompoundTag();
        var add=foreign("add",2,AttributeModifier.Operation.ADD_VALUE);
        var base=foreign("base",.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        var total=foreign("total",.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        attribute.addPermanentModifier(add);attribute.addPermanentModifier(base);attribute.addPermanentModifier(total);
        PlayerHealthCapacityB173.commit(data,attribute,20,30,true);assertEquals(30,attribute.getValue(),1E-9);
        attribute.setBaseValue(30);PlayerHealthCapacityB173.reconcile(data,attribute,true);
        assertEquals(57.6,attribute.getValue(),1E-9);assertFalse(attribute.hasModifier(PlayerHealthCapacityB173.MODIFIER));
        attribute.setBaseValue(10);PlayerHealthCapacityB173.reconcile(data,attribute,true);assertEquals(30,attribute.getValue(),1E-9);
        PlayerHealthCapacityB173.reconcile(data,attribute,false);assertEquals(21.6,attribute.getValue(),1E-9);
        assertEquals(10,attribute.getBaseValue());assertEquals(add,attribute.getModifier(add.id()));
        assertEquals(base,attribute.getModifier(base.id()));assertEquals(total,attribute.getModifier(total.id()));assertEquals(0,PlayerHealthCapacityB173.floor(data));
    }
    @Test void nativeAttributeNbtRestoresCapacityBeforeHealthAndFloorPersistsIndependently(){
        var original=attribute();var data=new CompoundTag();PlayerHealthCapacityB173.commit(data,original,20,24,true);
        CompoundTag nativeSaved=original.save();var restored=attribute();restored.load(nativeSaved);
        assertEquals(24,restored.getValue(),"Native LivingEntity loads attributes before clamping its saved Health");
        var restoredData=data.copy();PlayerHealthCapacityB173.reconcile(restoredData,restored,true);
        assertEquals(24,restored.getValue());assertEquals(24,PlayerHealthCapacityB173.floor(restoredData));
        assertTrue(restored.save().getList("modifiers",10).size()>0);
    }
    @Test void cloneCopiesOnlyCapacityAndLeavesNativeCurrentHealthAndUnrelatedDataAlone(){
        var old=new CompoundTag();old.putDouble(PlayerHealthCapacityB173.FLOOR,28);old.putFloat("Health",7);
        var copy=new CompoundTag();copy.putFloat("Health",20);copy.putString("foreign","retained");
        PlayerHealthCapacityB173.copyFloor(old,copy);var attribute=attribute();PlayerHealthCapacityB173.reconcile(copy,attribute,true);
        assertEquals(28,attribute.getValue());assertEquals(20,copy.getFloat("Health"));assertEquals("retained",copy.getString("foreign"));
    }
    @Test void repeatedReconcileKeepsModifierIdentityAndForeignZeroMultiplierRemainsAuthoritative(){
        var attribute=attribute();var data=new CompoundTag();PlayerHealthCapacityB173.commit(data,attribute,20,22,true);
        var owned=attribute.getModifier(PlayerHealthCapacityB173.MODIFIER);PlayerHealthCapacityB173.reconcile(data,attribute,true);
        assertSame(owned,attribute.getModifier(PlayerHealthCapacityB173.MODIFIER));
        var zero=foreign("zero",-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);attribute.addPermanentModifier(zero);
        PlayerHealthCapacityB173.reconcile(data,attribute,true);assertEquals(attribute.getAttribute().value().sanitizeValue(0),attribute.getValue());
        assertEquals(zero,attribute.getModifier(zero.id()));assertFalse(attribute.hasModifier(PlayerHealthCapacityB173.MODIFIER));
    }
    @Test void cloneRestoresOnlyHealthLostBeforeCapacityInstallationAndKeepsNativeDeathPolicy(){
        assertEquals(22,PlayerHealthCapacityB173.cloneHealth(22,20,20,24,false),"Nondeath transfer preserves old health rather than healing to full");
        assertEquals(14,PlayerHealthCapacityB173.cloneHealth(14,14,20,24,false));
        assertEquals(24,PlayerHealthCapacityB173.cloneHealth(0,20,20,24,true),"A native full-health respawn remains full at restored capacity");
        assertEquals(7,PlayerHealthCapacityB173.cloneHealth(22,7,20,24,false),"Earlier foreign health changes survive nondeath clone");
        assertEquals(7,PlayerHealthCapacityB173.cloneHealth(0,7,20,24,true),"Earlier foreign respawn health policy remains authoritative");
        assertEquals(1,PlayerHealthCapacityB173.cloneHealth(22,1,1,1,false),"Foreign capacity constraints still clamp restored health");
        assertEquals(20,PlayerHealthCapacityB173.cloneHealth(0,20,25,30,true),"Foreign modifier added before Clone does not imply healing to its new maximum");
    }
}
