package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

class BetaMenuConstructorsTest implements Opcodes {
    static final String SCREEN="legacyconnector/vb1_7_3/net/minecraft/client/gui/screen/Screen",HELPER="org/kylepitcock/legacyconnector/vb1_7_3/MenuConstructorStateB173",GUI="fixture/OriginalMenu";
    static Hierarchy hierarchy(){return new Hierarchy(new ShimIndex(Map.of(SCREEN,new ClassInfo(SCREEN,"java/lang/Object",List.of(),ACC_PUBLIC),GUI,new ClassInfo(GUI,SCREEN,List.of(),ACC_PUBLIC))),BetaMenuConstructorsTest.class.getClassLoader());}
    static byte[] base(){var w=new ClassWriter(0);w.visit(V17,ACC_PUBLIC,SCREEN,null,"java/lang/Object",null);var m=w.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(RETURN);m.visitMaxs(1,1);m.visitEnd();w.visitEnd();return w.toByteArray();}
    static byte[] helper(){var w=new ClassWriter(0);w.visit(V17,ACC_PUBLIC,HELPER,null,"java/lang/Object",null);w.visitField(ACC_PUBLIC|ACC_STATIC,"arguments","[Ljava/lang/Object;",null,null).visitEnd();w.visitField(ACC_PUBLIC|ACC_STATIC,"descriptor","Ljava/lang/String;",null,null).visitEnd();var m=w.visitMethod(ACC_PUBLIC|ACC_STATIC,"record","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)V",null,null);m.visitCode();m.visitVarInsn(ALOAD,3);m.visitFieldInsn(PUTSTATIC,HELPER,"arguments","[Ljava/lang/Object;");m.visitVarInsn(ALOAD,2);m.visitFieldInsn(PUTSTATIC,HELPER,"descriptor","Ljava/lang/String;");m.visitInsn(RETURN);m.visitMaxs(1,4);m.visitEnd();w.visitEnd();return w.toByteArray();}
    static ClassNode gui(){
        var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);w.visit(V17,ACC_PUBLIC,GUI,null,SCREEN,null);
        var m=w.visitMethod(ACC_PUBLIC,"<init>","(I)V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,SCREEN,"<init>","()V",false);var branch=new Label();m.visitVarInsn(ILOAD,1);m.visitJumpInsn(IFEQ,branch);m.visitIincInsn(1,10);m.visitLabel(branch);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"<init>","(ILjava/lang/String;)V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitIntInsn(BIPUSH,7);m.visitMethodInsn(INVOKESPECIAL,GUI,"<init>","(I)V",false);m.visitIincInsn(1,100);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"<init>","(IZ)V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,SCREEN,"<init>","()V",false);var success=new Label();m.visitVarInsn(ILOAD,2);m.visitJumpInsn(IFEQ,success);m.visitTypeInsn(NEW,"java/lang/IllegalStateException");m.visitInsn(DUP);m.visitMethodInsn(INVOKESPECIAL,"java/lang/IllegalStateException","<init>","()V",false);m.visitInsn(ATHROW);m.visitLabel(success);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        w.visitEnd();var node=new ClassNode();new ClassReader(w.toByteArray()).accept(node,0);return node;
    }
    @Test void capturesEntryArgumentsAcrossThisDelegationAndMutatedLocalsWithoutRecordingFailedConstruction()throws Exception{
        var node=gui();assertEquals(3,BetaMenuConstructors.apply(node,hierarchy(),LegacyVersion.VB1_7_3));assertEquals(0,BetaMenuConstructors.apply(node,hierarchy(),LegacyVersion.VB1_7_3));
        var writer=new ClassWriter(0);node.accept(writer);var bytes=Map.of(SCREEN,base(),HELPER,helper(),GUI,writer.toByteArray());
        var loader=new ClassLoader(getClass().getClassLoader()){
            @Override protected Class<?> findClass(String name)throws ClassNotFoundException{byte[] value=bytes.get(name.replace('.','/'));if(value==null)throw new ClassNotFoundException(name);return defineClass(name,value,0,value.length);}
        };
        var type=loader.loadClass(GUI.replace('/','.'));var capture=loader.loadClass(HELPER.replace('/','.'));
        Object gui=type.getConstructor(int.class,String.class).newInstance(5,"original");
        assertArrayEquals(new Object[]{5,"original"},(Object[])capture.getField("arguments").get(null));assertEquals("(ILjava/lang/String;)V",capture.getField("descriptor").get(null));
        for(var field:type.getDeclaredFields()){assertTrue(field.isSynthetic());field.setAccessible(true);assertNull(field.get(gui),"Successful constructors release temporary references");}
        Object before=capture.getField("arguments").get(null);assertThrows(java.lang.reflect.InvocationTargetException.class,()->type.getConstructor(int.class,boolean.class).newInstance(8,true));assertSame(before,capture.getField("arguments").get(null));
        var modern=gui();assertEquals(0,BetaMenuConstructors.apply(modern,hierarchy(),LegacyVersion.V1_7_10));
        var ordinary=gui();ordinary.name="fixture/Ordinary";ordinary.superName="java/lang/Object";assertEquals(0,BetaMenuConstructors.apply(ordinary,hierarchy(),LegacyVersion.VB1_7_3));
    }
    @Test void alwaysThrowingConstructorIsSkippedWithoutAddingFields(){
        var node=gui();var method=node.methods.stream().filter(m->m.desc.equals("(IZ)V")).findFirst().orElseThrow();
        method.instructions.clear();method.instructions.add(new VarInsnNode(ALOAD,0));method.instructions.add(new MethodInsnNode(INVOKESPECIAL,SCREEN,"<init>","()V",false));method.instructions.add(new InsnNode(ACONST_NULL));method.instructions.add(new InsnNode(ATHROW));
        node.methods.removeIf(m->m!=method);
        assertEquals(0,BetaMenuConstructors.apply(node,hierarchy(),LegacyVersion.VB1_7_3));assertTrue(node.fields.isEmpty());
        assertEquals(0,BetaMenuConstructors.apply(node,hierarchy(),LegacyVersion.VB1_7_3));
    }

}
