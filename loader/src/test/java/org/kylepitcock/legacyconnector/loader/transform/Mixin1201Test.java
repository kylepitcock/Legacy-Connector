package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Mixin handling of the 1.20.1 translation: names in annotation strings, relaxed requirements, dropped mixins, harmless configs. */
class Mixin1201Test implements Opcodes {
    private static ClassNode mixin(String name, String target) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = "java/lang/Object";
        AnnotationNode a = new AnnotationNode("Lorg/spongepowered/asm/mixin/Mixin;");
        a.values = new ArrayList<>(List.of("value", new ArrayList<Object>(List.of(Type.getObjectType(target)))));
        n.invisibleAnnotations = new ArrayList<>(List.of(a));
        return n;
    }

    private static AnnotationNode inject(MethodNode m, String desc, String method, String at, int require) {
        AnnotationNode a = new AnnotationNode(desc);
        AnnotationNode atNode = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
        atNode.values = new ArrayList<>(List.of("value", "INVOKE", "target", at));
        a.values = new ArrayList<>(List.of("method", new ArrayList<Object>(List.of(method)), "at", new ArrayList<Object>(List.of(atNode)), "require", require));
        if (m.invisibleAnnotations == null) m.invisibleAnnotations = new ArrayList<>();
        m.invisibleAnnotations.add(a);
        return a;
    }

    @Test
    void srgAndRenamedNamesInAnnotationsAreMoved() {
        ClassNode n = mixin("m/FooMixin", "net/minecraft/world/level/chunk/status/ChunkStatus");
        MethodNode m = new MethodNode(ACC_PRIVATE, "handler", "()V", null, null);
        n.methods.add(m);
        // an SRG method name, a renamed class in the @At owner, and a member of the renames table
        AnnotationNode a = inject(m, "Lorg/spongepowered/asm/mixin/injection/Inject;", "m_6075_(Lnet/minecraft/core/BlockSource;)V",
                "Lnet/minecraft/world/level/chunk/ChunkStatus;foo()V", 1);
        Mixin1201.Stats st = new Mixin1201.Stats();
        Mixin1201.apply(n, Map.of("m_6075_", "tick"), new Mc1201Hierarchy(), Mc1201Tables.get(), st);
        List<?> methods = (List<?>) a.values.get(1);
        assertEquals("tick(Lnet/minecraft/core/dispenser/BlockSource;)V", methods.get(0));
        AnnotationNode at = (AnnotationNode) ((List<?>) a.values.get(3)).get(0);
        assertEquals("Lnet/minecraft/world/level/chunk/status/ChunkStatus;foo()V", at.values.get(3));
        assertEquals(0, a.values.get(5), "require is relaxed");
        assertEquals(1, st.requireRemoved);
    }

    @Test
    void mixinWithMissingMinecraftTargetIsDropped() {
        ClassNode gone = mixin("m/GoneMixin", "net/minecraft/world/item/alchemy/PotionUtils");
        ClassNode there = mixin("m/ThereMixin", "net/minecraft/world/item/ItemStack");
        Mixin1201.Stats st = new Mixin1201.Stats();
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        Mixin1201.apply(gone, Map.of(), h, Mc1201Tables.get(), st);
        Mixin1201.apply(there, Map.of(), h, Mc1201Tables.get(), st);
        assertEquals(Set.of("m/GoneMixin"), st.droppedClasses);
        assertEquals(2, st.mixinClasses);
    }

    @Test
    void configIsNotRequiredAndDropsTheMissingMixins() {
        String json = "{\"required\":true,\"package\":\"m\",\"refmap\":\"x.refmap.json\",\"mixins\":[\"GoneMixin\",\"ThereMixin\"],\"client\":[\"sub.ClientMixin\"],\"injectors\":{\"defaultRequire\":1}}";
        List<String> notes = new ArrayList<>();
        String out = Mixin1201.config(json, Set.of("m/GoneMixin"), notes);
        JsonObject o = JsonParser.parseString(out).getAsJsonObject();
        assertFalse(o.get("required").getAsBoolean());
        assertFalse(o.has("refmap"));
        assertEquals(0, o.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        assertEquals(1, o.getAsJsonArray("mixins").size());
        assertEquals("ThereMixin", o.getAsJsonArray("mixins").get(0).getAsString());
        assertEquals(1, o.getAsJsonArray("client").size());
        assertEquals(1, notes.size());
    }

    @Test
    void nonMixinJsonIsLeftAlone() {
        String json = "{\"package\":\"x\",\"name\":\"y\"}";
        assertEquals(json, Mixin1201.config(json, Set.of(), null));
        assertTrue(Mixin1201.isMixin(mixin("m/A", "java/lang/Object")));
    }
}
