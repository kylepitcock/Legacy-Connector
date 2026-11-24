package org.kylepitcock.legacyconnector.loader.transform.mixin1201;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Vanilla methods whose parameters 1.21.1 reordered, removed or retyped, so that an {@code @Inject} into them cannot be repaired by adding parameters to
 * the handler (see {@link InjectionRepair}). {@code mixin-targets.tsv} names the old signature, the new one and, per old parameter, where its value comes
 * from; the repair then gives the mod's handler a thin 1.21.1-shaped wrapper that builds the old arguments and calls the original handler, and moves the
 * injector annotation to the wrapper. The same table renames {@code @At(target)} calls ({@code A} rows).
 *
 * <pre>
 * M  owner  name  oldDescriptor  newDescriptor  argmap
 * A  owner  name  oldDescriptor  newName  newDescriptor
 * </pre>
 * argmap: one token per old parameter, comma separated: an index into the new parameters, {@code Class.method@N} (static helper of the migrate package taking
 * new parameter N and answering the old parameter) or {@code Class.method@} (static helper with no argument). The callback parameter of the handler follows
 * unchanged.
 */
public final class TargetRemap {
    /** A method target: its new descriptor and the old parameters' sources. */
    public record Entry(String newDesc, List<String> argMap, String oldReturn) {
        /** True when the old target returned a value and the new one returns void: the handler's {@code CallbackInfoReturnable} is rebuilt from the {@code CallbackInfo}. */
        public boolean returnConverted() {
            return !oldReturn.isEmpty();
        }
    }

    private static final Map<String, Entry> METHODS = new HashMap<>();
    private static final Map<String, String[]> AT = new HashMap<>();
    private static boolean loaded;
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_RETURNABLE = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private static final String HELPER_PACKAGE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";

