package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** A readable mod field must not capture an inherited field renamed from an obfuscated reference. */
class BetaFieldShadowTest implements Opcodes {
    @Test void staticModRandomDoesNotShadowMappedInstanceRandom()throws Exception {
        var names=org.kylepitcock.legacyconnector.loader.mapping.BetaMappings.get().side(org.kylepitcock.legacyconnector.loader.mapping.BetaMappings.Side.CLIENT);
        String entity=names.officialByNamed.get("net/minecraft/entity/Entity");
        assertNotNull(entity,"generated Beta names available");
        String field=null;
        for(String candidate:List.of("bs","bN","random"))if("random".equals(names.field(entity,candidate,"Ljava/util/Random;")))field=candidate;
        assertNotNull(field,"original instance Random field mapped");
        var bytes=new ClassWriter(0);bytes.visit(V1_8,ACC_PUBLIC,"Shadow",null,entity,null);
        bytes.visitField(ACC_PUBLIC|ACC_STATIC,"random","Ljava/util/Random;",null,null).visitEnd();
        var ctor=bytes.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(ALOAD,0);ctor.visitMethodInsn(INVOKESPECIAL,entity,"<init>","()V",false);ctor.visitInsn(RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();
        var read=bytes.visitMethod(ACC_PUBLIC,"inherited","()Ljava/util/Random;",null,null);read.visitCode();read.visitVarInsn(ALOAD,0);read.visitFieldInsn(GETFIELD,"Shadow",field,"Ljava/util/Random;");read.visitInsn(ARETURN);read.visitMaxs(1,1);read.visitEnd();
        var own=bytes.visitMethod(ACC_PUBLIC|ACC_STATIC,"own","()Ljava/util/Random;",null,null);own.visitCode();own.visitFieldInsn(GETSTATIC,"Shadow","random","Ljava/util/Random;");own.visitInsn(ARETURN);own.visitMaxs(1,0);own.visitEnd();bytes.visitEnd();
        Map<String,byte[]> classes=new LinkedHashMap<>();classes.put("Shadow",bytes.toByteArray());
        BetaDeobfuscator.apply(new LegacyJar(Path.of("field-shadow.jar")),classes);
        var node=new ClassNode();new ClassReader(classes.values().iterator().next()).accept(node,0);
        var inherited=(FieldInsnNode)node.methods.stream().filter(m->m.name.equals("inherited")).findFirst().orElseThrow().instructions.get(1);
        assertEquals("net/minecraft/entity/Entity",inherited.owner);assertEquals("random",inherited.name);
        var ownField=(FieldInsnNode)node.methods.stream().filter(m->m.name.equals("own")).findFirst().orElseThrow().instructions.getFirst();
        assertEquals(node.name,ownField.owner);assertEquals(GETSTATIC,ownField.getOpcode());
        var base=new ClassWriter(0);base.visit(V1_8,ACC_PUBLIC,"net/minecraft/entity/Entity",null,"java/lang/Object",null);base.visitField(ACC_PUBLIC,"random","Ljava/util/Random;",null,null).visitEnd();
        var init=base.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(ALOAD,0);init.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitVarInsn(ALOAD,0);init.visitTypeInsn(NEW,"java/util/Random");init.visitInsn(DUP);init.visitMethodInsn(INVOKESPECIAL,"java/util/Random","<init>","()V",false);init.visitFieldInsn(PUTFIELD,"net/minecraft/entity/Entity","random","Ljava/util/Random;");init.visitInsn(RETURN);init.visitMaxs(3,1);init.visitEnd();base.visitEnd();
        class Loader extends ClassLoader {Class<?> define(String name,byte[] data){return defineClass(name,data,0,data.length);}}
        var loader=new Loader();loader.define("net.minecraft.entity.Entity",base.toByteArray());var mapped=loader.define(node.name.replace('/','.'),classes.values().iterator().next());
        var instance=mapped.getConstructor().newInstance();assertInstanceOf(Random.class,mapped.getMethod("inherited").invoke(instance));assertNull(mapped.getMethod("own").invoke(null));
    }
}
