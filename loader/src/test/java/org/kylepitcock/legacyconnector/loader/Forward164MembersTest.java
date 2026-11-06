package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.synth.LegacyFields;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;
import org.kylepitcock.legacyconnector.loader.synth.LegacyMethods;

/** The 1.6.4 to 1.7.10 member tables (forward/1.6.4/member-*.tsv): sources exist in 1.6.4, targets in 1.7.10, lookups follow supertypes. */
class Forward164MembersTest {
    private static final LegacyVersion V = LegacyVersion.V1_6_4;
    private static final LegacyHierarchyData H17 = LegacyHierarchyData.forVersion(LegacyVersion.V1_7_10);
    private static final LegacyHierarchyData H16 = LegacyHierarchyData.forVersion(V);

    private static final Map<String, Map<String, List<LegacyFields.Field>>> FIELDS = new java.util.HashMap<>();

    private static String rd(String desc) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            out.append(c);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                out.append(ForwardRenames.map(V, desc.substring(i + 1, end))).append(';');
                i = end;
            }
        }
        return out.toString();
    }

    /** Is {@code owner.name desc} declared by the class or a supertype, in the version's API data. */
    private static boolean has(LegacyVersion version, LegacyHierarchyData h, String owner, String name, String desc) {
        ArrayDeque<String> todo = new ArrayDeque<>(List.of(owner));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (!seen.add(c)) continue;
            for (var m : LegacyMethods.of(version, c)) if (m.name().equals(name) && m.desc().equals(desc)) return true;
            var e = h.get(c);
            if (e != null) {
                if (e.superName() != null) todo.add(e.superName());
                todo.addAll(e.interfaces());
            }
        }
        return false;
    }

    private static boolean hasField(LegacyVersion version, LegacyHierarchyData h, String owner, String name) {
        ArrayDeque<String> todo = new ArrayDeque<>(List.of(owner));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (!seen.add(c)) continue;
            for (var f : FIELDS.computeIfAbsent(version.id(), LegacyFields::byOwner).getOrDefault(c, List.of())) if (f.name().equals(name)) return true;
            var e = h.get(c);
            if (e != null) {
                if (e.superName() != null) todo.add(e.superName());
                todo.addAll(e.interfaces());
            }
        }
        return false;
    }

    @Test
    void tablesAreLoaded() {
        ForwardMembers t = ForwardMembers.forVersion(V);
        assertTrue(t.methodCount() > 300, "methods " + t.methodCount());
        assertTrue(t.fieldCount() > 100, "fields " + t.fieldCount());
        assertTrue(ForwardMembers.forVersion(LegacyVersion.V1_7_10).isEmpty());
    }

    @Test
    void nameRenamesStartInOneSixFourAndEndInOneSevenTen() {
        ForwardMembers t = ForwardMembers.forVersion(V);
        List<String> bad = new java.util.ArrayList<>();
        t.methodRules().forEach((key, to) -> {
            int dot = key.indexOf('.');
            int paren = key.indexOf('(');
            String owner = key.substring(0, dot);
            String name = key.substring(dot + 1, paren);
            String desc = key.substring(paren);
            if (H16.get(owner) == null) bad.add("1.6.4 has no class " + key);
            else if (!has(V, H16, owner, name, desc)) bad.add("1.6.4 has no " + key);
            String fo = ForwardRenames.map(V, owner);
            if (!has(LegacyVersion.V1_7_10, H17, fo, to, rd(desc))) bad.add("1.7.10 has no " + fo + "." + to + rd(desc) + " (for " + key + ")");
            if (name.equals(to)) bad.add("no-op rename " + key);
        });
        t.fieldRules().forEach((key, to) -> {
            int dot = key.indexOf('.');
            String owner = key.substring(0, dot);
            String fo = ForwardRenames.map(V, owner);
            if (!hasField(LegacyVersion.V1_7_10, H17, fo, to)) bad.add("1.7.10 has no field " + fo + "." + to + " (for " + key + ")");
        });
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void callRewritesPointAtRealMembers() {
        ForwardMembers t = ForwardMembers.forVersion(V);
        List<String> bad = new java.util.ArrayList<>();
        t.callRules().forEach((key, r) -> {
            String owner = r[1];
            if (H17.get(owner) == null) bad.add("1.7.10 has no class " + owner);
            switch (r[4]) {
                case "call" -> {
                    String to = r[5].equals("=") ? owner : r[5];
                    if (!has(LegacyVersion.V1_7_10, H17, to, r[6], r[7])) bad.add("1.7.10 has no " + to + "." + r[6] + r[7]);
                }
                case "call-pop", "droplast", "append-int" -> {
                    if (!has(LegacyVersion.V1_7_10, H17, owner, r[5], r[6])) bad.add("1.7.10 has no " + owner + "." + r[5] + r[6]);
                }
                case "wrap-text" -> {
                    if (!has(LegacyVersion.V1_7_10, H17, owner, r[5], r[6])) bad.add("1.7.10 has no " + owner + "." + r[5] + r[6]);
                }
                case "chain" -> {
                    String first = r[5].equals("=") ? owner : r[5];
                    if (!has(LegacyVersion.V1_7_10, H17, first, r[6], r[7])) bad.add("1.7.10 has no " + first + "." + r[6] + r[7]);
                    if (!has(LegacyVersion.V1_7_10, H17, r[8], r[9], r[10])) bad.add("1.7.10 has no " + r[8] + "." + r[9] + r[10]);
                }
                case "static" -> assertTrue(r[5].startsWith("legacyconnector/v1_6_4/"), key);
                default -> bad.add("unknown op " + r[4] + " in " + key);
            }
        });
        t.fieldCallRules().forEach((key, r) -> {
            if (r[4].equals("field")) {
                if (!hasField(LegacyVersion.V1_7_10, H17, r[1], r[5])) bad.add("1.7.10 has no field " + r[1] + "." + r[5]);
            } else {
                String to = r[4].equals("=") ? r[1] : r[4];
                if (!has(LegacyVersion.V1_7_10, H17, to, r[5], r[6])) bad.add("1.7.10 has no " + to + "." + r[5] + r[6]);
            }
        });
        t.constructorRules().forEach((key, r) -> {
            if (!has(LegacyVersion.V1_7_10, H17, r[1], "<init>", r[3]) && !LegacyMethods.of(LegacyVersion.V1_7_10, r[1]).isEmpty()) {
                // constructors are not in methods.txt; the class itself must exist
                if (H17.get(r[1]) == null) bad.add("1.7.10 has no class " + r[1]);
            }
        });
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void lookupsFollowSupertypes() {
        ForwardMembers t = ForwardMembers.forVersion(V);
        // declared on TileEntity, called through a vanilla subclass
        assertEquals("markDirty", t.method("net/minecraft/tileentity/TileEntityFurnace", "onInventoryChanged", "()V", null));
        // a mod's own TileEntity subclass is known only to the caller
        Map<String, String[]> locals = Map.of("my/ModTile", new String[] {"net/minecraft/tileentity/TileEntity"}, "my/Deeper", new String[] {"my/ModTile"});
        assertEquals("markDirty", t.method("my/Deeper", "onInventoryChanged", "()V", locals::get));
        // not in the table, not a vanilla class, another descriptor: unchanged
        assertEquals("onInventoryChanged", t.method("my/Unrelated", "onInventoryChanged", "()V", locals::get));
        assertEquals("onInventoryChanged", t.method("net/minecraft/tileentity/TileEntity", "onInventoryChanged", "(I)V", null));
        assertEquals("getTileEntity", t.method("net/minecraft/world/World", "getBlockTileEntity", "(III)Lnet/minecraft/tileentity/TileEntity;", null));
        assertEquals("getTileEntity", t.method("net/minecraft/world/WorldServer", "getBlockTileEntity", "(III)Lnet/minecraft/tileentity/TileEntity;", null));
        assertEquals("getCommandSenderName", t.method("net/minecraft/entity/player/EntityPlayerMP", "getEntityName", "()Ljava/lang/String;", null));
        assertEquals("fontRendererObj", t.field("net/minecraft/client/gui/inventory/GuiContainer", "fontRenderer", null));
        assertEquals("fontRenderer", t.field("net/minecraft/client/Minecraft", "fontRenderer", null));
    }

    @Test
    void syntheticTablesParse() {
        ForwardMembers t = ForwardMembers.of(V, List.of("# c", "M\ta/B\tx\t()V\ty", "F\ta/B\tf\tg", "R\ta/B\tz\t()V\tcall\t=\tw\t()V", "G\ta/B\th\tI\t=\tk\t()I", "C\ta/C\t(Ljava/lang/String;)V\t()V\tdrop"));
        assertEquals(1, t.methodCount());
        assertEquals(1, t.fieldCount());
        assertEquals(1, t.callRules().size());
        assertEquals(1, t.fieldCallRules().size());
        assertEquals(1, t.constructorRules().size());
    }
}
