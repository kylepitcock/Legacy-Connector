package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The base class of a 1.20.1 mod's subclasses of {@code ItemPredicate}, written into the translated jar. In 1.21.1 {@code ItemPredicate} is a record: javac refuses
 * to compile a class that extends it (so the layer cannot ship this class as source), but the JVM accepts the subclass once the access transformer has taken
 * {@code final} off, and 1.20.1 code extended it (Mekanism's {@code CustomItemPredicate}, Apotheosis, Forbidden Arcanus, Railcraft, Slashblade).
 * <pre>
 * public class $LegacyItemPredicate extends ItemPredicate {
 *     public $LegacyItemPredicate() { super(Optional.empty(), MinMaxBounds.Ints.ANY, DataComponentPredicate.EMPTY, Map.of()); }  // 1.20.1: new ItemPredicate() matched anything
 *     public boolean test(ItemStack s) { return matches(s); }       // 1.21.1 callers ask test; the mod overrides matches
 *     public boolean matches(ItemStack s) { return super.test(s); } // 1.20.1's default
 *     public JsonElement serializeToJson() { return JsonNull.INSTANCE; }
 * }
 * </pre>
 */
final class ItemPredicateStandIn1201 {
    private static final String PRED = "net/minecraft/advancements/critereon/ItemPredicate";
    private static final String STACK = "net/minecraft/world/item/ItemStack";

    private ItemPredicateStandIn1201() {}

    static byte[] generate(String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, PRED, null);

        MethodVisitor m = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
        m.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/advancements/critereon/MinMaxBounds$Ints", "ANY", "Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;");
        m.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/core/component/DataComponentPredicate", "EMPTY", "Lnet/minecraft/core/component/DataComponentPredicate;");
        m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Map", "of", "()Ljava/util/Map;", true);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, PRED, "<init>",
                "(Ljava/util/Optional;Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;Lnet/minecraft/core/component/DataComponentPredicate;Ljava/util/Map;)V", false);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();

        m = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "(L" + STACK + ";)Z", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ALOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, name, "matches", "(L" + STACK + ";)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();

        m = cw.visitMethod(Opcodes.ACC_PUBLIC, "matches", "(L" + STACK + ";)Z", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ALOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, PRED, "test", "(L" + STACK + ";)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();

        m = cw.visitMethod(Opcodes.ACC_PUBLIC, "serializeToJson", "()Lcom/google/gson/JsonElement;", null, null);
        m.visitCode();
        m.visitFieldInsn(Opcodes.GETSTATIC, "com/google/gson/JsonNull", "INSTANCE", "Lcom/google/gson/JsonNull;");
        m.visitInsn(Opcodes.ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
