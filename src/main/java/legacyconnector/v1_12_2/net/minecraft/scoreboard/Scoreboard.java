package legacyconnector.v1_12_2.net.minecraft.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Objectives, scores and teams: a view over the modern scoreboard (the server's, which the modern game saves and syncs). A
 * scoreboard a mod builds itself ({@code new Scoreboard()}) is a detached modern one.
 */
public class Scoreboard {
    private final net.minecraft.world.scores.Scoreboard scoreboard;

    public Scoreboard() {
        this(new net.minecraft.world.scores.Scoreboard());
    }

    public Scoreboard(net.minecraft.world.scores.Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public static Scoreboard mcc$of(net.minecraft.world.scores.Scoreboard scoreboard) {
        return scoreboard == null ? null : new Scoreboard(scoreboard);
    }

    public net.minecraft.world.scores.Scoreboard mcc$modern() {
        return scoreboard;
    }

    private static net.minecraft.world.scores.ScoreHolder holder(String name) {
        return net.minecraft.world.scores.ScoreHolder.forNameOnly(name);
    }

    public ScoreObjective getObjective(String name) {
        return ScoreObjective.mcc$of(scoreboard.getObjective(name));
    }

    public ScoreObjective addScoreObjective(String name, IScoreCriteria criteria) {
        if (name.length() > 16) throw new IllegalArgumentException("The objective name '" + name + "' is too long!");
        if (scoreboard.getObjective(name) != null) throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
        var modern = criteria == null ? net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY : criteria.mcc$modern();
        return ScoreObjective.mcc$of(scoreboard.addObjective(name, modern, net.minecraft.network.chat.Component.literal(name), modern.getDefaultRenderType(),
                false, null));
    }

    public Collection<ScoreObjective> getObjectivesFromCriteria(IScoreCriteria criteria) {
        List<ScoreObjective> out = new ArrayList<>();
        if (criteria == null) return out;
        for (var o : scoreboard.getObjectives()) if (o.getCriteria().getName().equals(criteria.getName())) out.add(ScoreObjective.mcc$of(o));
        return out;
    }

    public boolean entityHasObjective(String name, ScoreObjective objective) {
        return objective != null && scoreboard.getPlayerScoreInfo(holder(name), objective.mcc$modern()) != null;
    }

    public Score getOrCreateScore(String username, ScoreObjective objective) {
        if (username.length() > 40) throw new IllegalArgumentException("The player name '" + username + "' is too long!");
        scoreboard.getOrCreatePlayerScore(holder(username), objective.mcc$modern(), true);
        return new Score(this, objective, username);
    }

    public Collection<Score> getSortedScores(ScoreObjective objective) {
        List<Score> out = new ArrayList<>();
        for (var entry : scoreboard.listPlayerScores(objective.mcc$modern())) out.add(new Score(this, objective, entry.owner()));
        out.sort((a, b) -> Integer.compare(a.getScorePoints(), b.getScorePoints()));
        return out;
    }

    public Collection<ScoreObjective> getScoreObjectives() {
        List<ScoreObjective> out = new ArrayList<>();
        for (var o : scoreboard.getObjectives()) out.add(ScoreObjective.mcc$of(o));
        return out;
    }

    public Collection<String> getObjectiveNames() {
        List<String> out = new ArrayList<>();
        for (var h : scoreboard.getTrackedPlayers()) out.add(h.getScoreboardName());
        return out;
    }

    public void removeObjectiveFromEntity(String name, ScoreObjective objective) {
        if (objective == null) scoreboard.resetAllPlayerScores(holder(name));
        else scoreboard.resetSinglePlayerScore(holder(name), objective.mcc$modern());
    }

    public Collection<Score> getScores() {
        List<Score> out = new ArrayList<>();
        for (var o : scoreboard.getObjectives()) {
            ScoreObjective objective = ScoreObjective.mcc$of(o);
            for (var entry : scoreboard.listPlayerScores(o)) out.add(new Score(this, objective, entry.owner()));
        }
        return out;
    }

    public Map<ScoreObjective, Score> getObjectivesForEntity(String name) {
        Map<ScoreObjective, Score> out = new LinkedHashMap<>();
        for (var e : scoreboard.listPlayerScores(holder(name)).keySet()) {
            ScoreObjective objective = ScoreObjective.mcc$of(e);
            out.put(objective, new Score(this, objective, name));
        }
        return out;
    }

    public void removeObjective(ScoreObjective objective) {
        if (objective != null) scoreboard.removeObjective(objective.mcc$modern());
    }

    /** Slot 0 is the player list, 1 the sidebar, 2 below names, 3-18 the team-coloured sidebars. */
    public void setObjectiveInDisplaySlot(int slot, ScoreObjective objective) {
        var s = slot >= 0 && slot < net.minecraft.world.scores.DisplaySlot.values().length ? net.minecraft.world.scores.DisplaySlot.BY_ID.apply(slot) : null;
        if (s != null) scoreboard.setDisplayObjective(s, objective == null ? null : objective.mcc$modern());
    }

    public ScoreObjective getObjectiveInDisplaySlot(int slot) {
        var s = slot >= 0 && slot < net.minecraft.world.scores.DisplaySlot.values().length ? net.minecraft.world.scores.DisplaySlot.BY_ID.apply(slot) : null;
        return s == null ? null : ScoreObjective.mcc$of(scoreboard.getDisplayObjective(s));
    }

    public ScorePlayerTeam getTeam(String teamName) {
        return ScorePlayerTeam.mcc$of(scoreboard.getPlayerTeam(teamName));
    }

    public ScorePlayerTeam createTeam(String name) {
        if (name.length() > 16) throw new IllegalArgumentException("The team name '" + name + "' is too long!");
        if (scoreboard.getPlayerTeam(name) != null) throw new IllegalArgumentException("A team with the name '" + name + "' already exists!");
        return ScorePlayerTeam.mcc$of(scoreboard.addPlayerTeam(name));
    }

    public void removeTeam(ScorePlayerTeam playerTeam) {
        if (playerTeam != null) scoreboard.removePlayerTeam(playerTeam.mcc$modern());
    }

    public boolean addPlayerToTeam(String player, String newTeam) {
        if (player.length() > 40) throw new IllegalArgumentException("The player name '" + player + "' is too long!");
        var team = scoreboard.getPlayerTeam(newTeam);
        return team != null && scoreboard.addPlayerToTeam(player, team);
    }

    public boolean removePlayerFromTeams(String playerName) {
        return scoreboard.removePlayerFromTeam(playerName);
    }

    public void removePlayerFromTeam(String username, ScorePlayerTeam playerTeam) {
        if (playerTeam == null) return;
        if (scoreboard.getPlayersTeam(username) != playerTeam.mcc$modern()) {
            throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + playerTeam.getName() + "'.");
        }
        scoreboard.removePlayerFromTeam(username, playerTeam.mcc$modern());
    }

    public Collection<String> getTeamNames() {
        return scoreboard.getTeamNames();
    }

    public Collection<ScorePlayerTeam> getTeams() {
        List<ScorePlayerTeam> out = new ArrayList<>();
        for (var t : scoreboard.getPlayerTeams()) out.add(ScorePlayerTeam.mcc$of(t));
        return out;
    }

    public ScorePlayerTeam getPlayersTeam(String username) {
        return ScorePlayerTeam.mcc$of(scoreboard.getPlayersTeam(username));
    }

    public static String getObjectiveDisplaySlot(int id) {
        var s = id >= 0 && id < net.minecraft.world.scores.DisplaySlot.values().length ? net.minecraft.world.scores.DisplaySlot.BY_ID.apply(id) : null;
        return s == null ? null : s.getSerializedName();
    }

    public static int getObjectiveDisplaySlotNumber(String name) {
        var s = net.minecraft.world.scores.DisplaySlot.CODEC.byName(name);
        return s == null ? -1 : s.id();
    }

    public static String[] getDisplaySlotStrings() {
        var slots = net.minecraft.world.scores.DisplaySlot.values();
        String[] out = new String[slots.length];
        for (int i = 0; i < slots.length; i++) out[i] = slots[i].getSerializedName();
        return out;
    }

    public void removeEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        if (entityIn != null && entityIn.mcc$modern() != null) scoreboard.entityRemoved(entityIn.mcc$modern());
    }
}
