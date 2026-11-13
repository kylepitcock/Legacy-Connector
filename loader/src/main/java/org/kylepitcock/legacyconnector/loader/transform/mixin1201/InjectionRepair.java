package org.kylepitcock.legacyconnector.loader.transform.mixin1201;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Proves every injector of a mixin class against the 1.21.1 classes and repairs or removes the ones that cannot apply.
 *
 * <p>For each {@code method} selector (and each {@code @At(target)}) of an {@code @Inject}, {@code @Redirect}, {@code @ModifyArg(s)}, {@code @ModifyVariable},
 * {@code @ModifyConstant} or MixinExtras injector, the target is looked up in the {@link HostIndex}:
 * <ul>
 *   <li><b>exact</b>: the method exists as written, or (name only) the handler's captured parameters are a prefix of one overload: untouched;</li>
 *   <li><b>signature changed</b>: the name exists but not with that signature. When the old parameters reappear in order among the new ones, in one way only
 *       and with the return type unchanged (parameters were added), the selector gets the new descriptor and the {@code @Inject} handler gets the added
 *       parameters (ignored), see {@link HandlerResigner}. Anything else (removed, reordered or retyped parameters) cannot be repaired;</li>
 *   <li><b>gone</b>: the member (or its class) does not exist in 1.21.1, proven by a complete chain of indexed classes.</li>
 * </ul>
 * A gone or unrepairable selector is removed from its annotation, an annotation without selectors is removed from its handler and a handler without
 * injectors is removed from the class, each with one line in the report; the rest of the mixin and of the mod stays.
 *
 * <p>Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux
 * ({@code AutomaticMixinTranslator}: unique same-name target, ordered-subsequence parameter addition, handler layout checks, coupled selector and handler repair).
 */
