package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Offline bytecode inventory for a Forge 1.20.1 mod.
 *
 * <p>This tool does not load or execute mod classes. It inventories the Minecraft/Forge symbols
 * referenced by a jar so a version-specific adapter can be designed from evidence rather than guesses.</p>
 */
public final class Forge1201ApiUsageAnalyzer {
    private static final String FORGE_MOD_ANNOTATION = "Lnet/minecraftforge/fml/common/Mod;";
    private static final String NEOFORGE_MOD_ANNOTATION = "Lnet/neoforged/fml/common/Mod;";

    private Forge1201ApiUsageAnalyzer() {}

    public record ApiMemberReference(String owner, String name, String descriptor, int count) {
        public String display() {
            return owner.replace('/', '.') + "#" + name + descriptor + (count > 1 ? "  (x" + count + ")" : "");
        }
    }

    /**
     * A direct bytecode invocation from a mod-owned method to a Minecraft, Forge, or optional API method.
     * This preserves the caller method, unlike the global member inventory.
     */
    public record ApiMethodCallSite(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode,
            int count) {
        public String display() {
            return targetOwner.replace('/', '.') + "#" + targetName + targetDescriptor
                    + " <= " + callerClass.replace('/', '.') + "#" + callerMethod + callerDescriptor
                    + " [" + opcodeName(opcode) + "]"
                    + (count > 1 ? " (x" + count + ")" : "");
        }
    }

    public record ApiFieldCallSite(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode,
            int count) {
        public String display() {
            return targetOwner.replace('/', '.') + "#" + targetName + ":" + targetDescriptor
                    + " <= " + callerClass.replace('/', '.') + "#" + callerMethod + callerDescriptor
                    + " [" + opcodeName(opcode) + "]"
                    + (count > 1 ? " (x" + count + ")" : "");
        }
    }

    private record CallSiteKey(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode) {}

    private record FieldCallSiteKey(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode) {}

    public record Analysis(
            Forge1201JarInfo metadata,
            int classCount,
            List<String> modEntrypoints,
            List<String> mixinConfigs,
            String coreModClass,
            Map<String, Integer> packageReferences,
            List<String> referencedApiClasses,
            List<ApiMethodCallSite> methodCallSites,
            List<ApiFieldCallSite> fieldCallSites,
            List<ApiMemberReference> methodReferences,
            List<ApiMemberReference> fieldReferences) {

        public Analysis {
            modEntrypoints = List.copyOf(modEntrypoints);
            mixinConfigs = List.copyOf(mixinConfigs);
            packageReferences = Map.copyOf(packageReferences);
            referencedApiClasses = List.copyOf(referencedApiClasses);
            methodCallSites = List.copyOf(methodCallSites);
            fieldCallSites = List.copyOf(fieldCallSites);
            methodReferences = List.copyOf(methodReferences);
            fieldReferences = List.copyOf(fieldReferences);
        }
    }

    private record MemberKey(String owner, String name, String descriptor) {}

    private static final class ApiReferenceCollector extends Remapper {
        private final Set<String> classes = new LinkedHashSet<>();
        private final Map<String, Integer> packages = new TreeMap<>();
        private final Map<MemberKey, Integer> methods = new HashMap<>();
        private final Map<MemberKey, Integer> fields = new HashMap<>();
        private final Map<CallSiteKey, Integer> callSites = new HashMap<>();
        private final Map<FieldCallSiteKey, Integer> fieldCallSites = new HashMap<>();

        private void recordClass(String internalName) {
            if (!isGameApi(internalName)) return;
            classes.add(internalName);
            String[] pieces = internalName.split("/");
            String packageName = String.join("/", java.util.Arrays.copyOf(pieces, Math.min(3, pieces.length)));
            packages.merge(packageName, 1, Integer::sum);
        }

        @Override
        public String map(String internalName) {
            recordClass(internalName);
            return internalName;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            // ClassRemapper's member-name callback is also an authoritative reference to the owner.
            // Record it here rather than relying on the order in which ASM visits descriptors and instructions.
            recordClass(owner);
            if (isGameApi(owner)) methods.merge(new MemberKey(owner, name, descriptor), 1, Integer::sum);
            return name;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            recordClass(owner);
            if (isGameApi(owner)) fields.merge(new MemberKey(owner, name, descriptor), 1, Integer::sum);
            return name;
        }

