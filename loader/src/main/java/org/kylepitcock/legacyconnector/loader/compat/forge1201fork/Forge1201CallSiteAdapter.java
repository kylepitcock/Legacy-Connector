package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Applies explicitly selected, fail-fast call-site adaptations to Forge 1.20.1 class files.
 *
 * <p>Profiles are external JSON data. The transformer contains reusable operations only; mod-specific
 * class selectors and bytecode patterns can live outside the public loader repository.</p>
 *
 * <p>All selectors use the original input namespace and are evaluated before package relocation.
 * Each rule must match exactly one method, and every operation must match its declared instruction
 * count. A stale or partially matching profile aborts the archive transformation.</p>
 */
public final class Forge1201CallSiteAdapter {
    private static final int PROFILE_SCHEMA_VERSION = 1;

    private static final String OP_FIELD_DESCRIPTOR = "field-descriptor";
    private static final String OP_METHOD_DESCRIPTOR = "method-descriptor";
    private static final String OP_METHOD_DECLARATION_DESCRIPTOR = "method-declaration-descriptor";
    private static final String OP_CLASS_ANNOTATION_DESCRIPTOR = "class-annotation-descriptor";
    private static final String OP_CLASS_INNER_CLASS_REMOVE = "class-inner-class-remove";
    private static final String OP_FRAME_LOCAL_TYPE = "frame-local-type";
    private static final String OP_LOCAL_VARIABLE = "local-variable";
    private static final String OP_LAZY_DIST_GATE = "lazy-dist-gate";
    private static final String OP_CHECKCAST_TYPE = "checkcast-type";
    private static final String OP_INSTRUCTION_SEQUENCE = "instruction-sequence";
    private static final String OP_CONDITIONAL_BRANCH_OPCODE = "conditional-branch-opcode";

    private static final String NEOFORGE_DIST_DESCRIPTOR = "Lnet/neoforged/api/distmarker/Dist;";
    private static final String NEOFORGE_FML_ENVIRONMENT = "net/neoforged/fml/loading/FMLEnvironment";

    private Forge1201CallSiteAdapter() {}

    public record Profile(String id, List<Rule> rules) {
        public Profile {
            rules = List.copyOf(rules);
        }
    }

    public record Rule(
            String className,
            String methodName,
            String methodDescriptor,
            List<Operation> operations) {
        public Rule {
            operations = List.copyOf(operations);
        }

        String key() {
            return className + "#" + methodName + methodDescriptor;
        }
    }

    /** Bootstrap argument recipe for method-type, method-handle, and string constants. */
    public record BootstrapArgumentSpec(
            String kind,
            String descriptor,
            int tag,
            String owner,
            String name,
            boolean isInterface,
            String stringValue) {}

    /** An instruction recipe. Unused operands are null or -1 according to the instruction kind. */
    public record InstructionSpec(
            String kind,
            int opcode,
            int variable,
            String owner,
            String name,
            String descriptor,
            String typeName,
            String valueType,
            String value,
            List<BootstrapArgumentSpec> bootstrapArguments,
            boolean interfaceOwner) {
        public InstructionSpec {
            bootstrapArguments = List.copyOf(bootstrapArguments);
        }

        public InstructionSpec(
                String kind, int opcode, int variable, String owner, String name,
                String descriptor, String typeName, String valueType, String value) {
            this(kind, opcode, variable, owner, name, descriptor, typeName, valueType,
                    value, List.of(), false);
        }
    }

    public record Operation(
            String type,
            String owner,
            String name,
            String descriptor,
            String replacementDescriptor,
            String sourceType,
            String replacementType,
            int opcode,
            List<InstructionSpec> match,
            List<InstructionSpec> replacement,
            int minimumMaxStack,
            int minimumMaxLocals,
            int expectedMatches,
            int localIndex,
            String replacementName,
            int replacementOpcode) {
        public Operation {
            match = List.copyOf(match);
            replacement = List.copyOf(replacement);
        }
    }

    public record Stats(String profileId, int rulesApplied, int operationsApplied) {}

    /**
     * Reads and validates an external adapter profile.
     *
     * <p>Profile class names and descriptors refer to the original Forge 1.20.1 input JAR, not
     * the already-relocated intermediate representation.</p>
     */
    public static Profile load(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Adapter profile does not exist or is not a regular file: " + path);
        }

