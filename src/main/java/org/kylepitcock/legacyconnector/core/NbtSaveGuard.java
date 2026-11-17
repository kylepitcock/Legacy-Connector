package org.kylepitcock.legacyconnector.core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.problems.LegacyProblems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps a legacy tile entity or entity that cannot write its NBT (a mod's {@code writeToNBT} throwing, or producing a tag the chunk writer
 * rejects: a null string, a string over 64 KiB) from failing the chunk save. A failed write falls back to the LAST GOOD data of that object (cached
 * on every successful save and on load), so neither the chunk nor the object's earlier data is lost. Every failure goes to
 * {@link LegacyProblems} ({@code /legacyconnector problems}); the log gets one line per class (further ones are counted only).
 * Shared by every hosted version (1.7.10 and the forward layers, 1.12.2, Beta).
 */
public final class NbtSaveGuard {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/NbtGuard");
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private NbtSaveGuard() {}

    /**
     * Runs a tile entity's writer. @return the fresh tag when the write worked and the tag serializes, else null (the failure is recorded);
     * the caller then saves its last good tag (and keeps the fresh one as the new last good when the result is non-null).
     */
    public static CompoundTag attempt(Supplier<CompoundTag> writer, Object owner, ResourceLocation type, String where) {
        try {
            CompoundTag tag = writer.get();
            if (tag != null) validate(tag);
            return tag;
        } catch (RuntimeException | LinkageError | IOException | StackOverflowError e) {
            failed(owner, type, where, e);
            return null;
        }
    }

    /**
     * An entity's writer adds keys to {@code tag} in place. On success returns the keys it added (the new last good delta); on a failure
     * removes whatever the failed write left, merges {@code lastGood}, records the problem and returns {@code lastGood} unchanged.
     */
    public static CompoundTag guardInPlace(CompoundTag tag, CompoundTag lastGood, Runnable writer, Object owner, ResourceLocation type, String where) {
        Set<String> before = new HashSet<>(tag.getAllKeys());
        try {
            writer.run();
            CompoundTag delta = new CompoundTag();
            for (String key : tag.getAllKeys()) {
                if (!before.contains(key)) delta.put(key, tag.get(key).copy());
            }
            validate(delta);
            return delta;
        } catch (RuntimeException | LinkageError | IOException | StackOverflowError e) {
            for (String key : new HashSet<>(tag.getAllKeys())) if (!before.contains(key)) tag.remove(key);
            if (lastGood != null) for (String key : lastGood.getAllKeys()) if (!tag.contains(key)) tag.put(key, lastGood.get(key).copy());
            failed(owner, type, where, e);
            return lastGood;
        }
    }

    /** Throws when the chunk writer would: serializes the tag to nowhere. */
    public static void validate(CompoundTag tag) throws IOException {
        try (DataOutputStream out = new DataOutputStream(OutputStream.nullOutputStream())) {
            tag.write(out);
        }
    }

    /** Records one failure (problems list always, log once per class). */
    public static void failed(Object owner, ResourceLocation type, String where, Throwable e) {
        String cls = owner == null ? "?" : owner.getClass().getName();
        String ns = type == null ? LegacyProblems.OTHER : type.getNamespace();
        String subject = (type == null ? cls : type.toString()) + (where == null ? "" : " " + where);
        LegacyProblems.record(LegacyProblems.Kind.NBT_FAILURE, ns, subject, e + " (" + cls + ")");
        if (LOGGED.add(cls)) {
            LOG.warn("{} at {} could not write its NBT, so the last good data is saved instead (further failures of this class are only listed by /legacyconnector problems): {}", cls, where, e.toString());
        }
    }
}
