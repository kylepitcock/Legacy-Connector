package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class NativeMovementSpeedB173Test {
    @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private static AttributeInstance attribute(){return new AttributeInstance(Holder.direct(new RangedAttribute("fixture",.2,0,1024)),ignored->{});}
    private static AttributeModifier foreign(String name,double amount,AttributeModifier.Operation operation){return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("fixture",name),amount,operation);}
    @Test void fieldStunAndRestorationPreserveForeignAttributeState(){
        var attribute=attribute();var add=foreign("add",.1,AttributeModifier.Operation.ADD_VALUE);
        var base=foreign("base",.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        var total=foreign("total",.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        attribute.addPermanentModifier(add);attribute.addTransientModifier(base);attribute.addTransientModifier(total);
        float saved=(float)attribute.getValue();
        NativeMovementSpeedB173.commit(attribute,saved,0);assertEquals(0,attribute.getValue(),1E-9);assertEquals(.2,attribute.getBaseValue());
        assertEquals(add,attribute.getModifier(add.id()));assertEquals(base,attribute.getModifier(base.id()));assertEquals(total,attribute.getModifier(total.id()));
        var later=foreign("later",.25,AttributeModifier.Operation.ADD_VALUE);attribute.addTransientModifier(later);
        NativeMovementSpeedB173.commit(attribute,0,saved);
        assertFalse(attribute.hasModifier(NativeMovementSpeedB173.MODIFIER));assertEquals((.2+.1+.25)*1.5*1.2,attribute.getValue(),1E-9);
        assertEquals(later,attribute.getModifier(later.id()));assertEquals(.2,attribute.getBaseValue());
    }
    @Test void unchangedSnapshotsNeverDebuffAndPositiveWritesUseOneOwnedModifier(){
        var attribute=attribute();NativeMovementSpeedB173.commit(attribute,.2F,.2F);assertTrue(attribute.getModifiers().isEmpty());
        NativeMovementSpeedB173.commit(attribute,.2F,.1F);assertEquals(.1,attribute.getValue(),1E-7);assertEquals(1,attribute.getModifiers().size());
        NativeMovementSpeedB173.commit(attribute,.1F,.3F);assertEquals(.3,attribute.getValue(),1E-7);assertEquals(1,attribute.getModifiers().size());
        NativeMovementSpeedB173.commit(attribute,.3F,.2F);assertFalse(attribute.hasModifier(NativeMovementSpeedB173.MODIFIER));assertEquals(.2,attribute.getValue(),1E-9);
    }
    @Test void restorationKeepsForeignChangesBetweenOriginalReadAndCallbackCommit(){
        var attribute=attribute();float saved=(float)attribute.getValue();
        var nested=foreign("nested_native_callback",.1,AttributeModifier.Operation.ADD_VALUE);attribute.addTransientModifier(nested);
        NativeMovementSpeedB173.commit(attribute,saved,0);assertEquals(0,attribute.getValue());
        NativeMovementSpeedB173.commit(attribute,0,saved);
        assertFalse(attribute.hasModifier(NativeMovementSpeedB173.MODIFIER));assertEquals(.3,attribute.getValue(),1E-9);
        assertEquals(nested,attribute.getModifier(nested.id()));assertEquals(.2,attribute.getBaseValue());
    }
    @Test void repeatedCooldownRestorationRetainsForeignChangesAndDifferentIntentStartsNewOverride(){
        var attribute=attribute();float saved=(float)attribute.getValue();
        NativeMovementSpeedB173.commit(attribute,saved,0);
        var foreign=foreign("during_stun",.1,AttributeModifier.Operation.ADD_VALUE);attribute.addTransientModifier(foreign);
        NativeMovementSpeedB173.commit(attribute,0,saved);
        for(int tick=0;tick<80;tick++)NativeMovementSpeedB173.commit(attribute,(float)attribute.getValue(),saved);
        assertFalse(attribute.hasModifier(NativeMovementSpeedB173.MODIFIER));assertEquals(.3,attribute.getValue(),1E-9);
        var later=foreign("during_cooldown",.2,AttributeModifier.Operation.ADD_VALUE);attribute.addTransientModifier(later);
        NativeMovementSpeedB173.commit(attribute,(float)attribute.getValue(),saved);assertEquals(.5,attribute.getValue(),1E-9);
        float newSaved=(float)attribute.getValue();NativeMovementSpeedB173.commit(attribute,newSaved,0);assertEquals(0,attribute.getValue());
        NativeMovementSpeedB173.commit(attribute,0,newSaved);assertEquals(.5,attribute.getValue(),1E-9);
        assertFalse(attribute.hasModifier(NativeMovementSpeedB173.MODIFIER));assertEquals(foreign,attribute.getModifier(foreign.id()));assertEquals(later,attribute.getModifier(later.id()));
    }
    @Test void zeroNativeBaseSupportsExplicitPositiveWriteWithoutReplacingForeignMultipliers(){
        var attribute=attribute();attribute.setBaseValue(0);
        var multiplier=foreign("total",.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);attribute.addTransientModifier(multiplier);
        NativeMovementSpeedB173.commit(attribute,0,.3F);assertEquals(.3,attribute.getValue(),1E-7);assertEquals(0,attribute.getBaseValue());
        NativeMovementSpeedB173.commit(attribute,.3F,0);assertEquals(0,attribute.getValue());assertEquals(multiplier,attribute.getModifier(multiplier.id()));
        assertFalse(attribute.hasModifier(NativeMovementSpeedB173.MODIFIER));
    }
}