    private TargetRemap() {}

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = org.kylepitcock.legacyconnector.loader.transform.ForwardStage.D_1201.open("mixin-targets.tsv")) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length < 6) continue;
                if (f[0].equals("M")) METHODS.put(f[1] + "." + f[2] + f[3], new Entry(f[4], List.of(f[5].split(",")), f.length > 6 ? f[6].trim() : ""));
                else if (f[0].equals("A")) AT.put(f[1] + "." + f[2] + f[3], new String[] {f[4], f[5]});
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The remap of a method target, or null. */
    public static synchronized Entry method(String owner, String name, String oldDesc) {
        load();
        return METHODS.get(owner + "." + name + oldDesc);
    }

    /**
     * The old descriptor of the one table row of {@code owner.name} whose old parameters start with {@code captured} (a name-only selector without a refmap
     * names no descriptor), or null when no row or more than one fits.
     */
    public static synchronized String uniqueOldDescriptor(String owner, String name, List<Type> captured) {
        load();
        String prefix = owner + "." + name + "(";
        String found = null;
        for (String key : METHODS.keySet()) {
            if (!key.startsWith(prefix)) continue;
            String old = key.substring(owner.length() + 1 + name.length());
            Type[] args = Type.getArgumentTypes(old);
            if (captured.size() > args.length) continue;
            boolean ok = true;
            for (int i = 0; i < captured.size() && ok; i++) ok = captured.get(i).equals(args[i]);
            if (!ok) continue;
            if (found != null) return null;
            found = old;
        }
        return found;
    }

    /** The new "name+descriptor" of a called method an {@code @At(target)} names, or null. */
    public static synchronized String at(String owner, String name, String oldDesc) {
        load();
        String[] n = AT.get(owner + "." + name + oldDesc);
        return n == null ? null : n[0] + n[1];
    }

    /**
     * Gives {@code annotation} a wrapper handler with the 1.21.1 parameters: the wrapper takes the new target parameters and the callback, rebuilds the
     * old arguments of the original {@code handler} (the captured ones) and calls it. The annotation moves to the wrapper. Returns false (nothing changed)
     * when the handler's shape is not the one the table describes.
     */
    public static boolean wrap(ClassNode node, MethodNode handler, AnnotationNode annotation, Entry e) {
        Type[] ha = Type.getArgumentTypes(handler.desc);
        int cb = HandlerResigner.callbackIndex(ha);
        if (cb < 0 || cb != ha.length - 1 || cb > e.argMap().size()) return false;
        Type[] newArgs = Type.getArgumentTypes(e.newDesc());
        boolean isStatic = (handler.access & Opcodes.ACC_STATIC) != 0;
        boolean convert = e.returnConverted();
        List<Type> wrapperArgs = new ArrayList<>(List.of(newArgs));
        wrapperArgs.add(convert ? Type.getObjectType(CALLBACK_INFO) : ha[cb]);
        String desc = Type.getMethodDescriptor(Type.VOID_TYPE, wrapperArgs.toArray(new Type[0]));
        String name = handler.name + "$mcc1201";
        for (MethodNode m : node.methods) if (m.name.equals(name) && m.desc.equals(desc)) return false;
        InsnList il = new InsnList();
        int[] slots = new int[newArgs.length + 1];
        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < newArgs.length; i++) {
            slots[i] = slot;
            slot += newArgs[i].getSize();
        }
        slots[newArgs.length] = slot;
        // the new target returns void (CallbackInfo), the old one returned a value (CallbackInfoReturnable): the handler gets a returnable made from the info
        int cirSlot = slot + 1;
        if (convert) {
            il.add(new VarInsnNode(Opcodes.ALOAD, slots[newArgs.length]));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER_PACKAGE + "MixinTargetCompat", "returnableOf", "(L" + CALLBACK_INFO + ";)L" + CALLBACK_INFO_RETURNABLE + ";", false));
            il.add(new VarInsnNode(Opcodes.ASTORE, cirSlot));
        }
        if (!isStatic) il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        for (int p = 0; p < cb; p++) {
            String tok = e.argMap().get(p).trim();
            int at = tok.indexOf('@');
            if (at < 0) {
                int i = Integer.parseInt(tok);
                if (!newArgs[i].equals(ha[p])) return false;
                il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
                continue;
            }
            String helper = tok.substring(0, at);
            String arg = tok.substring(at + 1);
            int dot = helper.lastIndexOf('.');
            String cls = helper.substring(0, dot);
            if (!cls.contains("/")) cls = HELPER_PACKAGE + cls;
            String hd;
            if (arg.isEmpty()) {
                hd = "()" + ha[p].getDescriptor();
            } else {
                int i = Integer.parseInt(arg);
                il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
                hd = "(" + newArgs[i].getDescriptor() + ")" + ha[p].getDescriptor();
            }
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, helper.substring(dot + 1), hd, false));
        }
        il.add(new VarInsnNode(Opcodes.ALOAD, convert ? cirSlot : slots[newArgs.length]));
        boolean itf = (node.access & Opcodes.ACC_INTERFACE) != 0;
        il.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL, node.name, handler.name, handler.desc, itf));
        if (convert) {
            il.add(new VarInsnNode(Opcodes.ALOAD, cirSlot));
            il.add(new VarInsnNode(Opcodes.ALOAD, slots[newArgs.length]));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER_PACKAGE + "MixinTargetCompat", "propagate", "(L" + CALLBACK_INFO_RETURNABLE + ";L" + CALLBACK_INFO + ";)V", false));
        }
        il.add(new InsnNode(Opcodes.RETURN));
        MethodNode w = new MethodNode(handler.access, name, desc, null, null);
        w.instructions = il;
        w.maxLocals = slot + 2;
        // helper results (a long takes two slots) and the receiver: generous
        w.maxStack = 4 + ha.length * 2;
        // the injector annotation moves from the handler to the wrapper
        if (handler.invisibleAnnotations != null && handler.invisibleAnnotations.remove(annotation)) {
            if (w.invisibleAnnotations == null) w.invisibleAnnotations = new ArrayList<>();
            w.invisibleAnnotations.add(annotation);
        } else if (handler.visibleAnnotations != null && handler.visibleAnnotations.remove(annotation)) {
            if (w.visibleAnnotations == null) w.visibleAnnotations = new ArrayList<>();
            w.visibleAnnotations.add(annotation);
        } else {
            return false;
        }
        node.methods.add(w);
        return true;
    }
}
