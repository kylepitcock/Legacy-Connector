package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controlled API regression on actual archived entities, not a natural transformation proof. */
public final class BoundsSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173BoundsTest");
    private BoundsSelfTestB173(){}
    public static void register(IEventBus bus){
        if(Boolean.getBoolean("legacyconnector.entitySelfTest")||Boolean.getBoolean("legacyconnector.boundsSelfTest"))
            bus.addListener((ServerStartedEvent event)->run(event.getServer().overworld()));
    }
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static boolean near(double a,double b){return Math.abs(a-b)<1E-6;}
    private static void bounds(AABB actual,AABB expected,String detail){
        require(near(actual.minX,expected.minX)&&near(actual.minY,expected.minY)&&near(actual.minZ,expected.minZ)
                &&near(actual.maxX,expected.maxX)&&near(actual.maxY,expected.maxY)&&near(actual.maxZ,expected.maxZ),detail+": "+actual+" != "+expected);
    }
    private static void run(net.minecraft.server.level.ServerLevel level){
        int passed=0;
        for(String name:new String[]{"legacyconnector_b173.mocreatures.EntityWerewolf","legacyconnector_b173.morecreepsandweirdos.CREEPSEntityEvilChicken"}){
            var selected=AnimalsB173.all().stream().filter(i->i.registration().type().getName().equals(name)).findFirst();
            if(selected.isEmpty())continue;
            LegacyB173Animal host=null;
            try{
                var info=selected.get();var world=World.mcc$wrap(level);
                var original=(LivingEntity)info.registration().type().getConstructor(World.class).newInstance(world);
                // Retain the original constructor's state for evidence; no form, timers or RNG replaced.
                LOG.info("B1.7.3 BOUNDS TEST constructor {} health={} width={} height={} bounds={}",name,original.health,original.width,original.height,original.boundingBox.mcc$box());
                var pos=level.getSharedSpawnPos().above(48);level.getChunk(pos);
                original.setPosition(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
                require(world.spawnEntity(original),"actual original native spawn");
                host=(LegacyB173Animal)original.mcc$modern;
                EntityViewsB173.flush();
                var box=host.getBoundingBox();double x=original.x,y=original.y,z=original.z;
                original.x+=.3;original.y+=.2;original.z-=.1;host.flushView();
                require(near(host.getX(),x+.3)&&near(host.getY(),y+.2-original.standingEyeHeight)&&near(host.getZ(),z-.1),"raw position remains independently observable");
                bounds(host.getBoundingBox(),box,"raw coordinate writes do not relocate collision bounds");
                host.syncView();bounds(original.boundingBox.mcc$box(),box,"sync copies actual bounds rather than deriving from raw position");
                // The original public damage API enters nested native damage/sound callbacks.
                float health=host.getHealth();require(original.damage(null,1),"original public damage enters native callback");
                require(host.getHealth()<health,"nested native damage applied");
                bounds(host.getBoundingBox(),box,"nested native callback preserves independent box");
                host.move(MoverType.SELF,Vec3.ZERO);
                bounds(host.getBoundingBox(),box,"zero movement retains independent collision box");
                require(near(host.getX(),(box.minX+box.maxX)/2)&&near(host.getY(),box.minY)&&near(host.getZ(),(box.minZ+box.maxZ)/2),"zero movement normalizes raw coordinates from box");
                original.x+=.3;original.y+=.2;original.z-=.1;host.flushView();
                host.move(MoverType.SELF,new Vec3(.125,0,0));
                var moved=box.move(.125,0,0);bounds(host.getBoundingBox(),moved,"ordinary native move starts from existing collision box");
                require(near(host.getX(),(moved.minX+moved.maxX)/2)&&near(host.getY(),moved.minY)&&near(host.getZ(),(moved.minZ+moved.maxZ)/2),"ordinary movement normalizes coordinates from box");
                EntityViewsB173.wrap(host);original.boundingBox.translate(.25,.125,-.25);var edited=original.boundingBox.mcc$box();EntityViewsB173.flush();
                bounds(host.getBoundingBox(),edited,"direct original box edits survive referenced-view merge");
                EntityViewsB173.wrap(host);original.setPosition(x+2,y+1,z+2);var placed=original.boundingBox.mcc$box();EntityViewsB173.flush();
                bounds(host.getBoundingBox(),placed,"explicit original setPosition relocates collision bounds");
                require(near(host.getX(),x+2)&&near(host.getY(),y+1-original.standingEyeHeight)&&near(host.getZ(),z+2),"explicit original setPosition relocates raw position");
                float width=original.width+.25F,height=original.height+.25F;
                original.setBoundingBoxSpacing(width,height);
                bounds(original.boundingBox.mcc$box(),placed,"original dimension setter changes size fields only");
                host.flushView();bounds(host.getBoundingBox(),placed,"native dimension publication preserves explicit original box");
                require(near(host.getBbWidth(),width)&&near(host.getBbHeight(),height),"native dimension fields receive original size");
                host.move(MoverType.SELF,new Vec3(.125,0,0));
                bounds(host.getBoundingBox(),placed.move(.125,0,0),"native movement translates existing box without resizing to changed dimension fields");
                original.setPosition(original.x,original.y,original.z);host.flushView();
                bounds(host.getBoundingBox(),original.boundingBox.mcc$box(),"later explicit placement uses updated dimensions");
                require(near(host.getBoundingBox().getXsize(),width)&&near(host.getBoundingBox().getYsize(),height),"explicit placement constructs updated size bounds");
                passed++;LOG.info("B1.7.3 BOUNDS TEST: {} controlled raw position, nested damage, native move, direct box, placement and size API proofs passed",name);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 BOUNDS TEST failed: {}",name,error);}
            finally{if(host!=null)host.discard();EntityViewsB173.flush();}
        }
        neutral(level);
        if(passed>0)LOG.info("B1.7.3 BOUNDS TEST: {} actual-mod controlled API fixtures passed; natural cycles require separate live proof",passed);
    }
    private static void neutral(net.minecraft.server.level.ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();
        if(selected.isEmpty())return;
        net.minecraft.world.entity.Entity nativeEntity=null;
        try{
            var type=Class.forName("legacyconnector_b173.morecreepsandweirdos.CREEPSEntityMoney",true,selected.get().type().getClassLoader());
            var world=World.mcc$wrap(level);
            var original=(legacyconnector.vb1_7_3.net.minecraft.entity.Entity)type.getConstructor(World.class).newInstance(world);
            var pos=level.getSharedSpawnPos().above(48);original.setPosition(pos.getX()+8.5,pos.getY(),pos.getZ()+.5);
            require(world.spawnEntity(original),"actual original neutral spawn");nativeEntity=original.mcc$modern;
            var host=(B173EntityHost)nativeEntity;EntityViewsB173.flush();var box=nativeEntity.getBoundingBox();
            original.x+=.3;original.y+=.2;original.z-=.1;host.flushView();
            bounds(nativeEntity.getBoundingBox(),box,"neutral raw coordinates preserve collision bounds");
            nativeEntity.move(MoverType.SELF,Vec3.ZERO);
            bounds(nativeEntity.getBoundingBox(),box,"neutral zero move preserves collision bounds");
            require(near(nativeEntity.getX(),(box.minX+box.maxX)/2)&&near(nativeEntity.getY(),box.minY)&&near(nativeEntity.getZ(),(box.minZ+box.maxZ)/2),"neutral zero move normalizes raw coordinates");
            original.x+=.3;original.y+=.2;original.z-=.1;host.flushView();
            nativeEntity.move(MoverType.SELF,new Vec3(.125,0,0));
            bounds(nativeEntity.getBoundingBox(),box.move(.125,0,0),"neutral native move starts from existing bounds");
            original.setBoundingBoxSpacing(original.width+.25F,original.height+.25F);host.flushView();
            nativeEntity.move(MoverType.SELF,new Vec3(.125,0,0));
            bounds(nativeEntity.getBoundingBox(),box.move(.25,0,0),"neutral native move retains independently sized bounds");
            LOG.info("B1.7.3 BOUNDS TEST: original Money controlled neutral raw position, zero/native move and independent size proofs passed");
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 BOUNDS TEST failed: original Money neutral fixture",error);}
        finally{if(nativeEntity!=null)nativeEntity.discard();EntityViewsB173.flush();}
    }
}
