package org.kylepitcock.legacyconnector.loader.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Call-site redirects of a forward version (see {@code LegacyVersion#forward()}): a call whose method the 1.7.10 shim lacks or shapes
 * differently ({@code player.sendChatToPlayer(ChatMessageComponent)}, {@code GameRegistry.registerPlayerTracker}) becomes a static call to an
 * adapter. The stack shape stays the same, so stack map frames stay valid.
 *
 * <p>Table: {@code legacyconnector/forward/<id>/call-redirects.tsv}, one rule per line: {@code kind TAB owner TAB name TAB descriptor TAB
 * targetClass TAB targetMethod}. Kind {@code virtual} applies to virtual and interface calls on any shim type (not on the mods' own classes;
 * the owner column is {@code *}); the target is {@code static targetMethod(Object receiver, <the descriptor's arguments>)}. Kind {@code static}
 * applies to {@code INVOKESTATIC} on exactly that owner (a legacy name, relocated like the descriptor) and keeps the descriptor. In the
 * descriptor, owner and target class {@code {S}} stands for the prefix of the forwarded shim and {@code {O}} for the version's own shim
 * prefix; the descriptor is the one after relocation.</p>
 */
public final class ForwardRedirects {
    /** One rule: where the call goes, and whether the receiver is passed on (virtual) or there is none (static). */
    record Rule(String targetClass, String targetMethod, boolean isStatic) {}

    private static final Map<LegacyVersion, Map<String, Rule>> CACHE = new EnumMap<>(LegacyVersion.class);

    private ForwardRedirects() {}

    /** Number of calls redirected in {@code node}. */
    public static int apply(ClassNode node, LegacyVersion version) {
        if (version.forward() == null) return 0;
        Map<String, Rule> rules = rules(version);
        if (rules.isEmpty()) return 0;
        int n = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode m)) continue;
                int op = m.getOpcode();
                if (op == Opcodes.INVOKESTATIC) {
                    Rule rule = rules.get("static " + m.owner + "." + m.name + m.desc);
                    if (rule == null) continue;
                    m.owner = rule.targetClass();
                    m.name = rule.targetMethod();
                    m.itf = false;
                    n++;
                } else if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
                    if (!m.owner.startsWith("legacyconnector/")) continue; // a mod's own method of that name is not ours to redirect
                    Rule rule = rules.get("virtual " + m.name + m.desc);
                    if (rule == null) continue;
                    m.setOpcode(Opcodes.INVOKESTATIC);
                    m.owner = rule.targetClass();
                    m.name = rule.targetMethod();
                    m.desc = "(Ljava/lang/Object;" + m.desc.substring(1);
                    m.itf = false;
                    n++;
                }
            }
        }
        return n;
    }

    static synchronized Map<String, Rule> rules(LegacyVersion version) {
        return CACHE.computeIfAbsent(version, v -> {
            Map<String, Rule> out = new HashMap<>();
            String resource = "legacyconnector/forward/" + v.id() + "/call-redirects.tsv";
            try (InputStream in = ForwardRedirects.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) return out;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.strip().split("\t");
                    if (f.length < 6) throw new IllegalStateException("Corrupt redirect rule in " + resource + ": " + line);
                    boolean isStatic = f[0].equals("static");
                    if (!isStatic && !f[0].equals("virtual")) throw new IllegalStateException("Unknown redirect kind in " + resource + ": " + line);
                    String desc = expand(f[3], v);
                    String key = isStatic
                            ? "static " + v.linkPrefix() + org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames.map(v, f[1]) + "." + f[2] + desc
                            : "virtual " + f[2] + desc;
                    out.put(key, new Rule(expand(f[4], v), f[5].strip(), isStatic));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt redirect table " + resource, e);
            }
            return Map.copyOf(out);
        });
    }

    private static String expand(String s, LegacyVersion v) {
        return s.replace("{S}", v.linkPrefix()).replace("{O}", v.shimPrefix());
    }
}
