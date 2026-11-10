package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

class BetaActorTargetsTest implements Opcodes {
    static final String ENTITY="legacyconnector/vb1_7_3/net/minecraft/entity/Entity",LIVING="legacyconnector/vb1_7_3/net/minecraft/entity/LivingEntity";
    static Hierarchy hierarchy(){
        var index=new ShimIndex(Map.of(ENTITY,new ClassInfo(ENTITY,"java/lang/Object",List.of(),ACC_PUBLIC),LIVING,new ClassInfo(LIVING,ENTITY,List.of(),ACC_PUBLIC)));
        return new Hierarchy(index,BetaActorTargetsTest.class.getClassLoader());
    }
    static ClassNode owner(){var node=new ClassNode();node.version=V17;node.name="fixture/Original";node.superName="java/lang/Object";node.access=ACC_PUBLIC;return node;}
    static MethodNode method(String name,String desc,int access){
        var method=new MethodNode(access,name,desc,null,null);var type=Type.getReturnType(desc);
        switch(type.getSort()){
            case Type.VOID->method.instructions.add(new InsnNode(RETURN));
            case Type.LONG->{method.instructions.add(new InsnNode(LCONST_0));method.instructions.add(new InsnNode(LRETURN));}
            case Type.DOUBLE->{method.instructions.add(new InsnNode(DCONST_0));method.instructions.add(new InsnNode(DRETURN));}
            case Type.OBJECT,Type.ARRAY->{method.instructions.add(new InsnNode(ACONST_NULL));method.instructions.add(new InsnNode(ARETURN));}
            default->{method.instructions.add(new InsnNode(ICONST_1));method.instructions.add(new InsnNode(IRETURN));}
        }
        method.maxStack=2;method.maxLocals=16;return method;
    }
    @Test void wrapsOnlyOriginalBetaConcreteMethodsWithExactlyOneEntityParameter(){
        var node=owner();node.methods.add(method("target","(L"+ENTITY+";)Z",ACC_PUBLIC));node.methods.add(method("two","(L"+ENTITY+";L"+ENTITY+";)V",ACC_PUBLIC));node.methods.add(method("ordinary","(I)V",ACC_PUBLIC));
        assertEquals(1,BetaActorTargets.apply(node,hierarchy(),LegacyVersion.VB1_7_3));assertEquals(4,node.methods.size());
        var wrapper=node.methods.stream().filter(m->m.name.equals("target")).findFirst().orElseThrow();assertEquals(1,wrapper.tryCatchBlocks.size());
        assertTrue(node.methods.stream().anyMatch(m->m.name.startsWith("mcc$actor$")&&(m.access&ACC_PRIVATE)!=0&&(m.access&ACC_SYNTHETIC)!=0));
        assertEquals(0,BetaActorTargets.apply(node,hierarchy(),LegacyVersion.VB1_7_3));
        var modern=owner();modern.methods.add(method("target","(L"+ENTITY+";)Z",ACC_PUBLIC));assertEquals(0,BetaActorTargets.apply(modern,hierarchy(),LegacyVersion.V1_7_10));
    }
    @Test void wrapperFramesVerifyForStaticInstanceWideArgumentsReturnsAndExceptionalCleanup()throws Exception{
        for(String result:List.of("V","Z","J","D","Ljava/lang/Object;"))for(int access:List.of(ACC_PUBLIC,ACC_PUBLIC|ACC_STATIC)){
            var node=owner();var original=method("hit","(JDL"+LIVING+";I)"+result,access);original.visibleAnnotations=new ArrayList<>(List.of(new AnnotationNode("Lfixture/Marker;")));node.methods.add(original);
            assertEquals(1,BetaActorTargets.apply(node,hierarchy(),LegacyVersion.VB1_7_3));
            var wrapper=node.methods.stream().filter(m->m.name.equals("hit")).findFirst().orElseThrow();assertEquals("Lfixture/Marker;",wrapper.visibleAnnotations.getFirst().desc);
            new Analyzer<BasicValue>(new BasicInterpreter()).analyze(node.name,wrapper);
            assertEquals(2,java.util.stream.StreamSupport.stream(wrapper.instructions.spliterator(),false).filter(i->i instanceof MethodInsnNode call&&call.name.equals("close")).count());
            var writer=new ClassWriter(0);node.accept(writer);var loaded=new ClassNode();new ClassReader(writer.toByteArray()).accept(loaded,0);
            assertTrue(loaded.methods.stream().filter(m->m.name.equals("hit")).flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).anyMatch(i->i instanceof FrameNode));
        }
    }
}
