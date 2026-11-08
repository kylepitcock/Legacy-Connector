package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Moves reachable renderer-owned block effects onto hosted legacy entities' authoritative tick lane. */
public final class RendererBlockEffectsB173 {
    private static final String ENTRY="(Llegacyconnector/vb1_7_3/net/minecraft/entity/Entity;DDDFF)V";
    private static final String WORLD="legacyconnector/vb1_7_3/net/minecraft/world/World";
    private static final ResourceLocation NONE=ResourceLocation.withDefaultNamespace("missingno");
    private static final ClassValue<Boolean> EFFECTS=new ClassValue<>(){
        @Override protected Boolean computeValue(Class<?> renderer){return new Scan(renderer).effects();}
    };
    private RendererBlockEffectsB173(){}
    public static boolean hasBlockEffects(LegacyB173Animal host){
        var info=RenderersB173.info(host.legacy().getClass());return info!=null&&EFFECTS.get(info.renderer().getClass());
    }
    /** Caller owns the host callback transaction, including synchronization, field flushing and publication. */
    public static void render(LegacyB173Animal host){
        if(host.level().isClientSide)throw new IllegalStateException("Renderer block effects require an authoritative level");
        var view=host.legacy();var info=RenderersB173.info(view.getClass());if(info==null||!EFFECTS.get(info.renderer().getClass()))return;
        try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(host.level()),view);var views=EntityViewsB173.callbackScope()){
            LivingEntity camera=view;var player=view.world.getClosestPlayer(view,-1D);if(player!=null)camera=player;
            var selectedCamera=camera;
            var extras=new RendererContextB173.Extras(){
                @Override public LivingEntity camera(){return selectedCamera;}
                @Override public float yaw(){return selectedCamera.yaw;}
                @Override public float pitch(){return selectedCamera.pitch;}
            };
            // Dedicated and integrated servers share the original model object with client drawing.
            synchronized(info.renderer()){
                info.renderer().mcc$camera(new legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer.LivingEntityCamera(camera,camera.yaw,camera.pitch));
                // This pass executes original decisions only. Graphics do not resolve assets or atlas cells.
                try(var graphics=RendererContextB173.open(path->NONE,(draw,texture)->{},extras)){
                    info.renderer().render(view,0,view.standingEyeHeight,0,host.getYRot(),1F);
                }
            }
        }
    }
    private record Call(int opcode,String owner,String name,String descriptor){}
    private record Method(Class<?> owner,String name,String descriptor){}
    private record Code(Map<String,List<Call>> methods){}
    private static boolean mod(Class<?> type){return type.getName().startsWith("legacyconnector_b173.");}
    private static final class Scan {
        final Class<?> renderer;final ClassLoader loader;
        final Map<Class<?>,Code> classes=new HashMap<>();final HashSet<Method> visited=new HashSet<>();
        final ArrayDeque<Method> pending=new ArrayDeque<>();
        Scan(Class<?> renderer){this.renderer=renderer;loader=renderer.getClassLoader();}
        Code code(Class<?> type){return classes.computeIfAbsent(type,key->{
            var methods=new HashMap<String,List<Call>>();
            try(var input=key.getResourceAsStream("/"+Type.getInternalName(key)+".class")){
                if(input==null)throw new IllegalStateException("Missing renderer effect class bytes: "+key.getName());
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        var calls=new java.util.ArrayList<Call>();methods.put(name+descriptor,calls);
                        return new MethodVisitor(Opcodes.ASM9){
                            @Override public void visitMethodInsn(int opcode,String owner,String called,String desc,boolean itf){calls.add(new Call(opcode,owner,called,desc));}
                            @Override public void visitInvokeDynamicInsn(String called,String desc,Handle bootstrap,Object... args){
                                for(Object arg:args)if(arg instanceof Handle handle){int opcode=switch(handle.getTag()){case Opcodes.H_INVOKESTATIC->Opcodes.INVOKESTATIC;case Opcodes.H_INVOKESPECIAL,Opcodes.H_NEWINVOKESPECIAL->Opcodes.INVOKESPECIAL;case Opcodes.H_INVOKEINTERFACE->Opcodes.INVOKEINTERFACE;default->Opcodes.INVOKEVIRTUAL;};calls.add(new Call(opcode,handle.getOwner(),handle.getName(),handle.getDesc()));}
                            }
                        };
                    }
                },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            }catch(IOException error){throw new IllegalStateException("Cannot inspect renderer effects: "+key.getName(),error);}
            return new Code(methods);
        });}
        Class<?> type(String owner){
            try{return Class.forName(owner.replace('/','.'),false,loader);}
            catch(ClassNotFoundException error){throw new IllegalStateException("Unlinked renderer effect call owner: "+owner,error);}
        }
        Method resolve(Class<?> owner,String name,String descriptor){
            for(Class<?> current=owner;current!=null&&current!=Object.class;current=current.getSuperclass()){
                if(!eligible(current))return null;
                if(code(current).methods.containsKey(name+descriptor))return new Method(current,name,descriptor);
            }return null;
        }
        boolean eligible(Class<?> type){
            // Shim renderer methods are traversed only to discover the virtual hooks actually called
            // by render(). Other shim/API methods are boundaries; mod helper methods remain reachable.
            return mod(type)||legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderer.class.isAssignableFrom(type);
        }
        boolean effects(){
            var entry=resolve(renderer,"render",ENTRY);if(entry==null)return false;pending.add(entry);
            while(!pending.isEmpty()){
                var method=pending.removeFirst();if(!visited.add(method))continue;
                for(var call:code(method.owner).methods.get(method.name+method.descriptor)){
                    if(call.owner.equals(WORLD)&&(call.name.startsWith("setBlock")||call.name.equals("removeBlockEntity")))return true;
                    // Do not inspect JDK/host methods or unrelated API method bodies.
                    if(!call.owner.startsWith("legacyconnector_b173/")&&!call.owner.startsWith("legacyconnector/vb1_7_3/net/minecraft/client/render/entity/"))continue;
                    var owner=type(call.owner);
                    if((call.opcode==Opcodes.INVOKEVIRTUAL||call.opcode==Opcodes.INVOKEINTERFACE)&&owner.isAssignableFrom(renderer))owner=renderer;
                    var next=resolve(owner,call.name,call.descriptor);if(next!=null)pending.add(next);
                }
            }return false;
        }
    }
}