        private void recordCallSite(
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                int opcode,
                String targetOwner,
                String targetName,
                String targetDescriptor) {
            if (!isGameApi(targetOwner) && !isForgeOrOptionalApi(targetOwner)) return;
            CallSiteKey key = new CallSiteKey(
                    callerClass, callerMethod, callerDescriptor,
                    targetOwner, targetName, targetDescriptor, opcode);
            callSites.merge(key, 1, Integer::sum);
        }

        private void recordFieldCallSite(
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                int opcode,
                String targetOwner,
                String targetName,
                String targetDescriptor) {
            if (!isGameApi(targetOwner) && !isForgeOrOptionalApi(targetOwner)) return;
            FieldCallSiteKey key = new FieldCallSiteKey(
                    callerClass, callerMethod, callerDescriptor,
                    targetOwner, targetName, targetDescriptor, opcode);
            fieldCallSites.merge(key, 1, Integer::sum);
        }

        private static boolean isForgeOrOptionalApi(String name) {
            return name.startsWith("net/minecraftforge/")
                    || name.startsWith("legacyconnector/v1_20_1/net/minecraftforge/")
                    || name.startsWith("net/neoforged/")
                    || name.startsWith("top/theillusivec4/curios/")
                    || name.startsWith("com/illusivesoulworks/")
                    || name.startsWith("com/gelred/");
        }

        private static boolean isGameApi(String name) {
            return name.startsWith("net/minecraft/")
                    || name.startsWith("net/minecraftforge/")
                    || name.startsWith("legacyconnector/v1_20_1/net/minecraftforge/")
                    || name.startsWith("net/neoforged/")
                    || name.startsWith("com/mojang/blaze3d/")
                    || name.startsWith("com/mojang/math/")
                    || name.startsWith("org/spongepowered/asm/mixin/");
        }

        List<String> classList() {
            return classes.stream().sorted().toList();
        }

        Map<String, Integer> packageCounts() {
            return Map.copyOf(packages);
        }

        List<ApiMethodCallSite> methodCallSiteList() {
            return callSites.entrySet().stream()
                    .map(entry -> new ApiMethodCallSite(
                            entry.getKey().callerClass(),
                            entry.getKey().callerMethod(),
                            entry.getKey().callerDescriptor(),
                            entry.getKey().targetOwner(),
                            entry.getKey().targetName(),
                            entry.getKey().targetDescriptor(),
                            entry.getKey().opcode(),
                            entry.getValue()))
                    .sorted(Comparator.comparing(ApiMethodCallSite::targetOwner)
                            .thenComparing(ApiMethodCallSite::targetName)
                            .thenComparing(ApiMethodCallSite::targetDescriptor)
                            .thenComparing(ApiMethodCallSite::callerClass)
                            .thenComparing(ApiMethodCallSite::callerMethod)
                            .thenComparing(ApiMethodCallSite::callerDescriptor)
                            .thenComparingInt(ApiMethodCallSite::opcode))
                    .toList();
        }

        List<ApiFieldCallSite> fieldCallSiteList() {
            return fieldCallSites.entrySet().stream()
                    .map(entry -> new ApiFieldCallSite(
                            entry.getKey().callerClass(),
                            entry.getKey().callerMethod(),
                            entry.getKey().callerDescriptor(),
                            entry.getKey().targetOwner(),
                            entry.getKey().targetName(),
                            entry.getKey().targetDescriptor(),
                            entry.getKey().opcode(),
                            entry.getValue()))
                    .sorted(Comparator.comparing(ApiFieldCallSite::targetOwner)
                            .thenComparing(ApiFieldCallSite::targetName)
                            .thenComparing(ApiFieldCallSite::targetDescriptor)
                            .thenComparing(ApiFieldCallSite::callerClass)
                            .thenComparing(ApiFieldCallSite::callerMethod)
                            .thenComparing(ApiFieldCallSite::callerDescriptor)
                            .thenComparingInt(ApiFieldCallSite::opcode))
                    .toList();
        }

        List<ApiMemberReference> methodList() {
            return toReferences(methods);
        }

        List<ApiMemberReference> fieldList() {
            return toReferences(fields);
        }

