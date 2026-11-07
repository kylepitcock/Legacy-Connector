package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Hierarchy.Origin;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Tables.Bridge;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Tables.Call;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Tables.Field;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Minecraft 1.20.1 to 1.21.1 for one class of a mod (names are already Mojang's): the part of the migration that renaming cannot express.
 * <ol>
 *   <li><b>Calls</b> whose signature, owner semantics or argument types changed go to a static helper ({@code mc-calls.tsv}); constructors that
 *       became factories or take other arguments ({@code new ResourceLocation(String)}, {@code new MobEffectInstance(MobEffect, ...)}) are rewritten
 *       from the {@code NEW, DUP, args, INVOKESPECIAL} pattern to {@code args, INVOKESTATIC};</li>
 *   <li><b>Static fields</b> that changed type or name ({@code MobEffects.X} is a {@code Holder} now) are read in the new form and converted back;</li>
 *   <li><b>Bridge methods</b> for overridden methods whose signature changed ({@code BlockEntity.load(CompoundTag)} to
 *       {@code loadAdditional(CompoundTag, HolderLookup.Provider)}): the class gets the 1.21.1-shaped method, which calls the mod's method, like the
 *       bridge methods javac emits for covariant overrides ({@code mc-bridges.tsv});</li>
 *   <li>Calls of a bridged method that resolve to Minecraft's own implementation ({@code super.saveAdditional(tag)}, {@code blockEntity.load(tag)})
 *       get the extra {@code HolderLookup.Provider} argument from the context the helpers keep.</li>
 * </ol>
 * A call is only touched when no class of the mod declares the method on the way up from its receiver type: the mod's own methods keep their signatures.
 */
final class Mc1201Migration {
    private static final String CONTEXT = Mc1201Tables.HELPER_PACKAGE + "MigrateContext";
    private static final String PROVIDER = "net/minecraft/core/HolderLookup$Provider";
    private static final String HOLDER = "net/minecraft/core/Holder";

    /** What {@link #apply} did, per kind (for the report line). */
    static final class Counts {
        int calls;
        int constructors;
        int fields;
        int bridges;
        int providerCalls;

        int total() {
            return calls + constructors + fields + bridges + providerCalls;
        }

        void add(Counts o) {
            calls += o.calls;
            constructors += o.constructors;
            fields += o.fields;
            bridges += o.bridges;
            providerCalls += o.providerCalls;
        }

        @Override
        public String toString() {
            return calls + " call(s) redirected to helpers, " + constructors + " constructor(s) made factory calls, " + fields + " static field read(s) converted, "
                    + bridges + " bridge method(s) for changed overrides, " + providerCalls + " call(s) given the registry provider";
        }
    }

    private Mc1201Migration() {}

    static Counts apply(ClassNode node, Mc1201Hierarchy h) {
        return apply(node, h, Mc1201Tables.get());
    }

    static Counts apply(ClassNode node, Mc1201Hierarchy h, Mc1201Tables t) {
        Counts c = new Counts();
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            rewriteBody(m, h, t, c);
        }
        if ((node.access & Opcodes.ACC_INTERFACE) == 0) addBridges(node, h, t, c);
        return c;
    }

    // ------------------------------------------------------------------ calls and fields

    private static void rewriteBody(MethodNode m, Mc1201Hierarchy h, Mc1201Tables t, Counts c) {
        Deque<TypeInsnNode> news = new ArrayDeque<>();
        AbstractInsnNode next;
        for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = next) {
            next = in.getNext(); // InsnList.set/remove clear the replaced node's links
            if (in instanceof TypeInsnNode ti && ti.getOpcode() == Opcodes.NEW) {
                news.push(ti);
            } else if (in instanceof MethodInsnNode call) {
                if (call.name.equals("<init>")) {
                    TypeInsnNode made = call.getOpcode() == Opcodes.INVOKESPECIAL && !news.isEmpty() && news.peek().desc.equals(call.owner) ? news.pop() : null;
                    Call rule = t.calls.get(call.owner + ".<init>" + call.desc);
                    if (rule != null && rule.helper().startsWith("=")) {
                        inPlace(m, call, rule);
                        c.constructors++;
                    } else if (made != null && rule != null && made.getNext() != null && made.getNext().getOpcode() == Opcodes.DUP) {
                        m.instructions.remove(made.getNext());
                        m.instructions.remove(made);
                        String hd = rule.helperDesc() != null ? rule.helperDesc() : call.desc.substring(0, call.desc.length() - 1) + "L" + call.owner + ";";
                        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, rule.helper(), rule.helperName(), hd, false));
                        c.constructors++;
                    }
                } else if (call.owner.startsWith("[")) {
                    // array clone and friends
                } else if (redirect(m, call, h, t)) {
                    c.calls++;
                } else if (addProvider(m, call, h, t)) {
                    c.providerCalls++;
                }
            } else if (in instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC) {
                Field rule = t.fields.get(f.owner + "." + f.name + ":" + f.desc);
                if (rule != null) {
                    rewriteField(m, f, rule);
                    c.fields++;
                }
            }
        }
    }

    /** The first rule found at the receiver type or one of its Minecraft ancestors, if no mod class declares the method. */
    private static boolean redirect(MethodNode m, MethodInsnNode call, Mc1201Hierarchy h, Mc1201Tables t) {
        Call rule = null;
        boolean special = call.getOpcode() == Opcodes.INVOKESPECIAL;
        for (String owner : h.minecraftAncestors(call.owner)) {
            if (special) rule = t.calls.get(owner + "." + call.name + call.desc + "@special");
            if (rule == null) rule = t.calls.get(owner + "." + call.name + call.desc);
            if (rule != null) break;
        }
        if (rule == null) return false;
        if (call.getOpcode() != Opcodes.INVOKESTATIC && !special && h.declaredIn(call.owner, call.name, call.desc) == Origin.MOD) return false;
        if (special && h.declaredIn(call.owner, call.name, call.desc) == Origin.MOD) return false;
        String hd = rule.helperDesc();
        if (rule.helper().startsWith("=")) {
            inPlace(m, call, rule);
            return true;
        }
        if (hd == null) hd = call.getOpcode() == Opcodes.INVOKESTATIC ? call.desc : "(L" + rule.owner() + ";" + call.desc.substring(1);
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, rule.helper(), rule.helperName(), hd, false));
        return true;
    }

    /**
     * A rule that keeps the call but changes it: {@code =} renames and/or re-describes it (opcode and receiver stay), {@code =swap} also swaps the
     * last two arguments (both one slot wide: constructors that exchanged two parameters), {@code =pop} drops the last argument (one slot), {@code =ret} discards
     * the result of a method that used to return void.
     */
    private static void inPlace(MethodNode m, MethodInsnNode call, Call rule) {
        if (rule.helper().equals("=args")) {
            rewriteArgs(m, call, rule);
            return;
        }
        if (rule.helper().equals("=swap")) m.instructions.insertBefore(call, new InsnNode(Opcodes.SWAP));
        else if (rule.helper().equals("=pop")) m.instructions.insertBefore(call, new InsnNode(Opcodes.POP));
        else if (rule.helper().equals("=swappop")) {
            // drops the second to last argument (a constructor that lost its id parameter: CustomRecipe)
            m.instructions.insertBefore(call, new InsnNode(Opcodes.SWAP));
            m.instructions.insertBefore(call, new InsnNode(Opcodes.POP));
        }
        else if (rule.helper().equals("=ret")) m.instructions.insert(call, new InsnNode(Type.getReturnType(rule.helperDesc()).getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        if (!rule.helperName().equals("-")) call.name = rule.helperName();
        if (rule.helperDesc() != null) call.desc = rule.helperDesc();
    }

    /**
     * {@code =args}: the call keeps its name and receiver, its arguments are produced again from the old ones. The helper-name column lists one token per
     * new parameter: {@code N} (old argument N unchanged), {@code Class.method@a+b} (a static helper taking old arguments a and b, returning the new parameter),
     * and the helper-descriptor column is the new descriptor. The old arguments are spilled to fresh locals first, so any argument can be dropped, reordered or
     * converted (super constructors that lost an id, took a {@code Holder}...).
     */
    private static void rewriteArgs(MethodNode m, MethodInsnNode call, Call rule) {
        Type[] old = Type.getArgumentTypes(call.desc);
        Type[] now = Type.getArgumentTypes(rule.helperDesc());
        String[] tokens = rule.helperName().split(",");
        if (tokens.length != now.length) throw new IllegalStateException("=args row for " + call.owner + "." + call.name + call.desc + ": " + tokens.length + " tokens for " + now.length + " parameters");
        int[] slot = new int[old.length];
        int next = m.maxLocals;
        for (int i = 0; i < old.length; i++) {
            slot[i] = next;
            next += old[i].getSize();
        }
        InsnList il = new InsnList();
        for (int i = old.length - 1; i >= 0; i--) il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ISTORE), slot[i]));
        for (int j = 0; j < now.length; j++) {
            String tok = tokens[j].strip();
            int at = tok.indexOf('@');
            if (at < 0) {
                int i = Integer.parseInt(tok);
                il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ILOAD), slot[i]));
                continue;
            }
            String helper = tok.substring(0, at);
            StringBuilder desc = new StringBuilder("(");
            for (String a : tok.substring(at + 1).split("\\+")) {
                int i = Integer.parseInt(a);
                il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ILOAD), slot[i]));
                desc.append(old[i].getDescriptor());
            }
            desc.append(')').append(now[j].getDescriptor());
            int dot = helper.lastIndexOf('.');
            String cls = helper.substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, helper.substring(dot + 1), desc.toString(), false));
        }
        m.instructions.insertBefore(call, il);
        call.desc = rule.helperDesc();
        m.maxLocals = next;
        m.maxStack += 4;
    }

    private static void rewriteField(MethodNode m, FieldInsnNode f, Field rule) {
        String newName = rule.newName().equals("-") ? f.name : rule.newName();
        String newDesc = rule.newDesc().equals("-") ? f.desc : rule.newDesc();
        InsnList after = new InsnList();
        String post = rule.post();
        if (rule.newDesc().equals("none")) {
            // the field is gone: a helper without arguments produces the value
            int dot = post.lastIndexOf('.');
            String cls = post.substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            m.instructions.set(f, new MethodInsnNode(Opcodes.INVOKESTATIC, cls, post.substring(dot + 1), "()" + f.desc, false));
            return;
        }
        if (post.equals("value")) {
            after.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, HOLDER, "value", "()Ljava/lang/Object;", true));
            after.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getType(f.desc).getInternalName()));
        } else if (post.equals("location")) {
            after.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/resources/ResourceKey", "location", "()Lnet/minecraft/resources/ResourceLocation;", false));
        } else if (!post.equals("-")) {
            int dot = post.lastIndexOf('.');
            String cls = post.substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            after.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, post.substring(dot + 1), "(" + newDesc + ")" + f.desc, false));
        }
        f.name = newName;
        f.desc = newDesc;
        if (after.size() > 0) m.instructions.insert(f, after);
        m.maxStack = Math.max(m.maxStack, 2);
    }

    /** Context carried by an extra trailing parameter of the 1.21.1 method: the type to the {@link MigrateContext} getter that returns it. */
    private static final java.util.Map<String, String[]> EXTRAS = java.util.Map.of(
            PROVIDER, new String[] {"provider", "()L" + PROVIDER + ";"},
            "net/minecraft/network/syncher/SynchedEntityData$Builder", new String[] {"builder", "()Lnet/minecraft/network/syncher/SynchedEntityData$Builder;"});

    /** A call of a bridged method that Minecraft implements: appends the registry provider (or entity data builder) the 1.21.1 method needs. */
    private static boolean addProvider(MethodNode m, MethodInsnNode call, Mc1201Hierarchy h, Mc1201Tables t) {
        for (Bridge b : t.bridges) {
            if (!b.oldName().equals(call.name) || !b.oldDesc().equals(call.desc)) continue;
            if (call.getOpcode() == Opcodes.INVOKESTATIC) continue;
            Type[] oldArgs = Type.getArgumentTypes(b.oldDesc());
            Type[] newArgs = Type.getArgumentTypes(b.newDesc());
            if (newArgs.length != oldArgs.length + 1 || !b.args().isEmpty() || b.ret() != null) continue;
            String[] getter = EXTRAS.get(newArgs[newArgs.length - 1].getInternalName());
            if (getter == null) continue;
            if (!b.newDesc().equals(b.oldDesc().substring(0, b.oldDesc().indexOf(')')) + newArgs[newArgs.length - 1].getDescriptor() + b.oldDesc().substring(b.oldDesc().indexOf(')')))) continue;
            if (!h.isSubclassOf(call.owner, b.owner())) continue;
            if (h.declaredIn(call.owner, call.name, call.desc) == Origin.MOD) continue;
            m.instructions.insertBefore(call, new MethodInsnNode(Opcodes.INVOKESTATIC, CONTEXT, getter[0], getter[1], false));
            call.name = b.newName();
            call.desc = b.newDesc();
            m.maxStack += 1;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ bridge methods

    private static void addBridges(ClassNode node, Mc1201Hierarchy h, Mc1201Tables t, Counts c) {
        for (Bridge b : t.bridges) {
            MethodNode old = declared(node, b.oldName(), b.oldDesc());
            if (old == null || (old.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0) continue;
            if (declared(node, b.newName(), b.newDesc()) != null) continue;
            if (!h.isSubclassOf(node.name, b.owner()) && !h.reachesUnknown(node.name)) continue;
            MethodNode bridge = bridge(node, old, b);
            if (bridge == null) continue;
            node.methods.add(bridge);
            c.bridges++;
        }
    }

    private static MethodNode declared(ClassNode node, String name, String desc) {
        for (MethodNode m : node.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        return null;
    }

    private static MethodNode bridge(ClassNode node, MethodNode old, Bridge b) {
        Type[] newArgs = Type.getArgumentTypes(b.newDesc());
        int[] slots = new int[newArgs.length];
        int slot = 1;
        for (int i = 0; i < newArgs.length; i++) {
            slots[i] = slot;
            slot += newArgs[i].getSize();
        }
        Type[] oldArgs = Type.getArgumentTypes(b.oldDesc());
        List<String> tokens = b.args().isEmpty() ? identity(oldArgs.length) : b.args();
        if (tokens.size() != oldArgs.length) return null;
        InsnList il = new InsnList();
        if (b.remember() >= 0) {
            il.add(new VarInsnNode(Opcodes.ALOAD, slots[b.remember()]));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONTEXT, "remember", "(Ljava/lang/Object;)V", false));
        }
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int stack = 1;
        for (int p = 0; p < oldArgs.length; p++) {
            String tok = tokens.get(p);
            if (tok.equals("this")) {
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                stack += 1;
                continue;
            }
            if (tok.equals("null")) {
                il.add(new InsnNode(Opcodes.ACONST_NULL));
                stack += 1;
                continue;
            }
            String helper = null;
            int at = tok.indexOf('@');
            if (at > 0) {
                helper = tok.substring(0, at);
                tok = tok.substring(at + 1);
            }
            int i = Integer.parseInt(tok);
            il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
            if (helper != null) {
                int dot = helper.lastIndexOf('.');
                String cls = helper.substring(0, dot);
                if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, helper.substring(dot + 1), "(" + newArgs[i].getDescriptor() + ")" + oldArgs[p].getDescriptor(), false));
            } else if (!newArgs[i].equals(oldArgs[p]) && newArgs[i].getSort() >= Type.ARRAY && oldArgs[p].getSort() >= Type.ARRAY) {
                il.add(new TypeInsnNode(Opcodes.CHECKCAST, oldArgs[p].getInternalName()));
            } else if (!newArgs[i].equals(oldArgs[p])) {
                return null;
            }
            stack += Math.max(newArgs[i].getSize(), oldArgs[p].getSize());
        }
        boolean priv = (old.access & Opcodes.ACC_PRIVATE) != 0;
        il.add(new MethodInsnNode(priv ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, node.name, b.oldName(), b.oldDesc(), (node.access & Opcodes.ACC_INTERFACE) != 0));
        Type oldRet = Type.getReturnType(b.oldDesc());
        Type newRet = Type.getReturnType(b.newDesc());
        if (b.ret() != null && b.ret().startsWith("arg:")) {
            // the 1.21.1 method returns one of its parameters that the old method had no result for (playerWillDestroy returns the state)
            if (oldRet.getSort() != Type.VOID) il.add(new InsnNode(oldRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            int i = Integer.parseInt(b.ret().substring(4));
            il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
        } else if (b.ret() != null) {
            int dot = b.ret().lastIndexOf('.');
            String cls = b.ret().substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, b.ret().substring(dot + 1), "(" + oldRet.getDescriptor() + ")" + newRet.getDescriptor(), false));
        } else if (newRet.getSort() == Type.VOID && oldRet.getSort() != Type.VOID) {
            il.add(new InsnNode(oldRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        } else if (!oldRet.equals(newRet)) {
            if (oldRet.getSort() >= Type.ARRAY && newRet.getSort() >= Type.ARRAY) il.add(new TypeInsnNode(Opcodes.CHECKCAST, newRet.getInternalName()));
            else return null;
        }
        il.add(new InsnNode(newRet.getOpcode(Opcodes.IRETURN)));
        MethodNode out = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, b.newName(), b.newDesc(), null, null);
        out.instructions = il;
        out.maxLocals = slot;
        out.maxStack = Math.max(stack + 1, 3) + Math.max(newRet.getSize(), oldRet.getSize());
        return out;
    }

    private static List<String> identity(int n) {
        String[] t = new String[n];
        for (int i = 0; i < n; i++) t[i] = Integer.toString(i);
        return List.of(t);
    }
}
