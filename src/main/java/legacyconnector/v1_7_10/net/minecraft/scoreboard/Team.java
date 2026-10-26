package legacyconnector.v1_7_10.net.minecraft.scoreboard;

public abstract class Team {
    public boolean isSameTeam(Team other) {
        return other != null && this.equals(other);
    }

    public abstract String getRegisteredName();

    public abstract String formatString(String name);

    public abstract boolean func_98297_h();

    public abstract boolean getAllowFriendlyFire();
}
