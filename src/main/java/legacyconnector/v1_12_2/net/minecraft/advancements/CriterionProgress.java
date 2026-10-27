package legacyconnector.v1_12_2.net.minecraft.advancements;

/** Whether one criterion is obtained, and when: a view over the modern criterion progress. */
public class CriterionProgress {
    private final net.minecraft.advancements.CriterionProgress progress;

    public CriterionProgress(net.minecraft.advancements.CriterionProgress progress) {
        this.progress = progress;
    }

    public boolean isObtained() {
        return progress.isDone();
    }

    public void obtain() {
        progress.grant();
    }

    public void reset() {
        progress.revoke();
    }

    public java.util.Date getObtained() {
        var at = progress.getObtained();
        return at == null ? null : java.util.Date.from(at);
    }
}
