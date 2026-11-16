package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class Forge1201MobFinalizeSpawnEvolutionAuditTest {
    @TempDir
    Path tempDir;

    @Test
    void comparesEffectiveOverridesByTheCorrectPerRuntimeBaseDescriptor() throws Exception {
        Path sourceJar = tempDir.resolve("forge-source.jar");
        Path targetJar = tempDir.resolve("neoforge-target.jar");

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(sourceJar))) {
            addClass(jar, Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER, "java/lang/Object",
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.SOURCE_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
            addClass(jar, "net/minecraft/world/entity/Animal", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.SOURCE_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
            addClass(jar, "net/minecraft/world/entity/Zombie", "net/minecraft/world/entity/Animal",
                    new MethodSpec[0]);
            addClass(jar, "net/minecraft/world/entity/Skeleton", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.SOURCE_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
            addClass(jar, "net/minecraft/world/entity/Villager", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[0]);
            addClass(jar, "net/minecraft/world/entity/StaticHelperMob", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.SOURCE_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) });
        }

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(targetJar))) {
            addClass(jar, Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER, "java/lang/Object",
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.TARGET_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
            addClass(jar, "net/minecraft/world/entity/Animal", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.TARGET_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
            addClass(jar, "net/minecraft/world/entity/Zombie", "net/minecraft/world/entity/Animal",
                    new MethodSpec[0]);
            addClass(jar, "net/minecraft/world/entity/Skeleton", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[0]);
            addClass(jar, "net/minecraft/world/entity/Villager", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[0]);
            addClass(jar, "net/minecraft/world/entity/Warden", Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.TARGET_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
        }

        var result = Forge1201MobFinalizeSpawnEvolutionAudit.analyze(
                index(sourceJar, "source-classpath.txt"),
                index(targetJar, "target-classpath.txt"));

        assertEquals(5, result.sourceMobSubclassCount());
        assertEquals(5, result.targetMobSubclassCount());
        assertEquals(2, result.sourceEffectiveSubclassOverrideCount());
        assertEquals(2, result.targetEffectiveSubclassOverrideCount());
        assertFalse(result.selectorTransformationPerformed());
        assertFalse(result.runtimeMappingTableProduced());

        Map<String, Forge1201MobFinalizeSpawnEvolutionAudit.OwnerComparison> byOwner = new HashMap<>();
        result.owners().forEach(owner -> byOwner.put(owner.owner(), owner));

        var base = byOwner.get(Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER);
        assertNotNull(base);
        assertEquals("BASE_SIGNATURE_SET_CHANGED", base.classification());
        assertEquals(
                Forge1201MobFinalizeSpawnEvolutionAudit.SOURCE_BASE_DESCRIPTOR,
                base.sourceDeclaredFinalizeSpawnMethods().getFirst().descriptor());
        assertEquals(
                Forge1201MobFinalizeSpawnEvolutionAudit.TARGET_BASE_DESCRIPTOR,
                base.targetDeclaredFinalizeSpawnMethods().getFirst().descriptor());

        var inheritedOverride = byOwner.get("net/minecraft/world/entity/Zombie");
        assertEquals("OVERRIDE_PRESENT_BOTH_SAME_DECLARING_OWNER", inheritedOverride.classification());
        assertEquals("net/minecraft/world/entity/Animal", inheritedOverride.sourceDispatchDeclaringOwner());
        assertEquals("net/minecraft/world/entity/Animal", inheritedOverride.targetDispatchDeclaringOwner());

        var removedOverride = byOwner.get("net/minecraft/world/entity/Skeleton");
        assertEquals("SOURCE_OVERRIDE_REMOVED_TARGET_INHERITS_BASE", removedOverride.classification());
        assertEquals("net/minecraft/world/entity/Skeleton", removedOverride.sourceDispatchDeclaringOwner());
        assertEquals(Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                removedOverride.targetDispatchDeclaringOwner());

        var unchanged = byOwner.get("net/minecraft/world/entity/Villager");
        assertEquals("INHERITS_MOB_BASE_BOTH", unchanged.classification());

        var staticHelper = byOwner.get("net/minecraft/world/entity/StaticHelperMob");
        assertEquals("INHERITS_MOB_BASE_BOTH", staticHelper.classification());
        assertFalse(staticHelper.sourceDeclaredFinalizeSpawnMethods().getFirst().virtualInstanceMethod());
        assertEquals(Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER,
                staticHelper.sourceDispatchDeclaringOwner());

        var targetOnly = byOwner.get("net/minecraft/world/entity/Warden");
        assertEquals("TARGET_ONLY_MOB_SUBCLASS", targetOnly.classification());
        assertFalse(targetOnly.sourceClassPresent());
        assertTrue(targetOnly.targetMobSubclass());
    }

    @Test
    void keepsIncompleteEntityHierarchySeparateFromConfirmedNonMobClasses() throws Exception {
        Path sourceJar = tempDir.resolve("incomplete-source.jar");
        Path targetJar = tempDir.resolve("target.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(sourceJar))) {
            addClass(jar, Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER, "java/lang/Object",
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.SOURCE_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
            addClass(jar, "net/minecraft/world/entity/UnknownChild", "sample/UnavailableParent", new MethodSpec[0]);
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(targetJar))) {
            addClass(jar, Forge1201MobFinalizeSpawnEvolutionAudit.MOB_OWNER, "java/lang/Object",
                    new MethodSpec[] { new MethodSpec(Forge1201MobFinalizeSpawnEvolutionAudit.TARGET_BASE_DESCRIPTOR, Opcodes.ACC_PUBLIC) });
        }

        var result = Forge1201MobFinalizeSpawnEvolutionAudit.analyze(
                index(sourceJar, "incomplete-source-classpath.txt"),
                index(targetJar, "incomplete-target-classpath.txt"));

        assertTrue(result.sourceIncompleteEntityHierarchies().stream()
                .anyMatch(value -> value.startsWith("net/minecraft/world/entity/UnknownChild -> ")));
        assertEquals(0, result.sourceMobSubclassCount());
    }

    private Forge1201SourceRuntimeMemberVerifier.Index index(Path jar, String classpathName) throws Exception {
        Path classpath = tempDir.resolve(classpathName);
        Files.writeString(classpath, jar.toAbsolutePath() + System.lineSeparator(), StandardCharsets.UTF_8);
        return Forge1201SourceRuntimeMemberVerifier.readClasspath(classpath);
    }

    private record MethodSpec(String descriptor, int access) {}

    private static void addClass(
            JarOutputStream jar,
            String name,
            String superName,
            MethodSpec[] methods) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null);
        for (MethodSpec method : methods) {
            writer.visitMethod(method.access(), "finalizeSpawn", method.descriptor(), null, null).visitEnd();
        }
        writer.visitEnd();
        jar.putNextEntry(new JarEntry(name + ".class"));
        jar.write(writer.toByteArray());
        jar.closeEntry();
    }
}