public final class InjectionRepair {
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String MODIFY_VARIABLE = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
    private static final String AT = "Lorg/spongepowered/asm/mixin/injection/At;";
    /** Injectors whose handler does not mirror the parameters of the call an {@code @At(target)} names: the target descriptor can change alone. */
    private static final Set<String> AT_SELECTOR_ONLY = Set.of(INJECT, "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;", MODIFY_VARIABLE);
    private static final Set<String> INJECTORS = Set.of(INJECT, REDIRECT, "Lorg/spongepowered/asm/mixin/injection/ModifyArg;", "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            MODIFY_VARIABLE, "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;", "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;", "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;",
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;", "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;", "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;");
    private static final Pattern SELECTOR = Pattern.compile("^(L[^;]+;)?([A-Za-z0-9_$<>]+)(\\(.*)?$");
    private static final Pattern AT_TARGET = Pattern.compile("^L([^;]+);([A-Za-z0-9_$<>]+)(\\(.*|:.*)?$");

    /** What the proof found, summed over the mod. */
    public static final class Counts {
        public int exact;
        public int repaired;
        public int dropped;
        public int unproven;
        public int atExact;
        public int atRepaired;
        public int atDropped;
        /** Handlers that got parameters inserted (their class needs its frames recomputed). */
        public int handlersChanged;
        /** One line per repaired or dropped injection, for the compatibility report. */
        public final List<String> notes = new ArrayList<>();

        public String summary() {
            return exact + " injection selector(s) exact in 1.21.1, " + repaired + " re-signed for added parameters, " + dropped + " dropped (target gone or not repairable), "
                    + unproven + " not provable (kept); @At targets: " + atExact + " exact, " + atRepaired + " repaired, " + atDropped + " dropped";
        }
    }

    private enum Kind { EXACT, REPAIR, DEAD, KEEP }

    private record Outcome(Kind kind, String selector, List<ParamAlignment.Insert> inserts, String reason) {
        static final Outcome EXACT = new Outcome(Kind.EXACT, null, List.of(), null);
        static final Outcome KEEP = new Outcome(Kind.KEEP, null, List.of(), null);

        static Outcome dead(String why) {
            return new Outcome(Kind.DEAD, null, List.of(), why);
        }

        static Outcome repair(String selector, List<ParamAlignment.Insert> inserts) {
            return new Outcome(Kind.REPAIR, selector, inserts, null);
        }
    }

    private InjectionRepair() {}

    public static boolean isInjector(String desc) {
        return INJECTORS.contains(desc);
    }

    /**
     * The 1.20.1 descriptors the refmap gives for the {@code method} selectors of this class, by annotation (read before the annotation strings are rewritten).
     *
     * @param refmap annotation string to old descriptor (from {@link Refmaps}); may be null
     * @param mapDesc moves class names of a descriptor to their 1.21.1 names
     */
    public static Map<AnnotationNode, List<String>> refmapHints(ClassNode node, Map<String, String> refmap, java.util.function.UnaryOperator<String> mapDesc) {
        Map<AnnotationNode, List<String>> out = new IdentityHashMap<>();
        if (refmap == null || refmap.isEmpty()) return out;
        for (MethodNode m : node.methods) {
            for (AnnotationNode a : injectorAnnotations(m)) {
                List<Object> methods = stringList(a, "method");
                if (methods == null) continue;
                List<String> hints = new ArrayList<>();
                for (Object o : methods) {
                    String h = o instanceof String s ? refmap.get(s) : null;
                    hints.add(h == null ? null : mapDesc.apply(h));
                }
                out.put(a, hints);
            }
        }
        return out;
    }

    /**
     * Proves and repairs the injectors of {@code node}. {@code targets} are the (already renamed) mixin target classes. Returns true when something changed.
     */
    public static boolean run(ClassNode node, List<String> targets, HostIndex idx, Map<AnnotationNode, List<String>> hints, Counts counts) {
        List<String> hosts = new ArrayList<>();
        for (String t : targets) if (idx.hasClass(t)) hosts.add(t);
        if (hosts.isEmpty()) return false;
        boolean changed = false;
        for (MethodNode handler : new ArrayList<>(node.methods)) {
            List<AnnotationNode> injectors = injectorAnnotations(handler);
            if (injectors.isEmpty()) continue;
            changed |= repairHandler(node, handler, injectors, hosts, idx, hints, counts);
        }
        return changed;
    }

    private static boolean repairHandler(ClassNode node, MethodNode handler, List<AnnotationNode> injectors, List<String> hosts, HostIndex idx,
            Map<AnnotationNode, List<String>> hints, Counts counts) {
        String id = node.name + "." + handler.name + simple(handler.desc);
        // pass 1: judge every selector and @At; the handler is re-signed once, so all repairs of it must agree on the inserted parameters
        record Item(AnnotationNode a, List<Object> methods, List<Outcome> outcomes) {}
        List<Item> items = new ArrayList<>();
        List<ParamAlignment.Insert> handlerInserts = null;
        boolean conflict = false;
        for (AnnotationNode a : injectors) {
            List<Object> methods = stringList(a, "method");
            if (methods == null) continue;
            List<String> hs = hints.getOrDefault(a, List.of());
            List<Outcome> outs = new ArrayList<>();
            for (int i = 0; i < methods.size(); i++) {
                Outcome o = methods.get(i) instanceof String s ? selectorOutcome(a, handler, s, i < hs.size() ? hs.get(i) : null, hosts, idx) : Outcome.KEEP;
                outs.add(o);
                if (o.kind == Kind.REPAIR && !o.inserts.isEmpty()) {
                    if (handlerInserts == null) handlerInserts = o.inserts;
                    else if (!handlerInserts.equals(o.inserts)) conflict = true;
                }
            }
            items.add(new Item(a, methods, outs));
        }
        if (handlerInserts != null) {
            // a handler that also serves a selector that stays as it is cannot take the new parameters
            for (Item it : items) for (Outcome o : it.outcomes) if (o.kind == Kind.EXACT || o.kind == Kind.KEEP || o.kind == Kind.REPAIR && o.inserts.isEmpty()) conflict = true;
        }
        boolean resign = handlerInserts != null && !conflict;
        if (resign && !HandlerResigner.canResign(handler, handlerInserts)) {
            resign = false;
            conflict = true;
        }
        boolean changed = false;
        List<AnnotationNode> dead = new ArrayList<>();
        for (Item it : items) {
            List<Object> kept = new ArrayList<>();
            for (int i = 0; i < it.methods.size(); i++) {
                Outcome o = it.outcomes.get(i);
                Object sel = it.methods.get(i);
                Outcome eff = o.kind == Kind.REPAIR && !o.inserts.isEmpty() && conflict
                        ? Outcome.dead("signature changed and the handler " + handler.name + " cannot be re-signed (position-bound parameter annotations or conflicting repairs)") : o;
                switch (eff.kind) {
                    case EXACT -> {
                        counts.exact++;
                        kept.add(sel);
                    }
                    case KEEP -> {
                        counts.unproven++;
                        kept.add(sel);
                    }
                    case REPAIR -> {
                        counts.repaired++;
                        counts.notes.add("Mixin " + id + ": injection into " + sel + " re-signed for 1.21.1: " + (eff.inserts.isEmpty() ? "" : "parameter(s) " + describe(eff.inserts) + " inserted into the handler, ")
                                + "target is now " + eff.selector);
                        kept.add(eff.selector);
                        changed = true;
                    }
                    case DEAD -> {
                        counts.dropped++;
                        counts.notes.add("Mixin " + id + ": injection into " + sel + " dropped: " + eff.reason);
                        changed = true;
                    }
                }
            }
            if (kept.isEmpty() && !it.methods.isEmpty()) {
                dead.add(it.a);
                continue;
            }
            if (kept.size() != it.methods.size() || changed) setList(it.a, "method", kept);
            // the injection points
            if (atProof(it.a, handler, id, idx, counts, hosts)) changed = true;
            if (isDeadByAt(it.a)) dead.add(it.a);
        }
        if (resign && changed && HandlerResigner.resign(handler, handlerInserts)) counts.handlersChanged++;
        for (AnnotationNode d : dead) {
            if (handler.invisibleAnnotations != null) handler.invisibleAnnotations.remove(d);
            if (handler.visibleAnnotations != null) handler.visibleAnnotations.remove(d);
            changed = true;
        }
        if (!dead.isEmpty() && !hasMixinAnnotation(handler) && !referenced(node, handler)) node.methods.remove(handler);
        return changed;
    }

    // ---- method selectors ---------------------------------------------------------------------------------------------------

    private static Outcome selectorOutcome(AnnotationNode a, MethodNode handler, String text, String hint, List<String> hosts, HostIndex idx) {
        Matcher m = SELECTOR.matcher(text);
        if (!m.matches()) return Outcome.KEEP;
        String ownerPart = m.group(1);
        String name = m.group(2);
        String desc = m.group(3);
        if (desc != null && desc.indexOf(')') < 0) return Outcome.KEEP;
        if (unmappedOrSynthetic(name)) return Outcome.KEEP;
        List<String> owners = ownerPart != null ? List.of(ownerPart.substring(1, ownerPart.length() - 1)) : hosts;
        List<Outcome> outs = new ArrayList<>();
        for (String owner : owners) {
            HostIndex.Lookup lk = idx.hasClass(owner) ? idx.methods(owner, name) : null;
            if (lk == null) {
                // an owner the index lacks is a class 1.21.1 no longer has only when it is a Minecraft class
                outs.add(ownerPart != null && owner.startsWith("net/minecraft/") ? Outcome.dead("class " + owner.replace('/', '.') + " does not exist in 1.21.1") : Outcome.KEEP);
            } else {
                outs.add(one(idx, a.desc, handler, owner, ownerPart, name, desc, hint, lk, a));
            }
        }
        return combine(outs);
    }

    private static final Pattern UNMAPPED = Pattern.compile("^(m|f)_\\d+_$|^(method|field)_\\d+$|^func_\\d+_\\w+$");

    /** Names the proof does not judge: synthetic members of vanilla, and SRG or intermediary names that no table moved (a name that is not Mojang's proves nothing). */
    private static boolean unmappedOrSynthetic(String name) {
        return name.startsWith("lambda$") || name.startsWith("access$") || UNMAPPED.matcher(name).matches();
    }

    private static Outcome combine(List<Outcome> outs) {
        List<Outcome> alive = new ArrayList<>();
        for (Outcome o : outs) if (o.kind != Kind.DEAD) alive.add(o);
        if (alive.isEmpty()) return outs.get(0);
        if (alive.size() == 1) return alive.get(0);
        Outcome first = alive.get(0);
        for (Outcome o : alive) if (o.kind != first.kind || o.kind == Kind.REPAIR && !(o.selector.equals(first.selector) && o.inserts.equals(first.inserts))) return Outcome.KEEP;
        return first;
    }


    private static Outcome one(HostIndex idx, String injector, MethodNode handler, String owner, String ownerPart, String name, String desc, String hint, HostIndex.Lookup lk, AnnotationNode a) {
        String prefix = ownerPart == null ? "" : ownerPart;
        String original = prefix + name + (desc == null ? "" : desc);
        if (lk.gone()) return Outcome.dead("method " + name + " does not exist in " + owner.replace('/', '.') + " (1.21.1)");
        if (lk.declared().isEmpty()) return Outcome.KEEP; // only inherited (Mixin does not inject there) or the chain is unknown: nothing provable
        // a static target needs a static handler; an instance target takes either
        List<String> decl = new ArrayList<>();
        for (String d : lk.declared()) if (!Boolean.TRUE.equals(idx.isStatic(owner, name, d)) || (handler.access & Opcodes.ACC_STATIC) != 0) decl.add(d);
        if (desc != null && lk.declared().contains(desc)) return Outcome.EXACT;
        boolean inject = INJECT.equals(injector);
        if (!inject) {
            if (desc == null) return Outcome.EXACT;
            List<String> c = additions(desc, decl);
            if (c.size() != 1) return Outcome.dead("signature of " + name + desc + " changed in 1.21.1 and no overload is the old one with parameters added");
            List<ParamAlignment.Insert> ins = ParamAlignment.pureInsertions(desc, c.get(0));
            if (MODIFY_VARIABLE.equals(injector) && (has(a, "index") || mirrorsType(handler, ins))) return Outcome.dead("signature of " + name + desc + " changed (added parameter) and the local variable selection would move");
            if (!HandlerResigner.localsSurvive(handler, ins)) return Outcome.dead("signature of " + name + desc + " changed (added parameter) and a @Local capture would select another variable");
            return Outcome.repair(prefix + name + c.get(0), List.of());
        }
        // @Inject: the handler is (captured target parameters..., CallbackInfo[Returnable], sugar...)
        Type[] ha = Type.getArgumentTypes(handler.desc);
        int cb = HandlerResigner.callbackIndex(ha);
        if (cb < 0) return Outcome.KEEP;
        if (hasLocalsCapture(a)) return Outcome.KEEP; // the handler also takes the target's locals: its parameter list is not the target's
        List<Type> captured = new ArrayList<>(Arrays.asList(ha).subList(0, cb));
        boolean voidCb = ha[cb].getInternalName().endsWith("CallbackInfo");
        String old = desc != null ? desc : hint;
        if (old == null) {
            if (captured.isEmpty()) return Outcome.EXACT;
            for (String d : decl) if (callbackFits(voidCb, d) && isPrefix(captured, Arrays.asList(Type.getArgumentTypes(d)))) return Outcome.EXACT;
            List<String> cands = new ArrayList<>();
            List<ParamAlignment.Insert> ins = null;
            for (String d : decl) {
                if (!callbackFits(voidCb, d)) continue;
                List<Type> na = Arrays.asList(Type.getArgumentTypes(d));
                if (na.size() <= captured.size()) continue;
                List<ParamAlignment.Insert> i = ParamAlignment.insertions(captured, na);
                if (i == null || i.isEmpty() || i.size() > ParamAlignment.MAX_INSERTED) continue;
                cands.add(d);
                ins = i;
            }
            if (cands.size() != 1) return Outcome.dead("no overload of " + name + " takes the handler's parameters " + paramText(captured) + " (the 1.21.1 signature differs and the change is not an added parameter)");
            return finish(original, decl.size() > 1 ? prefix + name + cands.get(0) : original, leading(ins, captured.size()));
        }
        // the old signature is known (descriptor in the annotation, or from the refmap)
        Type[] oldArgs = Type.getArgumentTypes(old);
        if (desc == null && lk.declared().contains(old)) return Outcome.EXACT;
        if (!isPrefix(captured, Arrays.asList(oldArgs))) return Outcome.KEEP;
        Type oldReturn = Type.getReturnType(old);
        if (captured.isEmpty()) {
            if (desc == null) return Outcome.EXACT;
            // a handler that captures nothing follows the one overload with the same return type (it never sees the parameters), e.g. a wider refactor
            List<String> only = new ArrayList<>();
            for (String d : decl) if (Type.getReturnType(d).equals(oldReturn) && callbackFits(voidCb, d)) only.add(d);
            if (only.size() != 1) return Outcome.dead("signature of " + name + desc + " changed in 1.21.1 and no single overload keeps its return type");
            return finish(original, prefix + name + only.get(0), List.of());
        }
        List<String> c = new ArrayList<>();
        for (String d : additions(old, decl)) if (callbackFits(voidCb, d)) c.add(d);
        if (c.size() != 1) return Outcome.dead("signature of " + name + old + " changed in 1.21.1 and is not the old one with parameters added");
        List<ParamAlignment.Insert> needed = leading(ParamAlignment.pureInsertions(old, c.get(0)), captured.size());
        return finish(original, desc != null || decl.size() > 1 ? prefix + name + c.get(0) : original, needed);
    }

    /** A repair that changes neither the selector nor the handler is not a repair: the handler already fits (it captures a prefix of the new parameters). */
    private static Outcome finish(String original, String selector, List<ParamAlignment.Insert> needed) {
        return needed.isEmpty() && selector.equals(original) ? Outcome.EXACT : Outcome.repair(selector, needed);
    }

    /** The insertions that lie before the last captured parameter; a parameter added after it needs no handler change (the handler captures a prefix). */
    private static List<ParamAlignment.Insert> leading(List<ParamAlignment.Insert> ins, int capturedCount) {
        List<ParamAlignment.Insert> out = new ArrayList<>();
        for (ParamAlignment.Insert i : ins) if (i.index() < capturedCount) out.add(i);
        return out;
    }

    private static List<String> additions(String oldDesc, List<String> candidates) {
        List<String> out = new ArrayList<>();
        for (String d : candidates) if (ParamAlignment.pureInsertions(oldDesc, d) != null) out.add(d);
        return out;
    }

    private static boolean mirrorsType(MethodNode handler, List<ParamAlignment.Insert> ins) {
        Type[] a = Type.getArgumentTypes(handler.desc);
        if (a.length == 0) return false;
        for (ParamAlignment.Insert i : ins) if (i.desc().equals(a[0].getDescriptor())) return true;
        return false;
    }

    private static boolean callbackFits(boolean voidCallback, String targetDesc) {
        return voidCallback == (Type.getReturnType(targetDesc).getSort() == Type.VOID);
    }

    private static boolean isPrefix(List<Type> captured, List<Type> args) {
        if (captured.size() > args.size()) return false;
        for (int i = 0; i < captured.size(); i++) if (!captured.get(i).equals(args.get(i))) return false;
        return true;
    }

    private static boolean hasLocalsCapture(AnnotationNode a) {
        if (a.values == null) return false;
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            if ("locals".equals(a.values.get(i)) && a.values.get(i + 1) instanceof String[] e && e.length == 2 && !"NO_CAPTURE".equals(e[1])) return true;
        }
        return false;
    }

