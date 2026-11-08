package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.client.ModelPartsB173;

/** Client classes exercised with CPU capture only; no window, texture upload or graphics context. */
@Tag("actual-mod")
class OriginalRendererB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static Path latest(String entry)throws Exception {
        var root=Path.of("run-b173-single");org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(root),"user-owned actual mod fixtures unavailable");
        try(var paths=Files.walk(root)){
            var matches=paths.filter(p->p.toString().contains(".legacyconnector")&&p.toString().endsWith(".jar"))
                    .filter(p->{try(var zip=new ZipFile(p.toFile())){return zip.getEntry(entry)!=null;}catch(Exception e){throw new IllegalStateException(e);}})
                    .sorted(Comparator.comparingLong((Path p)->{try{return Files.getLastModifiedTime(p).toMillis();}catch(Exception e){throw new IllegalStateException(e);}}).reversed()).toList();
            org.junit.jupiter.api.Assumptions.assumeFalse(matches.isEmpty(),"translated user-owned original fixture unavailable: "+entry);
            System.out.println("Original renderer fixture: "+matches.getFirst().toAbsolutePath());return matches.getFirst();
        }
    }
    private static RenderContext.Capture capture(EntityRenderer renderer,LivingEntity entity){
        return capture(renderer,entity,null);
    }
    private static RenderContext.Capture capture(EntityRenderer renderer,LivingEntity entity,List<ModelGeometryB173.Draw> expected){
        var capture=RenderContext.beginCapture();
        try(var scope=RendererContextB173.open(path->net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fixture",path.substring(1).toLowerCase(Locale.ROOT)),(draw,texture)->{if(expected!=null)expected.add(draw);RenderContext.bindTexture(texture);ModelPartsB173.draw(draw);})){
            renderer.render(entity,0,0,0,0,.5F);return capture;
        }finally{RenderContext.endCapture();}
    }
    private static double edge(RenderContext.Capture capture){double n=0;for(int k=0;k<3;k++){double d=capture.get(0,0,k)-capture.get(0,1,k);n+=d*d;}return Math.sqrt(n);}
    private static void prove(String namespace,String entityName,String modelName,String rendererName,int faces)throws Exception {
        String prefix="legacyconnector_b173."+namespace+".";
        var jar=latest((prefix+rendererName).replace('.','/')+".class");
        var urls=new ArrayList<java.net.URL>();urls.add(jar.toUri().toURL());
        try(var entries=Files.list(jar.getParent())){for(var synth:entries.filter(p->p.getFileName().toString().startsWith("legacyconnector-synth-")&&p.toString().endsWith(".jar")).toList())urls.add(synth.toUri().toURL());}
        try(var loader=new URLClassLoader(urls.toArray(java.net.URL[]::new),OriginalRendererB173Test.class.getClassLoader())){
            var modelType=loader.loadClass(prefix+modelName);var rendererType=loader.loadClass(prefix+rendererName);
            assertFalse(rendererType.getSuperclass().getName().startsWith("legacyconnector_synth."),"fixture must be freshly translated against real renderer API");
            var model=(EntityModel)modelType.getConstructor().newInstance();
            var ctor=rendererType.getConstructor(namespace.equals("mocreatures")?EntityModel.class:modelType,float.class);
            var renderer=(EntityRenderer)ctor.newInstance(model,.25F);
            var type=loader.loadClass(prefix+entityName);var original=(LivingEntity)type.getConstructor(World.class).newInstance(new World(){});original.onGround=true;
            if(namespace.equals("mocreatures")){type.getField("adult").setBoolean(original,true);type.getField("typeint").setInt(original,3);type.getField("typechosen").setBoolean(original,false);}
            var expected=new ArrayList<ModelGeometryB173.Draw>();
            var idle=capture(renderer,original,expected);assertEquals(faces,idle.size());assertTrue(edge(idle)>0);
            int quad=0;
            for(var draw:expected)for(var box:draw.cuboids())for(var face:ModelGeometryB173.vertices(box)){
                for(int corner=0;corner<4;corner++){
                    var vertex=face.get(corner);var position=draw.transform().transformPosition(new org.joml.Vector3f(vertex.x()*draw.scale(),vertex.y()*draw.scale(),vertex.z()*draw.scale()));
                    assertEquals(position.x,idle.get(quad,corner,0),.00001);assertEquals(position.y,idle.get(quad,corner,1),.00001);assertEquals(position.z,idle.get(quad,corner,2),.00001);
                    assertEquals(vertex.u(),idle.get(quad,corner,3),.00001);assertEquals(vertex.v(),idle.get(quad,corner,4),.00001);
                }quad++;
            }
            assertEquals(faces,quad,"all 24 vertices per original part agree with common transformed capture");
            for(int q=0;q<faces;q++){assertEquals(original.mcc$texture().substring(1).toLowerCase(Locale.ROOT),idle.texture(q).getPath());for(int v=0;v<4;v++)for(int k=0;k<5;k++)assertTrue(Double.isFinite(idle.get(q,v,k)));}
            original.walkAnimationProgress=1.5F;original.walkAnimationSpeed=original.lastWalkAnimationSpeed=.8F;original.yaw=original.prevYaw=30;
            var moving=capture(renderer,original);assertEquals(faces,moving.size());boolean changed=false;for(int q=0;q<faces;q++)for(int v=0;v<4;v++)for(int k=0;k<3;k++)changed|=Math.abs(idle.get(q,v,k)-moving.get(q,v,k))>.0001;assertTrue(changed,"original model animates through actual renderer");
            if(namespace.equals("mocreatures")){
                type.getField("adult").setBoolean(original,false);type.getField("edad").setFloat(original,.5F);var young=capture(renderer,original);assertEquals(.5,edge(young)/edge(moving),.0001);
            }
        }
    }
    @Test void originalBunnyRendererEmitsNativeCaptureQuads()throws Exception {prove("mocreatures","EntityBunny","ModelBunny","RenderBunny",66);}
    @Test void originalHippoRendererEmitsNativeCaptureQuads()throws Exception {prove("morecreepsandweirdos","CREEPSEntityHippo","CREEPSModelHippo","CREEPSRenderHippo",96);}
}
