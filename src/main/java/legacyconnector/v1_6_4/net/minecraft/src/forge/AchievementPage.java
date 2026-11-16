package legacyconnector.v1_6_4.net.minecraft.src.forge;

import java.util.LinkedList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;

/** A named group of achievements (the layer lists them in the one achievements screen). */
public class AchievementPage {
    private final String name;
    private final LinkedList<Achievement> achievements = new LinkedList<>();

    public AchievementPage(String name, Achievement... achievements) {
        this.name = name;
        for (Achievement a : achievements) this.achievements.add(a);
    }

    public String getName() {
        return name;
    }

    public List<Achievement> getAchievements() {
        return achievements;
    }
}
