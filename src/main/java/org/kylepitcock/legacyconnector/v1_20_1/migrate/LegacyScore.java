package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.scores.ScoreAccess;

/**
 * Stand-in for 1.20.1's {@code net.minecraft.world.scores.Score} (a class holding one player's value for one objective). 1.21.1 hands out a
 * {@link ScoreAccess} instead; this wraps it and keeps the old method names ({@code mc-classes.tsv} renames {@code Score} to this class).
 */
public final class LegacyScore {
    private final ScoreAccess access;
    private final String owner;

    public LegacyScore(ScoreAccess access, String owner) {
        this.access = access;
        this.owner = owner;
    }

    public int getScore() {
        return access.get();
    }

    public void setScore(int value) {
        access.set(value);
    }

    public void add(int amount) {
        access.add(amount);
    }

    public void increment() {
        access.increment();
    }

    public void reset() {
        access.reset();
    }

    public String getOwner() {
        return owner;
    }
}
