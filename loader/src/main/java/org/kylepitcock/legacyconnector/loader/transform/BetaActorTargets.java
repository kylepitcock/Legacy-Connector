package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** A runtime-proved actor fallback around original methods with one explicit entity argument. */
public final class BetaActorTargets implements Opcodes {
    private static final String ENTITY="legacyconnector/vb1_7_3/net/minecraft/entity/Entity";
    private static final String HELPER="org/kylepitcock/legacyconnector/vb1_7_3/HookActorStateB173";
    private static final String SCOPE=HELPER+"$TargetScope";
    private BetaActorTargets(){}
    public static int apply(ClassNode node,Hierarchy hierarchy,LegacyVersion version){
        if(version!=LegacyVersion.VB1_7_3||(node.access&ACC_INTERFACE)!=0||node.name.startsWith("legacyconnector/")||node.name.startsWith("legacyconnector_synth/"))return 0;
        int count=0;
        for(var method:new ArrayList<>(node.methods)){
            if(method.name.startsWith("<")||(method.access&(ACC_ABSTRACT|ACC_NATIVE|ACC_SYNTHETIC|ACC_BRIDGE))!=0||alreadyScoped(method))continue;
            Type[] args=Type.getArgumentTypes(method.desc);int slot=(method.access&ACC_STATIC)!=0?0:1,entitySlot=-1,entities=0;
            for(var arg:args){if(arg.getSort()==Type.OBJECT&&entity(arg.getInternalName(),hierarchy,0)){entities++;entitySlot=slot;}slot+=arg.getSize();}
            if(entities!=1)continue;
            String oldName=method.name,bodyName="mcc$actor$"+count+"$"+oldName;
            while(hasMethod(node,bodyName,method.desc))bodyName="mcc$"+bodyName;
            var wrapper=new MethodNode(method.access,oldName,method.desc,method.signature,method.exceptions.toArray(String[]::new));
            wrapper.visibleAnnotations=method.visibleAnnotations;method.visibleAnnotations=null;
            wrapper.invisibleAnnotations=method.invisibleAnnotations;method.invisibleAnnotations=null;
            wrapper.visibleTypeAnnotations=method.visibleTypeAnnotations;method.visibleTypeAnnotations=null;
            wrapper.invisibleTypeAnnotations=method.invisibleTypeAnnotations;method.invisibleTypeAnnotations=null;
            wrapper.visibleParameterAnnotations=method.visibleParameterAnnotations;method.visibleParameterAnnotations=null;
            wrapper.invisibleParameterAnnotations=method.invisibleParameterAnnotations;method.invisibleParameterAnnotations=null;
            wrapper.visibleAnnotableParameterCount=method.visibleAnnotableParameterCount;wrapper.invisibleAnnotableParameterCount=method.invisibleAnnotableParameterCount;
            wrapper.parameters=method.parameters;method.parameters=null;
            method.name=bodyName;method.access=(method.access&(ACC_STATIC|ACC_STRICT|ACC_VARARGS))|ACC_PRIVATE|ACC_SYNTHETIC;
            emit(node.name,wrapper,bodyName,args,entitySlot,slot);
            // Compute only the new straight-line wrapper's frames, preserving every original method's existing frames.
            var shell=new ClassNode();shell.version=node.version;shell.access=node.access;shell.name=node.name;shell.superName=node.superName;shell.interfaces=new ArrayList<>(node.interfaces);shell.methods.add(wrapper);
            var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);shell.accept(writer);
            var framed=new ClassNode();new ClassReader(writer.toByteArray()).accept(framed,0);node.methods.add(framed.methods.getFirst());count++;
        }return count;
    }
    private static boolean alreadyScoped(MethodNode method){for(var insn:method.instructions)if(insn instanceof MethodInsnNode call&&call.owner.equals(HELPER)&&call.name.equals("enterTarget"))return true;return false;}
    private static boolean hasMethod(ClassNode node,String name,String desc){return node.methods.stream().anyMatch(method->method.name.equals(name)&&method.desc.equals(desc));}
    private static boolean entity(String name,Hierarchy hierarchy,int depth){
        if(name==null||depth>64)return false;if(name.equals(ENTITY))return true;if(name.startsWith("java/"))return false;
        var info=hierarchy.find(name);return info!=null&&entity(info.superName,hierarchy,depth+1);
    }
    private static void emit(String owner,MethodNode method,String body,Type[] args,int entitySlot,int scopeSlot){
        var code=method.instructions;Type result=Type.getReturnType(method.desc);int resultSlot=scopeSlot+1,exceptionSlot=resultSlot+result.getSize();
        code.add(new LdcInsnNode(Type.getObjectType(owner)));code.add(new LdcInsnNode(method.name));code.add(new LdcInsnNode(method.desc));code.add(new VarInsnNode(ALOAD,entitySlot));
        code.add(new MethodInsnNode(INVOKESTATIC,HELPER,"enterTarget","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;L"+ENTITY+";)L"+SCOPE+";",false));code.add(new VarInsnNode(ASTORE,scopeSlot));
        var start=new LabelNode();var end=new LabelNode();var handler=new LabelNode();code.add(start);
        int slot=(method.access&ACC_STATIC)!=0?0:1;if(slot==1)code.add(new VarInsnNode(ALOAD,0));
        for(var arg:args){code.add(new VarInsnNode(arg.getOpcode(ILOAD),slot));slot+=arg.getSize();}
        code.add(new MethodInsnNode((method.access&ACC_STATIC)!=0?INVOKESTATIC:INVOKESPECIAL,owner,body,method.desc,false));
        if(result.getSort()!=Type.VOID)code.add(new VarInsnNode(result.getOpcode(ISTORE),resultSlot));code.add(end);
        close(code,scopeSlot);if(result.getSort()!=Type.VOID)code.add(new VarInsnNode(result.getOpcode(ILOAD),resultSlot));code.add(new InsnNode(result.getOpcode(IRETURN)));
        code.add(handler);code.add(new VarInsnNode(ASTORE,exceptionSlot));close(code,scopeSlot);code.add(new VarInsnNode(ALOAD,exceptionSlot));code.add(new InsnNode(ATHROW));
        method.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));
    }
    private static void close(InsnList code,int slot){code.add(new VarInsnNode(ALOAD,slot));code.add(new MethodInsnNode(INVOKEVIRTUAL,SCOPE,"close","()V",false));}
}
