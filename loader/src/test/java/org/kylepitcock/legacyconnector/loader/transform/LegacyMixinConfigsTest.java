package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * {@link LegacyMixinConfigs}: which of a 1.12.2 mod's mixin configs survive translation (only mixins on classes of the translated
 * jar or on shim classes that have the members the mixin needs), the rewritten config, and the {@code @Shadow} field retargeting.
 */
class LegacyMixinConfigsTest implements Opcodes {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String SHIM_ENTITY = "legacyconnector/v1_12_2/net/minecraft/entity/Entity";
    private static final Gson GSON = new Gson();

    private static ClassNode mixin(String name, String superName, String... targets) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC | ACC_ABSTRACT;
        n.name = name;
        n.superName = superName;
        AnnotationVisitor av = n.visitAnnotation(MIXIN, false);
        AnnotationVisitor arr = av.visitArray("value");
        for (String t : targets) arr.visit(null, Type.getObjectType(t));
        arr.visitEnd();
        av.visitEnd();
        return n;
    }

    private static MethodNode inject(ClassNode n, String name, String... methods) {
        MethodNode m = new MethodNode(ACC_PRIVATE, name, "()V", null, null);
        AnnotationVisitor av = m.visitAnnotation(INJECT, true);
        AnnotationVisitor arr = av.visitArray("method");
        for (String s : methods) arr.visit(null, s);
        arr.visitEnd();
        av.visitEnd();
        m.visitInsn(RETURN);
        n.methods.add(m);
        return m;
    }

    private static byte[] bytes(ClassNode n) {
        ClassWriter w = new ClassWriter(0);
        n.accept(w);
        return w.toByteArray();
    }

    private static ClassNode plain(String name) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = "java/lang/Object";
        return n;
    }

    private static Hierarchy hierarchy(ClassNode... locals) {
        Map<String, ClassInfo> shim = new HashMap<>();
        shim.put("java/lang/Object", new ClassInfo("java/lang/Object", null, List.of(), ACC_PUBLIC));
        ClassInfo entity = new ClassInfo(SHIM_ENTITY, "java/lang/Object", List.of(), ACC_PUBLIC);
        entity.methods.put("onUpdate()V", ACC_PUBLIC);
        entity.methods.put("mcc$get$isDead(L" + SHIM_ENTITY + ";)Z", ACC_PUBLIC | ACC_STATIC);
        entity.fields.put("posX:D", ACC_PUBLIC);
        shim.put(entity.name, entity);
        Hierarchy h = new Hierarchy(new ShimIndex(shim), null, Map.of(), org.kylepitcock.legacyconnector.loader.LegacyVersion.V1_12_2);
        for (ClassNode l : locals) h.addLocal(ClassInfo.of(l));
        return h;
    }

    private static String config(String pkg, List<String> mixins, List<String> client, List<String> server) {
        JsonObject o = new JsonObject();
        o.addProperty("package", pkg);
        o.addProperty("required", true);
        o.addProperty("refmap", "mixins.mod.refmap.json");
        o.addProperty("plugin", "mod.MixinPlugin");
        if (mixins != null) o.add("mixins", GSON.toJsonTree(mixins));
        if (client != null) o.add("client", GSON.toJsonTree(client));
        if (server != null) o.add("server", GSON.toJsonTree(server));
        return o.toString();
    }

    private static Map<String, byte[]> files(String name, String json) {
        Map<String, byte[]> m = new LinkedHashMap<>();
        m.put(name, json.getBytes(StandardCharsets.UTF_8));
        return m;
    }

    @Test
    void mixinOnAClassOfTheJarKeepsItsConfigRewritten() {
        ClassNode target = plain("mod/Machine");
        ClassNode mx = mixin("mod/mixin/MachineMixin", "java/lang/Object", "mod/Machine");
        LegacyJar jar = new LegacyJar(Path.of("My Mod-1.0.jar"));
        Map<String, byte[]> other = files("mixins.mod.json", config("mod.mixin", List.of("MachineMixin"), null, null));
        List<String> notes = LegacyMixinConfigs.apply(jar, other, Map.of(mx.name, mx, target.name, target), hierarchy(target));

        String renamed = LegacyMixinConfigs.PREFIX + "My_Mod-1_0.mixins.mod.json";
        assertEquals(List.of(renamed), jar.mixinConfigResources);
        assertFalse(other.containsKey("mixins.mod.json"), "the original is moved");
        JsonObject out = GSON.fromJson(new String(other.get(renamed), StandardCharsets.UTF_8), JsonObject.class);
        assertFalse(out.get("required").getAsBoolean());
        assertFalse(out.has("refmap"));
        assertFalse(out.has("plugin"));
        assertEquals(0, out.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        assertEquals(1, out.getAsJsonArray("mixins").size());
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("1 of 1"), notes.get(0));
    }

    @Test
    void mixinOnAVanillaClassTheShimDoesNotHaveIsDroppedAndTheConfigStays() {
        ClassNode mx = mixin("mod/mixin/WorldMixin", "java/lang/Object", "net/minecraft/world/World");
        LegacyJar jar = new LegacyJar(Path.of("m.jar"));
        Map<String, byte[]> other = files("mixins.m.json", config("mod.mixin", List.of("WorldMixin"), null, null));
        List<String> notes = LegacyMixinConfigs.apply(jar, other, Map.of(mx.name, mx), hierarchy());
        assertTrue(jar.mixinConfigResources.isEmpty());
        assertTrue(other.containsKey("mixins.m.json"));
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("none of its 1 mixin(s)"), notes.get(0));
    }

    @Test
    void aMixinWhoseMixinSuperclassDoesNotApplyIsDroppedToo() {
        ClassNode target = plain("mod/Machine");
        ClassNode base = mixin("mod/mixin/Base", "java/lang/Object", "net/minecraft/world/World");
        ClassNode child = mixin("mod/mixin/Child", "mod/mixin/Base", "mod/Machine");
        ClassNode other1 = mixin("mod/mixin/Ok", "java/lang/Object", "mod/Machine");
        LegacyJar jar = new LegacyJar(Path.of("m.jar"));
        Map<String, byte[]> other = files("m.json", config("mod.mixin", List.of("Base", "Child", "Ok"), null, null));
        Map<String, ClassNode> nodes = Map.of(base.name, base, child.name, child, other1.name, other1, target.name, target);
        List<String> notes = LegacyMixinConfigs.apply(jar, other, nodes, hierarchy(target));
        JsonObject out = GSON.fromJson(new String(other.get(jar.mixinConfigResources.get(0)), StandardCharsets.UTF_8), JsonObject.class);
        assertEquals(List.of("Ok"), List.of(out.getAsJsonArray("mixins").get(0).getAsString()));
        assertEquals(1, out.getAsJsonArray("mixins").size());
        assertTrue(notes.get(0).contains("1 of 3"), notes.get(0));
        assertTrue(notes.get(0).contains("Base") && notes.get(0).contains("Child"), notes.get(0));
    }

    @Test
    void clientAndServerSectionsAreFilteredLikeTheMainOne() {
        ClassNode target = plain("mod/Machine");
        ClassNode a = mixin("mod/mixin/A", "java/lang/Object", "mod/Machine");
        ClassNode b = mixin("mod/mixin/B", "java/lang/Object", "net/minecraft/client/Gui");
        LegacyJar jar = new LegacyJar(Path.of("m.jar"));
        Map<String, byte[]> other = files("m.json", config("mod.mixin", List.of(), List.of("A", "B"), List.of("B")));
        LegacyMixinConfigs.apply(jar, other, Map.of(a.name, a, b.name, b, target.name, target), hierarchy(target));
        JsonObject out = GSON.fromJson(new String(other.get(jar.mixinConfigResources.get(0)), StandardCharsets.UTF_8), JsonObject.class);
        assertEquals(1, out.getAsJsonArray("client").size());
        assertEquals(0, out.getAsJsonArray("server").size());
    }

    @Test
    void jsonThatIsNotAMixinConfigIsIgnored() {
        LegacyJar jar = new LegacyJar(Path.of("m.jar"));
        Map<String, byte[]> other = new LinkedHashMap<>();
        other.put("assets/mod/x.json", "{\"package\":\"a\",\"mixins\":[\"B\"]}".getBytes(StandardCharsets.UTF_8));
        other.put("META-INF/y.json", "{\"package\":\"a\",\"mixins\":[\"B\"]}".getBytes(StandardCharsets.UTF_8));
        other.put("data.json", "{\"name\":\"x\"}".getBytes(StandardCharsets.UTF_8));
        other.put("pkg.json", "{\"package\":5}".getBytes(StandardCharsets.UTF_8));
        other.put("empty.json", "{\"package\":\"a.b\"}".getBytes(StandardCharsets.UTF_8));
        other.put("bad.json", "{\"package\": nope \"package\"".getBytes(StandardCharsets.UTF_8));
        Map<String, byte[]> before = new LinkedHashMap<>(other);
        List<String> notes = LegacyMixinConfigs.apply(jar, other, Map.of(), hierarchy());
        assertEquals(before.keySet(), other.keySet());
        assertTrue(jar.mixinConfigResources.isEmpty());
        assertTrue(notes.stream().noneMatch(s -> s.contains("empty.json")), "a package with no mixin lists is not reported");
    }

    @Test
    void mixinOnAShimClassNeedsItsInjectorsToResolveAgainstDeclaredMethods() {
        ClassNode ok = mixin("mod/mixin/Ok", "java/lang/Object", SHIM_ENTITY);
        inject(ok, "h", "onUpdate");
        ClassNode bad = mixin("mod/mixin/Bad", "java/lang/Object", SHIM_ENTITY);
        inject(bad, "h", "inheritedOrMissing");
        Hierarchy h = hierarchy();
        assertTrue(LegacyMixinConfigs.injectorsResolve(ok, SHIM_ENTITY, h));
        assertFalse(LegacyMixinConfigs.injectorsResolve(bad, SHIM_ENTITY, h));
        // selector forms: owner-qualified with descriptor, and a bare name with descriptor
        ClassNode sel = mixin("mod/mixin/Sel", "java/lang/Object", SHIM_ENTITY);
        inject(sel, "a", "Lnet/minecraft/entity/Entity;onUpdate()V", "onUpdate()V");
        assertTrue(LegacyMixinConfigs.injectorsResolve(sel, SHIM_ENTITY, h));

        LegacyJar jar = new LegacyJar(Path.of("m.jar"));
        Map<String, byte[]> other = files("m.json", config("mod.mixin", List.of("Ok", "Bad"), null, null));
        LegacyMixinConfigs.apply(jar, other, Map.of(ok.name, ok, bad.name, bad), h);
        JsonObject out = GSON.fromJson(new String(other.get(jar.mixinConfigResources.get(0)), StandardCharsets.UTF_8), JsonObject.class);
        assertEquals(1, out.getAsJsonArray("mixins").size());
        assertEquals("Ok", out.getAsJsonArray("mixins").get(0).getAsString());
    }

    @Test
    void injectorWithoutAMethodNameDoesNotResolve() {
        ClassNode mx = mixin("mod/mixin/Empty", "java/lang/Object", SHIM_ENTITY);
        inject(mx, "h");
        assertFalse(LegacyMixinConfigs.injectorsResolve(mx, SHIM_ENTITY, hierarchy()));
    }

    @Test
    void shadowFieldsMustExistOnTheShimClassOrHaveAnAccessor() {
        ClassNode mx = mixin("mod/mixin/S", "java/lang/Object", SHIM_ENTITY);
        FieldNode f = new FieldNode(ACC_PROTECTED, "isDead", "Z", null, null);
        f.visitAnnotation(SHADOW, true).visitEnd();
        mx.fields.add(f);
        FieldNode g = new FieldNode(ACC_PROTECTED, "posX", "D", null, null);
        g.visitAnnotation(SHADOW, true).visitEnd();
        mx.fields.add(g);
        Hierarchy h = hierarchy();
        assertTrue(LegacyMixinConfigs.shadowsExist(mx, SHIM_ENTITY, h), "isDead through the accessor, posX as a field");
        FieldNode missing = new FieldNode(ACC_PROTECTED, "nope", "I", null, null);
        missing.visitAnnotation(SHADOW, true).visitEnd();
        mx.fields.add(missing);
        assertFalse(LegacyMixinConfigs.shadowsExist(mx, SHIM_ENTITY, h));
    }

    @Test
    void shadowFieldAccessesMoveToTheVanillaClassAndTheDeclarationsGo() {
        ClassNode mx = mixin("mod/mixin/EntityMixin", "java/lang/Object", "net/minecraft/entity/Entity");
        FieldNode f = new FieldNode(ACC_PROTECTED, "isDead", "Z", null, null);
        f.visitAnnotation(SHADOW, true).visitEnd();
        mx.fields.add(f);
        MethodNode m = new MethodNode(ACC_PRIVATE, "check", "()Z", null, null);
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, "mod/mixin/EntityMixin", "isDead", "Z");
        m.visitInsn(IRETURN);
        mx.methods.add(m);

        // a mixin on the jar's own class: its shadows stay (Mixin applies them normally)
        ClassNode own = plain("mod/Machine");
        ClassNode ownMixin = mixin("mod/mixin/OwnMixin", "java/lang/Object", "mod/Machine");
        FieldNode of = new FieldNode(ACC_PROTECTED, "energy", "I", null, null);
        of.visitAnnotation(SHADOW, true).visitEnd();
        ownMixin.fields.add(of);

        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put(mx.name, bytes(mx));
        classes.put(own.name, bytes(own));
        classes.put(ownMixin.name, bytes(ownMixin));
        List<String> notes = LegacyMixinConfigs.retargetShadowFields(classes);
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("1 access(es)"), notes.get(0));

        ClassNode after = new ClassNode();
        new ClassReader(classes.get(mx.name)).accept(after, 0);
        assertTrue(after.fields.isEmpty(), "@Shadow declaration dropped");
        FieldInsnNode access = null;
        for (var insn : after.methods.get(0).instructions) if (insn instanceof FieldInsnNode fi) access = fi;
        assertNotNull(access);
        assertEquals("net/minecraft/entity/Entity", access.owner);
        assertEquals("isDead", access.name);

        ClassNode ownAfter = new ClassNode();
        new ClassReader(classes.get(ownMixin.name)).accept(ownAfter, 0);
        assertEquals(1, ownAfter.fields.size());
        assertNotNull(ownAfter.fields.get(0));
    }

    @Test
    void shadowFieldsOfAMixinSuperclassAreRetargetedInTheSubclass() {
        ClassNode base = mixin("mod/mixin/Base", "java/lang/Object", "net/minecraft/entity/Entity");
        FieldNode f = new FieldNode(ACC_PROTECTED, "motionX", "D", null, null);
        f.visitAnnotation(SHADOW, true).visitEnd();
        base.fields.add(f);
        ClassNode child = mixin("mod/mixin/Child", "mod/mixin/Base", "net/minecraft/entity/Entity");
        MethodNode m = new MethodNode(ACC_PRIVATE, "read", "()D", null, null);
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, "mod/mixin/Child", "motionX", "D");
        m.visitInsn(DRETURN);
        child.methods.add(m);
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put(base.name, bytes(base));
        classes.put(child.name, bytes(child));
        // javac names the subclass as the owner of an inherited shadow access
        List<String> notes = LegacyMixinConfigs.retargetShadowFields(classes);
        assertEquals(1, notes.size());
        ClassNode after = new ClassNode();
        new ClassReader(classes.get(child.name)).accept(after, 0);
        FieldInsnNode access = null;
        for (var insn : after.methods.get(0).instructions) if (insn instanceof FieldInsnNode fi) access = fi;
        assertNotNull(access);
        assertEquals("net/minecraft/entity/Entity", access.owner);
    }

    @Test
    void noMixinsMeansNoNotes() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("mod/Plain", bytes(plain("mod/Plain")));
        assertTrue(LegacyMixinConfigs.retargetShadowFields(classes).isEmpty());
    }
}