        final JsonObject root;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) throw new IOException("Adapter profile root must be a JSON object");
            root = parsed.getAsJsonObject();
        } catch (RuntimeException malformed) {
            throw new IOException("Cannot parse adapter profile " + path, malformed);
        }

        int schemaVersion = requiredInt(root, "schemaVersion", "profile");
        if (schemaVersion != PROFILE_SCHEMA_VERSION) {
            throw new IOException("Unsupported adapter profile schemaVersion " + schemaVersion
                    + "; supported version is " + PROFILE_SCHEMA_VERSION);
        }
        String id = requiredString(root, "id", "profile");
        JsonArray jsonRules = requiredArray(root, "rules", "profile");
        if (jsonRules.isEmpty()) throw new IOException("Adapter profile must contain at least one rule");

        List<Rule> rules = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (int index = 0; index < jsonRules.size(); index++) {
            JsonElement element = jsonRules.get(index);
            if (!element.isJsonObject()) throw new IOException("rules[" + index + "] must be an object");
            JsonObject jsonRule = element.getAsJsonObject();
            String location = "rules[" + index + "]";
            String className = requiredInternalName(jsonRule, "className", location);
            String methodName = requiredString(jsonRule, "methodName", location);
            String methodDescriptor = requiredString(jsonRule, "methodDescriptor", location);
            if (!methodDescriptor.startsWith("(")) {
                throw new IOException(location + ".methodDescriptor must be a JVM method descriptor");
            }

            JsonArray jsonOperations = requiredArray(jsonRule, "operations", location);
            if (jsonOperations.isEmpty()) {
                throw new IOException(location + ".operations must not be empty");
            }
            List<Operation> operations = new ArrayList<>();
            for (int operationIndex = 0; operationIndex < jsonOperations.size(); operationIndex++) {
                JsonElement operationElement = jsonOperations.get(operationIndex);
                if (!operationElement.isJsonObject()) {
                    throw new IOException(location + ".operations[" + operationIndex + "] must be an object");
                }
                operations.add(parseOperation(operationElement.getAsJsonObject(),
                        location + ".operations[" + operationIndex + "]"));
            }

            Rule rule = new Rule(className, methodName, methodDescriptor, operations);
            if (!keys.add(rule.key())) throw new IOException("Duplicate adapter rule: " + rule.key());
            rules.add(rule);
        }
        return new Profile(id, rules);
    }

    public static final class Session {
        private final Profile profile;
        private final Map<String, Rule> pending = new LinkedHashMap<>();
        private final Set<String> generatedLazyHelpers = new HashSet<>();
        private int operationsApplied;

        public Session(Profile profile) {
            if (profile == null) throw new IllegalArgumentException("Adapter profile is required");
            this.profile = profile;
            for (Rule rule : profile.rules()) pending.put(rule.key(), rule);
        }

        /** Applies the profile rules belonging to this class, if any. */
        public void apply(ClassNode classNode) throws IOException {
            if (classNode == null) throw new IllegalArgumentException("Class node is required");
            List<Rule> classRules = profile.rules().stream()
                    .filter(rule -> rule.className().equals(classNode.name))
                    .toList();
            for (Rule rule : classRules) {
                if (!pending.containsKey(rule.key())) {
                    throw new IOException("Adapter rule was encountered more than once: " + rule.key());
                }

                List<MethodNode> matches = classNode.methods.stream()
                        .filter(method -> method.name.equals(rule.methodName())
                                && method.desc.equals(rule.methodDescriptor()))
                        .toList();
                if (matches.size() != 1) {
                    throw new IOException("Adapter rule " + rule.key() + " expected exactly one method, found "
                            + matches.size());
                }

                MethodNode method = matches.get(0);
                for (Operation operation : rule.operations()) {
                    applyOperation(classNode, rule, method, operation, generatedLazyHelpers);
                    operationsApplied++;
                }
                pending.remove(rule.key());
            }
        }

        /** Fails if any required class/method rule was absent from the input archive. */
        public Stats finish() throws IOException {
            if (!pending.isEmpty()) {
                throw new IOException("Adapter profile '" + profile.id() + "' did not match required methods: "
                        + String.join(", ", pending.keySet()));
            }
            return new Stats(profile.id(), profile.rules().size(), operationsApplied);
        }
    }

    private static void applyOperation(
            ClassNode classNode,
            Rule rule,
            MethodNode method,
            Operation operation,
            Set<String> generatedLazyHelpers) throws IOException {
        switch (operation.type()) {
            case OP_FIELD_DESCRIPTOR -> replaceFieldDescriptor(rule, method, operation);
            case OP_METHOD_DESCRIPTOR -> replaceMethodDescriptor(rule, method, operation);
            case OP_METHOD_DECLARATION_DESCRIPTOR -> replaceMethodDeclarationDescriptor(rule, method, operation);
            case OP_CLASS_ANNOTATION_DESCRIPTOR -> replaceClassAnnotationDescriptor(classNode, rule, operation);
            case OP_CLASS_INNER_CLASS_REMOVE -> removeClassInnerClassMetadata(classNode, rule, operation);
            case OP_FRAME_LOCAL_TYPE -> replaceFrameLocalType(rule, method, operation);
            case OP_LOCAL_VARIABLE -> replaceLocalVariable(rule, method, operation);
            case OP_LAZY_DIST_GATE -> rewriteLazyDistGate(classNode, rule, method, operation, generatedLazyHelpers);
            case OP_CHECKCAST_TYPE -> replaceCheckcastType(rule, method, operation);
            case OP_INSTRUCTION_SEQUENCE -> rewriteInstructionSequence(rule, method, operation);
            case OP_CONDITIONAL_BRANCH_OPCODE -> rewriteConditionalBranchOpcode(rule, method, operation);
            default -> throw new IOException("Unsupported adapter operation '" + operation.type()
                    + "' in " + rule.key());
        }
    }

    /**
     * Rewrites one exact class-level annotation descriptor. Annotation values are preserved and
     * are subsequently passed through the normal type remapper.
     */
    private static void replaceClassAnnotationDescriptor(
            ClassNode classNode, Rule rule, Operation operation) throws IOException {
        int matches = 0;
        matches += replaceAnnotationDescriptor(classNode.visibleAnnotations, operation);
        matches += replaceAnnotationDescriptor(classNode.invisibleAnnotations, operation);
        requireExpectedMatches(rule, operation, matches);
    }

    /** Removes one exact obsolete InnerClasses entry after a nested class is replaced by a top-level type. */
    private static void removeClassInnerClassMetadata(
            ClassNode classNode, Rule rule, Operation operation) throws IOException {
        int matches = 0;
        if (classNode.innerClasses != null) {
            int before = classNode.innerClasses.size();
            classNode.innerClasses.removeIf(innerClass ->
                    Objects.equals(innerClass.name, operation.sourceType())
                            && Objects.equals(innerClass.outerName, operation.owner())
                            && Objects.equals(innerClass.innerName, operation.name()));
            matches = before - classNode.innerClasses.size();
        }
        requireExpectedMatches(rule, operation, matches);
    }

    private static int replaceAnnotationDescriptor(
            List<AnnotationNode> annotations, Operation operation) {
        if (annotations == null) return 0;
        int matches = 0;
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(operation.descriptor())) {
                annotation.desc = operation.replacementDescriptor();
                matches++;
            }
        }
        return matches;
    }

    private static void replaceMethodDeclarationDescriptor(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        int matches = method.desc.equals(operation.descriptor()) ? 1 : 0;
        requireExpectedMatches(rule, operation, matches);
        method.desc = operation.replacementDescriptor();
    }

    /**
     * Replaces a local's declared type in an explicit full stack-map frame.
     * Compressed frames are intentionally not inferred: profiles must target a frame whose
     * complete local array is present, otherwise the operation fails its exact match count.
     */
    private static void replaceFrameLocalType(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        List<FrameNode> matches = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (!(instruction instanceof FrameNode frame)
                    || (frame.type != Opcodes.F_FULL && frame.type != Opcodes.F_NEW)
                    || frame.local == null) {
                continue;
            }
            int entryIndex = frameLocalEntryIndex(frame.local, operation.localIndex());
            if (entryIndex >= 0 && Objects.equals(frame.local.get(entryIndex), operation.sourceType())) {
                matches.add(frame);
            }
        }
        requireExpectedMatches(rule, operation, matches.size());
        for (FrameNode frame : matches) {
            int entryIndex = frameLocalEntryIndex(frame.local, operation.localIndex());
            frame.local.set(entryIndex, operation.replacementType());
        }
    }

    private static int frameLocalEntryIndex(List<Object> locals, int requestedSlot) {
        int slot = 0;
        for (int index = 0; index < locals.size(); index++) {
            if (slot == requestedSlot) return index;
            Object value = locals.get(index);
            boolean categoryTwo = value instanceof Integer tag
                    && (tag == Opcodes.LONG || tag == Opcodes.DOUBLE);
            slot += categoryTwo ? 2 : 1;
            if (slot > requestedSlot) return -1;
        }
        return -1;
    }

    private static void replaceLocalVariable(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        List<LocalVariableNode> matches = method.localVariables == null
                ? List.of()
                : method.localVariables.stream()
                        .filter(local -> local.index == operation.localIndex()
                                && local.name.equals(operation.name())
                                && local.desc.equals(operation.descriptor()))
                        .toList();
        requireExpectedMatches(rule, operation, matches.size());
        for (LocalVariableNode local : matches) {
            local.name = operation.replacementName();
            local.desc = operation.replacementDescriptor();
        }
    }

    /**
     * Replaces DistExecutor's static owner with one private helper that keeps the Supplier lazy
     * and checks FMLEnvironment.dist before obtaining and running its Runnable.
     */
    private static void rewriteLazyDistGate(
            ClassNode classNode,
            Rule rule,
            MethodNode method,
            Operation operation,
            Set<String> generatedLazyHelpers) throws IOException {
        List<MethodInsnNode> matches = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESTATIC
                    && call.owner.equals(operation.owner())
                    && call.name.equals(operation.name())
                    && call.desc.equals(operation.descriptor())) {
                matches.add(call);
            }
        }
        requireExpectedMatches(rule, operation, matches.size());

        String helperKey = classNode.name + "#" + operation.replacementName() + operation.descriptor();
        if (!generatedLazyHelpers.contains(helperKey)) {
            boolean collision = classNode.methods.stream().anyMatch(existing ->
                    existing.name.equals(operation.replacementName())
                            && existing.desc.equals(operation.descriptor()));
            if (collision) {
                throw new IOException("Lazy dist-gate helper name already exists in " + classNode.name
                        + ": " + operation.replacementName() + operation.descriptor());
            }
            classNode.methods.add(createLazyDistGateHelper(operation.replacementName(), operation.descriptor()));
            generatedLazyHelpers.add(helperKey);
        }

        for (MethodInsnNode call : matches) {
            call.owner = classNode.name;
            call.name = operation.replacementName();
            call.itf = false;
        }
    }

    private static MethodNode createLazyDistGateHelper(String helperName, String descriptor) {
        MethodNode helper = new MethodNode(
                Opcodes.ASM9,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                helperName,
                descriptor,
                null,
                null);
        LabelNode skip = new LabelNode();
        helper.instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                NEOFORGE_FML_ENVIRONMENT,
                "dist",
                NEOFORGE_DIST_DESCRIPTOR));
        helper.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        helper.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ACMPNE, skip));
        helper.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        helper.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/function/Supplier",
                "get",
                "()Ljava/lang/Object;",
                true));
        helper.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Runnable"));
        helper.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/lang/Runnable",
                "run",
                "()V",
                true));
        helper.instructions.add(skip);
        helper.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        helper.instructions.add(new InsnNode(Opcodes.RETURN));
        helper.maxStack = 2;
        helper.maxLocals = 2;
        return helper;
    }

    private static void replaceFieldDescriptor(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        int matches = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == operation.opcode()
                    && field.owner.equals(operation.owner())
                    && field.name.equals(operation.name())
                    && field.desc.equals(operation.descriptor())) {
                field.desc = operation.replacementDescriptor();
                matches++;
            }
        }
        requireExpectedMatches(rule, operation, matches);
    }

    private static void replaceMethodDescriptor(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        int matches = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == operation.opcode()
                    && call.owner.equals(operation.owner())
                    && call.name.equals(operation.name())
                    && call.desc.equals(operation.descriptor())) {
                call.desc = operation.replacementDescriptor();
                matches++;
            }
        }
        requireExpectedMatches(rule, operation, matches);
    }

    private static void replaceCheckcastType(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        int matches = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode typeInsn
                    && typeInsn.getOpcode() == Opcodes.CHECKCAST
                    && typeInsn.desc.equals(operation.sourceType())) {
                typeInsn.desc = operation.replacementType();
                matches++;
            }
        }
        requireExpectedMatches(rule, operation, matches);
    }

    /**
     * Replaces exact contiguous instruction patterns declared by an external profile.
     * Labels and frames are never silently consumed: a match crossing a control-flow boundary fails.
     */
    private static void rewriteInstructionSequence(Rule rule, MethodNode method, Operation operation)
            throws IOException {
        List<List<AbstractInsnNode>> matches = findSequenceMatches(method, operation.match());
        if (matches.size() != operation.expectedMatches()) {
            String detail = describeSequenceMismatch(method, operation.match());
            throw new IOException("Adapter operation '" + operation.type() + "' in " + rule.key()
                    + " expected " + operation.expectedMatches() + " matches, found " + matches.size()
                    + "; " + detail);
        }
        Set<AbstractInsnNode> claimedInstructions = new HashSet<>();
        for (List<AbstractInsnNode> match : matches) {
            for (AbstractInsnNode instruction : match) {
                if (!claimedInstructions.add(instruction)) {
                    throw new IOException("Instruction-sequence adapter in " + rule.key()
                            + " produced overlapping matches");
                }
            }
        }

        for (int index = matches.size() - 1; index >= 0; index--) {
            List<AbstractInsnNode> matched = matches.get(index);
            ensureNoControlFlowMarkerInside(rule, matched);

            AbstractInsnNode first = matched.get(0);
            InsnList replacement = new InsnList();
            for (InstructionSpec spec : operation.replacement()) replacement.add(createInstruction(spec));
            if (replacement.size() > 0) method.instructions.insertBefore(first, replacement);
            matched.forEach(method.instructions::remove);
        }

        method.maxStack = Math.max(method.maxStack, operation.minimumMaxStack());
        method.maxLocals = Math.max(method.maxLocals, operation.minimumMaxLocals());
    }

    /**
     * Replaces a conditional branch opcode while retaining the exact existing target label.
     * The profile selector and expected count keep the stack-shape change caller-specific.
     */
    private static void rewriteConditionalBranchOpcode(
            Rule rule, MethodNode method, Operation operation) throws IOException {
        List<JumpInsnNode> matches = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof JumpInsnNode jump
                    && jump.getOpcode() == operation.opcode()) {
                matches.add(jump);
            }
        }
        requireExpectedMatches(rule, operation, matches.size());
        for (JumpInsnNode jump : matches) {
            method.instructions.set(jump, new JumpInsnNode(operation.replacementOpcode(), jump.label));
        }
    }

    private static List<List<AbstractInsnNode>> findSequenceMatches(
            MethodNode method, List<InstructionSpec> pattern) throws IOException {
        List<List<AbstractInsnNode>> matches = new ArrayList<>();
        for (AbstractInsnNode start : method.instructions) {
            if (start.getOpcode() < 0 || !matches(start, pattern.get(0))) continue;

            List<AbstractInsnNode> nodes = new ArrayList<>(pattern.size());
            AbstractInsnNode cursor = start;
            boolean matched = true;
            for (InstructionSpec spec : pattern) {
                while (cursor != null && cursor.getOpcode() < 0) cursor = cursor.getNext();
                if (cursor == null || !matches(cursor, spec)) {
                    matched = false;
                    break;
                }
                nodes.add(cursor);
                cursor = cursor.getNext();
            }
            if (matched) matches.add(List.copyOf(nodes));
        }
        return matches;
    }

    /**
     * Explains the first failed element for a candidate sequence. This diagnostic does not
     * relax or alter the exact matching rules.
     */
    private static String describeSequenceMismatch(
            MethodNode method, List<InstructionSpec> pattern) throws IOException {
        for (AbstractInsnNode start : method.instructions) {
            if (start.getOpcode() < 0 || !matches(start, pattern.get(0))) continue;

            AbstractInsnNode cursor = start;
            for (int index = 0; index < pattern.size(); index++) {
                while (cursor != null && cursor.getOpcode() < 0) cursor = cursor.getNext();
                InstructionSpec expected = pattern.get(index);
                if (cursor == null) {
                    return "candidate beginning " + describeInstruction(start)
                            + " ended before pattern[" + index + "] expected " + expected;
                }
                if (!matches(cursor, expected)) {
                    return "candidate beginning " + describeInstruction(start)
                            + " failed at pattern[" + index + "]; expected " + expected
                            + ", actual " + describeInstruction(cursor);
                }
                cursor = cursor.getNext();
            }
        }
        return "no instruction matched the first pattern element " + pattern.get(0);
    }

    private static String describeInstruction(AbstractInsnNode instruction) {
        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            return "invokedynamic[name=" + dynamic.name + ", desc=" + dynamic.desc
                    + ", bootstrap=" + dynamic.bsm + ", arguments="
                    + java.util.Arrays.deepToString(dynamic.bsmArgs) + "]";
        }
        if (instruction instanceof TypeInsnNode type) {
            return "type[opcode=" + type.getOpcode() + ", desc=" + type.desc + "]";
        }
        if (instruction instanceof MethodInsnNode method) {
            return "method[opcode=" + method.getOpcode() + ", owner=" + method.owner
                    + ", name=" + method.name + ", desc=" + method.desc + ", itf=" + method.itf + "]";
        }
        if (instruction instanceof FieldInsnNode field) {
            return "field[opcode=" + field.getOpcode() + ", owner=" + field.owner
                    + ", name=" + field.name + ", desc=" + field.desc + "]";
        }
        if (instruction instanceof LdcInsnNode ldc) {
            return "ldc[value=" + ldc.cst + "]";
        }
        return instruction.getClass().getSimpleName() + "[opcode=" + instruction.getOpcode() + "]";
    }

    private static boolean matches(AbstractInsnNode instruction, InstructionSpec spec) throws IOException {
        if (instruction.getOpcode() != spec.opcode()) return false;
        return switch (spec.kind()) {
            case "var" -> instruction instanceof VarInsnNode variable && variable.var == spec.variable();
            case "method" -> instruction instanceof MethodInsnNode call
                    && call.owner.equals(spec.owner())
                    && call.name.equals(spec.name())
                    && call.desc.equals(spec.descriptor());
            case "field" -> instruction instanceof FieldInsnNode field
                    && field.owner.equals(spec.owner())
                    && field.name.equals(spec.name())
                    && field.desc.equals(spec.descriptor());
            case "type" -> instruction instanceof TypeInsnNode typeInsn
                    && typeInsn.desc.equals(spec.typeName());
            case "class-literal" -> instruction instanceof LdcInsnNode ldc
                    && ldc.cst instanceof Type type && type.getSort() == Type.OBJECT
                    && type.getInternalName().equals(spec.typeName());
            case "invokedynamic" -> matchesInvokeDynamic(instruction, spec);
            case "ldc" -> instruction instanceof LdcInsnNode ldc
                    && Objects.equals(ldc.cst, constantValue(spec));
            case "insn" -> instruction instanceof InsnNode;
            default -> false;
        };
    }

    private static AbstractInsnNode createInstruction(InstructionSpec spec) throws IOException {
        return switch (spec.kind()) {
            case "var" -> new VarInsnNode(spec.opcode(), spec.variable());
            case "method" -> new MethodInsnNode(spec.opcode(), spec.owner(), spec.name(),
                    spec.descriptor(), spec.opcode() == Opcodes.INVOKEINTERFACE);
            case "field" -> new FieldInsnNode(spec.opcode(), spec.owner(), spec.name(), spec.descriptor());
            case "type" -> new TypeInsnNode(spec.opcode(), spec.typeName());
            case "class-literal" -> new LdcInsnNode(Type.getObjectType(spec.typeName()));
            case "invokedynamic" -> createInvokeDynamic(spec);
            case "ldc" -> new LdcInsnNode(constantValue(spec));
            case "insn" -> new InsnNode(spec.opcode());
            default -> throw new IOException("Unsupported instruction recipe kind: " + spec.kind());
        };
    }

    private static boolean matchesInvokeDynamic(AbstractInsnNode instruction, InstructionSpec spec)
            throws IOException {
        if (!(instruction instanceof InvokeDynamicInsnNode dynamic)
                || !dynamic.name.equals(spec.name())
                || !dynamic.desc.equals(spec.descriptor())
                || !dynamic.bsm.getOwner().equals(spec.owner())
                || !dynamic.bsm.getName().equals(spec.typeName())
                || !dynamic.bsm.getDesc().equals(spec.valueType())
                || dynamic.bsm.getTag() != handleTagValue(spec.value(), "invokedynamic bootstrap tag")
                || dynamic.bsm.isInterface() != spec.interfaceOwner()
                || dynamic.bsmArgs.length != spec.bootstrapArguments().size()) {
            return false;
        }
        for (int index = 0; index < dynamic.bsmArgs.length; index++) {
            Object actual = dynamic.bsmArgs[index];
            BootstrapArgumentSpec expected = spec.bootstrapArguments().get(index);
            if (expected.kind().equals("method-type")) {
                if (!(actual instanceof Type type)
                        || type.getSort() != Type.METHOD
                        || !type.getDescriptor().equals(expected.descriptor())) return false;
            } else if (expected.kind().equals("handle")) {
                if (!(actual instanceof Handle handle)
                        || handle.getTag() != expected.tag()
                        || !handle.getOwner().equals(expected.owner())
                        || !handle.getName().equals(expected.name())
                        || !handle.getDesc().equals(expected.descriptor())
                        || handle.isInterface() != expected.isInterface()) return false;
            } else if (expected.kind().equals("string")) {
                if (!(actual instanceof String string) || !string.equals(expected.stringValue())) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    private static InvokeDynamicInsnNode createInvokeDynamic(InstructionSpec spec) throws IOException {
        Object[] arguments = new Object[spec.bootstrapArguments().size()];
        for (int index = 0; index < arguments.length; index++) {
            BootstrapArgumentSpec argument = spec.bootstrapArguments().get(index);
            if (argument.kind().equals("method-type")) {
                arguments[index] = Type.getMethodType(argument.descriptor());
            } else if (argument.kind().equals("handle")) {
                arguments[index] = new Handle(argument.tag(), argument.owner(), argument.name(),
                        argument.descriptor(), argument.isInterface());
            } else if (argument.kind().equals("string")) {
                arguments[index] = argument.stringValue();
            } else {
                throw new IOException("Unsupported bootstrap argument kind: " + argument.kind());
            }
        }
        Handle bootstrap = new Handle(handleTagValue(spec.value(), "invokedynamic bootstrap tag"),
                spec.owner(), spec.typeName(), spec.valueType(), spec.interfaceOwner());
        return new InvokeDynamicInsnNode(spec.name(), spec.descriptor(), bootstrap, arguments);
    }

    private static Object constantValue(InstructionSpec spec) throws IOException {
        try {
            return switch (spec.valueType()) {
                case "float" -> Float.valueOf(spec.value());
                case "double" -> Double.valueOf(spec.value());
                case "int" -> Integer.valueOf(spec.value());
                case "long" -> Long.valueOf(spec.value());
                case "string" -> spec.value();
                default -> throw new IOException("Unsupported LDC valueType: " + spec.valueType());
            };
        } catch (NumberFormatException invalid) {
            throw new IOException("Invalid LDC constant value '" + spec.value() + "'", invalid);
        }
    }

    private static void ensureNoControlFlowMarkerInside(Rule rule, List<AbstractInsnNode> matched)
            throws IOException {
        if (matched.size() < 2) return;
        AbstractInsnNode cursor = matched.get(0).getNext();
        AbstractInsnNode last = matched.get(matched.size() - 1);
        while (cursor != null && cursor != last) {
            if (cursor instanceof LabelNode || cursor instanceof FrameNode
                    || cursor instanceof org.objectweb.asm.tree.JumpInsnNode
                    || cursor instanceof org.objectweb.asm.tree.TableSwitchInsnNode
                    || cursor instanceof org.objectweb.asm.tree.LookupSwitchInsnNode) {
                throw new IOException("Instruction-sequence adapter in " + rule.key()
                        + " crosses a label, frame, or control-flow instruction");
            }
            cursor = cursor.getNext();
        }
        if (cursor != last) {
            throw new IOException("Instruction-sequence adapter in " + rule.key()
                    + " contains a non-contiguous instruction sequence");
        }
    }

    private static void requireExpectedMatches(Rule rule, Operation operation, int actual) throws IOException {
        if (actual != operation.expectedMatches()) {
            throw new IOException("Adapter operation '" + operation.type() + "' in " + rule.key()
                    + " expected " + operation.expectedMatches() + " matches, found " + actual);
        }
    }

    private static Operation parseOperation(JsonObject json, String location) throws IOException {
        String type = requiredString(json, "type", location);
        int expected = requiredInt(json, "expectedMatches", location);
        if (expected < 1) throw new IOException(location + ".expectedMatches must be at least 1");

        return switch (type) {
            case OP_FIELD_DESCRIPTOR, OP_METHOD_DESCRIPTOR -> new Operation(
                    type,
                    requiredInternalName(json, "owner", location),
                    requiredString(json, "name", location),
                    requiredString(json, "descriptor", location),
                    requiredString(json, "replacementDescriptor", location),
                    null, null,
                    requiredOpcode(json, "opcode", location, type.equals(OP_FIELD_DESCRIPTOR)),
                    List.of(), List.of(), 0, 0, expected, -1, null, -1);
            case OP_METHOD_DECLARATION_DESCRIPTOR -> new Operation(
                    type, null, null,
                    requiredString(json, "descriptor", location),
                    requiredString(json, "replacementDescriptor", location),
                    null, null, -1, List.of(), List.of(), 0, 0, expected, -1, null, -1);
            case OP_CLASS_ANNOTATION_DESCRIPTOR -> new Operation(
                    type, null, null,
                    requiredString(json, "descriptor", location),
                    requiredString(json, "replacementDescriptor", location),
                    null, null, -1, List.of(), List.of(), 0, 0, expected, -1, null, -1);
            case OP_CLASS_INNER_CLASS_REMOVE -> new Operation(
                    type,
                    requiredInternalName(json, "outerClassName", location),
                    requiredString(json, "innerName", location),
                    null, null,
                    requiredInternalName(json, "innerClassName", location),
                    null, -1, List.of(), List.of(), 0, 0, expected, -1, null, -1);
            case OP_FRAME_LOCAL_TYPE -> {
                int localIndex = requiredInt(json, "localIndex", location);
                if (localIndex < 0) throw new IOException(location + ".localIndex must not be negative");
                yield new Operation(
                        type, null, null, null, null,
                        requiredInternalName(json, "sourceType", location),
                        requiredInternalName(json, "replacementType", location),
                        -1, List.of(), List.of(), 0, 0, expected, localIndex, null, -1);
            }
            case OP_LOCAL_VARIABLE -> {
                int localIndex = requiredInt(json, "localIndex", location);
                if (localIndex < 0) throw new IOException(location + ".localIndex must not be negative");
                yield new Operation(
                        type, null,
                        requiredString(json, "name", location),
                        requiredString(json, "descriptor", location),
                        requiredString(json, "replacementDescriptor", location),
                        null, null, -1, List.of(), List.of(), 0, 0, expected,
                        localIndex, requiredString(json, "replacementName", location), -1);
            }
            case OP_LAZY_DIST_GATE -> new Operation(
                    type,
                    requiredInternalName(json, "owner", location),
                    requiredString(json, "name", location),
                    requiredString(json, "descriptor", location),
                    null, null, null, Opcodes.INVOKESTATIC,
                    List.of(), List.of(), 0, 0, expected, -1,
                    requiredString(json, "replacementName", location), -1);
            case OP_CHECKCAST_TYPE -> new Operation(
                    type, null, null, null, null,
                    requiredInternalName(json, "sourceType", location),
                    requiredInternalName(json, "replacementType", location),
                    -1, List.of(), List.of(), 0, 0, expected, -1, null, -1);
            case OP_INSTRUCTION_SEQUENCE -> {
                JsonArray match = requiredArray(json, "match", location);
                JsonArray replacement = requiredArray(json, "replaceWith", location);
                if (match.isEmpty()) throw new IOException(location + ".match must not be empty");
                List<InstructionSpec> matchSpecs = parseInstructionArray(match, location + ".match");
                List<InstructionSpec> replacementSpecs =
                        parseInstructionArray(replacement, location + ".replaceWith");
                int minimumMaxStack = optionalInt(json, "minimumMaxStack", 0, location);
                int minimumMaxLocals = optionalInt(json, "minimumMaxLocals", 0, location);
                if (minimumMaxStack < 0 || minimumMaxLocals < 0) {
                    throw new IOException(location + " minimum max values must not be negative");
                }
                yield new Operation(type, null, null, null, null, null, null, -1,
                        matchSpecs, replacementSpecs, minimumMaxStack, minimumMaxLocals, expected, -1, null, -1);
            }
            case OP_CONDITIONAL_BRANCH_OPCODE -> {
                int sourceOpcode = requiredAnyOpcode(json, location);
                int replacementOpcode = opcodeValue(requiredString(json, "replacementOpcode", location), location);
                if (!isConditionalBranchOpcode(sourceOpcode)
                        || !isConditionalBranchOpcode(replacementOpcode)) {
                    throw new IOException(location + " must use conditional branch opcodes");
                }
                yield new Operation(type, null, null, null, null, null, null, sourceOpcode,
                        List.of(), List.of(), 0, 0, expected, -1, null, replacementOpcode);
            }
            default -> throw new IOException(location + " has unsupported operation type '" + type + "'");
        };
    }
    private static List<InstructionSpec> parseInstructionArray(JsonArray array, String location)
            throws IOException {
        List<InstructionSpec> result = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) throw new IOException(location + "[" + index + "] must be an object");
            result.add(parseInstruction(element.getAsJsonObject(), location + "[" + index + "]"));
        }
        return List.copyOf(result);
    }

    private static InstructionSpec parseInstruction(JsonObject json, String location) throws IOException {
        String kind = requiredString(json, "kind", location);
        return switch (kind) {
            case "var" -> {
                int opcode = requiredAnyOpcode(json, location);
                if (!isVariableOpcode(opcode)) throw new IOException(location + " has a non-variable opcode");
                int variable = requiredInt(json, "var", location);
                if (variable < 0) throw new IOException(location + ".var must not be negative");
                yield new InstructionSpec(kind, opcode, variable, null, null, null, null, null, null);
            }
            case "method" -> {
                int opcode = requiredAnyOpcode(json, location);
                if (!isMethodOpcode(opcode)) throw new IOException(location + " has a non-invocation opcode");
                yield new InstructionSpec(kind, opcode, -1,
                        requiredInternalName(json, "owner", location),
                        requiredString(json, "name", location),
                        requiredString(json, "descriptor", location),
                        null, null, null);
            }
            case "field" -> {
                int opcode = requiredAnyOpcode(json, location);
                if (!isFieldOpcode(opcode)) throw new IOException(location + " has a non-field opcode");
                yield new InstructionSpec(kind, opcode, -1,
                        requiredInternalName(json, "owner", location),
                        requiredString(json, "name", location),
                        requiredString(json, "descriptor", location),
                        null, null, null);
            }
            case "type" -> {
                int opcode = requiredAnyOpcode(json, location);
                if (opcode != Opcodes.NEW && opcode != Opcodes.ANEWARRAY
                        && opcode != Opcodes.CHECKCAST && opcode != Opcodes.INSTANCEOF) {
                    throw new IOException(location + " has an unsupported type opcode");
                }
                yield new InstructionSpec(kind, opcode, -1, null, null, null,
                        requiredInternalName(json, "typeName", location), null, null);
            }
            case "class-literal" -> new InstructionSpec(
                    kind, Opcodes.LDC, -1, null, null, null,
                    requiredInternalName(json, "typeName", location), null, null);
            case "invokedynamic" -> {
                String dynamicName = requiredString(json, "name", location);
                String dynamicDescriptor = requiredString(json, "descriptor", location);
                try {
                    Type.getMethodType(dynamicDescriptor);
                } catch (IllegalArgumentException invalid) {
                    throw new IOException(location + ".descriptor must be a valid method descriptor", invalid);
                }
                JsonObject bootstrap = requiredObject(json, "bootstrap", location);
                String bootstrapTag = requiredString(bootstrap, "tag", location + ".bootstrap");
                handleTagValue(bootstrapTag, location + ".bootstrap.tag");
                String bootstrapOwner = requiredInternalName(bootstrap, "owner", location + ".bootstrap");
                String bootstrapName = requiredString(bootstrap, "name", location + ".bootstrap");
                String bootstrapDescriptor = requiredString(bootstrap, "descriptor", location + ".bootstrap");
                try {
                    Type.getMethodType(bootstrapDescriptor);
                } catch (IllegalArgumentException invalid) {
                    throw new IOException(location + ".bootstrap.descriptor must be a valid method descriptor", invalid);
                }
                boolean bootstrapInterface = optionalBoolean(bootstrap, "isInterface", false, location + ".bootstrap");
                List<BootstrapArgumentSpec> arguments = parseBootstrapArguments(
                        requiredArray(json, "bootstrapArguments", location), location + ".bootstrapArguments");
                yield new InstructionSpec(kind, Opcodes.INVOKEDYNAMIC, -1,
                        bootstrapOwner, dynamicName, dynamicDescriptor,
                        bootstrapName, bootstrapDescriptor, bootstrapTag, arguments, bootstrapInterface);
            }
            case "ldc" -> {
                String valueType = requiredString(json, "valueType", location);
                JsonElement value = json.get("value");
                if (value == null || !value.isJsonPrimitive()) {
                    throw new IOException(location + ".value must be a JSON primitive");
                }
                if (valueType.equals("string") && !value.getAsJsonPrimitive().isString()) {
                    throw new IOException(location + ".value must be a string for valueType string");
                }
                if (!valueType.equals("string") && !value.getAsJsonPrimitive().isNumber()) {
                    throw new IOException(location + ".value must be numeric for valueType " + valueType);
                }
                String rawValue = value.getAsString();
                // Validate the supported constant type now, not only when the operation executes.
                InstructionSpec spec = new InstructionSpec(kind, Opcodes.LDC, -1, null, null, null,
                        null, valueType, rawValue);
                constantValue(spec);
                yield spec;
            }
            case "insn" -> {
                int opcode = requiredAnyOpcode(json, location);
                if (!isSimpleOpcode(opcode)) throw new IOException(location + " has an opcode with operands");
                yield new InstructionSpec(kind, opcode, -1, null, null, null, null, null, null);
            }
            default -> throw new IOException(location + " has unsupported instruction kind '" + kind + "'");
        };
    }

    private static List<BootstrapArgumentSpec> parseBootstrapArguments(JsonArray array, String location)
            throws IOException {
        List<BootstrapArgumentSpec> arguments = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            String itemLocation = location + "[" + index + "]";
            if (!element.isJsonObject()) throw new IOException(itemLocation + " must be an object");
            JsonObject argument = element.getAsJsonObject();
            String kind = requiredString(argument, "kind", itemLocation);
            switch (kind) {
                case "method-type" -> {
                    String descriptor = requiredString(argument, "descriptor", itemLocation);
                    try {
                        Type methodType = Type.getMethodType(descriptor);
                        if (methodType.getSort() != Type.METHOD) {
                            throw new IllegalArgumentException("not a method type");
                        }
                    } catch (IllegalArgumentException invalid) {
                        throw new IOException(itemLocation + ".descriptor must be a valid method descriptor", invalid);
                    }
                    arguments.add(new BootstrapArgumentSpec(kind, descriptor, -1, null, null, false, null));
                }
                case "handle" -> {
                    int tag = handleTagValue(requiredString(argument, "tag", itemLocation), itemLocation + ".tag");
                    String owner = requiredInternalName(argument, "owner", itemLocation);
                    String name = requiredString(argument, "name", itemLocation);
                    String descriptor = requiredString(argument, "descriptor", itemLocation);
                    boolean isInterface = optionalBoolean(argument, "isInterface", false, itemLocation);
                    arguments.add(new BootstrapArgumentSpec(kind, descriptor, tag, owner, name, isInterface, null));
                }
                case "string" -> {
                    JsonElement value = argument.get("value");
                    if (value == null || !value.isJsonPrimitive()
                            || !value.getAsJsonPrimitive().isString()) {
                        throw new IOException(itemLocation + ".value must be a string for bootstrap argument kind string");
                    }
                    arguments.add(new BootstrapArgumentSpec(kind, null, -1, null, null, false, value.getAsString()));
                }
                default -> throw new IOException(itemLocation + " has unsupported bootstrap argument kind '" + kind
                        + "' (supported: method-type, handle, string)");
            }
        }
        return List.copyOf(arguments);
    }

    private static int handleTagValue(String name, String location) throws IOException {
        try {
            Field field = Opcodes.class.getField(name);
            int tag = field.getInt(null);
            if (tag < Opcodes.H_GETFIELD || tag > Opcodes.H_INVOKEINTERFACE) {
                throw new IOException(location + " is not a JVM method-handle tag: " + name);
            }
            return tag;
        } catch (NoSuchFieldException | IllegalAccessException invalid) {
            throw new IOException(location + " is unknown: " + name, invalid);
        }
    }

    private static boolean optionalBoolean(JsonObject json, String property, boolean defaultValue, String location)
            throws IOException {
        JsonElement element = json.get(property);
        if (element == null) return defaultValue;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IOException(location + "." + property + " must be a boolean");
        }
        return element.getAsBoolean();
    }

    private static JsonObject requiredObject(JsonObject json, String property, String location) throws IOException {
        JsonElement element = json.get(property);
        if (element == null || !element.isJsonObject()) {
            throw new IOException(location + "." + property + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static int requiredOpcode(JsonObject json, String property, String location, boolean field)
            throws IOException {
        int opcode = opcodeValue(requiredString(json, property, location), location);
        if (field ? !isFieldOpcode(opcode) : !isMethodOpcode(opcode)) {
            throw new IOException(location + ".opcode is not valid for " + (field ? "field" : "method") + " operations");
        }
        return opcode;
    }

    private static int requiredAnyOpcode(JsonObject json, String location) throws IOException {
        return opcodeValue(requiredString(json, "opcode", location), location);
    }

    private static int opcodeValue(String name, String location) throws IOException {
        try {
            Field field = Opcodes.class.getField(name);
            int opcode = field.getInt(null);
            if (opcode < 0 || opcode > 201) throw new IOException(location + ".opcode is not a JVM instruction: " + name);
            return opcode;
        } catch (NoSuchFieldException | IllegalAccessException invalid) {
            throw new IOException(location + ".opcode is unknown: " + name, invalid);
        }
    }

    private static boolean isVariableOpcode(int opcode) {
        return opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD || opcode == Opcodes.FLOAD
                || opcode == Opcodes.DLOAD || opcode == Opcodes.ALOAD || opcode == Opcodes.ISTORE
                || opcode == Opcodes.LSTORE || opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE
                || opcode == Opcodes.ASTORE || opcode == Opcodes.RET;
    }

    private static boolean isMethodOpcode(int opcode) {
        return opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL
                || opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKEINTERFACE;
    }

    private static boolean isFieldOpcode(int opcode) {
        return opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC
                || opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD;
    }

    private static boolean isConditionalBranchOpcode(int opcode) {
        return opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE
                || opcode == Opcodes.IFNULL
                || opcode == Opcodes.IFNONNULL;
    }

    private static boolean isSimpleOpcode(int opcode) {
        return opcode == Opcodes.NOP
                || opcode >= Opcodes.ACONST_NULL && opcode <= Opcodes.DCONST_1
                || opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD
                || opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE
                || opcode >= Opcodes.POP && opcode <= Opcodes.SWAP
                || opcode >= Opcodes.IADD && opcode <= Opcodes.LXOR
                || opcode >= Opcodes.I2L && opcode <= Opcodes.DCMPG
                || opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN
                || opcode == Opcodes.ARRAYLENGTH
                || opcode == Opcodes.ATHROW
                || opcode == Opcodes.MONITORENTER
                || opcode == Opcodes.MONITOREXIT;
    }

    private static String requiredInternalName(JsonObject json, String property, String location)
            throws IOException {
        String value = requiredString(json, property, location);
        if (value.indexOf('.') >= 0 || value.startsWith("/") || value.endsWith("/")
                || value.contains("//")) {
            throw new IOException(location + "." + property + " must be a JVM internal name using '/' separators");
        }
        return value;
    }

    private static String requiredString(JsonObject json, String property, String location) throws IOException {
        JsonElement element = json.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isBlank()) {
            throw new IOException(location + "." + property + " must be a non-blank string");
        }
        return element.getAsString();
    }

    private static int requiredInt(JsonObject json, String property, String location) throws IOException {
        JsonElement element = json.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IOException(location + "." + property + " must be an integer");
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException invalid) {
            throw new IOException(location + "." + property + " must be an integer", invalid);
        }
    }

    private static int optionalInt(JsonObject json, String property, int defaultValue, String location)
            throws IOException {
        return json.has(property) ? requiredInt(json, property, location) : defaultValue;
    }

    private static JsonArray requiredArray(JsonObject json, String property, String location) throws IOException {
        JsonElement element = json.get(property);
        if (element == null || !element.isJsonArray()) {
            throw new IOException(location + "." + property + " must be an array");
        }
        return element.getAsJsonArray();
    }
}
