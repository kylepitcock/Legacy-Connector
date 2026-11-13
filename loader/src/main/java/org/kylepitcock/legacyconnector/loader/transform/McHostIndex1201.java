package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.transform.mixin1201.HostIndex;

/**
 * The mixin repair's view of the 1.21.1 classes: the member index of the gap report ({@link McMembers1201}) walked up the class tree of
 * {@link Mc1201Hierarchy}. No second index exists; a chain that leaves the index is reported as incomplete and then nothing is claimed.
 */
final class McHostIndex1201 implements HostIndex {
    private static final Set<String> OBJECT_METHODS = Set.of("equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait", "clone", "finalize");

    private final Mc1201Hierarchy h;

    McHostIndex1201(Mc1201Hierarchy h) {
        this.h = h;
    }

    @Override
    public boolean hasClass(String owner) {
        return McMembers1201.hasClass(owner);
    }

    @Override
    public Lookup methods(String owner, String name) {
        McMembers1201.Members own = McMembers1201.get(owner);
        if (own == null) return null;
        List<String> declared = named(own, name);
        List<String> chain = new ArrayList<>(declared);
        boolean[] complete = {true};
        Set<String> seen = new HashSet<>();
        seen.add(owner);
        for (String s : h.supers(owner)) walk(s, name, chain, complete, seen);
        return new Lookup(declared, chain, complete[0]);
    }

    private void walk(String cls, String name, List<String> chain, boolean[] complete, Set<String> seen) {
        if (!seen.add(cls)) return;
        if (cls.equals("java/lang/Object")) {
            if (OBJECT_METHODS.contains(name)) complete[0] = false; // the index does not carry Object's signatures
            return;
        }
        McMembers1201.Members mm = McMembers1201.get(cls);
        if (mm == null) {
            complete[0] = false;
            return;
        }
        for (String d : named(mm, name)) if (!chain.contains(d)) chain.add(d);
        for (String s : h.supers(cls)) walk(s, name, chain, complete, seen);
    }

    private static List<String> named(McMembers1201.Members mm, String name) {
        List<String> out = new ArrayList<>();
        String prefix = name + "(";
        for (String m : mm.methods()) if (m.startsWith(prefix)) out.add(m.substring(name.length()));
        return out;
    }

    @Override
    public Boolean isStatic(String owner, String name, String desc) {
        return McMembers1201.isStatic(owner, name + desc);
    }

    @Override
    public Boolean hasField(String owner, String name) {
        return field(owner, name, new HashSet<>());
    }

    private Boolean field(String owner, String name, Set<String> seen) {
        if (owner.startsWith("java/") || !seen.add(owner)) return false;
        McMembers1201.Members mm = McMembers1201.get(owner);
        if (mm == null) return null;
        for (String f : mm.fields()) if (f.startsWith(name + ":")) return true;
        Boolean result = false;
        for (String s : h.supers(owner)) {
            Boolean r = field(s, name, seen);
            if (r == null) result = null;
            else if (r) return true;
        }
        return result;
    }
}