    // ---- @At targets --------------------------------------------------------------------------------------------------------

    /** Repairs and prunes the {@code @At(target)} of an injector; returns true when something changed. */
    private static boolean atProof(AnnotationNode injector, MethodNode handler, String id, HostIndex idx, Counts counts, List<String> hosts) {
        if (injector.values == null) return false;
        boolean changed = false;
        for (int i = 0; i + 1 < injector.values.size(); i += 2) {
            if (!"at".equals(injector.values.get(i))) continue;
            Object v = injector.values.get(i + 1);
            List<Object> ats = v instanceof List<?> l ? castList(l) : null;
            if (ats == null) continue;
            for (Iterator<Object> it = ats.iterator(); it.hasNext(); ) {
                if (!(it.next() instanceof AnnotationNode at) || !AT.equals(at.desc)) continue;
                Outcome o = atOutcome(injector.desc, handler, at, idx);
                switch (o.kind) {
                    case EXACT -> counts.atExact++;
                    case REPAIR -> {
                        counts.atRepaired++;
                        counts.notes.add("Mixin " + id + ": @At target repaired for 1.21.1: " + o.selector);
                        setString(at, "target", o.selector);
                        changed = true;
                    }
                    case DEAD -> {
                        counts.atDropped++;
                        counts.notes.add("Mixin " + id + ": injection point dropped: " + o.reason);
                        it.remove();
                        changed = true;
                    }
                    default -> { }
                }
            }
            if (ats.isEmpty()) setList(injector, "at", ats);
        }
        return changed;
    }

