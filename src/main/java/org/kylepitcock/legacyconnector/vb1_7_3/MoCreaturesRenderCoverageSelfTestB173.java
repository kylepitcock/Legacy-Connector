package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Conditional original models, passes and scalar tracking; CPU capture does not claim GPU verification. */
public final class MoCreaturesRenderCoverageSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173MocRenderCoverageTest");
    private MoCreaturesRenderCoverageSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC RENDER COVERAGE TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private record Part(ResourceLocation texture,int color,List<Float> vertices,List<Float> uv){}
    private static final class Frame {
        final List<Part> parts=new ArrayList<>();
        void add(ModelGeometryB173.Draw draw,ResourceLocation texture){var points=new ArrayList<Float>();var uv=new ArrayList<Float>();for(var cuboid:draw.cuboids())for(var face:ModelGeometryB173.vertices(cuboid))for(var vertex:face){var p=new org.joml.Vector3f(vertex.x()*draw.scale(),vertex.y()*draw.scale(),vertex.z()*draw.scale());draw.transform().transformPosition(p);require(Float.isFinite(p.x+p.y+p.z),"finite conditional model vertices");points.add(p.x);points.add(p.y);points.add(p.z);uv.add(vertex.u());uv.add(vertex.v());}parts.add(new Part(texture,RendererContextB173.argb(),points,uv));}
        Set<ResourceLocation> textures(){var result=new HashSet<ResourceLocation>();for(var part:parts)result.add(part.texture);return result;}
        int vertices(){return parts.stream().mapToInt(p->p.vertices.size()).sum();}
        double edge(){var p=parts.getFirst().vertices;double sum=0;for(int i=0;i<3;i++)sum+=Math.pow(p.get(i)-p.get(i+3),2);return Math.sqrt(sum);}
    }
    private static Frame frame(LegacyB173Animal host){var result=new Frame();AnimalRenderingB173.draw(host,.5F,result::add);require(!result.parts.isEmpty(),"actual conditional renderer emits model parts");return result;}
    private static void same(Frame source,Frame copy,String detail){require(source.parts.size()==copy.parts.size(),detail+" model-part count");for(int p=0;p<source.parts.size();p++){var a=source.parts.get(p);var b=copy.parts.get(p);require(a.texture.equals(b.texture)&&a.color==b.color&&a.uv.equals(b.uv)&&a.vertices.size()==b.vertices.size(),detail+" model texture/color/shape");for(int i=0;i<a.vertices.size();i++)require(Math.abs(a.vertices.get(i)-b.vertices.get(i))<1E-5,detail+" transformed vertex");}}
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final ContentB173.Registration registration;
        final Map<String,java.util.function.Supplier<byte[]>> resources;final List<LegacyB173Animal> hosts=new ArrayList<>();
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration,Map<String,java.util.function.Supplier<byte[]>> resources){this.level=level;this.registration=registration;this.resources=resources;}
        LegacyB173Animal host(String name)throws ReflectiveOperationException {var type=Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());var original=(LivingEntity)type.getConstructor(World.class).newInstance(World.mcc$wrap(level));var info=AnimalsB173.all().stream().filter(i->i.registration().type()==type).findFirst().orElseThrow();var host=new LegacyB173Animal(info.type(),level,original);hosts.add(host);return host;}
        ResourceLocation texture(LegacyB173Animal host,String path){return MobTexturesB173.resolve(RenderersB173.info(host.legacy().getClass()).mod(),path);}
        Frame proof(LegacyB173Animal host,String detail){
            // Some originals choose the next frame's skin late in the decoration pass.
            frame(host);var source=frame(host);host.flushView();host.publishRenderState();
            for(var texture:source.textures()){var resource=resources.get("assets/"+texture.getNamespace()+"/"+texture.getPath());require(resource!=null,"conditional skin exists in generated client pack: "+texture);var bytes=resource.get();require(bytes.length>8&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71,"conditional skin contains PNG bytes");}
            var values=host.getEntityData().getNonDefaultValues();require(values!=null&&!values.isEmpty(),"conditional state has native tracking entries");
            var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(host.getId(),values);
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            var copy=new LegacyB173Animal(AnimalsB173.info(host.getType()).type(),level,null);hosts.add(copy);
            try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);copy.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());}finally{buffer.release();}
            copy.applyTrackedRenderData();copy.setPos(host.position());copy.setOnGround(host.onGround());frame(copy);same(source,frame(copy),detail+" native packet");return source;
        }
        @Override public void close(){for(var host:hosts)host.discard();EntityViewsB173.flush();}
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;RenderersB173.discover();var resources=new LinkedHashMap<String,java.util.function.Supplier<byte[]>>();new MobTexturesB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,resources);var cases=List.of(new Case("original roped big cat line strips",MoCreaturesRenderCoverageSelfTestB173::rope),new Case("werewolf forms and hunch",MoCreaturesRenderCoverageSelfTestB173::werewolf),new Case("ogre decoration pass",MoCreaturesRenderCoverageSelfTestB173::ogre),new Case("horse saddle chest and wings",MoCreaturesRenderCoverageSelfTestB173::horse),new Case("water creature skins and growth",MoCreaturesRenderCoverageSelfTestB173::water),new Case("furniture contents and used litter",MoCreaturesRenderCoverageSelfTestB173::furniture));var failures=new ArrayList<String>();for(var test:cases)try(var c=new Context(level,selected.get(),resources)){test.proof.run(c);LOG.info("B1.7.3 MOC RENDER COVERAGE TEST: {} passed",test.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(test.name+": "+error);LOG.error("B1.7.3 MOC RENDER COVERAGE TEST failed: {}",test.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC RENDER COVERAGE TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC RENDER COVERAGE TEST: all {} original-renderer CPU proofs passed",cases.size());}
    private static void rope(Context c)throws ReflectiveOperationException {
        var cat=c.host("EntityBigCat");cat.setPos(8,100,8);put(cat.legacy(),"adult",true);put(cat.legacy(),"tamed",true);
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"B173Rope");
        var player=new net.minecraft.server.level.ServerPlayer(c.level.getServer(),c.level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(c.level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        try{
            player.setPos(12,100,8);c.level.addNewPlayer(player);put(cat.legacy(),"roper",PlayerViewB173.of(player));cat.flushView();cat.publishRenderState();
            var lines=new ArrayList<List<RendererContextB173.Vertex>>();
            var extras=new RendererContextB173.Extras(){
                @Override public void line(RendererContextB173.Vertex start,RendererContextB173.Vertex end,org.joml.Matrix4f transform,RendererContextB173.Style style){
                    require(!style.texture2d()&&!style.lighting(),"original rope disables texture and lighting");
                    require(start.argb()==0xFF7F664C&&end.argb()==0xFF7F664C,"original rope brown reaches both native line endpoints");
                    require(Double.isFinite(start.x()+start.y()+start.z()+end.x()+end.y()+end.z()),"original rope emits finite endpoints");
                    lines.add(List.of(start,end));
                }
            };
            AnimalRenderingB173.draw(cat,.5F,(draw,texture)->{},extras);
            require(lines.size()==48,"original rope emits three 17-vertex line strips, not placeholder quads: "+lines.size());
            for(int strip=0;strip<3;strip++)for(int i=0;i<15;i++)require(lines.get(strip*16+i).getLast().equals(lines.get(strip*16+i+1).getFirst()),"original rope strip preserves adjoining endpoint continuity");
            var copy=new LegacyB173Animal(AnimalsB173.info(cat.getType()).type(),c.level,null);c.hosts.add(copy);
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),c.level.registryAccess());
            try{
                var values=cat.getEntityData().getNonDefaultValues();require(values!=null,"roped cat has tracked state");
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(cat.getId(),values));
                copy.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());
            }finally{buffer.release();}
            copy.applyTrackedRenderData();var roper=(legacyconnector.vb1_7_3.net.minecraft.entity.Entity)field(copy.legacy(),"roper").get(copy.legacy());
            require(roper!=null&&roper.mcc$modern==player&&roper.mcc$modern.getUUID().equals(player.getUUID()),"native metadata packet resolves original roper to exact player UUID");
            lines.clear();AnimalRenderingB173.draw(copy,.5F,(draw,texture)->{},extras);require(lines.size()==48,"packet clone executes all original rope line strips");
        }finally{put(cat.legacy(),"roper",null);player.discard();EntityViewsB173.flush();}
    }
    private static void werewolf(Context c)throws ReflectiveOperationException {
        var host=c.host("EntityWerewolf");put(host.legacy(),"wereboolean",false);put(host.legacy(),"humanform",true);var human=c.proof(host,"human form");require(human.textures().contains(c.texture(host,"/mob/werehuman.png")),"original human model skin selected");
        put(host.legacy(),"humanform",false);put(host.legacy(),"hunched",false);var wolf=c.proof(host,"wolf form");require(wolf.textures().contains(c.texture(host,"/mob/werewolf.png")),"original wolf model skin selected");require(!human.parts.equals(wolf.parts),"original form switch changes captured model");
        put(host.legacy(),"hunched",true);var hunched=c.proof(host,"hunched wolf");require(!wolf.parts.equals(hunched.parts),"original hunch changes transformed wolf geometry");
    }
    private static void ogre(Context c)throws ReflectiveOperationException {
        var host=c.host("EntityOgre");put(host.legacy(),"ogreattack",false);put(host.legacy(),"ogreboolean",true);var body=c.proof(host,"ogre body");put(host.legacy(),"ogreboolean",false);var decorated=c.proof(host,"ogre second model");require(decorated.vertices()>body.vertices()&&decorated.textures().contains(c.texture(host,"/mob/ogreb.png")),"original ogre decoration predicate adds second model and skin");
    }
    private static void horse(Context c)throws ReflectiveOperationException {
        var host=c.host("EntityHorse");put(host.legacy(),"adult",true);put(host.legacy(),"tamed",false);put(host.legacy(),"horseboolean",false);host.legacy().getClass().getMethod("setType",int.class).invoke(host.legacy(),1);put(host.legacy(),"rideable",false);var bare=c.proof(host,"bare horse");require(bare.textures().contains(c.texture(host,"/mob/horsea.png")),"original unsaddled horse pass skin");
        put(host.legacy(),"rideable",true);var saddled=c.proof(host,"saddled horse");require(saddled.textures().contains(c.texture(host,"/mob/horseasaddle.png")),"original saddle pass skin");
        put(host.legacy(),"chestedhorse",true);var chested=c.proof(host,"chested horse");require(chested.vertices()>saddled.vertices(),"original chest flag adds both bags");
        put(host.legacy(),"fwingb",1F);put(host.legacy(),"fwinge",1F);put(host.legacy(),"fwingc",.8F);put(host.legacy(),"fwingd",.8F);var wings=c.proof(host,"horse wings");require(!chested.parts.equals(wings.parts),"original wing animation changes geometry");
    }
    private static void water(Context c)throws ReflectiveOperationException {
        var dolphin=c.host("EntityDolphin");put(dolphin.legacy(),"tamed",false);put(dolphin.legacy(),"b",1F);for(int type:new int[]{1,6}){dolphin.legacy().getClass().getMethod("setType",int.class).invoke(dolphin.legacy(),type);var skin=c.proof(dolphin,"dolphin type "+type);require(skin.textures().contains(c.texture(dolphin,type==1?"/mob/dolphin.png":"/mob/dolphin6.png")),"original dolphin subtype skin");}
        var shark=c.host("EntityShark");put(shark.legacy(),"tamed",false);put(shark.legacy(),"b",1F);var adult=c.proof(shark,"adult shark");put(shark.legacy(),"b",.5F);var young=c.proof(shark,"juvenile shark");require(Math.abs(young.edge()/adult.edge()-.5)<1E-5,"original shark growth scales native captured model by half");
    }
    private static void furniture(Context c)throws ReflectiveOperationException {
        var bed=c.host("EntityKittyBed");put(bed.legacy(),"pickedUp",false);put(bed.legacy(),"hasMilk",false);put(bed.legacy(),"hasFood",false);var empty=c.proof(bed,"empty bed");put(bed.legacy(),"sheetcolour",14);put(bed.legacy(),"hasMilk",true);put(bed.legacy(),"hasFood",true);put(bed.legacy(),"milklevel",.5F);var full=c.proof(bed,"full colored bed");require(full.vertices()>empty.vertices(),"original bed contents add milk and food model parts");put(bed.legacy(),"pickedUp",true);var carried=c.proof(bed,"carried bed");require(carried.vertices()<full.vertices(),"original carried bed hides trays and contents");
        var litter=c.host("EntityLitterBox");put(litter.legacy(),"usedlitter",false);var clean=c.proof(litter,"clean litter");put(litter.legacy(),"usedlitter",true);var used=c.proof(litter,"used litter");require(used.vertices()==clean.vertices()&&used.parts.stream().map(Part::vertices).toList().equals(clean.parts.stream().map(Part::vertices).toList())&&!used.parts.getLast().uv.equals(clean.parts.getLast().uv),"original used litter preserves fill geometry and selects different texture coordinates");
    }
}
