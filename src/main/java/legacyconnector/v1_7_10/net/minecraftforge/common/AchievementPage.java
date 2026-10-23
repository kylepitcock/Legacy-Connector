package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/**
 * Forge 1.7.10 named group of achievements. Pages are recorded and served to legacy code; 1.21 has no achievement
 * screen for them to appear in.
 */
public class AchievementPage {
    private static final LinkedList<AchievementPage> achievementPages = new LinkedList<>();

    private final String name;
    private final LinkedList<Achievement> achievements;

    public AchievementPage(String name, Achievement... achievements) {
        this.name = name;
        this.achievements = new LinkedList<>(Arrays.asList(achievements));
    }

    public String getName() {
        return name;
    }

    public List<Achievement> getAchievements() {
        return achievements;
    }

    public static synchronized void registerAchievementPage(AchievementPage page) {
        if (getAchievementPage(page.getName()) != null) {
            throw new RuntimeException("Duplicate achievement page name \"" + page.getName() + "\"!");
        }
        achievementPages.add(page);
    }

    public static synchronized AchievementPage getAchievementPage(int index) {
        return achievementPages.get(index);
    }

    public static synchronized AchievementPage getAchievementPage(String name) {
        for (AchievementPage page : achievementPages) if (page.getName().equals(name)) return page;
        return null;
    }

    public static synchronized Set<AchievementPage> getAchievementPages() {
        return new LinkedHashSet<>(achievementPages);
    }

    public static synchronized boolean isAchievementInPages(Achievement achievement) {
        for (AchievementPage page : achievementPages) if (page.getAchievements().contains(achievement)) return true;
        return false;
    }

    public static synchronized String getTitle(int index) {
        return index == -1 ? "Minecraft" : getAchievementPage(index).getName();
    }
}
