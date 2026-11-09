package org.kylepitcock.legacyconnector.compat.b173;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Opt-in execution of installed original callbacks against a recording world; native lifecycle tests remain separate. */
public final class PlasticCraftEntitiesSelfTest {
    private PlasticCraftEntitiesSelfTest(){}
    public static void run(){
        if(!Boolean.getBoolean("legacyconnector.b173.actualPlasticCraftEntities"))return;
        try{
            var mod=ContentB173.loadedMods().stream().filter(m->m.getClass().getSimpleName().equals("mod_PlasticCraft")).findFirst().orElseThrow();
            String prefix=mod.getClass().getPackageName()+".";
            var loader=mod.getClass().getClassLoader();
            Class<?> boatType=loader.loadClass(prefix+"EntityPlasticBoat"),c4Type=loader.loadClass(prefix+"EntityC4Primed");
            require(NeutralEntitiesB173.supports(boatType),"boat admission: "+NeutralEntitiesB173.unsupportedReason(boatType));
            require(NeutralEntitiesB173.supports(c4Type),"C4 admission: "+NeutralEntitiesB173.unsupportedReason(c4Type));
            var dry=new RecordingWorld();var boat=create(boatType,dry);
            boat.tick();require(close(boat.velocityY,-.03999999910593033D*.949999988079071D),"original dry gravity and damping");
            var wet=new RecordingWorld();wet.water=true;boat=create(boatType,wet);
            boat.tick();require(close(boat.velocityY,.007000000216066837D*.949999988079071D),"five original fluid slices produce full buoyancy");
            var rider=new PlayerEntity(wet){};require(boat.interact(rider)&&rider.vehicle==boat&&boat.passenger==rider,"original interaction mounts rider");
            rider.velocityX=.5D;boat.velocityX=0;boat.tick();require(close(boat.velocityX,.5D*.2D*.9900000095367432D),"original rider velocity steers boat");
            boat.updatePassengerPosition();double angle=boat.yaw*Math.PI/180;
            require(close(rider.x,boat.x+Math.cos(angle)*.4)&&close(rider.z,boat.z+Math.sin(angle)*.4),"original passenger yaw offset");
            require(boat.interact(rider)&&rider.vehicle==null&&boat.passenger==null,"original repeated interaction dismounts");
            boat.damage(rider,5);require(boat.dead&&wet.spawned.size()==1,"original damage threshold drops one boat");
            var dropped=(legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity)wet.spawned.getFirst();
            var boatItem=(legacyconnector.vb1_7_3.net.minecraft.item.Item)mod.getClass().getField("itemPLASTICBOAT").get(null);
            require(dropped.stack.itemId==boatItem.id&&dropped.stack.count==1,"original boat item drop");
            var blast=new RecordingWorld();var c4=create(c4Type,blast);c4Type.getField("a").setInt(c4,2);c4Type.getField("power").setInt(c4,7);
            var tag=new NbtCompound();c4.writeNbt(tag);require(tag.getByte("Fuse")==2,"original fuse NBT");
            var restored=create(c4Type,blast);restored.readNbt(tag);require(c4Type.getField("a").getInt(restored)==2,"original fuse readback");
            c4.tick();c4.tick();require(!c4.dead&&blast.explosions==0,"post-decrement fuse retains two ticks");
            c4.tick();require(c4.dead&&blast.explosions==1&&blast.power==7&&blast.source==null,"original fuse expires into one configured explosion");
            require(blast.smoke==2,"original fuse smoke callbacks");
            renderer(loader.loadClass(prefix+"RenderPlasticBoat"),create(boatType,new RecordingWorld()),5,0);
            c4=create(c4Type,new RecordingWorld());c4Type.getField("a").setInt(c4,2);
            renderer(loader.loadClass(prefix+"RenderC4Primed"),c4,0,12);
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 ACTUAL PLASTIC ENTITIES TEST: original boat buoyancy, mount, steering, passenger position, damage/drop; C4 fuse, smoke, explosion, NBT; both original renderers passed");
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Actual PlasticCraft entity callback test",error);}
    }
    private static Entity create(Class<?> type,World world)throws ReflectiveOperationException{return (Entity)type.getConstructor(World.class,double.class,double.class,double.class).newInstance(world,0D,4D,0D);}
    private static void renderer(Class<?> type,Entity original,int models,int quads)throws ReflectiveOperationException{
        var renderer=(legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer)type.getConstructor().newInstance();
        require(RenderersB173.unsupported(renderer)==null,"original renderer admission: "+RenderersB173.unsupported(renderer));
        int[] count={0,0};
        try(var scope=RendererContextB173.open(path->net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fixture",path.replaceFirst("^/","").toLowerCase(Locale.ROOT)),(draw,texture)->count[0]++,new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f transform,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){count[1]++;}
        })){renderer.render(original,0,0,0,0,.5F);}
        require(count[0]==models&&count[1]==quads,"original renderer geometry: "+Arrays.toString(count));
    }
    private static boolean close(double a,double b){return Math.abs(a-b)<1E-7;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static final class RecordingWorld extends World {
        boolean water;int explosions,smoke;float power;Entity source;
        final List<Entity> spawned=new ArrayList<>();
        @Override public boolean isFluidInBox(Box box,legacyconnector.vb1_7_3.net.minecraft.block.material.Material material){return water;}
        @Override public List<Entity> getEntities(Entity except,Box box){return List.of();}
        @Override public int getBlockId(int x,int y,int z){return 0;}
        @Override public boolean spawnEntity(Entity entity){spawned.add(entity);return true;}
        @Override public void addParticle(String name,double x,double y,double z,double vx,double vy,double vz){if(name.equals("smoke"))smoke++;}
        @Override public legacyconnector.vb1_7_3.net.minecraft.world.explosion.Explosion createExplosion(Entity source,double x,double y,double z,float power){this.source=source;this.power=power;explosions++;return null;}
    }
}
