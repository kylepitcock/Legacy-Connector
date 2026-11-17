package org.kylepitcock.legacyconnector.mixinplugin;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup self check of the layer's own mixins: which injectors were really applied. Several injectors use {@code require = 0}, so a moved target or another
 * mod removing the injection point leaves the feature silently dead. {@link LegacyMixinPlugin}'s extension records, for every transformed class that carries one
 * of our mixins, how many call sites reference each merged handler method (Mixin renames a merged handler to {@code prefix$ABCdef0$[source$]name}; an injector
 * that found no injection point leaves its handler in the class with no caller). {@link #report} (first server start and client title screen) compares that with the
 * injectors declared by the mixin classes: one summary line, one warning per unapplied FEATURE injector; the deliberately optional coremod hooks
 * ({@link CoremodHook}) are only counted. Targets that were not loaded yet are counted separately. Nothing runs per tick; the recording is a single pass over a
 * class that was being transformed anyway, and nothing here throws.
 */
public final class MixinSelfCheck {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Mixins");
    static final List<String> CONFIGS = List.of("legacyconnector.mixins.json", "legacyconnector.compat.mixins.json");
    private static final String HOOK_DESC = Type.getDescriptor(CoremodHook.class);
    private static final Set<String> INJECTORS = Set.of("Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyConstant", "ModifyVariable", "WrapOperation", "WrapMethod",
            "ModifyExpressionValue", "ModifyReturnValue", "WrapWithCondition", "ModifyReceiver");
    /** {@code inject$zzz000$name}, {@code wrapOperation$zzz001$legacyconnector$name}: prefix, 3 letters of the class id, 3 hex digits of the method id. */
    private static final Pattern HANDLER = Pattern.compile("^[a-zA-Z]+\\$[a-z]{3}[0-9a-f]{3}\\$(.+)$");

    /** Per transformed target class (internal name): our mixin classes (internal names) applied to it and the call-site count of each merged handler method. */
    private static final Map<String, TargetRecord> RECORDS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean CHECKED_ONCE = new AtomicBoolean();

    private MixinSelfCheck() {}

    record TargetRecord(Set<String> mixins, Map<String, Integer> handlerRefs) {}

    record Injector(String handler, String kind, List<String> methods) {}

    record MixinDesc(String name, List<String> targets, List<Injector> injectors, boolean hook, boolean clientOnly, boolean serverOnly) {}

    public record Result(int applied, int featureNotApplied, int hookNotApplied, int pending, List<String> warnings) {
        String summary() {
            return "Legacy Connector mixins: " + applied + " injectors applied, " + featureNotApplied + " not applied"
                    + (hookNotApplied > 0 ? " (plus " + hookNotApplied + " optional coremod hook injectors not applied, by design)" : "")
                    + (pending > 0 ? ", " + pending + " not checked yet (target class not loaded)" : "");
        }
    }

    // ---- recording (during class transformation) ----

    /** Called by the Mixin extension after all mixins were applied to {@code target}; {@code ours} are the internal names of our mixin classes applied to it. */
    static void record(ClassNode target, Collection<String> ours) {
        try {
            if (ours.isEmpty()) return;
            RECORDS.put(target.name, new TargetRecord(Set.copyOf(ours), handlerRefs(target)));
        } catch (Throwable t) {
            LOG.debug("mixin self check: could not record {}", target == null ? null : target.name, t);
        }
    }

    /** Call-site count (direct calls and method handles from inside the class) of every method that looks like a merged injector handler. */
    static Map<String, Integer> handlerRefs(ClassNode node) {
        Map<String, Integer> refs = new HashMap<>();
        for (MethodNode m : node.methods) {
            if (HANDLER.matcher(m.name).matches()) refs.putIfAbsent(m.name, 0);
        }
        if (refs.isEmpty()) return refs;
        for (MethodNode m : node.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode mi) {
                    if (mi.owner.equals(node.name)) refs.computeIfPresent(mi.name, (k, v) -> v + 1);
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (Object arg : indy.bsmArgs) {
                        if (arg instanceof Handle h && h.getOwner().equals(node.name)) refs.computeIfPresent(h.getName(), (k, v) -> v + 1);
                    }
                }
            }
        }
        return refs;
    }

    // ---- the declared side (what the mixin classes ask for) ----

    static MixinDesc describe(ClassNode mixin, boolean clientOnly, boolean serverOnly) {
        List<String> targets = new ArrayList<>();
        boolean hook = false;
        for (AnnotationNode a : annotations(mixin)) {
            if (HOOK_DESC.equals(a.desc)) hook = true;
            if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(a.desc) || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                String key = (String) a.values.get(i);
                Object val = a.values.get(i + 1);
                if (!(val instanceof List<?> list)) continue;
                for (Object o : list) {
                    if (key.equals("value") && o instanceof Type t) targets.add(t.getInternalName());
                    else if (key.equals("targets") && o instanceof String s) targets.add(s.replace('.', '/'));
                }
            }
        }
        List<Injector> injectors = new ArrayList<>();
        for (MethodNode m : mixin.methods) {
            List<AnnotationNode> all = new ArrayList<>();
            if (m.invisibleAnnotations != null) all.addAll(m.invisibleAnnotations);
            if (m.visibleAnnotations != null) all.addAll(m.visibleAnnotations);
            for (AnnotationNode a : all) {
                String kind = injectorKind(a.desc);
                if (kind == null) continue;
                List<String> methods = new ArrayList<>();
                if (a.values != null) {
                    for (int i = 0; i + 1 < a.values.size(); i += 2) {
                        if ("method".equals(a.values.get(i)) && a.values.get(i + 1) instanceof List<?> l) for (Object o : l) methods.add(String.valueOf(o));
                    }
                }
                injectors.add(new Injector(m.name, kind, methods));
                break;
            }
        }
        return new MixinDesc(mixin.name, targets, injectors, hook, clientOnly, serverOnly);
    }

    private static List<AnnotationNode> annotations(ClassNode c) {
        List<AnnotationNode> all = new ArrayList<>();
        if (c.invisibleAnnotations != null) all.addAll(c.invisibleAnnotations);
        if (c.visibleAnnotations != null) all.addAll(c.visibleAnnotations);
        return all;
    }

    private static String injectorKind(String desc) {
        if (!desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") && !desc.startsWith("Lcom/llamalad7/mixinextras/injector/")) return null;
        String simple = desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1);
        return INJECTORS.contains(simple) ? simple : null;
    }

    // ---- comparison ----

    static Result evaluate(List<MixinDesc> mixins, Map<String, TargetRecord> records, boolean client) {
        int applied = 0, feature = 0, hookMissing = 0, pending = 0;
        List<String> warnings = new ArrayList<>();
        for (MixinDesc mixin : mixins) {
            if (mixin.injectors.isEmpty()) continue;
            if (mixin.clientOnly && !client) continue;
            if (mixin.serverOnly && client) continue;
            List<String> loaded = new ArrayList<>();
            for (String t : mixin.targets) if (records.containsKey(t)) loaded.add(t);
            for (Injector inj : mixin.injectors) {
                if (loaded.isEmpty()) {
                    pending++;
                    continue;
                }
                boolean ok = false;
                for (String t : loaded) {
                    TargetRecord rec = records.get(t);
                    if (rec.mixins.contains(mixin.name) && refs(rec, inj.handler) > 0) ok = true;
                }
                if (ok) {
                    applied++;
                } else if (mixin.hook) {
                    hookMissing++;
                } else {
                    feature++;
                    warnings.add("mixin injector not applied: " + simple(mixin.name) + "." + inj.handler + " (@" + inj.kind + (inj.methods.isEmpty() ? "" : " " + inj.methods)
                            + ") on " + loaded.get(0).replace('/', '.') + ": its feature silently does nothing" + why(records.get(loaded.get(0)), mixin.name, inj.handler));
                }
            }
        }
        return new Result(applied, feature, hookMissing, pending, warnings);
    }

    /** Why an injector counts as not applied: the mixin never reached the target, its handler was not merged, or it was merged without a call site. */
    private static String why(TargetRecord rec, String mixin, String handler) {
        if (!rec.mixins.contains(mixin)) return " (the mixin was not applied to the class)";
        int merged = 0;
        for (String k : rec.handlerRefs.keySet()) {
            Matcher m = HANDLER.matcher(k);
            if (m.matches() && (m.group(1).equals(handler) || m.group(1).endsWith("$" + handler))) merged++;
        }
        return merged == 0 ? " (the handler was not merged)" : " (the handler was merged but nothing calls it: injection point not found)";
    }

    /** Call sites of the merged handler for {@code name}: the merged method is {@code prefix$ABCdef0$[source$]name}. */
    private static int refs(TargetRecord rec, String name) {
        int sum = 0;
        for (Map.Entry<String, Integer> e : rec.handlerRefs.entrySet()) {
            Matcher m = HANDLER.matcher(e.getKey());
            if (!m.matches()) continue;
            String rest = m.group(1);
            if (rest.equals(name) || rest.endsWith("$" + name)) sum += e.getValue();
        }
        return sum;
    }

    private static String simple(String internal) {
        return internal.substring(internal.lastIndexOf('/') + 1);
    }

    // ---- reporting ----

    /** Emits the summary (and the new warnings); safe to call several times, never throws. Returns the result, or null when the check could not run. */
    public static Result report(boolean client, String phase) {
        try {
            Result r = evaluate(readMixins(), RECORDS, client);
            LOG.info("{} ({})", r.summary(), phase);
            for (String w : r.warnings) if (WARNED.add(w)) LOG.warn("Legacy Connector {}", w);
            return r;
        } catch (Throwable t) {
            LOG.warn("Legacy Connector mixin self check failed: {}", t.toString());
            return null;
        }
    }

    /** Runs {@link #report} once per game launch. */
    public static void reportOnce(boolean client, String phase) {
        if (CHECKED_ONCE.compareAndSet(false, true)) report(client, phase);
    }

    private static List<MixinDesc> readMixins() throws Exception {
        List<MixinDesc> out = new ArrayList<>();
        for (String config : CONFIGS) {
            String json;
            try (InputStream in = MixinSelfCheck.class.getClassLoader().getResourceAsStream(config)) {
                if (in == null) continue;
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            out.addAll(readConfig(json, name -> {
                try (InputStream in = MixinSelfCheck.class.getClassLoader().getResourceAsStream(name + ".class")) {
                    return in == null ? null : in.readAllBytes();
                } catch (java.io.IOException e) {
                    return null;
                }
            }));
        }
        return out;
    }

    interface ClassBytes {
        byte[] get(String internalName);
    }

    static List<MixinDesc> readConfig(String json, ClassBytes loader) {
        List<MixinDesc> out = new ArrayList<>();
        Matcher pkg = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!pkg.find()) return out;
        String prefix = pkg.group(1).replace('.', '/') + "/";
        Matcher arrays = Pattern.compile("\"(mixins|client|server)\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        while (arrays.find()) {
            String kind = arrays.group(1);
            Matcher names = Pattern.compile("\"([^\"]+)\"").matcher(arrays.group(2));
            while (names.find()) {
                byte[] bytes = loader.get(prefix + names.group(1).replace('.', '/'));
                if (bytes == null) continue;
                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                out.add(describe(node, kind.equals("client"), kind.equals("server")));
            }
        }
        return out;
    }

    /** For tests: the recorded state. */
    static Map<String, TargetRecord> records() {
        return RECORDS;
    }

    static Set<String> hookNames(List<MixinDesc> mixins) {
        Set<String> s = new HashSet<>();
        for (MixinDesc m : mixins) if (m.hook) s.add(simple(m.name));
        return s;
    }
}
