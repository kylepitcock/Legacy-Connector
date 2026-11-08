package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual conditional models and private-archive assets; graphics-free adapter proofs only. */
public final class MoreCreepsRenderCoverageSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsRenderCoverageTest");
    private MoreCreepsRenderCoverageSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent e)->run(e.getServer().overworld()));}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    private record Frame(List<ResourceLocation> textures,List<RendererContextB173.Style> styles,int cuboids){}
    private static Frame draw(LegacyB173Animal host){var textures=new ArrayList<ResourceLocation>();var styles=new ArrayList<RendererContextB173.Style>();int[] count={0};AnimalRenderingB173.draw(host,.5F,(draw,texture)->{textures.add(texture);styles.add(RendererContextB173.style());count[0]+=draw.cuboids().size();});require(count[0]>0,"actual native adapter captures model geometry");return new Frame(textures,styles,count[0]);}
    private static void publish(LegacyB173Animal host){require(host.save(new net.minecraft.nbt.CompoundTag()),"original scalar render state publishes through native save callback");}
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final List<LegacyB173Animal> hosts=new ArrayList<>();final Map<String,Supplier<byte[]>> resources=new HashMap<>();
        Context(net.minecraft.server.level.ServerLevel level){this.level=level;new MobTexturesB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,resources);}
        LegacyB173Animal host(String name)throws ReflectiveOperationException {var info=AnimalsB173.all().stream().filter(i->i.registration().type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&i.registration().type().getSimpleName().equals(name)).findFirst().orElseThrow();var original=(LivingEntity)info.registration().type().getConstructor(World.class).newInstance(World.mcc$wrap(level));var host=new LegacyB173Animal(info.type(),level,original);hosts.add(host);return host;}
        void texture(LegacyB173Animal host,String path){host.legacy().mcc$setTexture(path);publish(host);var frame=draw(host);var info=RenderersB173.info(host.legacy().getClass());var expected=MobTexturesB173.resolve(info.mod(),path);require(!expected.equals(ResourceLocation.withDefaultNamespace("missingno"))&&frame.textures.contains(expected),"actual renderer binds concrete variant resource "+path);var bytes=resources.get("assets/"+expected.getNamespace()+"/"+expected.getPath());require(bytes!=null&&Arrays.equals(bytes.get(),RenderersB173.textureBytes(info,path)),"generated pack preserves exact original variant PNG "+path);}
        @Override public void close(){hosts.forEach(net.minecraft.world.entity.Entity::discard);EntityViewsB173.flush();}
    }
    @FunctionalInterface private interface Proof{void run(Context c)throws Exception;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){if(ContentB173.entities().stream().noneMatch(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")))return;RenderersB173.discover();var failures=new ArrayList<String>();for(var test:List.of(new Case("complete archive PNG pack coverage",MoreCreepsRenderCoverageSelfTestB173::assets),new Case("concrete conditional texture families",MoreCreepsRenderCoverageSelfTestB173::variants),new Case("ManDog flight and BigBaby hammer tracked poses",MoreCreepsRenderCoverageSelfTestB173::poses),new Case("original GooGoat blend passes",MoreCreepsRenderCoverageSelfTestB173::goat)))try(var c=new Context(level)){test.proof.run(c);LOG.info("B1.7.3 CREEPS RENDER COVERAGE TEST: {} passed",test.name);}catch(Exception|LinkageError error){failures.add(test.name+": "+error);LOG.error("B1.7.3 CREEPS RENDER COVERAGE TEST failed: {}",test.name,error);}if(failures.isEmpty())LOG.info("B1.7.3 CREEPS RENDER COVERAGE TEST: all four original CPU proofs passed; no live GPU claim");else LOG.error("B1.7.3 CREEPS RENDER COVERAGE TEST: {} failures: {}",failures.size(),failures);}
    private static void assets(Context c)throws Exception {
        var host=c.host("CREEPSEntityBubbleScum");var info=RenderersB173.info(host.legacy().getClass());var root=info.mod().container.getModInfo().getOwningFile().getFile().getSecureJar().getRootPath();int count=0;
        try(var files=java.nio.file.Files.walk(root)){for(var file:files.filter(java.nio.file.Files::isRegularFile).filter(p->p.toString().endsWith(".png")).toList()){String path=root.relativize(file).toString().replace('\\','/');var id=MobTexturesB173.resolve(info.mod(),path);var bytes=c.resources.get("assets/"+id.getNamespace()+"/"+id.getPath());require(bytes!=null&&Arrays.equals(bytes.get(),java.nio.file.Files.readAllBytes(file)),"every archived PNG survives generated pack: "+path);count++;}}
        require(count>=233,"reviewed MoreCreeps archive exposes complete mob and item PNG set: "+count);LOG.info("B1.7.3 CREEPS RENDER COVERAGE TEST: {} original PNG assets preserved",count);
        c.texture(host,"/mob/creeps/bubblescum.png");
        var pink=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(RenderersB173.textureBytes(info,"/mob/creeps/bubblescum.png")));require(pink!=null,"original BubbleScum PNG decodes");int pinkPixels=0;var colors=new HashSet<Integer>();for(int y=0;y<pink.getHeight();y++)for(int x=0;x<pink.getWidth();x++){int pixel=pink.getRGB(x,y);colors.add(pixel);if(((pixel>>>16)&255)>150&&((pixel>>>8)&255)<100&&(pixel&255)>120)pinkPixels++;}require(pinkPixels>0&&colors.size()>16,"BubbleScum original asset deliberately contains pink colors and detailed multicolor artwork");
    }
    private static void variants(Context c)throws Exception {
        for(String[] family:new String[][]{{"CREEPSEntityBigBaby","bigbaby","0","4"},{"CREEPSEntityDigBug","digbug","0","3"},{"CREEPSEntityPreacher","preacher","0","2"},{"CREEPSEntityGooGoat","googoat","1","5"}}){var host=c.host(family[0]);for(int i=Integer.parseInt(family[2]);i<=Integer.parseInt(family[3]);i++)c.texture(host,"/mob/creeps/"+family[1]+i+".png");}
    }
    private static void poses(Context c)throws Exception {
        var dog=c.host("CREEPSEntityManDog");put(dog.legacy(),"superfly",true);publish(dog);draw(dog);var dogRenderer=(legacyconnector.vb1_7_3.net.minecraft.client.render.entity.LivingEntityRenderer)RenderersB173.info(dog.legacy().getClass()).renderer();var dogModel=dogRenderer.mcc$model();var body=get(dogModel,"bipedBody");require(Math.abs((Float)get(body,"pitch")-Math.PI/2)<.00001,"actual ManDog flying callback rotates torso horizontally");
        var baby=c.host("CREEPSEntityBigBaby");put(baby.legacy(),"hammerswing",1.1F);put(baby.legacy(),"modelsize",1.4F);publish(baby);draw(baby);var babyModel=((legacyconnector.vb1_7_3.net.minecraft.client.render.entity.LivingEntityRenderer)RenderersB173.info(baby.legacy().getClass()).renderer()).mcc$model();require(Math.abs((Float)get(get(babyModel,"armR"),"pitch")-1.1F)<.00001,"actual BigBaby renderer transfers hammer swing into original arm pose");
        var values=baby.getEntityData().getNonDefaultValues();require(values!=null,"variant tracked render fields exist");var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(baby.getId(),values);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),c.level.registryAccess());var info=AnimalsB173.all().stream().filter(i->i.type()==baby.getType()).findFirst().orElseThrow();var copy=new LegacyB173Animal(info.type(),c.level,null);c.hosts.add(copy);
        try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);copy.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());copy.applyTrackedRenderData();}finally{buffer.release();}
        require((Float)get(copy.legacy(),"hammerswing")==1.1F&&(Float)get(copy.legacy(),"modelsize")==1.4F,"native tracking packet preserves conditional hammer and scale state");draw(copy);require(Math.abs((Float)get(get(babyModel,"armR"),"pitch")-1.1F)<.00001,"replicated variant executes original hammer branch");
    }
    private static void goat(Context c)throws Exception {
        var goat=c.host("CREEPSEntityGooGoat");put(goat.legacy(),"goatsize",1F);publish(goat);var frame=draw(goat);
        require(frame.styles.stream().anyMatch(s->s.blend()&&s.blendSrc()==770&&s.blendDst()==1),"actual GooGoat base model requests additive blending");require(frame.styles.stream().anyMatch(s->s.blend()&&s.blendSrc()==770&&s.blendDst()==771),"actual GooGoat decoration requests alpha blending");
    }
}
