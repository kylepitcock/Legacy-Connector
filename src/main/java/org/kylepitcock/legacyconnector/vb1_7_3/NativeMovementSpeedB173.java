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
    private static final class Intent {final double baseline;Float releasedTarget;Intent(double baseline){this.baseline=baseline;}}
    private static final Map<AttributeInstance,Intent> INTENTS=Collections.synchronizedMap(new WeakHashMap<>());
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
        Intent intent=INTENTS.get(attribute);
        if(intent!=null&&intent.releasedTarget!=null){
            if(same(wanted,intent.releasedTarget)){attribute.removeModifier(MODIFIER);return;}
            // A different explicit write starts a new override, rather than inheriting an old restoration target.
            INTENTS.remove(attribute);intent=null;
        }
        attribute.removeModifier(MODIFIER);
        double underlying=attribute.getValue();
        // Restoring the original saved speed releases our override even if another mod changed its own state meanwhile.
        if(intent!=null&&same(wanted,intent.baseline)){
            // Some original callbacks repeat restoration throughout a cooldown. Those writes must keep foreign changes.
            intent.releasedTarget=wanted;return;
        }
        if(same(wanted,underlying)){INTENTS.remove(attribute);return;}
        if(intent==null)INTENTS.put(attribute,new Intent(before));
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
