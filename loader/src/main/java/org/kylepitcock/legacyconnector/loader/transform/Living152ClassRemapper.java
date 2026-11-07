package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;

/**
 * A {@link ClassRemapper} for Forge 1.5.2 jars that keeps the real {@code EntityLiving} where the jar means the class itself rather than the
 * "any living entity" type: a mod entity's superclass and the {@code invokespecial} calls on it (the constructor chain, {@code super.method()}).
 * Everywhere else the class rename turns {@code EntityLiving} into {@code EntityLivingBase} (see {@link LivingCasts152}). The names it keeps carry
 * {@link LegacyRemapper#LIVING_KEEP}, which {@link LegacyRemapper#map} resolves.
 */
final class Living152ClassRemapper extends ClassRemapper {
    private final LegacyRemapper remapper;

    Living152ClassRemapper(ClassVisitor next, LegacyRemapper remapper) {
        super(Opcodes.ASM9, next, remapper);
        this.remapper = remapper;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (remapper.isLiving152(superName)) superName = superName + LegacyRemapper.LIVING_KEEP;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mapped = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mapped == null) return null;
        return new MethodVisitor(Opcodes.ASM9, mapped) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKESPECIAL && remapper.isLiving152(owner)) owner = owner + LegacyRemapper.LIVING_KEEP;
                super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
            }
        };
    }
}
