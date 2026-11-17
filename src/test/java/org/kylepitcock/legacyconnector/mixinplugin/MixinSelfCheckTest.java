package org.kylepitcock.legacyconnector.mixinplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class MixinSelfCheckTest {
    private static final String BEGIN = "<!-- coremod-hooks:begin (generated from @CoremodHook; run the test with MIXIN_DOCS_UPDATE=1 to rewrite) -->";
    private static final String END = "<!-- coremod-hooks:end -->";

    private static MethodNode method(String name, boolean callsHandler, String... handlers) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, name, "()V", null, null);
        m.instructions = new InsnList();
        if (callsHandler) for (String h : handlers) m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/Target", h, "()V", false));
        m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        return m;
    }

    private static ClassNode target(String... calledHandlers) {
        ClassNode c = new ClassNode();
        c.name = "net/minecraft/Target";
        // merged handlers as Mixin names them; "applied" ones are called from a target method, "dead" ones only exist
        c.methods.add(method("handler", false));
        c.methods.add(method("inject$zzz000$legacyconnector$onApplied", false));
        c.methods.add(method("wrapoperation$zzz001$onWrapped", false));
        c.methods.add(method("inject$zzz002$onDead", false));
        c.methods.add(method("tick", true, calledHandlers));
        return c;
    }

    private static ClassNode mixin(String name, boolean hook, String... handlers) {
        ClassNode c = new ClassNode();
        c.name = name;
        c.invisibleAnnotations = new ArrayList<>();
        AnnotationNode mixin = new AnnotationNode("Lorg/spongepowered/asm/mixin/Mixin;");
        mixin.values = List.of("value", List.of(Type.getObjectType("net/minecraft/Target")));
        c.invisibleAnnotations.add(mixin);
        if (hook) c.invisibleAnnotations.add(new AnnotationNode(Type.getDescriptor(CoremodHook.class)));
        for (String h : handlers) {
            MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE, h, "()V", null, null);
            AnnotationNode inj = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Inject;");
            inj.values = List.of("method", List.of("tick"));
            m.invisibleAnnotations = List.of(inj);
            c.methods.add(m);
        }
        return c;
    }

    @Test
    void countsCallSitesPerHandlerAndIgnoresPlainMethods() {
        Map<String, Integer> refs = MixinSelfCheck.handlerRefs(
                target("inject$zzz000$legacyconnector$onApplied", "inject$zzz000$legacyconnector$onApplied", "wrapoperation$zzz001$onWrapped"));
        assertEquals(3, refs.size(), refs.toString());
        assertEquals(2, refs.get("inject$zzz000$legacyconnector$onApplied"));
        assertEquals(1, refs.get("wrapoperation$zzz001$onWrapped"));
        assertEquals(0, refs.get("inject$zzz002$onDead"));
    }

    @Test
    void describeReadsTargetsInjectorsAndHookMarker() {
        MixinSelfCheck.MixinDesc d = MixinSelfCheck.describe(mixin("pkg/XMixin", true, "onApplied", "onDead"), false, false);
        assertEquals(List.of("net/minecraft/Target"), d.targets());
        assertEquals(2, d.injectors().size());
        assertEquals(List.of("tick"), d.injectors().get(0).methods());
        assertTrue(d.hook());
    }

    @Test
    void evaluateSeparatesAppliedFeatureHookAndPending() {
        ClassNode t = target("inject$zzz000$legacyconnector$onApplied", "wrapoperation$zzz001$onWrapped");
        Map<String, MixinSelfCheck.TargetRecord> records = Map.of("net/minecraft/Target",
                new MixinSelfCheck.TargetRecord(Set.of("pkg/FeatureMixin", "pkg/HookMixin"), MixinSelfCheck.handlerRefs(t)));
        MixinSelfCheck.MixinDesc feature = MixinSelfCheck.describe(mixin("pkg/FeatureMixin", false, "onApplied", "onWrapped", "onDead"), false, false);
        MixinSelfCheck.MixinDesc hook = MixinSelfCheck.describe(mixin("pkg/HookMixin", true, "onDead"), false, false);
        ClassNode otherTarget = mixin("pkg/NotLoadedMixin", false, "onX");
        otherTarget.invisibleAnnotations.get(0).values = List.of("value", List.of(Type.getObjectType("net/minecraft/Other")));
        MixinSelfCheck.MixinDesc pending = MixinSelfCheck.describe(otherTarget, false, false);
        MixinSelfCheck.MixinDesc clientOnly = MixinSelfCheck.describe(mixin("pkg/ClientMixin", false, "onDead"), true, false);

        MixinSelfCheck.Result r = MixinSelfCheck.evaluate(List.of(feature, hook, pending, clientOnly), records, false);
        assertEquals(2, r.applied());
        assertEquals(1, r.featureNotApplied());
        assertEquals(1, r.hookNotApplied());
        assertEquals(1, r.pending());
        assertEquals(1, r.warnings().size());
        assertTrue(r.warnings().get(0).contains("FeatureMixin.onDead"), r.warnings().get(0));
        assertTrue(r.summary().startsWith("Legacy Connector mixins: 2 injectors applied, 1 not applied"), r.summary());

        // on a client the client-only mixin counts too
        assertEquals(2, MixinSelfCheck.evaluate(List.of(feature, hook, pending, clientOnly), records, true).featureNotApplied());
    }

    @Test
    void mixinMissingFromTheTargetRecordIsNotApplied() {
        ClassNode t = target("inject$zzz000$legacyconnector$onApplied");
        Map<String, MixinSelfCheck.TargetRecord> records = Map.of("net/minecraft/Target", new MixinSelfCheck.TargetRecord(Set.of("pkg/Other"), MixinSelfCheck.handlerRefs(t)));
        MixinSelfCheck.Result r = MixinSelfCheck.evaluate(List.of(MixinSelfCheck.describe(mixin("pkg/FeatureMixin", false, "onApplied"), false, false)), records, true);
        assertEquals(1, r.featureNotApplied());
    }

    @Test
    void realConfigsParseAndEveryFeatureMixinIsKnown() throws IOException {
        List<MixinSelfCheck.MixinDesc> all = new ArrayList<>();
        for (String config : MixinSelfCheck.CONFIGS) {
            String json = Files.readString(Path.of("src/main/resources", config), StandardCharsets.UTF_8);
            all.addAll(MixinSelfCheck.readConfig(json, name -> {
                try {
                    var in = MixinSelfCheckTest.class.getClassLoader().getResourceAsStream(name + ".class");
                    return in == null ? null : in.readAllBytes();
                } catch (IOException e) {
                    return null;
                }
            }));
        }
        assertTrue(all.size() > 100, "mixin classes found: " + all.size());
        int injectors = all.stream().mapToInt(m -> m.injectors().size()).sum();
        assertTrue(injectors > 150, "injectors found: " + injectors);
        // the compiled annotations agree with the sources (and so with the wiki list below)
        assertEquals(hooksFromSources(), MixinSelfCheck.hookNames(all));
    }

    private static Set<String> hooksFromSources() throws IOException {
        Set<String> out = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/org/kylepitcock/legacyconnector/mixin"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                if (Files.readString(p, StandardCharsets.UTF_8).matches("(?s).*\\R@CoremodHook\\R.*")) out.add(p.getFileName().toString().replace(".java", ""));
            }
        }
        return out;
    }

    @Test
    void wikiListsExactlyTheAnnotatedCoremodHooks() throws IOException {
        Path doc = Path.of("docs/wiki/Mixin-Audit.md");
        String text = Files.readString(doc, StandardCharsets.UTF_8);
        String generated = BEGIN + "\n\n" + String.join(", ", hooksFromSources()) + "\n\n" + END;
        int b = text.indexOf("<!-- coremod-hooks:begin");
        int e = text.indexOf(END);
        if (System.getenv("MIXIN_DOCS_UPDATE") != null && b >= 0 && e > b) {
            Files.writeString(doc, text.substring(0, b) + generated + text.substring(e + END.length()), StandardCharsets.UTF_8);
            return;
        }
        assertTrue(b >= 0 && e > b, "docs/wiki/Mixin-Audit.md has no coremod-hooks section");
        assertEquals(generated, text.substring(b, e + END.length()).replace("\r\n", "\n"), "docs/wiki/Mixin-Audit.md is out of date: run this test with MIXIN_DOCS_UPDATE=1");
    }
}
