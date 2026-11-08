package legacyconnector.v1_7_10.net.minecraft.client.gui.achievement;

import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * The "Achievement get!" popup. Modern toasts replace it: mods call {@code Minecraft.guiAchievement.displayAchievement(achievement)}
 * to announce one they awarded themselves (Ars Magica's compendium).
 */
public class GuiAchievement {
    /** Pops up the achievement (its name and description). */
    public void displayAchievement(Achievement achievement) {
        show(achievement, "Achievement Get!");
    }

    /** Pops up the achievement without the "get" title (the stat screen's hover form). */
    public void displayInformation(Achievement achievement) {
        show(achievement, "Achievement");
    }

    /** 1.6.4: queues the achievement information popup. */
    public void queueAchievementInformation(Achievement achievement) {
        displayInformation(achievement);
    }

    public void func_146256_a(Achievement achievement) {
        displayAchievement(achievement);
    }

    public void func_146255_b(Achievement achievement) {
        displayInformation(achievement);
    }

    private static void show(Achievement achievement, String title) {
        if (achievement == null) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.getToasts() == null) return;
        String name = achievement.getStatName() == null ? "" : achievement.getStatName().getUnformattedText();
        mc.execute(() -> SystemToast.add(mc.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal(title),
                Component.literal(name)));
    }
}
