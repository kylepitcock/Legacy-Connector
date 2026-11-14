package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.file.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.LivingEntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;

/** Actual ModLoader renderer registrations, isolated by callback and admitted only with an implemented rendering closure. */
public final class RenderersB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Renderers");
    public record Info(LegacyModRecord mod,Class<?> entityType,EntityRenderer renderer){}
    private static final Map<Class<?>,Info> REGISTERED=new LinkedHashMap<>();
    private static final Set<String> SUPPORTED=Set.of("render","renderCape","renderEars","renderHand","animateModel","setAngles","applyTranslation","applyHandSwingRotation","getHandSwingProgress","getHeadBob","renderMore","bindDecorationTexture","bindTexture","getDeathYaw","applyScale","getOverlayColor");
    private static final Set<String> MODEL_GL=Set.of("glPushMatrix","glPopMatrix","glTranslatef","glTranslated","glScalef","glScaled","glRotatef","glColor3f","glColor4f","glBindTexture","glEnable","glDisable","glBlendFunc","glDepthMask","glDepthFunc","glMatrixMode","glLoadIdentity","glNormal3f");
    private static boolean discovered;
    private RenderersB173(){}
    public static synchronized void discover(){
        if(discovered)return;discovered=true;
        for(LegacyModRecord mod:LegacyMods.ordered()){
            if(!PlatformB173.VERSION.equals(mod.version)||LegacyLifecycle.isFailed(mod.modernId)||!(mod.instance instanceof BaseMod base))continue;
            Map<Object,Object> captured=new LinkedHashMap<>();
            ContentB173.enter(mod);
            try{base.AddRenderer(captured);}
            catch(RuntimeException|LinkageError e){LOG.warn("Beta AddRenderer for {} failed after {} entries; completed registrations remain isolated",mod.modernId,captured.size(),e);}
            finally{ContentB173.exit();}
            for(var entry:captured.entrySet()){
                if(!(entry.getKey() instanceof Class<?> type)||!(entry.getValue() instanceof EntityRenderer renderer))continue;
                String reason=unsupported(renderer);
                if(reason!=null){LOG.warn("Beta renderer for {} not admitted: {}",type.getName(),reason);continue;}
                REGISTERED.put(type,new Info(mod,type,renderer));
                LOG.info("Captured original Beta renderer {} for {}",renderer.getClass().getName(),type.getName());
            }
        }
    }
    private static boolean api(Class<?> type){return type.getName().startsWith("legacyconnector.")||type.getName().startsWith("legacyconnector_synth.");}
    private static Set<String> apiMethods(Class<?> type)throws java.io.IOException{
        Set<String> methods=new HashSet<>();
        for(Class<?> parent=type.getSuperclass();parent!=null&&parent!=Object.class;parent=parent.getSuperclass()){
            if(!api(parent))continue;
            try(var input=parent.getResourceAsStream("/"+parent.getName().replace('.','/')+".class")){
                if(input==null)throw new java.io.IOException("missing API class bytes");
                new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                    @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        if((access&(org.objectweb.asm.Opcodes.ACC_STATIC|org.objectweb.asm.Opcodes.ACC_PRIVATE))==0&&!name.startsWith("<"))methods.add(name+descriptor);return null;
                    }
                },org.objectweb.asm.ClassReader.SKIP_CODE|org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
            }
        }
        return methods;
    }
    private static String closure(Class<?> type){
        for(Class<?> c=type;c!=null&&c!=Object.class;c=c.getSuperclass())if(c.getName().startsWith("legacyconnector_synth."))return "synthesized renderer/model superclass "+c.getName();
        try{
            Set<String> methods=apiMethods(type);
            for(Class<?> c=type;c!=null&&!api(c);c=c.getSuperclass()){
                String[] reason={null};
                try(var input=c.getResourceAsStream("/"+c.getName().replace('.','/')+".class")){
                    if(input==null)return "missing original renderer/model bytes";
                    new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                        @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                            if((access&(org.objectweb.asm.Opcodes.ACC_STATIC|org.objectweb.asm.Opcodes.ACC_PRIVATE|org.objectweb.asm.Opcodes.ACC_SYNTHETIC|org.objectweb.asm.Opcodes.ACC_BRIDGE))==0&&methods.contains(name+descriptor)&&!SUPPORTED.contains(name))reason[0]="unsupported renderer/model override "+name;
                            return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9){
                                @Override public void visitMethodInsn(int opcode,String owner,String called,String desc,boolean itf){
                                    if(owner.startsWith("legacyconnector_synth/vb1_7_3/")&&!AnimalsB173.supportedVanillaStatic(type,opcode,owner,called,desc))reason[0]="unsupported synthesized renderer/model callback "+owner+"."+called;
                                    if(owner.equals("legacyconnector/vb1_7_3/org/lwjgl/opengl/GL11")&&!MODEL_GL.contains(called))reason[0]="unsupported model GL call "+called;
                                }
                                @Override public void visitFieldInsn(int opcode,String owner,String field,String desc){if(owner.startsWith("legacyconnector_synth/vb1_7_3/"))reason[0]="unsupported synthesized renderer/model field "+owner+"."+field;}
                            };
                        }
                    },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
                }
                if(reason[0]!=null)return reason[0];
            }
            return null;
        }catch(java.io.IOException|RuntimeException|LinkageError e){return "cannot inspect renderer/model closure: "+e.getClass().getSimpleName();}
    }
    public static String unsupported(EntityRenderer renderer){
        String rendererReason=closure(renderer.getClass());if(rendererReason!=null)return rendererReason;
        try{
            for(Class<?> c=renderer.getClass();c!=null&&c!=Object.class;c=c.getSuperclass())for(var field:c.getDeclaredFields()){
                if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||!EntityModel.class.isAssignableFrom(field.getType()))continue;
                if(!field.trySetAccessible())return "inaccessible renderer model field "+field.getName();
                if(field.get(renderer) instanceof EntityModel model){String reason=closure(model.getClass());if(reason!=null)return reason;}
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){return "cannot inspect renderer models: "+error.getClass().getSimpleName();}
        if(!(renderer instanceof LivingEntityRenderer living))return null;
        return unsupportedLiving(living);
    }
    private static String unsupportedLiving(LivingEntityRenderer renderer){
        if(renderer.mcc$model()==null)return "no original model";
        String reason=closure(renderer.getClass());if(reason!=null)return reason;
        reason=closure(renderer.mcc$model().getClass());if(reason!=null)return reason;
        EntityModel decoration=renderer.mcc$decorationModel();return decoration==null?null:closure(decoration.getClass());
    }
    public static Info info(Class<?> type){
        for(Class<?> c=type;c!=null;c=c.getSuperclass()){var info=REGISTERED.get(c);if(info!=null)return info;}
        return inherited(type);
    }
    private static final Map<Class<?>,Info> INHERITED=new java.util.concurrent.ConcurrentHashMap<>();
    /** A subclass of a base-game animal the mod gave no renderer of its own is drawn as Beta's renderer manager did: by the nearest base-game ancestor's renderer. */
    private static Info inherited(Class<?> type){
        Info cached=INHERITED.get(type);if(cached!=null)return cached;
        EntityRenderer renderer=null;Class<?> base=null;
        for(Class<?> c=type;c!=null&&renderer==null;c=c.getSuperclass()){
            if(c==legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity.class){base=c;renderer=new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.PigEntityRenderer(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.PigEntityModel(),new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.PigEntityModel(.5F),.7F);}
            else if(c==legacyconnector.vb1_7_3.net.minecraft.entity.passive.ChickenEntity.class){base=c;renderer=new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.ChickenEntityRenderer(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.ChickenEntityModel(),.3F);}
            else if(c==legacyconnector.vb1_7_3.net.minecraft.entity.passive.SheepEntity.class){base=c;renderer=new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.SheepEntityRenderer(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.SheepEntityModel(),new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.SheepWoolEntityModel(),.7F);}
        }
        if(renderer==null)return null;
        LegacyModRecord owner=ContentB173.ownerOf(type);if(owner==null)return null;
        Info info=new Info(owner,type,renderer);INHERITED.put(type,info);return info;
    }
    public static boolean supports(Class<?> type){return info(type)!=null;}
    public static List<Info> all(){return List.copyOf(REGISTERED.values());}
    public static byte[] textureBytes(Info info,String name){
        if(info==null||name==null)return null;String path=name.startsWith("/")?name.substring(1):name;
        if(path.contains(":")||Arrays.asList(path.replace('\\','/').split("/")).contains(".."))return null;
        try{Path file=info.mod().container.getModInfo().getOwningFile().getFile().findResource(path.split("/"));return Files.isRegularFile(file)?Files.readAllBytes(file):null;}
        catch(java.io.IOException|RuntimeException e){return null;}
    }
}
