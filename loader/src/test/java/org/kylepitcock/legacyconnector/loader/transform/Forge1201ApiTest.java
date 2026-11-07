package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** The Forge 1.20.1 facade rewrites, rename tables and metadata conversion. */
class Forge1201ApiTest {
    private static final String CAP = "Llegacyconnector/v1_20_1/net/minecraftforge/common/capabilities/Capability;";
    private static final String LAZY = "Llegacyconnector/v1_20_1/net/minecraftforge/common/util/LazyOptional;";

    private static ClassNode with(MethodInsnNode call) {
        ClassNode node = new ClassNode();
        node.name = "my/Mod";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        m.instructions.add(call);
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(m);
        return node;
    }

    private static MethodInsnNode firstCall(ClassNode node) {
        return (MethodInsnNode) node.methods.get(0).instructions.getFirst();
    }

    @Test
    void tableLoadsAndLinesAreClean() {
        Map<String, String> table = ForgeApiRewrites1201.table();
        assertFalse(table.isEmpty());
        for (var e : table.entrySet()) {
            assertFalse(e.getKey().contains("\r") || e.getValue().contains("\r"), "carriage return in " + e.getKey());
            assertTrue(e.getKey().indexOf('(') > 0, e.getKey());
        }
    }

    @Test
    void instanceCallGetsItsReceiverFirst() {
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/neoforged/neoforge/registries/DeferredHolder", "isPresent", "()Z", false));
        assertEquals(1, ForgeApiRewrites1201.apply(node));
        MethodInsnNode call = firstCall(node);
        assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
        assertEquals("legacyconnector/v1_20_1/ForgeApi1201", call.owner);
        assertEquals("holderIsPresent", call.name);
        assertEquals("(Lnet/neoforged/neoforge/registries/DeferredHolder;)Z", call.desc);
    }

    @Test
    void getCapabilityOnAnyReceiverGoesToTheCapabilityHelper() {
        String desc = "(" + CAP + "Lnet/minecraft/core/Direction;)" + LAZY;
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/level/block/entity/BlockEntity", "getCapability", desc, false));
        assertEquals(1, ForgeApiRewrites1201.apply(node));
        MethodInsnNode call = firstCall(node);
        assertEquals("legacyconnector/v1_20_1/Caps1201", call.owner);
        assertEquals("getCapability", call.name);
        assertEquals("(Lnet/minecraft/world/level/block/entity/BlockEntity;" + CAP + "Lnet/minecraft/core/Direction;)" + LAZY, call.desc);
    }

    @Test
    void superCallOfAMinecraftParentUsesTheSuperHelper() {
        String desc = "(" + CAP + "Lnet/minecraft/core/Direction;)" + LAZY;
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/world/level/block/entity/BlockEntity", "getCapability", desc, false));
        assertEquals(1, ForgeApiRewrites1201.apply(node));
        assertEquals("superGetCapability", firstCall(node).name);
    }

    @Test
    void superCallOfAModParentIsLeftAlone() {
        String desc = "(" + CAP + "Lnet/minecraft/core/Direction;)" + LAZY;
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKESPECIAL, "my/BaseBlockEntity", "getCapability", desc, false));
        assertEquals(0, ForgeApiRewrites1201.apply(node));
    }

    @Test
    void aClassThatDeclaresGetCapabilityBecomesAProvider() {
        ClassNode node = new ClassNode();
        node.name = "my/Be";
        node.superName = "net/minecraft/world/level/block/entity/BlockEntity";
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "getCapability", ForgeApiRewrites1201.GET_CAPABILITY_DESC, null, null));
        assertTrue(ForgeApiRewrites1201.markProviders(node));
        assertTrue(node.interfaces.contains(ForgeApiRewrites1201.PROVIDER));
        assertFalse(ForgeApiRewrites1201.markProviders(node), "only once");
    }

    @Test
    void modsTomlGetsTheMixinConfigsOfTheManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("MixinConfigs", "a.mixins.json, b.mixins.json");
        assertEquals(List.of("a.mixins.json", "b.mixins.json"), Forge1201Transformer.mixinConfigs(manifest));
        String toml = Forge1201Transformer.modsToml("modLoader=\"javafml\"\nloaderVersion=\"[47,)\"\n[[mods]]\nmodId=\"x\"\n", List.of("a.mixins.json"));
        assertTrue(toml.contains("loaderVersion=\"[1,)\""), toml);
        assertTrue(toml.contains("[[mixins]]\nconfig=\"a.mixins.json\""), toml);
    }

    @Test
    void stripRefmapKeepsTheJsonValidWhateverTheKeyOrder() {
        assertEquals("{\"package\":\"p\"}", Forge1201Transformer.stripRefmap("{\"package\":\"p\",\"refmap\":\"r.json\"}"));
        assertEquals("{\"package\":\"p\"}", Forge1201Transformer.stripRefmap("{\"refmap\":\"r.json\",\"package\":\"p\"}"));
    }

    @Test
    void nestedMixinExtrasIsDroppedFromTheJarJarMetadata() {
        String json = "{\"jars\":[{\"identifier\":{\"group\":\"g\",\"artifact\":\"mixinextras-forge\"},\"path\":\"META-INF/jarjar/mixinextras-forge-0.3.6.jar\"},"
                + "{\"identifier\":{\"group\":\"g\",\"artifact\":\"spectrelib-forge\"},\"path\":\"META-INF/jarjar/spectrelib-forge-1.jar\"}]}";
        String out = Forge1201Transformer.jarJarMetadata(json);
        assertFalse(out.contains("mixinextras"), out);
        assertTrue(out.contains("spectrelib"), out);
    }

    @Test
    void renameTablesHaveNoCarriageReturnsAndSensibleTargets() throws IOException {
        for (String f : List.of("forge-event-renames.tsv", "forge-fml-renames.tsv", "forge-curated-renames.tsv", "forge-call-rewrites.tsv")) {
            try (InputStream in = Forge1201ApiTest.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/" + f)) {
                assertTrue(in != null, f);
                for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (l.isBlank() || l.startsWith("#")) continue;
                    String[] a = l.split("\t");
                    assertEquals(2, a.length, f + ": " + l);
                    if (!f.equals("forge-call-rewrites.tsv")) assertTrue(a[0].startsWith("net/minecraftforge/") && a[1].startsWith("net/neoforged/"), f + ": " + l);
                }
            }
        }
    }
}
