package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.mapping.BetaMappings;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

class BetaShimHierarchyTest implements Opcodes {
    private static final String ROOT="legacyconnector/vb1_7_3/";
    private static final String API="AbsentToolApi";
    private static BetaMappings.Names names(){return BetaMappings.get().side(BetaMappings.Side.CLIENT);}
    private static String official(String canonical){return Objects.requireNonNull(names().officialByNamed.get("net/minecraft/"+canonical));}
    private static byte[] fixture(boolean ownField){
        String item=official("item/Item"),stack=official("item/ItemStack"),block=official("block/Block");
        var w=new ClassWriter(0);w.visit(V1_8,ACC_PUBLIC,"ToolChild",null,API,null);
        if(ownField)w.visitField(ACC_PUBLIC,"bg","I",null,null).visitEnd();
        var m=w.visitMethod(ACC_PUBLIC,"fluent","()L"+item+";",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitLdcInsn("fixture");m.visitMethodInsn(INVOKEVIRTUAL,"ToolChild","a","(Ljava/lang/String;)L"+item+";",false);m.visitInsn(ARETURN);m.visitMaxs(2,1);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"durability","()L"+item+";",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitIntInsn(SIPUSH,100);m.visitMethodInsn(INVOKEVIRTUAL,API,"e","(I)L"+item+";",false);m.visitInsn(ARETURN);m.visitMaxs(2,1);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"count","()I",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,"ToolChild","bg","I");m.visitInsn(IRETURN);m.visitMaxs(1,1);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"a","(L"+stack+";L"+block+";)F",null,null);m.visitCode();m.visitInsn(FCONST_1);m.visitInsn(FRETURN);m.visitMaxs(1,3);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
    private static ShimIndex index(boolean shadow){
        var api=new ClassInfo(ROOT+"net/minecraft/src/"+API,ROOT+"net/minecraft/src/IntermediateToolApi",List.of(),ACC_PUBLIC);
        var parent=new ClassInfo(ROOT+"net/minecraft/src/IntermediateToolApi",ROOT+"net/minecraft/item/Item",List.of(),ACC_PUBLIC);
        // Missing unrelated signature classes must not be resolved while walking this known hierarchy.
        api.methods.put("unrelated()Lfixture/not/Installed;",ACC_PUBLIC);api.fields.put("optional:Lfixture/not/Installed;",ACC_PUBLIC);
        if(shadow)api.fields.put("bg:I",ACC_PUBLIC);
        return new ShimIndex(new HashMap<>(Map.of(api.name,api,parent.name,parent)));
    }
    private static ClassNode translate(ShimIndex shim,boolean ownField){
        Map<String,byte[]> classes=new LinkedHashMap<>();classes.put("ToolChild",fixture(ownField));
        BetaDeobfuscator.apply(new LegacyJar(Path.of("known-api-hierarchy.jar")),classes,shim);
        var node=new ClassNode();new ClassReader(classes.values().iterator().next()).accept(node,0);return node;
    }
    private static MethodInsnNode call(ClassNode node,String method){for(var instruction:node.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow().instructions)if(instruction instanceof MethodInsnNode call)return call;throw new AssertionError("call missing");}
    private static FieldInsnNode field(ClassNode node){for(var instruction:node.methods.stream().filter(m->m.name.equals("count")).findFirst().orElseThrow().instructions)if(instruction instanceof FieldInsnNode field)return field;throw new AssertionError("field missing");}
    @Test void absentIndexedApiParentMapsInheritedFluentCallsFieldsAndVanillaOverrides(){
        var node=translate(index(false),false);
        assertEquals("setTranslationKey",call(node,"fluent").name);assertEquals("setMaxDamage",call(node,"durability").name);
        assertEquals("net/minecraft/src/"+API,node.superName);
        assertEquals("net/minecraft/item/Item",field(node).owner);assertEquals("maxCount",field(node).name);
        assertTrue(node.methods.stream().anyMatch(m->m.name.equals("getMiningSpeedMultiplier")&&m.desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/Block;)F")));
    }
    @Test void unindexedMissingApiIsNotGuessedFromMemberSpelling(){
        var node=translate(ShimIndex.empty(),false);assertEquals("a",call(node,"fluent").name);assertEquals("e",call(node,"durability").name);
        assertEquals("bg",field(node).name);assertEquals(node.name,field(node).owner);
        assertTrue(node.methods.stream().anyMatch(m->m.name.equals("a")&&m.desc.endsWith(")F")));
    }
    @Test void localOrIndexedApiFieldsRetainTheirShadowingIdentity(){
        var local=translate(index(false),true);assertEquals(local.name,field(local).owner);assertEquals("bg",field(local).name);
        var api=translate(index(true),false);assertEquals("net/minecraft/src/"+API,field(api).owner);assertEquals("bg",field(api).name);
    }
}
