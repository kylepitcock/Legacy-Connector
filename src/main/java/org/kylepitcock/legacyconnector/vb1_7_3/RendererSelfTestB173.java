package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Original archived renderer callbacks; these names never select production behavior. */
public final class RendererSelfTestB173 {
    private RendererSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent e)->run(e.getServer().overworld()));}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("B1.7.3 RENDERER TEST: "+detail);}
    private static Object invoke(Object object,String name,Class<?>[] types,Object... args)throws ReflectiveOperationException {
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var method=c.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,args);}catch(NoSuchMethodException ignored){}
        throw new NoSuchMethodException(name);
    }
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(object,value);return;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object get(Object object,String name)throws ReflectiveOperationException {
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static final class Capture {
        private final ArrayList<double[][]> quads=new ArrayList<>();
        private final ArrayList<net.minecraft.resources.ResourceLocation> textures=new ArrayList<>();
        void add(ModelGeometryB173.Draw draw,net.minecraft.resources.ResourceLocation texture){
            for(var box:draw.cuboids())for(var face:ModelGeometryB173.vertices(box)){
                var corners=new double[4][5];
                for(int i=0;i<4;i++){var vertex=face.get(i);var position=draw.transform().transformPosition(new org.joml.Vector3f(vertex.x()*draw.scale(),vertex.y()*draw.scale(),vertex.z()*draw.scale()));corners[i]=new double[]{position.x,position.y,position.z,vertex.u(),vertex.v()};}
                quads.add(corners);textures.add(texture);
            }
        }
        int size(){return quads.size();}
        double get(int quad,int corner,int component){return quads.get(quad)[corner][component];}
        net.minecraft.resources.ResourceLocation texture(int quad){return textures.get(quad);}
    }
    private static final java.util.Map<Capture,java.util.List<net.minecraft.resources.ResourceLocation>> TEXTURES=new java.util.IdentityHashMap<>();
    private static Capture capture(Object renderer,LivingEntity original)throws ReflectiveOperationException {
        return capture(renderer,original,0);
    }
    private static Capture capture(Object renderer,LivingEntity original,double renderY)throws ReflectiveOperationException {
        var capture=new Capture();
        var textures=new ArrayList<net.minecraft.resources.ResourceLocation>();TEXTURES.put(capture,textures);
        try(var scope=RendererContextB173.open(path->net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("b173_fixture",path.replaceFirst("^/","").toLowerCase(java.util.Locale.ROOT)),(draw,texture)->{textures.add(texture);capture.add(draw,texture);})) {
            invoke(renderer,"render",new Class<?>[]{Entity.class,double.class,double.class,double.class,float.class,float.class},original,0D,renderY,0D,0F,.5F);
            return capture;
        }
    }
    private static void samePositions(Capture expected,Capture actual,String detail){
        require(expected.size()==actual.size(),detail+" face count");
        for(int q=0;q<expected.size();q++)for(int c=0;c<4;c++)for(int k=0;k<5;k++)require(Math.abs(expected.get(q,c,k)-actual.get(q,c,k))<.0001,detail+" transformed position/UV quad="+q+" corner="+c+" component="+k+" expected="+expected.get(q,c,k)+" actual="+actual.get(q,c,k));
    }
    /** Equivalent-frame comparisons restore per-render animation counters, which actual models may advance. */
    private static java.util.Map<Field,Object> modelFrame(Object renderer)throws ReflectiveOperationException {
        Object model=invoke(renderer,"mcc$model",new Class<?>[]{});var state=new java.util.LinkedHashMap<Field,Object>();
        for(Class<?> c=model.getClass();c!=null&&c!=Object.class;c=c.getSuperclass())for(var field:c.getDeclaredFields())
            if(field.getType().isPrimitive()&&!Modifier.isStatic(field.getModifiers())&&!Modifier.isFinal(field.getModifiers())){field.setAccessible(true);state.put(field,field.get(model));}
        return state;
    }
    private static void restoreFrame(Object renderer,java.util.Map<Field,Object> state)throws ReflectiveOperationException {
        Object model=invoke(renderer,"mcc$model",new Class<?>[]{});for(var entry:state.entrySet())entry.getKey().set(model,entry.getValue());
    }
    private static String pose(LegacyB173Animal host){var view=host.legacy();return "native[x="+host.getX()+",y="+host.getY()+",z="+host.getZ()+",yaw="+host.getYRot()+",prevYaw="+host.yRotO+",head="+host.getYHeadRot()+",prevHead="+host.yHeadRotO+",body="+host.yBodyRot+",prevBody="+host.yBodyRotO+",pitch="+host.getXRot()+",prevPitch="+host.xRotO+",ticks="+host.tickCount+",pose="+host.getPose()+",ground="+host.onGround()+",motion="+host.getDeltaMovement()+"] view[eye="+view.standingEyeHeight+",age="+view.age+",body="+view.bodyYaw+",prevBody="+view.lastBodyYaw+",yaw="+view.yaw+",prevYaw="+view.prevYaw+",pitch="+view.pitch+",prevPitch="+view.prevPitch+",walk="+view.walkAnimationProgress+",walkSpeed="+view.walkAnimationSpeed+",prevWalkSpeed="+view.lastWalkAnimationSpeed+",hurt="+view.hurtTime+",death="+view.deathTime+",texture="+view.mcc$texture()+"] scalars="+AnimalRenderStateB173.capture(view);}
    private static void trackedPacket(net.minecraft.server.level.ServerLevel level,LegacyB173Animal host,AnimalsB173.Info info,Object renderer,boolean bunny)throws ReflectiveOperationException {
        if(bunny){put(host.legacy(),"adult",false);put(host.legacy(),"edad",.5F);invoke(host.legacy(),"setType",new Class<?>[]{int.class},3);}
        else host.legacy().standingEyeHeight=-.76F;
        host.flushView();require(host.save(new net.minecraft.nbt.CompoundTag()),"publish original fields before native tracking packet");
        var values=host.getEntityData().getNonDefaultValues();require(values!=null&&!values.isEmpty(),"native nondefault tracking entries exist");
        var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(host.getId(),values);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket decoded;
        try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);decoded=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);}
        finally{buffer.release();}
        var copy=new LegacyB173Animal(info.type(),level,null);
        try{
            var spawn=new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(host.getId(),host.getUUID(),host.getX(),host.getY(),host.getZ(),host.getXRot(),host.getYRot(),host.getType(),0,host.getDeltaMovement(),host.getYHeadRot());
            var spawnBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try{net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.encode(spawnBuffer,spawn);copy.recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.decode(spawnBuffer));}
            finally{spawnBuffer.release();}
            require(copy.getType()==host.getType()&&copy.position().equals(host.position())&&copy.getYHeadRot()==host.getYHeadRot(),"native spawn packet restores pose and original registered type");
            copy.getEntityData().assignValues(decoded.packedItems());copy.applyTrackedRenderData();copy.setOnGround(host.onGround());
            require(Math.abs(copy.legacy().standingEyeHeight-host.legacy().standingEyeHeight)<.0001&&copy.legacy().mcc$texture().equals(host.legacy().mcc$texture()),"native tracking packet carries eye offset and original texture consistently");
            require(Math.abs(copy.legacy().width-host.legacy().width)<.0001&&Math.abs(copy.legacy().height-host.legacy().height)<.0001,"native tracking packet carries original dimensions");
            if(bunny)require(Math.abs((Float)get(copy.legacy(),"edad")-.5F)<.0001&&(Integer)get(copy.legacy(),"typeint")==3,"native tracking packet carries original growth/type fields");
            var frame=modelFrame(renderer);var sourceGeometry=nativeCapture(host);restoreFrame(renderer,frame);var copyGeometry=nativeCapture(copy);
            samePositions(sourceGeometry,copyGeometry,"native tracking packet render source="+pose(host)+" copy="+pose(copy));
            require(sourceGeometry.texture(0).equals(copyGeometry.texture(0)),"native tracking packet preserves rendered texture resource");
            var view=copy.prepareRenderView(.5F);
            restoreFrame(renderer,frame);
            samePositions(capture(renderer,view,view.standingEyeHeight),copyGeometry,"original eye-offset render origin equals native adapter");
        }finally{copy.discard();}
    }
    private static double edge(Capture capture){double sum=0;for(int k=0;k<3;k++){double d=capture.get(0,0,k)-capture.get(0,1,k);sum+=d*d;}return Math.sqrt(sum);}
    private static Capture nativeCapture(LegacyB173Animal host){
        var capture=new Capture();
        AnimalRenderingB173.draw(host,.5F,capture::add);return capture;
    }
    private static net.minecraft.nbt.CompoundTag wire(net.minecraft.nbt.CompoundTag snapshot){
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{buffer.writeNbt(snapshot);var decoded=buffer.readNbt();require(snapshot.equals(decoded),"original scalar snapshot survives packet NBT encoding");return decoded;}
        finally{buffer.release();}
    }
    private static void geometry(Capture capture,int quads){
        require(capture.size()==quads,"original renderer exact face count "+capture.size()+" expected "+quads);
        for(int q=0;q<capture.size();q++)for(int corner=0;corner<4;corner++)for(int k=0;k<5;k++)require(Double.isFinite(capture.get(q,corner,k)),"finite original transformed positions/UVs");
        require(edge(capture)>0,"nondegenerate original geometry");
    }
    private static void spawns(net.minecraft.server.level.ServerLevel level,LegacyB173Animal host,ContentB173.Registration registration)throws ReflectiveOperationException {
        var rule=SpawnRulesB173.all().stream().filter(r->r.type()==registration.type()&&r.group().mcc$modern()==net.minecraft.world.entity.MobCategory.CREATURE).findFirst().orElseThrow();
        var plains=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).value();
        var entries=plains.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap();
        require(entries.stream().anyMatch(entry->entry.type==host.getType()&&entry.getWeight().asInt()==rule.weight()),"loaded native plains biome preserves actual AddSpawn weight "+rule.weight());
        var admitted=new java.util.HashSet<net.minecraft.world.entity.EntityType<?>>();AnimalsB173.all().forEach(i->admitted.add(i.type()));MobsB173.all().forEach(i->admitted.add(i.type()));
        for(var entry:entries)if(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getNamespace().equals(registration.mod().modernId))
            require(admitted.contains(entry.type),"loaded biome contains only admitted original host types");
        var position=level.getSharedSpawnPos().offset(160,16,160);host.setPos(position.getX()+.5,position.getY(),position.getZ()+.5);host.syncView();
        var below=net.minecraft.core.BlockPos.containing(host.legacy().x,host.legacy().boundingBox.minY-1,host.legacy().z);
        var previous=level.getBlockState(below);long time=level.getDayTime();
        put(host.legacy(),"random",new java.util.Random(){@Override public int nextInt(int bound){return 0;}});
        try{
            level.setDayTime(1000);level.setBlock(below,net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),3);
            require(host.checkSpawnRules(level,net.minecraft.world.entity.MobSpawnType.NATURAL),"native natural spawn executes original grass/light/collision callback");
            require(host.getMaxSpawnClusterSize()==host.legacy().getLimitPerChunk(),"native cluster limit executes original callback");
            level.setBlock(below,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            require(!host.checkSpawnRules(level,net.minecraft.world.entity.MobSpawnType.NATURAL),"native natural spawn respects original missing-grass refusal");
        }finally{level.setBlock(below,previous,3);level.setDayTime(time);}
    }
    private static void run(net.minecraft.server.level.ServerLevel level){
        RenderersB173.discover();
        for(var registration:ContentB173.entities()){
            String name=registration.type().getSimpleName();boolean bunny=name.equals("EntityBunny");
            if(!bunny&&!name.equals("CREEPSEntityHippo"))continue;
            try{
                var info=RenderersB173.info(registration.type());require(info!=null,"original AddRenderer registration exists for "+name);
                Object renderer=info.renderer();String rendererName=bunny?"RenderBunny":"CREEPSRenderHippo";
                require(renderer.getClass().getSimpleName().equals(rendererName),"registry retains original renderer instance");
                var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
                original.onGround=true;
                if(bunny){put(original,"typeint",3);put(original,"typechosen",false);put(original,"adult",true);}
                var adult=capture(renderer,original);geometry(adult,bunny?66:96);
                String texture=original.mcc$texture();
                require(texture!=null&&(!bunny||texture.equals("/mob/bunnyc.png")),"original render chooses original texture");
                byte[] png=RenderersB173.textureBytes(info,texture);
                require(png!=null&&png.length>8&&(png[0]&255)==137&&png[1]==80&&png[2]==78&&png[3]==71,"original PNG resource bytes");
                require(!TEXTURES.get(adult).isEmpty()&&TEXTURES.get(adult).stream().allMatch(t->t.getPath().equals(texture.substring(1).toLowerCase(java.util.Locale.ROOT))),"original texture bound to every captured part");
                for(int q=0;q<adult.size();q++)require(adult.texture(q).equals(TEXTURES.get(adult).getFirst()),"shared quad capture retains original texture binding");
                original.yaw=original.prevYaw=30;original.pitch=original.prevPitch=15;
                original.walkAnimationSpeed=original.lastWalkAnimationSpeed=.8F;original.walkAnimationProgress=1.5F;
                var animated=capture(renderer,original);geometry(animated,bunny?66:96);
                boolean animatedChange=false;for(int q=0;q<animated.size();q++)for(int c=0;c<4;c++)for(int k=0;k<3;k++)animatedChange|=Math.abs(animated.get(q,c,k)-adult.get(q,c,k))>.0001;
                require(animatedChange,"original model animation changes rendered geometry");
                original.yaw=original.prevYaw=original.pitch=original.prevPitch=0;original.walkAnimationSpeed=original.lastWalkAnimationSpeed=original.walkAnimationProgress=0;
                var nativeInfo=AnimalsB173.all().stream().filter(i->i.registration().type()==registration.type()).findFirst().orElseThrow();
                var nativeOriginal=(LivingEntity)registration.type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
                var nativeHost=new LegacyB173Animal(nativeInfo.type(),level,nativeOriginal);nativeHost.setOnGround(true);
                try {
                    spawns(level,nativeHost,registration);
                    if(bunny){put(nativeOriginal,"adult",true);put(nativeOriginal,"typeint",3);put(nativeOriginal,"typechosen",false);}
                    var nativeAdult=nativeCapture(nativeHost);geometry(nativeAdult,bunny?66:96);
                    require(Math.abs(edge(nativeAdult)/edge(adult)-1)<.0001,"native adapter preserves original renderer geometry scale");
                    require(nativeAdult.texture(0).equals(MobTexturesB173.resolve(info.mod(),nativeOriginal.mcc$texture())),"native adapter binds original generated-pack resource");
                    var resources=new java.util.LinkedHashMap<String,java.util.function.Supplier<byte[]>>();
                    new MobTexturesB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,resources);
                    var bound=nativeAdult.texture(0);var resource=resources.get("assets/"+bound.getNamespace()+"/"+bound.getPath());
                    require(resource!=null&&java.util.Arrays.equals(resource.get(),RenderersB173.textureBytes(info,nativeOriginal.mcc$texture())),"generated client pack preserves exact original PNG bytes");
                    if(bunny){
                        var source=(LivingEntity)registration.type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
                        put(source,"adult",false);put(source,"edad",.5F);put(source,"typeint",3);put(source,"typechosen",false);
                        var first=wire(AnimalRenderStateB173.capture(source));nativeHost.applyRenderState(first);
                        var nativeYoung=nativeCapture(nativeHost);geometry(nativeYoung,66);
                        require(Math.abs(edge(nativeYoung)/edge(nativeAdult)-.5)<.0001,"native adapter consumes synchronized original juvenile growth");
                        put(source,"edad",.6F);nativeHost.applyRenderState(wire(AnimalRenderStateB173.capture(source)));
                        require((Boolean)get(nativeOriginal,"typechosen"),"native snapshot update preserves prior original renderer selection");
                        var nativeGrowing=nativeCapture(nativeHost);require(Math.abs(edge(nativeGrowing)/edge(nativeAdult)-.6)<.0001,"native adapter consumes subsequent growth packet");
                    }
                    trackedPacket(level,nativeHost,nativeInfo,renderer,bunny);
                }finally{nativeHost.discard();}
                if(bunny){
                    var serverState=(LivingEntity)registration.type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
                    put(serverState,"adult",false);put(serverState,"edad",.5F);put(serverState,"typeint",3);put(serverState,"typechosen",false);
                    var firstSnapshot=AnimalRenderStateB173.capture(serverState);
                    AnimalRenderStateB173.apply(original,null,firstSnapshot);
                    var syncedYoung=capture(renderer,original);geometry(syncedYoung,66);
                    require(Math.abs(edge(syncedYoung)/edge(adult)-.5)<.0001&&(Boolean)get(original,"typechosen"),"initial server snapshot drives original growth scale and local type choice");
                    put(serverState,"edad",.6F);var nextSnapshot=AnimalRenderStateB173.capture(serverState);
                    AnimalRenderStateB173.apply(original,firstSnapshot,nextSnapshot);
                    require((Boolean)get(original,"typechosen")&&(Integer)get(original,"typeint")==3,"unrelated server growth update preserves original renderer's local type selection");
                    var syncedGrowth=capture(renderer,original);geometry(syncedGrowth,66);
                    require(Math.abs(edge(syncedGrowth)/edge(adult)-.6)<.0001,"subsequent server snapshot updates rendered growth");
                    put(original,"adult",false);put(original,"edad",.5F);
                    var young=capture(renderer,original);geometry(young,66);
                    require(Math.abs(edge(young)/edge(adult)-.5)<.0001,"original getHeadBob stretches juvenile by growth");
                    original.onGround=false;original.velocityY=.6;
                    var airborne=capture(renderer,original);geometry(airborne,66);
                    boolean moved=false;for(int q=0;q<66;q++)for(int c=0;c<4;c++)for(int k=0;k<3;k++)moved|=Math.abs(airborne.get(q,c,k)-young.get(q,c,k))>.0001;
                    require(moved,"original airborne applyScale/rotBunny changes transformed geometry");
                }else{
                    try(var scope=RendererContextB173.open(path->null,(draw,textureLocation)->{})){invoke(renderer,"applyScale",new Class<?>[]{LivingEntity.class,float.class},original,.5F);
                        var scale=RendererContextB173.matrix().getScale(new org.joml.Vector3f());
                        require(Math.abs(scale.x-2)<.0001&&Math.abs(scale.y-2)<.0001&&Math.abs(scale.z-2)<.0001,"original fattenup scales all axes by two");
                    }
                }
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173RendererTest").info("B1.7.3 RENDERER TEST: original {} registry, virtual render, {} textured faces, transforms and original scale callbacks passed (headless capture)",rendererName,bunny?66:96);
            }catch(ReflectiveOperationException e){throw new IllegalStateException("B1.7.3 RENDERER TEST: "+name,e);}
            finally{TEXTURES.clear();}
        }
    }
}
