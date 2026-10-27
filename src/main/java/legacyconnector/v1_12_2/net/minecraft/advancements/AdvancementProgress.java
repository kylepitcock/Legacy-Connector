package legacyconnector.v1_12_2.net.minecraft.advancements;

/** A player's progress on one advancement: a view over the modern progress. */
public class AdvancementProgress implements Comparable<AdvancementProgress> {
    private final net.minecraft.advancements.AdvancementProgress progress;

    public AdvancementProgress() {
        this(new net.minecraft.advancements.AdvancementProgress());
    }

    public AdvancementProgress(net.minecraft.advancements.AdvancementProgress progress) {
        this.progress = progress;
    }

    public net.minecraft.advancements.AdvancementProgress mcc$modern() {
        return progress;
    }

    public boolean isDone() {
        return progress.isDone();
    }

    public boolean hasProgress() {
        return progress.hasProgress();
    }

    public boolean grantCriterion(String criterionIn) {
        return progress.grantProgress(criterionIn);
    }

    public boolean revokeCriterion(String criterionIn) {
        return progress.revokeProgress(criterionIn);
    }

    public float getPercent() {
        return progress.getPercent();
    }

    public String getProgressText() {
        var text = progress.getProgressText();
        return text == null ? null : text.getString();
    }

    /** 1.12.2's spelling. */
    public Iterable<String> getRemaningCriteria() {
        return progress.getRemainingCriteria();
    }

    public Iterable<String> getCompletedCriteria() {
        return progress.getCompletedCriteria();
    }

    public CriterionProgress getCriterionProgress(String criterionIn) {
        var c = progress.getCriterion(criterionIn);
        return c == null ? null : new CriterionProgress(c);
    }

    @Override
    public int compareTo(AdvancementProgress other) {
        return progress.compareTo(other.progress);
    }

    @Override
    public String toString() {
        return progress.toString();
    }
}
