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

/** Actual conditional renderer callbacks exercise the native living immediate-mode sink. */
public final class MoreCreepsRenderVariantSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsRenderVariantTest");
    private static final ResourceLocation TERRAIN=ResourceLocation.fromNamespaceAndPath("legacyconnector","b173/sheets/terrain");
    private static final ResourceLocation FIRE=ResourceLocation.withDefaultNamespace("textures/block/fire_0.png");
    private MoreCreepsRenderVariantSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS RENDER VARIANT TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);field.set(object,value);return;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private record Quad(List<RendererContextB173.Vertex> vertices,ResourceLocation texture){}
    private static final class Capture implements RendererContextB173.Extras {
        final List<Quad> quads=new ArrayList<>();int modelDraws;LivingEntity eye;
        @Override public LivingEntity camera(){return eye;}
        @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f transform,ResourceLocation texture,RendererContextB173.Style style){quads.add(new Quad(List.copyOf(vertices),texture));}
        void model(ModelGeometryB173.Draw draw,ResourceLocation texture){modelDraws++;}
    }
    private static Capture living(LegacyB173Animal host){var capture=new Capture();AnimalRenderingB173.draw(host,.5F,capture::model,capture);return capture;}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&r.type().getSimpleName().equals("CREEPSEntityHotdog")).findFirst();if(selected.isEmpty())return;
        RenderersB173.discover();var registration=selected.get();var failures=new ArrayList<String>();
        try{
            var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
            put(original,"name","");put(original,"dogsize",1F);put(original,"firepower",0);
            var nativeInfo=AnimalsB173.all().stream().filter(i->i.registration().type()==registration.type()).findFirst().orElseThrow();
            var host=new LegacyB173Animal(nativeInfo.type(),level,original);
            try{
                var cold=living(host);require(cold.modelDraws>0&&cold.quads.isEmpty(),"original zero-firepower Hotdog draws body without fire overlay");
                put(original,"firepower",1);
                var info=RenderersB173.info(registration.type());require(info!=null,"original Hotdog renderer registered");
                // Capture original atlas coordinates separately: no fixture patches to vanilla Block.FIRE.
                var raw=new Capture();
                info.renderer().mcc$camera(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer.LivingEntityCamera(original,0,0));
                try(var scope=RendererContextB173.open(path->path.equals("/terrain.png")?TERRAIN:MobTexturesB173.resolve(info.mod(),path),raw::model,raw)){
                    info.renderer().render(original,0,original.standingEyeHeight,0,0,.5F);
                }
                int expected=(int)Math.ceil(original.height/original.width);
                require(expected>0&&raw.quads.size()==expected,"original burnDog emits one first-loop quad per height/width layer; exhausted second loop emits none");
                try{
                    // Clean Beta uu.<clinit>: new yq(51,31). The mod reads FIRE.textureId directly.
                    for(var quad:raw.quads){double minU=quad.vertices.stream().mapToDouble(RendererContextB173.Vertex::u).min().orElseThrow();double minV=quad.vertices.stream().mapToDouble(RendererContextB173.Vertex::v).min().orElseThrow();require(quad.texture.equals(TERRAIN)&&Math.abs(minU-15D/16)<1E-6&&Math.abs(minV-1D/16)<1E-6,"original FIRE texture input must preserve Beta atlas cell31: texture="+quad.texture+" minU="+minU+" minV="+minV);}
                    LOG.info("B1.7.3 CREEPS RENDER VARIANT TEST: original fire atlas input passed");
                }catch(RuntimeException error){failures.add(error.getMessage());LOG.error("B1.7.3 CREEPS RENDER VARIANT TEST: original fire atlas input failed",error);}
                try{
                    var actual=living(host);require(actual.modelDraws>0&&actual.quads.size()==expected,"native living sink retains original body and conditional fire quad count");
                    for(var quad:actual.quads){require(quad.texture.equals(FIRE),"native living fire overlay must resolve original terrain cell to native fire sprite: "+quad.texture);for(var v:quad.vertices)require(Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v())&&v.u()>=0&&v.u()<=1&&v.v()>=0&&v.v()<=1,"native living fire overlay has finite geometry and sprite-local UVs");}
                    var reference=new Capture();reference.eye=original;EntityRenderingB173.drawPrepared(host,host.prepareRenderView(.5F),.5F,reference::model,reference);
                    require(actual.quads.equals(reference.quads),"living native sink matches nonliving original-renderer atlas resolution");
                    for(int i=0;i<raw.quads.size();i++){var q=raw.quads.get(i);var resolved=EntityRenderingB173.resolveQuad(q.vertices,q.texture);require(actual.quads.get(i).equals(new Quad(resolved.vertices(),resolved.texture())),"living immediate quad matches shared resolver coordinates and sprite");}
                    LOG.info("B1.7.3 CREEPS RENDER VARIANT TEST: original Hotdog cold/fire native sprite and UV proofs passed");
                }catch(RuntimeException error){failures.add(error.getMessage());LOG.error("B1.7.3 CREEPS RENDER VARIANT TEST: native fire overlay failed",error);}
            }finally{host.discard();EntityViewsB173.flush();}
        }catch(ReflectiveOperationException error){throw new IllegalStateException("original Hotdog variant fixture",error);}
        if(!failures.isEmpty())LOG.error("B1.7.3 CREEPS RENDER VARIANT TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 CREEPS RENDER VARIANT TEST: all original-callback proofs passed");
    }
}
