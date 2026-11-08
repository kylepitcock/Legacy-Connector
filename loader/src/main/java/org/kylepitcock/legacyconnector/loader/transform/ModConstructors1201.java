package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A Forge 1.20.1 {@code @Mod} constructor may take the {@code FMLJavaModLoadingContext} (the way to the mod bus); NeoForge only injects {@code IEventBus},
 * {@code ModContainer}, {@code Dist} and {@code FMLModContainer}, and wants exactly one public constructor. The original constructor becomes package
 * private and a public {@code (IEventBus, ModContainer)} one calls it with the facade's context (which reads the container FML is constructing the mod for).
 */
final class ModConstructors1201 {
    private static final String MOD = "Lnet/neoforged/fml/common/Mod;";
    private static final String CONTEXT = "Llegacyconnector/v1_20_1/net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;";
    private static final String CONTEXT_OWNER = "legacyconnector/v1_20_1/net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext";
    private static final String BUS = "Lnet/neoforged/bus/api/IEventBus;";
    private static final String CONTAINER = "Lnet/neoforged/fml/ModContainer;";
    private static final String DIST = "Lnet/neoforged/api/distmarker/Dist;";

    private ModConstructors1201() {}

    /** @return the number of main classes that got the generated constructor */
    static int apply(List<ClassNode> nodes) {
        int changed = 0;
        for (ClassNode n : nodes) {
            if (n.visibleAnnotations == null || n.visibleAnnotations.stream().noneMatch(a -> a.desc.equals(MOD))) continue;
            MethodNode original = null;
            for (MethodNode m : n.methods) {
                if (!m.name.equals("<init>") || (m.access & Opcodes.ACC_PUBLIC) == 0) continue;
                if (m.desc.contains(CONTEXT)) {
                    original = m;
                    break;
                }
            }
            if (original == null) continue;
            Type[] params = Type.getArgumentTypes(original.desc);
            boolean supported = true;
            for (Type p : params) {
                String d = p.getDescriptor();
                if (!d.equals(CONTEXT) && !d.equals(BUS) && !d.equals(CONTAINER) && !d.equals(DIST)) supported = false;
            }
            if (!supported) continue;
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(" + BUS + CONTAINER + ")V", null, null);
            bridge.visitCode();
            bridge.visitVarInsn(Opcodes.ALOAD, 0);
            for (Type p : params) {
                switch (p.getDescriptor()) {
                    case CONTEXT -> bridge.visitMethodInsn(Opcodes.INVOKESTATIC, CONTEXT_OWNER, "get", "()" + CONTEXT, false);
                    case BUS -> bridge.visitVarInsn(Opcodes.ALOAD, 1);
                    case CONTAINER -> bridge.visitVarInsn(Opcodes.ALOAD, 2);
                    default -> bridge.visitFieldInsn(Opcodes.GETSTATIC, "net/neoforged/fml/loading/FMLEnvironment", "dist", DIST);
                }
            }
            bridge.visitMethodInsn(Opcodes.INVOKESPECIAL, n.name, "<init>", original.desc, false);
            bridge.visitInsn(Opcodes.RETURN);
            bridge.visitMaxs(params.length + 2, 3);
            bridge.visitEnd();
            original.access &= ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
            List<MethodNode> methods = new ArrayList<>(n.methods);
            methods.add(bridge);
            n.methods = methods;
            changed++;
        }
        return changed;
    }
}
