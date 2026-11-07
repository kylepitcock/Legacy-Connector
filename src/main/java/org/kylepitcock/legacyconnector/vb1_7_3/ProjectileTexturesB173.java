package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Constant textures bound by registered legacy renderers, without loading client render classes on a server. */
public final class ProjectileTexturesB173 {
    private record Texture(ResourceLocation id, byte[] png) {}
    private static final Map<String,Texture> TEXTURES=new java.util.concurrent.ConcurrentHashMap<>();
    public static final ResourceLocation FALLBACK=ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public static void discover() {
        for(var mod:org.kylepitcock.legacyconnector.core.LegacyMods.all())
            if(PlatformB173.VERSION.equals(mod.version) && mod.instance!=null)
                registrations(mod,mod.mainClassName.replace('.','/'),"AddRenderer",new HashSet<>());
    }
    private static ClassNode read(LegacyModRecord mod,String name) throws java.io.IOException {
        var path=mod.container.getModInfo().getOwningFile().getFile().findResource((name+".class").split("/"));
        ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(path)).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return node;
    }
    private static void registrations(LegacyModRecord mod,String owner,String method,Set<String> seen) {
        if(!owner.startsWith("legacyconnector_b173/") || !seen.add(owner+"#"+method))return;
        try {
            for(MethodNode m:read(mod,owner).methods) {
                if(!m.name.equals(method))continue;
                String entity=null,renderer=null;
                for(var ins:m.instructions) {
                    if(ins instanceof LdcInsnNode ldc && ldc.cst instanceof Type t) { entity=t.getInternalName();renderer=null; }
                    if(ins instanceof TypeInsnNode type && type.getOpcode()==Opcodes.NEW && entity!=null && renderer==null)renderer=type.desc;
                    if(ins instanceof MethodInsnNode call) {
                        if(call.getOpcode()==Opcodes.INVOKESTATIC && call.desc.contains("Ljava/util/Map;"))registrations(mod,call.owner,call.name,seen);
                        if(call.name.equals("put") && call.owner.equals("java/util/Map")) {
                            if(entity!=null && renderer!=null) texture(mod,entity,renderer);
                            entity=null;renderer=null;
                        }
                    }
                }
            }
        } catch(java.io.IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").debug("Could not inspect renderer registrations {}",owner,e);
        }
    }
    private static void texture(LegacyModRecord mod,String entity,String renderer) throws java.io.IOException {
        Set<String> paths=new LinkedHashSet<>();
        for(MethodNode method:read(mod,renderer).methods)for(var ins:method.instructions) {
            if(ins instanceof MethodInsnNode call && call.name.equals("bindTexture") && call.desc.equals("(Ljava/lang/String;)V")) {
                var prev=ins.getPrevious();while(prev!=null && prev.getOpcode()<0)prev=prev.getPrevious();
                if(prev instanceof LdcInsnNode ldc && ldc.cst instanceof String path && path.endsWith(".png"))paths.add(path);
            }
        }
        // Animated/conditional selections need a real renderer bridge; don't guess among several textures.
        if(paths.size()!=1)return;
        String path=paths.iterator().next().replaceFirst("^/","");
        var file=mod.container.getModInfo().getOwningFile().getFile().findResource(path.split("/"));
        if(!Files.isRegularFile(file))return;
        var id=ResourceLocation.fromNamespaceAndPath(mod.modernId,"textures/b173/projectiles/"+renderer.substring(renderer.lastIndexOf('/')+1).toLowerCase(Locale.ROOT)+".png");
        TEXTURES.put(entity.replace('/','.'),new Texture(id,Files.readAllBytes(file)));
    }
    public static ResourceLocation of(Class<?> entity) { var t=TEXTURES.get(entity.getName());return t==null?FALLBACK:t.id; }
    public static void contribute(Map<String,Supplier<byte[]>> out) {
        for(var t:TEXTURES.values())out.put("assets/"+t.id.getNamespace()+"/"+t.id.getPath(),()->t.png);
    }
}
