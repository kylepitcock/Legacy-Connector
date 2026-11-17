/*
 * Copyright (c) 2026 kentvillamor08
 * Licensed under the MIT License.
 * See LICENSE in the project root.
 */
package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;

/**
 * Scoped, nest-safe context for the CompoundTag that Forge 1.20.1 passed to
 * EntityType spawn/create and Mob.finalizeSpawn.
 *
 * <p>Pending scopes associate an old tag with one EntityType invocation.
 * Every target EntityType.create call opens a Create scope, even when it has
 * no matching Pending scope. This masks an outer scope from nested creates.
 * The exact Mob.finalizeSpawn invocation opens an Active scope for that Mob.
 * A generated legacy override bridge reads the active tag using
 * {@link #currentTagFor(Object)}.</p>
 *
 * <p>All scopes must be closed in LIFO order on the thread that opened them.
 * Callers should use try-with-resources so exceptions and early returns cannot
 * leak a spawn tag into later entity creation.</p>
 */
public final class SpawnContext {
    private enum Kind {
        PENDING,
        CREATE,
        ACTIVE
    }

    private static final class Frame {
        private final Kind kind;
        private final Object owner;
        private final CompoundTag tag;

        private Frame(Kind kind, Object owner, CompoundTag tag) {
            this.kind = kind;
            this.owner = owner;
            this.tag = tag;
        }
    }

    private static final ThreadLocal<Deque<Frame>> FRAMES = new ThreadLocal<>();

    private SpawnContext() {}

    /**
     * Opens a pending legacy tag for one EntityType. A null tag is meaningful:
     * it represents the source ItemStack.getTag() / explicit overload result
     * when no root tag existed.
     */
    public static Scope pushPending(Object entityType, CompoundTag tag) {
        return push(new Frame(Kind.PENDING, Objects.requireNonNull(entityType, "entityType"), tag));
    }

    /**
     * Opens a Create scope for every target EntityType.create invocation.
     * Only a Pending frame at the top of the stack and owned by the same
     * EntityType is visible to this create. An unrelated or nested create
     * therefore cannot accidentally consume an ancestor's pending tag.
     */
    public static Scope enterCreate(Object entityType) {
        Objects.requireNonNull(entityType, "entityType");
        Deque<Frame> frames = FRAMES.get();
        CompoundTag tag = null;
        if (frames != null) {
            Frame top = frames.peek();
            if (top != null && top.kind == Kind.PENDING && top.owner == entityType) {
                tag = top.tag;
            }
        }
        return push(new Frame(Kind.CREATE, entityType, tag));
    }

    /**
     * Opens an Active scope using the tag from the Create scope immediately
     * surrounding the target Mob.finalizeSpawn invocation.
     */
    public static Scope enterActiveFromCreate(Object mob) {
        Objects.requireNonNull(mob, "mob");
        Deque<Frame> frames = FRAMES.get();
        Frame top = frames == null ? null : frames.peek();
        CompoundTag tag = top != null && top.kind == Kind.CREATE ? top.tag : null;
        return push(new Frame(Kind.ACTIVE, mob, tag));
    }

    /**
     * Opens an Active scope for a direct legacy virtual finalizeSpawn call
     * whose old fifth argument is available at the call site.
     */
    public static Scope pushActive(Object mob, CompoundTag tag) {
        return push(new Frame(Kind.ACTIVE, Objects.requireNonNull(mob, "mob"), tag));
    }

    /**
     * Returns the tag belonging to the innermost Active frame for this exact
     * object identity. An active null tag intentionally shadows an outer
     * active tag for the same Mob.
     */
    public static CompoundTag currentTagFor(Object mob) {
        if (mob == null) return null;
        Deque<Frame> frames = FRAMES.get();
        if (frames == null || frames.isEmpty()) return null;
        for (Iterator<Frame> it = frames.iterator(); it.hasNext();) {
            Frame frame = it.next();
            if (frame.kind == Kind.ACTIVE && frame.owner == mob) {
                return frame.tag;
            }
        }
        return null;
    }

    private static Scope push(Frame frame) {
        Deque<Frame> frames = FRAMES.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            FRAMES.set(frames);
        }
        frames.push(frame);
        return new Scope(frames, frame, Thread.currentThread());
    }

    /** A single frame in the spawn-context stack. */
    public static final class Scope implements AutoCloseable {
        private final Deque<Frame> frames;
        private final Frame frame;
        private final Thread ownerThread;
        private boolean closed;

        private Scope(Deque<Frame> frames, Frame frame, Thread ownerThread) {
            this.frames = frames;
            this.frame = frame;
            this.ownerThread = ownerThread;
        }

        @Override
        public void close() {
            if (closed) return;
            if (Thread.currentThread() != ownerThread) {
                throw new IllegalStateException("SpawnContext scope closed on a different thread");
            }
            if (frames.peek() != frame) {
                throw new IllegalStateException("SpawnContext scopes must be closed in LIFO order");
            }
            frames.pop();
            closed = true;
            if (frames.isEmpty()) {
                FRAMES.remove();
            }
        }
    }
}
