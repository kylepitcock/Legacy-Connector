package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

class BetaClassForNameTest implements Opcodes {
    static final String HELPER="org/kylepitcock/legacyconnector/vb1_7_3/ClassNamesB173";

    /** A Beta mod's {@code Class.forName("DimensionTwilightForest")}: a bare name of its own (relocated) package. */
    static ClassNode mod(){
        var node=new ClassNode();node.version=V1_6;node.access=ACC_PUBLIC;node.name="legacyconnector_b173/fixture/mod_Fixture";node.superName="java/lang/Object";
        var method=new MethodNode(ACC_PUBLIC,"load","()V",null,null);
        method.instructions.add(new LdcInsnNode("DimensionFixture"));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC,"java/lang/Class","forName","(Ljava/lang/String;)Ljava/lang/Class;",false));
        method.instructions.add(new InsnNode(POP));method.instructions.add(new InsnNode(RETURN));method.maxStack=1;method.maxLocals=1;
        node.methods.add(method);return node;
    }
    static MethodInsnNode call(ClassNode node){
        for(var insn:node.methods.get(0).instructions)if(insn instanceof MethodInsnNode call&&call.name.equals("forName"))return call;
        return null;
    }

    @Test void forNameAlreadyRedirectedByTheJdkCompatibilityPassStillResolvesTheModsOwnBareNames(){
        var node=mod();
        // the JDK pass runs first in the transformer and replaces Class.forName with its own helper
        assertTrue(JavaCompatRedirects.apply(node)>0);
        assertEquals("org/kylepitcock/legacyconnector/compat/jdk/ReflectionCompat",call(node).owner);
        assertEquals(1,BetaClassForName.apply(node,LegacyVersion.VB1_7_3));
        var call=call(node);
        assertEquals(HELPER,call.owner);assertEquals("(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;",call.desc);
        assertEquals(Type.getObjectType(node.name),((LdcInsnNode)call.getPrevious()).cst,"the caller class names the package to search");
    }

    @Test void plainForNameIsRewrittenAndOtherVersionsAreLeftAlone(){
        var node=mod();assertEquals(1,BetaClassForName.apply(node,LegacyVersion.VB1_7_3));assertEquals(HELPER,call(node).owner);
        var other=mod();assertEquals(0,BetaClassForName.apply(other,LegacyVersion.V1_7_10));assertEquals("java/lang/Class",call(other).owner);
    }
}
