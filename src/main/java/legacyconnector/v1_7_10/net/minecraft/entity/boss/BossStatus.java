package legacyconnector.v1_7_10.net.minecraft.entity.boss;

/** The boss the HUD bar shows; legacy code sets it from the boss entity tick. */
public final class BossStatus {
    public static float healthScale;
    public static int statusBarTime;
    public static String bossName;
    public static boolean hasColorModifier;

    private BossStatus() {}

    public static void setBossStatus(IBossDisplayData boss, boolean colorModifier) {
        float max = boss.getMaxHealth();
        healthScale = max <= 0 ? 0 : boss.getHealth() / max;
        statusBarTime = 100;
        var name = boss.func_145748_c_();
        bossName = name == null ? "" : name.getFormattedText();
        hasColorModifier = colorModifier;
    }
}
