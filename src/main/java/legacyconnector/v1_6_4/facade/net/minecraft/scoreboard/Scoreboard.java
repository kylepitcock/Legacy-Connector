package legacyconnector.v1_6_4.facade.net.minecraft.scoreboard;

/** Facade of the 1.7.10 shim's {@code Scoreboard} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Scoreboard {
    private Scoreboard() {}

    /** 1.6.4 {@code addPlayerToTeam(player, team)}: 1.7.10 names the team by its registered name and answers whether the player was added. */
    public static void addPlayerToTeam(legacyconnector.v1_7_10.net.minecraft.scoreboard.Scoreboard self, String player,
            legacyconnector.v1_7_10.net.minecraft.scoreboard.ScorePlayerTeam team) {
        if (team != null) self.func_151392_a(player, team.getRegisteredName());
    }
}