        private static List<ApiMemberReference> toReferences(Map<MemberKey, Integer> source) {
            return source.entrySet().stream()
                    .map(entry -> new ApiMemberReference(
                            entry.getKey().owner(),
                            entry.getKey().name(),
                            entry.getKey().descriptor(),
                            entry.getValue()))
                    .sorted(Comparator.comparingInt(ApiMemberReference::count).reversed()
                            .thenComparing(ApiMemberReference::owner)
                            .thenComparing(ApiMemberReference::name)
                            .thenComparing(ApiMemberReference::descriptor))
                    .toList();
        }
    }

    public static Analysis analyze(Path jarPath) throws IOException {
        Forge1201JarInfo metadata = Forge1201JarInspector.inspectForge1201(jarPath);
        if (metadata == null) {
            throw new IllegalArgumentException("The file is not recognized as a Forge 1.20.1 mod jar: " + jarPath);
        }
        return analyzeClasses(jarPath, metadata, FORGE_MOD_ANNOTATION);
    }

    /**
     * Inventories the output of the preliminary Forge 1.20.1 -> NeoForge 1.21.1
     * transformer. The returned Analysis has null source metadata because the input
     * is expected to contain neoforge.mods.toml rather than mods.toml.
     */
    public static Analysis analyzeTranslated(Path jarPath) throws IOException {
        if (jarPath == null || !java.nio.file.Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Translated JAR does not exist: " + jarPath);
        }
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            if (zip.getEntry("META-INF/neoforge.mods.toml") == null
                    || zip.getEntry("META-INF/mods.toml") != null) {
                throw new IllegalArgumentException(
                        "Input must be a metadata-translated JAR containing "
                                + "META-INF/neoforge.mods.toml");
            }
        }
        return analyzeClasses(jarPath, null, NEOFORGE_MOD_ANNOTATION);
    }

    private static Analysis analyzeClasses(
            Path jarPath, Forge1201JarInfo metadata, String modAnnotation) throws IOException {
        ApiReferenceCollector references = new ApiReferenceCollector();
        List<String> entrypoints = new ArrayList<>();
        List<String> mixinConfigs = new ArrayList<>();
        String coreModClass = null;
        int classCount = 0;

        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry manifestEntry = zip.getEntry(JarFile.MANIFEST_NAME);
            if (manifestEntry != null) {
                try (var in = zip.getInputStream(manifestEntry)) {
                    Manifest manifest = new Manifest(in);
                    coreModClass = manifest.getMainAttributes().getValue("FMLCorePlugin");
                    String configs = manifest.getMainAttributes().getValue("MixinConfigs");
                    if (configs != null) {
                        for (String config : configs.split(",")) {
                            if (!config.isBlank()) mixinConfigs.add(config.trim());
                        }
                    }
                }
            }

            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                classCount++;
                try (var in = zip.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(in);
                    reader.accept(new ClassRemapper(new ClassVisitor(Opcodes.ASM9) {
                        private String className;

                        @Override
                        public void visit(int version, int access, String name, String signature,
                                          String superName, String[] interfaces) {
                            className = name;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            // Preserve the containing mod method for each direct Forge/optional API call.
                            // This is separate from the global member inventory, which only knows that
                            // a symbolic reference exists somewhere in the class.
                            String callerClass = className;
                            String callerMethod = name;
                            String callerDescriptor = descriptor;
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String targetName,
                                                            String targetDescriptor, boolean isInterface) {
                                    references.recordCallSite(
                                            callerClass, callerMethod, callerDescriptor,
                                            opcode, owner, targetName, targetDescriptor);
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String targetName,
                                                           String targetDescriptor) {
                                    references.recordFieldCallSite(
                                            callerClass, callerMethod, callerDescriptor,
                                            opcode, owner, targetName, targetDescriptor);
                                }
                            };
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            if (!modAnnotation.equals(descriptor)) return null;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String name, Object value) {
                                    if (name.equals("value") || name.equals("modid")) {
                                        entrypoints.add(className + " (" + value + ")");
                                    }
                                }
                            };
                        }
                    }, references), 0);
                } catch (RuntimeException malformedClass) {
                    throw new IOException("Cannot analyze class " + entry.getName() + " in " + jarPath, malformedClass);
                }
            }
        }

        return new Analysis(
                metadata,
                classCount,
                entrypoints.stream().distinct().sorted().toList(),
                mixinConfigs.stream().distinct().sorted().toList(),
                coreModClass,
                references.packageCounts(),
                references.classList(),
                references.methodCallSiteList(),
                references.fieldCallSiteList(),
                references.methodList(),
                references.fieldList());
    }

    private static String opcodeName(int opcode) {
        return switch (opcode) {
            case Opcodes.NOP -> "NOP";
            case Opcodes.ACONST_NULL -> "ACONST_NULL";
            case Opcodes.ICONST_M1 -> "ICONST_M1";
            case Opcodes.ICONST_0 -> "ICONST_0";
            case Opcodes.ICONST_1 -> "ICONST_1";
            case Opcodes.ICONST_2 -> "ICONST_2";
            case Opcodes.ICONST_3 -> "ICONST_3";
            case Opcodes.ICONST_4 -> "ICONST_4";
            case Opcodes.ICONST_5 -> "ICONST_5";
            case Opcodes.BIPUSH -> "BIPUSH";
            case Opcodes.SIPUSH -> "SIPUSH";
            case Opcodes.ILOAD -> "ILOAD";
            case Opcodes.LLOAD -> "LLOAD";
            case Opcodes.FLOAD -> "FLOAD";
            case Opcodes.DLOAD -> "DLOAD";
            case Opcodes.ALOAD -> "ALOAD";
            case Opcodes.ISTORE -> "ISTORE";
            case Opcodes.LSTORE -> "LSTORE";
            case Opcodes.FSTORE -> "FSTORE";
            case Opcodes.DSTORE -> "DSTORE";
            case Opcodes.ASTORE -> "ASTORE";
            case Opcodes.POP -> "POP";
            case Opcodes.POP2 -> "POP2";
            case Opcodes.DUP -> "DUP";
            case Opcodes.DUP_X1 -> "DUP_X1";
            case Opcodes.DUP_X2 -> "DUP_X2";
            case Opcodes.DUP2 -> "DUP2";
            case Opcodes.IADD -> "IADD";
            case Opcodes.LADD -> "LADD";
            case Opcodes.FADD -> "FADD";
            case Opcodes.DADD -> "DADD";
            case Opcodes.ISUB -> "ISUB";
            case Opcodes.LSUB -> "LSUB";
            case Opcodes.FSUB -> "FSUB";
            case Opcodes.DSUB -> "DSUB";
            case Opcodes.IMUL -> "IMUL";
            case Opcodes.LMUL -> "LMUL";
            case Opcodes.FMUL -> "FMUL";
            case Opcodes.DMUL -> "DMUL";
            case Opcodes.IRETURN -> "IRETURN";
            case Opcodes.LRETURN -> "LRETURN";
            case Opcodes.FRETURN -> "FRETURN";
            case Opcodes.DRETURN -> "DRETURN";
            case Opcodes.ARETURN -> "ARETURN";
            case Opcodes.RETURN -> "RETURN";
            case Opcodes.GETSTATIC -> "GETSTATIC";
            case Opcodes.PUTSTATIC -> "PUTSTATIC";
            case Opcodes.GETFIELD -> "GETFIELD";
            case Opcodes.PUTFIELD -> "PUTFIELD";
            case Opcodes.INVOKEVIRTUAL -> "INVOKEVIRTUAL";
            case Opcodes.INVOKESPECIAL -> "INVOKESPECIAL";
            case Opcodes.INVOKESTATIC -> "INVOKESTATIC";
            case Opcodes.INVOKEINTERFACE -> "INVOKEINTERFACE";
            case Opcodes.INVOKEDYNAMIC -> "INVOKEDYNAMIC";
            case Opcodes.NEW -> "NEW";
            case Opcodes.NEWARRAY -> "NEWARRAY";
            case Opcodes.ANEWARRAY -> "ANEWARRAY";
            case Opcodes.ARRAYLENGTH -> "ARRAYLENGTH";
            case Opcodes.ATHROW -> "ATHROW";
            case Opcodes.CHECKCAST -> "CHECKCAST";
            case Opcodes.INSTANCEOF -> "INSTANCEOF";
            case Opcodes.IFEQ -> "IFEQ";
            case Opcodes.IFNE -> "IFNE";
            case Opcodes.IFLT -> "IFLT";
            case Opcodes.IFGE -> "IFGE";
            case Opcodes.IFGT -> "IFGT";
            case Opcodes.IFLE -> "IFLE";
            case Opcodes.IF_ICMPEQ -> "IF_ICMPEQ";
            case Opcodes.IF_ICMPNE -> "IF_ICMPNE";
            case Opcodes.IF_ICMPLT -> "IF_ICMPLT";
            case Opcodes.IF_ICMPGE -> "IF_ICMPGE";
            case Opcodes.IF_ICMPGT -> "IF_ICMPGT";
            case Opcodes.IF_ICMPLE -> "IF_ICMPLE";
            case Opcodes.IF_ACMPEQ -> "IF_ACMPEQ";
            case Opcodes.IF_ACMPNE -> "IF_ACMPNE";
            case Opcodes.GOTO -> "GOTO";
            case Opcodes.IFNULL -> "IFNULL";
            case Opcodes.IFNONNULL -> "IFNONNULL";
            default -> "OPCODE_" + opcode;
        };
    }

    public static void print(Analysis analysis) {
        System.out.println("Forge 1.20.1 API inventory");
        if (analysis.metadata() == null) {
            System.out.println("Jar: translated NeoForge 1.21.1 artifact");
            System.out.println("Metadata: " + "META-INF/neoforge.mods.toml");
        } else {
            System.out.println("Jar: " + analysis.metadata().path().getFileName());
            System.out.println("Mods: " + analysis.metadata().mods().stream()
                    .map(mod -> mod.modId() + " " + mod.version() + " (" + mod.displayName() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("none"));
            System.out.println("Declared dependencies:");
            for (Forge1201JarInfo.Mod mod : analysis.metadata().mods()) {
                for (Forge1201JarInfo.Dependency dependency : mod.dependencies()) {
                    System.out.println("  " + (dependency.mandatory() ? "required " : "optional ")
                            + dependency.modId() + " " + dependency.versionRange()
                            + " [" + dependency.ordering() + ", " + dependency.side() + "]");
                }
            }
        }
        System.out.println("Classes scanned: " + analysis.classCount());
        System.out.println("Forge @Mod entrypoints: " + analysis.modEntrypoints());
        System.out.println("Mixin configs: " + (analysis.mixinConfigs().isEmpty() ? "none declared" : analysis.mixinConfigs()));
        System.out.println("Coremod class: " + (analysis.coreModClass() == null ? "none" : analysis.coreModClass()));

        System.out.println("\nReferenced API packages:");
        analysis.packageReferences().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> System.out.println("  " + entry.getKey().replace('/', '.') + ": " + entry.getValue()));

        System.out.println("\nReferenced API classes (" + analysis.referencedApiClasses().size() + "):");
        analysis.referencedApiClasses().forEach(name -> System.out.println("  " + name.replace('/', '.')));

        System.out.println("\nMinecraft / Forge / optional API method call sites (" + analysis.methodCallSites().size() + "):");
        analysis.methodCallSites().forEach(callSite -> System.out.println("  " + callSite.display()));

        System.out.println("\nMinecraft / Forge / optional API field access call sites (" + analysis.fieldCallSites().size() + "):");
        analysis.fieldCallSites().forEach(callSite -> System.out.println("  " + callSite.display()));

        System.out.println("\nMost-used API method references:");
        analysis.methodReferences().stream().limit(80).forEach(reference -> System.out.println("  " + reference.display()));

        System.out.println("\nMost-used API field references:");
        analysis.fieldReferences().stream().limit(50).forEach(reference -> System.out.println("  " + reference.display()));
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException(
                    "Usage: inspectForge1201 <path-to-forge-1.20.1-jar> [comma-separated-class#method-trace-filter]");
        }
        Path jarPath = Path.of(args[0]);
        print(analyze(jarPath));
        if (args.length == 2 && !args[1].isBlank()) {
            printSelectedMethodBytecode(jarPath, parseTraceFilter(args[1]));
        }
    }

    private static Set<String> parseTraceFilter(String value) {
        Set<String> result = new LinkedHashSet<>();
        for (String entry : value.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) result.add(trimmed.replace('/', '.'));
        }
        return Set.copyOf(result);
    }

    /**
     * Prints a compact ASM disassembly for explicitly selected methods. This is an inspection aid:
     * it does not execute or define classes and does not modify the input archive.
     */
    static void printSelectedMethodBytecode(Path jarPath, Set<String> selectedMethods) throws IOException {
        Set<String> found = new LinkedHashSet<>();
        System.out.println("\nSelected method bytecode traces (read-only ASM disassembly):");
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                try (var in = zip.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(in);
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        private String className;

                        @Override
                        public void visit(int version, int access, String name, String signature,
                                          String superName, String[] interfaces) {
                            className = name.replace('/', '.');
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            String key = className + "#" + name;
                            if (!selectedMethods.contains(key) && !selectedMethods.contains(className + "#*")) {
                                return null;
                            }
                            found.add(key + descriptor);
                            return new MethodVisitor(Opcodes.ASM9) {
                                private final List<String> instructions = new ArrayList<>();
                                private final Map<org.objectweb.asm.Label, String> labels = new java.util.IdentityHashMap<>();
                                private int nextLabel;
                                private int index;

                                private String label(org.objectweb.asm.Label label) {
                                    return labels.computeIfAbsent(label, ignored -> "L" + nextLabel++);
                                }

                                private void add(String instruction) {
                                    instructions.add(String.format("%04d: %s", index++, instruction));
                                }

                                @Override
                                public void visitInsn(int opcode) {
                                    add(opcodeName(opcode));
                                }

                                @Override
                                public void visitIntInsn(int opcode, int operand) {
                                    add(opcodeName(opcode) + " " + operand);
                                }

                                @Override
                                public void visitVarInsn(int opcode, int var) {
                                    add(opcodeName(opcode) + " " + var);
                                }

                                @Override
                                public void visitTypeInsn(int opcode, String type) {
                                    add(opcodeName(opcode) + " " + type.replace('/', '.'));
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String fieldName,
                                                           String fieldDescriptor) {
                                    add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#"
                                            + fieldName + " " + fieldDescriptor);
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String methodName,
                                                            String methodDescriptor, boolean isInterface) {
                                    add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#"
                                            + methodName + methodDescriptor);
                                }

                                @Override
                                public void visitInvokeDynamicInsn(String methodName, String methodDescriptor,
                                                                   org.objectweb.asm.Handle bootstrapMethodHandle,
                                                                   Object... bootstrapMethodArguments) {
                                    add("INVOKEDYNAMIC " + methodName + methodDescriptor
                                            + " bootstrap=" + bootstrapMethodHandle
                                            + " args=" + java.util.Arrays.toString(bootstrapMethodArguments));
                                }

                                @Override
                                public void visitJumpInsn(int opcode, org.objectweb.asm.Label target) {
                                    add(opcodeName(opcode) + " " + label(target));
                                }

                                @Override
                                public void visitLabel(org.objectweb.asm.Label label) {
                                    add(label(label) + ":");
                                }

                                @Override
                                public void visitLdcInsn(Object value) {
                                    add("LDC " + String.valueOf(value));
                                }

                                @Override
                                public void visitIincInsn(int var, int increment) {
                                    add("IINC " + var + " " + increment);
                                }

                                @Override
                                public void visitTableSwitchInsn(int min, int max,
                                                                 org.objectweb.asm.Label defaultLabel,
                                                                 org.objectweb.asm.Label... switchLabels) {
                                    List<String> targets = new ArrayList<>();
                                    for (org.objectweb.asm.Label target : switchLabels) targets.add(label(target));
                                    add("TABLESWITCH " + min + ".." + max + " default=" + label(defaultLabel)
                                            + " targets=" + targets);
                                }

                                @Override
                                public void visitLookupSwitchInsn(org.objectweb.asm.Label defaultLabel,
                                                                  int[] keys,
                                                                  org.objectweb.asm.Label[] switchLabels) {
                                    List<String> targets = new ArrayList<>();
                                    for (int i = 0; i < switchLabels.length; i++) {
                                        targets.add(keys[i] + ":" + label(switchLabels[i]));
                                    }
                                    add("LOOKUPSWITCH default=" + label(defaultLabel) + " targets=" + targets);
                                }

                                @Override
                                public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                                    add("MULTIANEWARRAY " + descriptor + " " + dimensions);
                                }

                                @Override
                                public void visitEnd() {
                                    System.out.println("\n  " + key + descriptor);
                                    if (instructions.isEmpty()) {
                                        System.out.println("    [no instructions]");
                                    } else {
                                        instructions.forEach(instruction -> System.out.println("    " + instruction));
                                    }
                                }
                            };
                        }
                    }, 0);
                }
            }
        }

        for (String selected : selectedMethods) {
            boolean wildcard = selected.endsWith("#*");
            String prefix = wildcard ? selected.substring(0, selected.length() - 1) : selected + "(";
            boolean matched = found.stream().anyMatch(value -> value.startsWith(prefix));
            if (!matched) System.out.println("  NOT FOUND: " + selected);
        }
    }
}