    private static boolean isDeadByAt(AnnotationNode injector) {
        if (injector.values == null) return false;
        for (int i = 0; i + 1 < injector.values.size(); i += 2) {
            if ("at".equals(injector.values.get(i)) && injector.values.get(i + 1) instanceof List<?> l && l.isEmpty()) return true;
        }
        return false;
    }

    private static Outcome atOutcome(String injector, MethodNode handler, AnnotationNode at, HostIndex idx) {
        String kind = string(at, "value");
        String target = string(at, "target");
        if (kind == null || target == null) return Outcome.KEEP;
        boolean call = kind.equals("INVOKE") || kind.equals("INVOKE_ASSIGN");
        boolean field = kind.equals("FIELD");
        if (!call && !field) return Outcome.KEEP;
        Matcher m = AT_TARGET.matcher(target);
        if (!m.matches()) return Outcome.KEEP;
        String owner = m.group(1);
        String name = m.group(2);
        String rest = m.group(3);
        if (unmappedOrSynthetic(name)) return Outcome.KEEP;
        boolean mc = owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/blaze3d/") || owner.startsWith("net/neoforged/neoforge/");
        if (!mc) return Outcome.KEEP;
        if (!idx.hasClass(owner)) return Outcome.dead("class " + owner.replace('/', '.') + " does not exist in 1.21.1 (@At target " + name + ")");
        if (field) {
            Boolean has = idx.hasField(owner, name);
            return Boolean.FALSE.equals(has) ? Outcome.dead("field " + name + " does not exist in " + owner.replace('/', '.') + " (1.21.1)") : Outcome.EXACT;
        }
        if (rest == null || !rest.startsWith("(")) return Outcome.KEEP;
        HostIndex.Lookup lk = idx.methods(owner, name);
        if (lk == null) return Outcome.KEEP;
        if (lk.gone()) return Outcome.dead("method " + name + " does not exist in " + owner.replace('/', '.') + " (1.21.1)");
        if (lk.chain().contains(rest)) return Outcome.EXACT;
        if (!lk.chainComplete()) return Outcome.KEEP;
        List<String> c = additions(rest, lk.chain());
        if (c.size() != 1) return Outcome.dead("call " + name + rest + " has another signature in 1.21.1 and no overload is the old one with parameters added");
        String newTarget = "L" + owner + ";" + name + c.get(0);
        if (AT_SELECTOR_ONLY.contains(injector)) return Outcome.repair(newTarget, List.of());
        return Outcome.dead("call " + name + rest + " gained parameters in 1.21.1 and the handler of this injector mirrors the call");
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------

    /** The injector annotations of a handler (visible and invisible). */
    static List<AnnotationNode> injectorAnnotations(MethodNode m) {
        List<AnnotationNode> out = new ArrayList<>();
        for (List<AnnotationNode> l : Arrays.asList(m.invisibleAnnotations, m.visibleAnnotations)) {
            if (l != null) for (AnnotationNode a : l) if (INJECTORS.contains(a.desc)) out.add(a);
        }
        return out;
    }

    private static boolean hasMixinAnnotation(MethodNode m) {
        for (List<AnnotationNode> l : Arrays.asList(m.invisibleAnnotations, m.visibleAnnotations)) {
            if (l != null) for (AnnotationNode a : l) if (a.desc.startsWith("Lorg/spongepowered/asm/mixin/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/")) {
                if (!a.desc.equals("Lorg/spongepowered/asm/mixin/Unique;")) return true;
            }
        }
        return false;
    }

    private static boolean referenced(ClassNode node, MethodNode target) {
        for (MethodNode m : node.methods) {
            if (m == target) continue;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = in.getNext()) {
                if (in instanceof MethodInsnNode mi && mi.owner.equals(node.name) && mi.name.equals(target.name) && mi.desc.equals(target.desc)) return true;
                if (in instanceof InvokeDynamicInsnNode d) {
                    for (Object arg : d.bsmArgs) {
                        if (arg instanceof Handle h && h.getOwner().equals(node.name) && h.getName().equals(target.name) && h.getDesc().equals(target.desc)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static String describe(List<ParamAlignment.Insert> ins) {
        StringBuilder sb = new StringBuilder();
        for (ParamAlignment.Insert i : ins) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(Type.getType(i.desc()).getClassName()).append(" at ").append(i.index());
        }
        return sb.toString();
    }

    private static String paramText(List<Type> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) sb.append(i > 0 ? ", " : "").append(types.get(i).getClassName());
        return sb.append(')').toString();
    }

    private static String simple(String desc) {
        return desc;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(List<?> l) {
        return (List<Object>) l;
    }

    /** The mutable list value of an annotation key, or null (a single string is turned into a list). */
    private static List<Object> stringList(AnnotationNode a, String key) {
        if (a.values == null) return null;
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            if (!key.equals(a.values.get(i))) continue;
            Object v = a.values.get(i + 1);
            if (v instanceof List<?> l) return castList(l);
            if (v instanceof String s) {
                List<Object> l = new ArrayList<>(List.of(s));
                a.values.set(i + 1, l);
                return l;
            }
        }
        return null;
    }

    private static void setList(AnnotationNode a, String key, List<Object> value) {
        for (int i = 0; i + 1 < a.values.size(); i += 2) if (key.equals(a.values.get(i))) a.values.set(i + 1, value);
    }

    private static boolean has(AnnotationNode a, String key) {
        if (a.values == null) return false;
        for (int i = 0; i + 1 < a.values.size(); i += 2) if (key.equals(a.values.get(i))) return true;
        return false;
    }

    private static String string(AnnotationNode a, String key) {
        if (a.values == null) return null;
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            if (!key.equals(a.values.get(i))) continue;
            Object v = a.values.get(i + 1);
            if (v instanceof String s) return s;
            if (v instanceof String[] e && e.length == 2) return e[1];
        }
        return null;
    }

    private static void setString(AnnotationNode a, String key, String value) {
        for (int i = 0; i + 1 < a.values.size(); i += 2) if (key.equals(a.values.get(i))) a.values.set(i + 1, value);
    }
}
