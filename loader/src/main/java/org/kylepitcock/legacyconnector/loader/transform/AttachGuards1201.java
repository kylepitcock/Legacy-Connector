package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Forge 1.20.1's event bus filtered generic events by their type argument: a handler taking {@code AttachCapabilitiesEvent<Entity>} was only called for
 * entities. NeoForge 1.21.1's bus has no generic events, so the facade event reaches every handler with every object. A handler method (not a lambda:
 * javac gives those no generic signature) whose parameter is declared with a type argument gets an {@code instanceof} guard at its head, so it only
 * runs for matching objects, like on Forge.
 */
final class AttachGuards1201 {
    static final String EVENT = "legacyconnector/v1_20_1/net/minecraftforge/event/AttachCapabilitiesEvent";

    private AttachGuards1201() {}

    static int apply(ClassNode node, Set<String> jarClasses) {
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.signature == null || m.instructions == null || m.instructions.size() == 0 || !m.desc.endsWith(")V")) continue;
            Type[] args = Type.getArgumentTypes(m.desc);
            int slot = (m.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
            int want = -1;
            int wantSlot = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i].getSort() == Type.OBJECT && args[i].getInternalName().equals(EVENT)) {
                    want = i;
                    wantSlot = slot;
                    break;
                }
                slot += args[i].getSize();
            }
            if (want < 0) continue;
            String target = typeArgument(m.signature, want);
            if (target == null || !(target.startsWith("net/minecraft/") || jarClasses.contains(target))) continue;
            InsnList guard = new InsnList();
            LabelNode go = new LabelNode();
            guard.add(new VarInsnNode(Opcodes.ALOAD, wantSlot));
            guard.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EVENT, "getObject", "()Ljava/lang/Object;", false));
            guard.add(new TypeInsnNode(Opcodes.INSTANCEOF, target));
            guard.add(new JumpInsnNode(Opcodes.IFNE, go));
            guard.add(new InsnNode(Opcodes.RETURN));
            guard.add(go);
            m.instructions.insert(guard);
            n++;
        }
        return n;
    }

    /** The class named by the type argument of the {@code index}th parameter of a method signature, or null. */
    static String typeArgument(String signature, int index) {
        String[] found = new String[1];
        int[] param = {-1};
        try {
            new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public SignatureVisitor visitParameterType() {
                    param[0]++;
                    final int mine = param[0];
                    return new SignatureVisitor(Opcodes.ASM9) {
                        boolean isEvent;

                        @Override
                        public void visitClassType(String name) {
                            isEvent = name.equals(EVENT);
                        }

                        @Override
                        public SignatureVisitor visitTypeArgument(char wildcard) {
                            if (!isEvent || mine != index || wildcard != '=') return this;
                            return new SignatureVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitClassType(String name) {
                                    if (found[0] == null) found[0] = name;
                                }

                                @Override
                                public SignatureVisitor visitTypeArgument(char w) {
                                    return this;
                                }
                            };
                        }
                    };
                }
            });
        } catch (RuntimeException e) {
            return null;
        }
        return found[0];
    }
}
