package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual original particle callbacks and constructor-state transport, without graphics or native client classes. */
public final class ParticleSelfTestB173 {
    private ParticleSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173ParticleTest").error("B1.7.3 PARTICLE TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("B1.7.3 PARTICLE TEST: "+detail);}
    private static List<Tessellator.Vertex> render(Particle particle){Tessellator.beginCapture();List<Tessellator.Vertex> result;try{particle.render(Tessellator.INSTANCE,.5F,1,1,0,0,0);}finally{result=Tessellator.endCapture();}return result;}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var mod=org.kylepitcock.legacyconnector.core.LegacyMods.ordered().stream().filter(m->m.mainClassName.equals("legacyconnector_b173.morecreepsandweirdos.mod_MoreCreepsAndWeirdos")).findFirst();if(mod.isEmpty())return;
        var override=ContentB173.textureOverrides().stream().filter(o->o.mod()==mod.get()&&o.sheet().equals("/gui/items.png")).findFirst().orElseThrow();
        var world=World.mcc$wrap(level);int checked=0;
        for(String suffix:List.of("Atoms","Blood","Bubbles","Confetti","Dirt","Pee","Smoke","Spit"))try{
            Class<?> type=Class.forName(mod.get().mainClass.getPackageName()+".CREEPSFx"+suffix,true,mod.get().mainClass.getClassLoader());
            Particle original;
            try{original=(Particle)type.getConstructor(World.class,double.class,double.class,double.class,int.class,float.class).newInstance(world,level.getSharedSpawnPos().getX()+.5,level.getSharedSpawnPos().getY()+20D,level.getSharedSpawnPos().getZ()+.5,override.index(),1F);}
            catch(NoSuchMethodException ignored){original=(Particle)type.getConstructor(World.class,double.class,double.class,double.class,int.class).newInstance(world,level.getSharedSpawnPos().getX()+.5,level.getSharedSpawnPos().getY()+20D,level.getSharedSpawnPos().getZ()+.5,override.index());}
            require(ParticlesB173.unsupported(type)==null,"actual original FX closure "+suffix);
            require(original.getGroup()==2&&original.textureId==override.index(),"original FX keeps its group and selected sprite "+suffix);
            var before=render(original);require(before.size()==4,"original billboard emits one quad "+suffix);
            for(var vertex:before)require(Double.isFinite(vertex.x())&&Double.isFinite(vertex.y())&&Double.isFinite(vertex.z())&&Double.isFinite(vertex.u())&&Double.isFinite(vertex.v()),"finite original geometry "+suffix);
            Particle restored=ParticlesB173.restore(ParticlesB173.decode(ParticlesB173.encode(ParticlesB173.capture(original))),world);
            require(restored.getClass()==type&&render(restored).equals(before),"state packet round-trip retains exact original geometry/colors/UVs "+suffix);
            original.maxParticleAge=3;double y=original.y;original.tick();require(original.particleAge==1&&original.y!=y,"original tick advances age and motion "+suffix);
            for(int i=0;i<4;i++)original.tick();require(original.dead,"original particle lifetime removes itself "+suffix);checked++;
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Original particle fixture failed: "+suffix,error);}
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173ParticleTest").info("B1.7.3 PARTICLE TEST: {} original FX constructors, callbacks, selected sprites, geometry/color/UV packet round-trips and lifetimes passed (headless; native atlas drawing requires client proof)",checked);
    }
}
