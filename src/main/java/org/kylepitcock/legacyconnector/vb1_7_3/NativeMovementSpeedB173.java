package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Explicit original field writes own one transient modifier, preserving native bases and foreign modifiers. */
public final class NativeMovementSpeedB173 {
    static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath("legacyconnector","beta_movement_speed");
    private static final Map<AttributeInstance,Double> BASELINES=Collections.synchronizedMap(new WeakHashMap<>());
    private NativeMovementSpeedB173(){}
    static float read(net.minecraft.world.entity.LivingEntity entity){
        var attribute=entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return attribute==null?entity.getSpeed():(float)attribute.getValue();
    }
    static void commit(net.minecraft.world.entity.LivingEntity entity,float before,float wanted){
        var attribute=entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attribute!=null)commit(attribute,before,wanted);
    }
    static void commit(AttributeInstance attribute,float before,float wanted){
        if(Float.compare(before,wanted)==0||!Float.isFinite(wanted))return;
        attribute.removeModifier(MODIFIER);
        double underlying=attribute.getValue();
        Double baseline=BASELINES.get(attribute);
        // Restoring the original saved speed releases our override even if another mod changed its own state meanwhile.
        if(same(wanted,underlying)||baseline!=null&&same(wanted,baseline)){
            BASELINES.remove(attribute);return;
        }
        if(baseline==null)BASELINES.put(attribute,(double)before);
        if(underlying>0){
            attribute.addTransientModifier(new AttributeModifier(MODIFIER,Math.max(0,wanted)/underlying-1,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }else if(wanted>0){
            // A zero base may still permit an additive field write. Foreign zero multipliers remain authoritative.
            double additive=attribute.getBaseValue(),factor=1,total=1;
            for(var modifier:attribute.getModifiers()){
                if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE)additive+=modifier.amount();
                else if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)factor+=modifier.amount();
                else if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)total*=1+modifier.amount();
            }
            double multiplier=factor*total;
            if(multiplier>0&&Double.isFinite(multiplier))attribute.addTransientModifier(new AttributeModifier(MODIFIER,wanted/multiplier-additive,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }
    private static boolean same(double requested,double underlying){return Float.compare((float)requested,(float)underlying)==0;}
}
