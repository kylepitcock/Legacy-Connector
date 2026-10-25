package org.kylepitcock.legacyconnector.loader.synth;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Bytecode for the values synthesized APIs return. Chosen to keep legacy callers running: legacy code iterates
 * returned collections without null checks, but probes lookups (ids, names) with {@code != null} loops.
 */
final class Defaults {
    private Defaults() {}

    /** Collection interfaces / classes -> concrete mutable implementation. */
    private static final Map<String, String> COLLECTIONS = Map.ofEntries(
            Map.entry("java/util/List", "java/util/ArrayList"),
            Map.entry("java/util/Collection", "java/util/ArrayList"),
            Map.entry("java/lang/Iterable", "java/util/ArrayList"),
            Map.entry("java/util/ArrayList", "java/util/ArrayList"),
            Map.entry("java/util/LinkedList", "java/util/LinkedList"),
            Map.entry("java/util/Set", "java/util/HashSet"),
            Map.entry("java/util/HashSet", "java/util/HashSet"),
            Map.entry("java/util/LinkedHashSet", "java/util/LinkedHashSet"),
            Map.entry("java/util/Map", "java/util/HashMap"),
            Map.entry("java/util/HashMap", "java/util/HashMap"),
            Map.entry("java/util/LinkedHashMap", "java/util/LinkedHashMap"),
            Map.entry("java/util/TreeMap", "java/util/TreeMap"));

    /** Guava immutable collections -> their static empty factory. */
    private static final Map<String, String> IMMUTABLES = Map.of(
            "com/google/common/collect/ImmutableList", "of",
            "com/google/common/collect/ImmutableSet", "of",
            "com/google/common/collect/ImmutableMap", "of",
            "com/google/common/collect/ImmutableCollection", "com/google/common/collect/ImmutableList",
            "com/google/common/collect/ImmutableMultimap", "of",
            "com/google/common/collect/ImmutableSortedSet", "of");

    private static final String STACK_1122 = "legacyconnector/v1_12_2/net/minecraft/item/ItemStack";
    private static final String LIST_1122 = "legacyconnector/v1_12_2/net/minecraft/util/NonNullList";

    /** A synthesized 1.12.2 client class: defaults of that type are blank instances, not null. */
    static boolean blankable(Type t) {
        if (t.getSort() != Type.OBJECT) return false;
        String n = t.getInternalName();
        return n.startsWith("legacyconnector_synth/v1_12_2/net/minecraft/client/") || n.startsWith("legacyconnector_synth/v1_12_2/net/minecraftforge/client/");
    }

    static boolean isCollection(String desc) {
        return desc.startsWith("L") && COLLECTIONS.containsKey(desc.substring(1, desc.length() - 1));
    }

    static void pushDefault(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.VOID -> { }
            case Type.LONG -> mv.visitInsn(Opcodes.LCONST_0);
            case Type.FLOAT -> mv.visitInsn(Opcodes.FCONST_0);
            case Type.DOUBLE -> mv.visitInsn(Opcodes.DCONST_0);
            case Type.ARRAY -> {
                mv.visitInsn(Opcodes.ICONST_0);
                if (t.getDimensions() > 1 || t.getElementType().getSort() == Type.OBJECT) {
                    mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getType(t.getDescriptor().substring(1)).getInternalName());
                } else {
                    mv.visitIntInsn(Opcodes.NEWARRAY, switch (t.getElementType().getSort()) {
                        case Type.BOOLEAN -> Opcodes.T_BOOLEAN;
                        case Type.CHAR -> Opcodes.T_CHAR;
                        case Type.BYTE -> Opcodes.T_BYTE;
                        case Type.SHORT -> Opcodes.T_SHORT;
                        case Type.LONG -> Opcodes.T_LONG;
                        case Type.FLOAT -> Opcodes.T_FLOAT;
                        case Type.DOUBLE -> Opcodes.T_DOUBLE;
                        default -> Opcodes.T_INT;
                    });
                }
            }
            case Type.OBJECT -> {
                String impl = COLLECTIONS.get(t.getInternalName());
                String immutable = IMMUTABLES.get(t.getInternalName());
                if (t.getInternalName().equals(STACK_1122)) {
                    // 1.12 stacks are never null (callers test isEmpty())
                    mv.visitFieldInsn(Opcodes.GETSTATIC, STACK_1122, "EMPTY", "L" + STACK_1122 + ";");
                } else if (t.getInternalName().equals(LIST_1122)) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIST_1122, "create", "()L" + LIST_1122 + ";", false);
                } else if (immutable != null) {
                    String owner = immutable.equals("of") ? t.getInternalName() : immutable;
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "of", "()L" + owner + ";", false);
                } else if (impl != null) {
                    mv.visitTypeInsn(Opcodes.NEW, impl);
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, impl, "<init>", "()V", false);
                } else if (blankable(t)) {
                    // 1.12.2 client singletons (Minecraft.getMinecraft().gameSettings, ...) are chained without null checks: an inert blank object
                    mv.visitLdcInsn(t.getClassName());
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/kylepitcock/legacyconnector/core/synth/Synth", "blank", "(Ljava/lang/String;)Ljava/lang/Object;", false);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                } else {
                    // Strings stay null: lookups (EntityList.getStringFromID, ...) are probed in `!= null` loops.
                    mv.visitInsn(Opcodes.ACONST_NULL);
                }
            }
            default -> mv.visitInsn(Opcodes.ICONST_0);
        }
    }

    static void returnDefault(MethodVisitor mv, Type t) {
        pushDefault(mv, t);
        mv.visitInsn(t.getOpcode(Opcodes.IRETURN));
    }

    private static String boxed(Type t) {
        return switch (t.getSort()) {
            case Type.BOOLEAN -> "java/lang/Boolean";
            case Type.CHAR -> "java/lang/Character";
            case Type.BYTE -> "java/lang/Byte";
            case Type.SHORT -> "java/lang/Short";
            case Type.INT -> "java/lang/Integer";
            case Type.LONG -> "java/lang/Long";
            case Type.FLOAT -> "java/lang/Float";
            case Type.DOUBLE -> "java/lang/Double";
            default -> null;
        };
    }

    static void box(MethodVisitor mv, Type t) {
        String b = boxed(t);
        if (b != null) mv.visitMethodInsn(Opcodes.INVOKESTATIC, b, "valueOf", "(" + t.getDescriptor() + ")L" + b + ";", false);
    }

    /** Object on the stack -> value of type {@code t}. Primitives go through the runtime so any Number/Boolean/null works. */
    static void unbox(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.VOID -> mv.visitInsn(Opcodes.POP);
            case Type.OBJECT, Type.ARRAY -> mv.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
            default -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, SynthJarBuilder.RT, "to" + switch (t.getSort()) {
                case Type.BOOLEAN -> "Boolean";
                case Type.CHAR -> "Char";
                case Type.BYTE -> "Byte";
                case Type.SHORT -> "Short";
                case Type.LONG -> "Long";
                case Type.FLOAT -> "Float";
                case Type.DOUBLE -> "Double";
                default -> "Int";
            }, "(Ljava/lang/Object;)" + t.getDescriptor(), false);
        }
    }
}
