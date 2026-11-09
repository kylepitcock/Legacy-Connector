package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.joml.Matrix4f;

/** Actual archive callbacks; CPU state/geometry proofs, never a claim about GPU appearance. */
public final class RenderEffectsSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173RenderEffectsTest");
    private RenderEffectsSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->run(event.getServer().overworld()));}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static Field field(Object object,String name)throws ReflectiveOperationException{for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException{field(object,name).set(object,value);}
    private record Part(RendererContextB173.Style style,int argb,int cuboids){}
    private record Quad(List<RendererContextB173.Vertex> vertices,Matrix4f matrix,ResourceLocation texture,RendererContextB173.Style style,int argb){}
    private record Frame(List<Part> parts,List<Quad> quads){}
    private static Frame draw(LegacyB173Animal host){
        var parts=new ArrayList<Part>();var quads=new ArrayList<Quad>();
        AnimalRenderingB173.draw(host,.5F,(model,texture)->parts.add(new Part(RendererContextB173.style(),RendererContextB173.argb(),model.cuboids().size())),new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f matrix,ResourceLocation texture,RendererContextB173.Style style){quads.add(new Quad(List.copyOf(vertices),new Matrix4f(matrix),texture,style,RendererContextB173.argb()));}
        });
        require(!parts.isEmpty()&&parts.stream().mapToInt(Part::cuboids).sum()>0,"actual original callback emits model geometry");
        require(!RendererContextB173.active(),"draw closes its renderer scope");return new Frame(parts,quads);
    }
    private static final class Context implements AutoCloseable{
        final net.minecraft.server.level.ServerLevel level;final List<LegacyB173Animal> hosts=new ArrayList<>();
        Context(net.minecraft.server.level.ServerLevel level){this.level=level;}
        LegacyB173Animal host(String pkg,String name)throws ReflectiveOperationException{
            var info=AnimalsB173.all().stream().filter(i->i.registration().type().getName().equals("legacyconnector_b173."+pkg+"."+name)).findFirst().orElseThrow();
            var original=(LivingEntity)info.registration().type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
            var host=new LegacyB173Animal(info.type(),level,original);hosts.add(host);return host;
        }
        @Override public void close(){hosts.forEach(net.minecraft.world.entity.Entity::discard);EntityViewsB173.flush();}
    }
    private static boolean overlay(Part part){return !part.style.texture2d()&&part.style.blend()&&part.style.depthFunc()==514;}
    private static void overlayState(Frame frame,int count,String label){
        var overlays=frame.parts.stream().filter(RenderEffectsSelfTestB173::overlay).toList();require(overlays.size()==count,label+" emits exactly one overlay per original model draw: "+overlays.size()+" expected "+count);
        for(var part:overlays){var s=part.style;require(s.depthTest()&&s.depthWrite()&&s.blendSrc()==770&&s.blendDst()==771,label+" retains original EQUAL depth and alpha blending");require((part.argb>>>24)==102&&(part.argb&65535)==0,label+" has original red alpha .4");}
    }
    private static void effects(Context c,String pkg,String name,boolean custom)throws Exception{
        var host=c.host(pkg,name);
        if(name.equals("EntityBunny")){put(host.legacy(),"typechosen",true);put(host.legacy(),"adult",true);}
        if(name.equals("EntityOgre")){put(host.legacy(),"ogreboolean",false);put(host.legacy(),"ogreattack",false);}
        if(name.equals("EntityWerewolf")){put(host.legacy(),"humanform",false);put(host.legacy(),"wereboolean",false);}
        host.publishRenderState();var healthy=draw(host);int base=healthy.parts.size();
        require(healthy.parts.stream().noneMatch(RenderEffectsSelfTestB173::overlay),name+" healthy has no tint pass");
        if(custom){String gate=name.equals("EntityOgre")?"ogreboolean":"wereboolean";put(host.legacy(),gate,true);host.publishRenderState();require(draw(host).parts.size()<base,name+" original pass gate controls additional decoration geometry");put(host.legacy(),gate,false);host.publishRenderState();}
        for(int[] state:new int[][]{{7,0},{0,8},{7,8}}){host.hurtTime=state[0];host.deathTime=state[1];var frame=draw(host);overlayState(frame,base,name);int expectedColor=(102<<24)|(Math.round(Math.max(0,Math.min(1,host.legacy().getBrightnessAtEyes(.5F)))*255)<<16);require(frame.parts.stream().filter(RenderEffectsSelfTestB173::overlay).allMatch(p->p.argb==expectedColor),name+" exact red overlay follows original brightness");require(frame.parts.size()==base*2,name+" hurt/death adds exactly one model pass, including combined state");}
        host.hurtTime=0;host.deathTime=0;var restored=draw(host);require(restored.parts.equals(healthy.parts),name+" next healthy draw restores original style, color and model draw counts");
    }
    private static void hotdog(Context c)throws Exception{
        var host=c.host("morecreepsandweirdos","CREEPSEntityHotdog");put(host.legacy(),"name","");put(host.legacy(),"dogsize",1F);put(host.legacy(),"firepower",0);host.publishRenderState();
        var cold=draw(host);require(cold.quads.isEmpty(),"original cold Hotdog emits no fire");
        put(host.legacy(),"firepower",250);host.publishRenderState();var hot=draw(host);int expected=(int)Math.ceil(host.legacy().height/host.legacy().width);
        require(hot.quads.size()==expected&&hot.parts.equals(cold.parts),"original fire adds only height/width immediate layers and retains body");
        var fire=ResourceLocation.withDefaultNamespace("textures/block/fire_0.png");
        for(var quad:hot.quads){require(quad.texture.equals(fire)&&!quad.style.lighting()&&quad.style.texture2d()&&quad.argb==-1,"original Hotdog flame is white, textured, unlit native fire_0");require(quad.vertices.size()==4,"original fire layer is a quad");for(var v:quad.vertices)require(v.argb()==-1&&Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v())&&v.u()>=0&&v.u()<=1&&v.v()>=0&&v.v()<=1,"original fire has finite white geometry and local sprite UV");require(quad.vertices.get(0).x()>quad.vertices.get(1).x()&&quad.vertices.get(2).y()>quad.vertices.get(1).y(),"original burnDog layer retains rectangular orientation");}
        float width=1;for(int layer=0;layer<hot.quads.size();layer++){
            var vertices=hot.quads.get(layer).vertices;double right=width-.5F,bottom=.75F+layer,top=2.15F+layer;
            double[][] expectedVertices={{right,bottom,0,15.99/16,15.99/16},{-.5,bottom,0,0,15.99/16},{-.5,top,0,0,0},{right,top,0,15.99/16,0}};
            for(int i=0;i<4;i++){var v=vertices.get(i);var e=expectedVertices[i];require(Math.abs(v.x()-e[0])<1E-5&&Math.abs(v.y()-e[1])<1E-5&&v.z()==e[2]&&Math.abs(v.u()-e[3])<1E-5&&Math.abs(v.v()-e[4])<1E-5,"original burnDog preserves exact layer coordinates and 15.99 pixel UV inset");}width*=.9F;
        }
        var values=host.getEntityData().getNonDefaultValues();require(values!=null,"Hotdog publishes native tracked scalar fields");
        var info=AnimalsB173.all().stream().filter(i->i.type()==host.getType()).findFirst().orElseThrow();var copy=new LegacyB173Animal(info.type(),c.level,null);c.hosts.add(copy);
        var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(host.getId(),values);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),c.level.registryAccess());
        try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);copy.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());copy.applyTrackedRenderData();}finally{buffer.release();}
        require(field(copy.legacy(),"firepower").getInt(copy.legacy())==250,"native packet preserves original Hotdog firepower");var replicated=draw(copy);require(replicated.quads.equals(hot.quads),"replicated Hotdog executes identical original fire geometry and style");
        put(host.legacy(),"firepower",0);host.publishRenderState();require(draw(host).quads.isEmpty(),"Hotdog returns to cold rendering after tracked state change");
    }
    @FunctionalInterface private interface Proof{void run(Context context)throws Exception;}
    private static void proof(net.minecraft.server.level.ServerLevel level,String name,Proof proof){try(var c=new Context(level)){proof.run(c);LOG.info("B1.7.3 RENDER EFFECTS TEST: {} passed (CPU only)",name);}catch(Exception|LinkageError error){LOG.error("B1.7.3 RENDER EFFECTS TEST failed: {}",name,error);}}
    private static void run(net.minecraft.server.level.ServerLevel level){
        RenderersB173.discover();
        boolean moc=ContentB173.entities().stream().anyMatch(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures"));
        boolean creeps=ContentB173.entities().stream().anyMatch(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos"));
        if(moc){proof(level,"ordinary Bunny hurt/death restoration",c->effects(c,"mocreatures","EntityBunny",false));proof(level,"original Ogre own overlay",c->effects(c,"mocreatures","EntityOgre",true));proof(level,"original Werewolf own overlay",c->effects(c,"mocreatures","EntityWerewolf",true));}
        if(creeps){proof(level,"ordinary Hippo hurt/death restoration",c->effects(c,"morecreepsandweirdos","CREEPSEntityHippo",false));proof(level,"original Hotdog cold/fire tracked immediate quads",RenderEffectsSelfTestB173::hotdog);}
    }
}
