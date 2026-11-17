package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;

/**
 * Forge 3.x's named group of achievements ({@code getName}, {@code getAchievements}, checked in the 3.3.7 source). It is the shim's own page class under the 3.x
 * name, so a page a mod registers shows in the achievements screen (a button cycles the pages; achievements in no page stay on the "Minecraft" page).
 */
public class AchievementPage extends legacyconnector.v1_7_10.net.minecraftforge.common.AchievementPage {
    public AchievementPage(String name, Achievement... achievements) {
        super(name, achievements);
    }
}
