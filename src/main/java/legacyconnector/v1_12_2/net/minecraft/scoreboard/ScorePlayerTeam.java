package legacyconnector.v1_12_2.net.minecraft.scoreboard;

import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** A scoreboard team: a view over the modern player team. */
public class ScorePlayerTeam extends Team {
    private final net.minecraft.world.scores.PlayerTeam team;

    public ScorePlayerTeam(Scoreboard scoreboardIn, String name) {
        this(new net.minecraft.world.scores.PlayerTeam(scoreboardIn.mcc$modern(), name));
    }

    public ScorePlayerTeam(net.minecraft.world.scores.PlayerTeam team) {
        this.team = team;
    }

    public static ScorePlayerTeam mcc$of(net.minecraft.world.scores.PlayerTeam team) {
        return team == null ? null : new ScorePlayerTeam(team);
    }

    public net.minecraft.world.scores.PlayerTeam mcc$modern() {
        return team;
    }

    @Override
    public String getName() {
        return team.getName();
    }

    public String getDisplayName() {
        return team.getDisplayName().getString();
    }

    public void setDisplayName(String name) {
        team.setDisplayName(net.minecraft.network.chat.Component.literal(name));
    }

    @Override
    public Collection<String> getMembershipCollection() {
        return team.getPlayers();
    }

    public String getPrefix() {
        return team.getPlayerPrefix().getString();
    }

    public void setPrefix(String prefix) {
        team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(prefix));
    }

    public String getSuffix() {
        return team.getPlayerSuffix().getString();
    }

    public void setSuffix(String suffix) {
        team.setPlayerSuffix(net.minecraft.network.chat.Component.literal(suffix));
    }

    @Override
    public String formatString(String input) {
        return getPrefix() + input + getSuffix();
    }

    public static String formatPlayerName(Team teamIn, String string) {
        return teamIn == null ? string : teamIn.formatString(string);
    }

    @Override
    public boolean getAllowFriendlyFire() {
        return team.isAllowFriendlyFire();
    }

    public void setAllowFriendlyFire(boolean friendlyFire) {
        team.setAllowFriendlyFire(friendlyFire);
    }

    @Override
    public boolean getSeeFriendlyInvisiblesEnabled() {
        return team.canSeeFriendlyInvisibles();
    }

    public void setSeeFriendlyInvisiblesEnabled(boolean friendlyInvisibles) {
        team.setSeeFriendlyInvisibles(friendlyInvisibles);
    }

    @Override
    public EnumVisible getNameTagVisibility() {
        return EnumVisible.mcc$of(team.getNameTagVisibility());
    }

    @Override
    public EnumVisible getDeathMessageVisibility() {
        return EnumVisible.mcc$of(team.getDeathMessageVisibility());
    }

    public void setNameTagVisibility(EnumVisible visibility) {
        if (visibility != null) team.setNameTagVisibility(visibility.mcc$modern());
    }

    public void setDeathMessageVisibility(EnumVisible visibility) {
        if (visibility != null) team.setDeathMessageVisibility(visibility.mcc$modern());
    }

    @Override
    public CollisionRule getCollisionRule() {
        return CollisionRule.mcc$of(team.getCollisionRule());
    }

    public void setCollisionRule(CollisionRule rule) {
        if (rule != null) team.setCollisionRule(rule.mcc$modern());
    }

    @Override
    public TextFormatting getColor() {
        return mcc$colour(team.getColor());
    }

    public void setColor(TextFormatting color) {
        var f = color == null ? null : net.minecraft.ChatFormatting.getByName(color.name());
        team.setColor(f == null ? net.minecraft.ChatFormatting.RESET : f);
    }

    public int getFriendlyFlags() {
        return team.packOptions();
    }

    public void setFriendlyFlags(int flags) {
        team.unpackOptions(flags);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ScorePlayerTeam t && t.team == team;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(team);
    }
}
