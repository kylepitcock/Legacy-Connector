package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Captures exact successful original Screen constructor inputs without changing original frames or locals. */
public final class BetaMenuConstructors implements Opcodes {
    private static final String SCREEN="legacyconnector/vb1_7_3/net/minecraft/client/gui/screen/Screen";
    private static final String HELPER="org/kylepitcock/legacyconnector/vb1_7_3/MenuConstructorStateB173";
    private BetaMenuConstructors(){}
    public static int apply(ClassNode node,Hierarchy hierarchy,LegacyVersion version){
        if(version!=LegacyVersion.VB1_7_3||(node.access&ACC_INTERFACE)!=0||node.name.startsWith("legacyconnector/")||node.name.startsWith("legacyconnector_synth/")||!screen(node.name,hierarchy,0))return 0;
        if(node.methods.stream().filter(method->method.name.equals("<init>")).count()>32)return 0;
        int count=0;
        for(var method:node.methods){
            if(!method.name.equals("<init>")||method.instructions.size()>8192||already(method))continue;
            boolean returns=false;for(var instruction:method.instructions)if(instruction.getOpcode()==RETURN){returns=true;break;}
            if(!returns)continue; // An always-throwing constructor can never publish a successful GUI.
            Type[] args=Type.getArgumentTypes(method.desc);if(args.length>8)continue;
            String field="mcc$menuArguments$"+count;
            while(hasField(node,field))field="mcc$"+field;
            node.fields.add(new FieldNode(ACC_PRIVATE|ACC_SYNTHETIC|ACC_TRANSIENT,field,"[Ljava/lang/Object;",null,null));
            var entry=new InsnList();entry.add(new VarInsnNode(ALOAD,0));integer(entry,args.length);entry.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));
            int local=1;
            for(int index=0;index<args.length;index++){
                var type=args[index];entry.add(new InsnNode(DUP));integer(entry,index);entry.add(new VarInsnNode(type.getOpcode(ILOAD),local));box(entry,type);entry.add(new InsnNode(AASTORE));local+=type.getSize();
            }
            // JVM constructors may assign their own declared fields before superclass initialization.
            // A field per overload retains the outer inputs across this(...), including mutated parameter locals.
            entry.add(new FieldInsnNode(PUTFIELD,node.name,field,"[Ljava/lang/Object;"));method.instructions.insert(entry);
            for(var instruction:method.instructions.toArray())if(instruction.getOpcode()==RETURN){
                var capture=new InsnList();capture.add(new VarInsnNode(ALOAD,0));capture.add(new LdcInsnNode(Type.getObjectType(node.name)));capture.add(new LdcInsnNode(method.desc));capture.add(new VarInsnNode(ALOAD,0));capture.add(new FieldInsnNode(GETFIELD,node.name,field,"[Ljava/lang/Object;"));
                capture.add(new MethodInsnNode(INVOKESTATIC,HELPER,"record","(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)V",false));
                capture.add(new VarInsnNode(ALOAD,0));capture.add(new InsnNode(ACONST_NULL));capture.add(new FieldInsnNode(PUTFIELD,node.name,field,"[Ljava/lang/Object;"));method.instructions.insertBefore(instruction,capture);
            }
            method.maxStack=Math.max(method.maxStack,8);count++;
        }
        return count;
    }
    private static boolean hasField(ClassNode node,String name){return node.fields.stream().anyMatch(field->field.name.equals(name));}
    private static boolean already(MethodNode method){for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals(HELPER)&&call.name.equals("record"))return true;return false;}
    private static boolean screen(String name,Hierarchy hierarchy,int depth){
        if(name==null||depth>64)return false;if(name.equals(SCREEN))return true;if(name.startsWith("java/"))return false;
        var info=hierarchy.find(name);return info!=null&&screen(info.superName,hierarchy,depth+1);
    }
    private static void integer(InsnList code,int value){code.add(new IntInsnNode(BIPUSH,value));}
    private static void box(InsnList code,Type type){
        String owner=switch(type.getSort()){case Type.BOOLEAN->"java/lang/Boolean";case Type.BYTE->"java/lang/Byte";case Type.SHORT->"java/lang/Short";case Type.CHAR->"java/lang/Character";case Type.INT->"java/lang/Integer";case Type.LONG->"java/lang/Long";case Type.FLOAT->"java/lang/Float";case Type.DOUBLE->"java/lang/Double";default->null;};
        if(owner!=null)code.add(new MethodInsnNode(INVOKESTATIC,owner,"valueOf","("+type.getDescriptor()+")L"+owner+";",false));
    }
}
