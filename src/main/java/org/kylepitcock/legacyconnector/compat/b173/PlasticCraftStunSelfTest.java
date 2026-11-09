package org.kylepitcock.legacyconnector.compat.b173;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.*;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Two-phase native AI test: let the original six-second Stun timer advance through normal client hooks. */
public final class PlasticCraftStunSelfTest {
    private static final ResourceLocation FOREIGN=ResourceLocation.fromNamespaceAndPath("legacyconnector","fixture_native_speed");
    private record Probe(double base,double speed,AttributeModifier foreign,long started){}
    private static final Map<UUID,Probe> PROBES=new HashMap<>();
    private PlasticCraftStunSelfTest(){}
    public static UUID begin(ServerLevel level,BlockPos pos)throws ReflectiveOperationException{
        require(Boolean.getBoolean("legacyconnector.b173.actualPlasticCraftEntities"),"actual entity fixture property required");
        var mod=ContentB173.loadedMods().stream().filter(m->m.getClass().getSimpleName().equals("mod_PlasticCraft")).findFirst().orElseThrow();
        var goo=(legacyconnector.vb1_7_3.net.minecraft.block.Block)mod.getClass().getField("blockGOOPLASTIC").get(null);
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);require(cow!=null,"native cow created");
        cow.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);cow.setPersistenceRequired();
        var speed=cow.getAttribute(Attributes.MOVEMENT_SPEED);
        var foreign=new AttributeModifier(FOREIGN,.125,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);speed.addTransientModifier(foreign);
        double baseline=speed.getValue(),base=speed.getBaseValue();require(baseline>0&&!cow.isNoAi(),"AI-enabled native cow baseline");
        require(level.addFreshEntity(cow),"native cow spawned");
        try{
            // Native block contact invokes the installed original freeze callback and its exact six/four-second timers.
            goo.mcc$modern.stepOn(level,pos,goo.mcc$modern.defaultBlockState(),cow);
            require(speed.getValue()==0,"original Goo callback suppresses native movement attribute");
            require(speed.getBaseValue()==base&&foreign.equals(speed.getModifier(FOREIGN)),"foreign native attribute state retained during stun");
            PROBES.put(cow.getUUID(),new Probe(base,baseline,foreign,System.nanoTime()));
            return cow.getUUID();
        }catch(RuntimeException|Error error){cow.discard();throw error;}
    }
    public static String check(ServerLevel level,UUID id,boolean restored){
        Probe probe=PROBES.get(id);require(probe!=null,"native stun probe exists");
        var entity=level.getEntity(id);require(entity instanceof net.minecraft.world.entity.animal.Cow,"native test cow remains loaded");
        var cow=(net.minecraft.world.entity.animal.Cow)entity;var speed=cow.getAttribute(Attributes.MOVEMENT_SPEED);
        require(!cow.isNoAi(),"native cow AI remains enabled");
        require(speed.getBaseValue()==probe.base&&probe.foreign.equals(speed.getModifier(FOREIGN)),"foreign base and modifier remain unchanged");
        if(!restored){require(speed.getValue()==0,"native cow stays stunned before original timer ends");return "native AI cow movement speed remains zero";}
        require(System.nanoTime()-probe.started>=6_000_000_000L,"wait for the original six-second timer");
        require(Math.abs(speed.getValue()-probe.speed)<1E-7,"original timer restores native effective movement speed");
        require(speed.getModifiers().size()==1,"only original foreign modifier remains after restoration");
        cow.discard();PROBES.remove(id);
        return "native AI cow baseline -> zero -> six-second restoration; foreign attribute state preserved";
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
