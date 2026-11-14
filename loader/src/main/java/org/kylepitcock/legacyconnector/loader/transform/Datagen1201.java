package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Data generation code of a Forge 1.20.1 mod is dead code in a running pack: {@code runData} never runs there, and the data it would write is already in the jar.
 * 1.21.1 changed the datagen API completely (RecipeOutput instead of FinishedRecipe, Criterion instead of CriterionTriggerInstance, registry lookups in the
 * providers), so keeping the code only produces unresolved references. This pass takes it out of the translated classes:
 * <ul>
 *   <li>a class that is a data provider (extends or implements a {@code net.minecraft.data}, model-generator or NeoForge data class, or a jar class that already is
 *       one, or is nested in one) becomes an empty shell: no fields, no interfaces, constructors that throw;</li>
 *   <li>a method of any other class that takes a {@code GatherDataEvent}, or whose body uses only-datagen classes (recipe/advancement builders, providers), keeps its
 *       signature and returns the default value.</li>
 * </ul>
 * Nothing in a running game calls these; the jar's own resources (data pack JSON) are untouched.
 */
final class Datagen1201 {
    private Datagen1201() {}

    /** Result counters. */
    static final class Stats {
        int classes;
        int methods;

        @Override
        public String toString() {
            return classes + " datagen class(es) emptied, " + methods + " datagen method(s) emptied";
        }
    }

    private static final String[] DATAGEN_PREFIXES = {
            "net/minecraft/data/recipes/", "net/minecraft/data/loot/", "net/minecraft/data/advancements/", "net/minecraft/data/models/",
            "net/minecraft/data/tags/", "net/minecraft/data/info/", "net/minecraft/data/structures/", "net/minecraft/data/registries/",
            "net/neoforged/neoforge/client/model/generators/", "net/neoforged/neoforge/common/data/", "net/neoforged/neoforge/data/",
    };
    private static final Set<String> DATAGEN_CLASSES = Set.of(
            "net/minecraft/data/DataProvider", "net/minecraft/data/DataGenerator", "net/minecraft/data/CachedOutput", "net/minecraft/data/HashCache",
            "net/minecraft/data/PackOutput", "net/minecraft/data/PackOutput$Target", "net/minecraft/data/BlockFamilies", "net/minecraft/data/BlockFamily",
            "net/minecraft/data/BlockFamily$Variant", "net/minecraft/advancements/Advancement$Builder", "net/minecraft/advancements/AdvancementHolder$Builder", "net/minecraft/data/worldgen/BootstapContext",
            "net/minecraft/data/worldgen/BootstrapContext", "net/minecraft/world/level/storage/loot/LootDataId");

    /** Other mods' data generation classes by name (library providers, a package called datagen, record builders). */
    private static final java.util.regex.Pattern DATAGEN_NAME = java.util.regex.Pattern.compile(
            "(?i).*/datagens?/.*|.*(RecipeProvider|LootTableProvider|LootSubProvider|BlockLootTables|TagsProvider|LanguageProvider|ItemModelProvider|BlockStateProvider|RecipeBuilder|AdvancementProvider|SoundDefinitionsProvider|ParticleDescriptionProvider)([$].*)?$");

    private static boolean datagenName(String owner) {
        if (owner == null) return false;
        // the world generation "BlockStateProvider" (stateproviders) shares its name with the model data provider
        if (owner.startsWith("net/minecraft/world/level/levelgen/")) return false;
        if (DATAGEN_CLASSES.contains(owner)) return true;
        if (DATAGEN_NAME.matcher(owner).matches()) return true;
        for (String p : DATAGEN_PREFIXES) if (owner.startsWith(p)) return true;
        return false;
    }

    /** Applies the pass to all classes of a jar. */
    static Stats apply(List<ClassNode> nodes) {
        Stats stats = new Stats();
        if (Boolean.getBoolean("legacyconnector.keepDatagen")) return stats; // diagnostics: measure the unresolved references with the datagen code in place
        Map<String, ClassNode> byName = new java.util.HashMap<>();
        for (ClassNode n : nodes) byName.put(n.name, n);
        Set<String> datagen = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ClassNode n : nodes) {
                if (datagen.contains(n.name) || (n.access & Opcodes.ACC_ENUM) != 0) continue;
                if (isDatagenClass(n, datagen)) {
                    datagen.add(n.name);
                    changed = true;
                }
            }
        }
        for (ClassNode n : nodes) {
            if (datagen.contains(n.name)) {
                shell(n, byName.keySet());
                stats.classes++;
            }
            for (MethodNode m : n.methods) if (emptyIfDatagen(m)) stats.methods++;
            if (!datagen.contains(n.name)) {
                // abstract methods that only datagen implementations override
                int before = n.methods.size();
                n.methods.removeIf(m -> (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 && !m.name.equals("<init>") && descriptorUsesDatagen(m.desc));
                stats.methods += before - n.methods.size();
            }
        }
        return stats;
    }

    private static boolean isDatagenClass(ClassNode n, Set<String> known) {
        if ((n.access & Opcodes.ACC_INTERFACE) == 0 && DATAGEN_NAME.matcher(n.name).matches()
                && !(n.superName != null && n.superName.startsWith("net/minecraft/world/level/levelgen/"))) return true;
        if (datagenSuper(n.superName, known)) return true;
        if (n.interfaces != null) for (String i : n.interfaces) if (datagenSuper(i, known)) return true;
        int dollar = n.name.lastIndexOf('$');
        if (dollar > 0 && known.contains(n.name.substring(0, dollar))) return true;
        return false;
    }

    private static boolean datagenSuper(String name, Set<String> known) {
        return name != null && (known.contains(name) || datagenName(name));
    }

    /** A provider class turns into a shell: nothing to link, nothing to run. */
    private static void shell(ClassNode n, Set<String> jar) {
        n.signature = null;
        n.interfaces = new ArrayList<>();
        // static fields stay: a tag provider class doubles as the holder of the mod's TagKey constants
        List<org.objectweb.asm.tree.FieldNode> fields = new ArrayList<>();
        for (org.objectweb.asm.tree.FieldNode f : n.fields) if ((f.access & Opcodes.ACC_STATIC) != 0) fields.add(f);
        n.fields = fields;
        n.visibleAnnotations = null;
        n.invisibleAnnotations = null;
        n.visibleTypeAnnotations = null;
        n.invisibleTypeAnnotations = null;
        n.recordComponents = null;
        n.permittedSubclasses = null;
        n.nestMembers = null;
        // the super class becomes Object unless it is another class of the jar (which is a shell too, or kept as it was)
        if (n.superName != null && !jar.contains(n.superName) && !n.superName.equals("java/lang/Record")) n.superName = "java/lang/Object";
        List<MethodNode> kept = new ArrayList<>();
        for (MethodNode m : n.methods) {
            if ((m.access & Opcodes.ACC_STATIC) != 0) {
                m.signature = null; // generic signatures name the datagen types too
                kept.add(m);
                continue;
            }
            if (!m.name.equals("<init>") || descriptorUsesDatagen(m.desc)) continue;
            m.signature = null;
            m.visibleAnnotations = null;
            m.invisibleAnnotations = null;
            m.visibleParameterAnnotations = null;
            m.invisibleParameterAnnotations = null;
            m.exceptions = new ArrayList<>();
            m.tryCatchBlocks = new ArrayList<>();
            m.localVariables = null;
            m.instructions.clear();
            throwing(m);
            kept.add(m);
        }
        n.methods = kept;
    }

    private static void throwing(MethodNode m) {
        m.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/UnsupportedOperationException"));
        m.instructions.add(new InsnNode(Opcodes.DUP));
        m.instructions.add(new LdcInsnNode("data generation is not available in a running game"));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false));
        m.instructions.add(new InsnNode(Opcodes.ATHROW));
        m.maxStack = 3;
    }

    /** A method of a normal class that is only reachable from data generation returns the default value. */
    private static boolean emptyIfDatagen(MethodNode m) {
        if (m.instructions == null || m.instructions.size() == 0 || (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return false;
        if (m.name.equals("<init>") || m.name.equals("<clinit>")) return false;
        boolean datagen = descriptorUsesDatagen(m.desc);
        for (Type t : Type.getArgumentTypes(m.desc)) {
            if (t.getSort() == Type.OBJECT && t.getInternalName().equals("net/neoforged/neoforge/data/event/GatherDataEvent")) datagen = true;
        }
        if (!datagen) datagen = usesDatagenOnly(m);
        if (!datagen) return false;
        m.signature = null;
        m.instructions.clear();
        m.tryCatchBlocks = new ArrayList<>();
        m.localVariables = null;
        m.visibleLocalVariableAnnotations = null;
        m.invisibleLocalVariableAnnotations = null;
        Type ret = Type.getReturnType(m.desc);
        switch (ret.getSort()) {
            case Type.VOID -> m.instructions.add(new InsnNode(Opcodes.RETURN));
            case Type.LONG -> {
                m.instructions.add(new InsnNode(Opcodes.LCONST_0));
                m.instructions.add(new InsnNode(Opcodes.LRETURN));
            }
            case Type.FLOAT -> {
                m.instructions.add(new InsnNode(Opcodes.FCONST_0));
                m.instructions.add(new InsnNode(Opcodes.FRETURN));
            }
            case Type.DOUBLE -> {
                m.instructions.add(new InsnNode(Opcodes.DCONST_0));
                m.instructions.add(new InsnNode(Opcodes.DRETURN));
            }
            case Type.OBJECT, Type.ARRAY -> {
                m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                m.instructions.add(new InsnNode(Opcodes.ARETURN));
            }
            default -> {
                m.instructions.add(new InsnNode(Opcodes.ICONST_0));
                m.instructions.add(new InsnNode(Opcodes.IRETURN));
            }
        }
        m.maxStack = 2;
        return true;
    }

    private static boolean descriptorUsesDatagen(String desc) {
        for (Type t : Type.getArgumentTypes(desc)) if (datagenType(t)) return true;
        return datagenType(Type.getReturnType(desc));
    }

    private static boolean datagenType(Type t) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        return t.getSort() == Type.OBJECT && datagenName(t.getInternalName());
    }

    private static boolean usesDatagenOnly(MethodNode m) {
        for (AbstractInsnNode in : m.instructions) {
            String owner = null;
            if (in instanceof MethodInsnNode mi) owner = mi.owner;
            else if (in instanceof FieldInsnNode fi) owner = fi.owner;
            else if (in instanceof TypeInsnNode ti) owner = ti.desc;
            else if (in instanceof MultiANewArrayInsnNode) continue;
            if (owner != null && datagenName(owner)) return true;
        }
        return false;
    }
}
