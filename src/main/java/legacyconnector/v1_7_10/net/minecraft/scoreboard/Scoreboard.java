package legacyconnector.v1_7_10.net.minecraft.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;

/**
 * The legacy scoreboard as a view of the level's modern one (the server's on the server, the synced copy on a client), so
 * objectives, scores and teams legacy code makes are the game's own (FiskTag keeps its kills and deaths in objectives).
 */
public class Scoreboard {
    final net.minecraft.world.scores.Scoreboard modern;

    public Scoreboard(net.minecraft.world.scores.Scoreboard modern) {
        this.modern = modern;
    }

    private static final Map<net.minecraft.world.scores.Scoreboard, Scoreboard> VIEWS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static Scoreboard mcc$wrap(net.minecraft.world.scores.Scoreboard modern) {
        return modern == null ? null : VIEWS.computeIfAbsent(modern, Scoreboard::new);
    }

    public net.minecraft.world.scores.Scoreboard mcc$modern() {
        return modern;
    }

    private ScoreObjective view(Objective objective) {
        return objective == null ? null : new ScoreObjective(this, objective);
    }

    private ScorePlayerTeam view(PlayerTeam team) {
        return team == null ? null : new ScorePlayerTeam(this, team);
    }

    public ScoreObjective getObjective(String name) {
        return name == null ? null : view(modern.getObjective(name));
    }

    public ScoreObjective addScoreObjective(String name, IScoreObjectiveCriteria criteria) {
        if (modern.getObjective(name) != null) throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
        var modernCriteria = criteria instanceof ScoreDummyCriteria c ? c.mcc$modern() : net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY;
        return view(modern.addObjective(name, modernCriteria, Component.literal(name), modernCriteria.getDefaultRenderType(), false, null));
    }

    public Collection func_96520_a(IScoreObjectiveCriteria criteria) {
        List<ScoreObjective> out = new ArrayList<>();
        for (Objective o : modern.getObjectives()) if (ScoreDummyCriteria.mcc$wrap(o.getCriteria()) == criteria) out.add(view(o));
        return out;
    }

    public Score getValueFromObjective(String name, ScoreObjective objective) {
        return new Score(this, objective, name);
    }

    public Collection getSortedScores(ScoreObjective objective) {
        List<Score> out = new ArrayList<>();
        for (var entry : modern.listPlayerScores(objective.modern)) out.add(new Score(this, objective, entry.owner()));
        out.sort(Score.scoreComparator);
        return out;
    }

    public Collection getScoreObjectives() {
        List<ScoreObjective> out = new ArrayList<>();
        for (Objective o : modern.getObjectives()) out.add(view(o));
        return out;
    }

    public Collection getObjectiveNames() {
        return new ArrayList<>(modern.getObjectiveNames());
    }

    /** Resets a holder's scores (in every objective). */
    public void func_96515_c(String name) {
        modern.resetAllPlayerScores(ScoreHolder.forNameOnly(name));
    }

    public Collection func_96528_e() {
        List<Score> out = new ArrayList<>();
        for (var holder : modern.getTrackedPlayers()) {
            for (var e : modern.listPlayerScores(holder).object2IntEntrySet()) out.add(new Score(this, view(e.getKey()), holder.getScoreboardName()));
        }
        return out;
    }

    public Map func_96510_d(String name) {
        Map<ScoreObjective, Score> out = new HashMap<>();
        for (var e : modern.listPlayerScores(ScoreHolder.forNameOnly(name)).object2IntEntrySet()) {
            ScoreObjective o = view(e.getKey());
            out.put(o, new Score(this, o, name));
        }
        return out;
    }

    public void func_96519_k(ScoreObjective objective) {
        if (objective != null) modern.removeObjective(objective.modern);
    }

    public void setObjectiveInDisplaySlot(int slot, ScoreObjective objective) {
        DisplaySlot display = DisplaySlot.BY_ID.apply(slot);
        if (display != null) modern.setDisplayObjective(display, objective == null ? null : objective.modern);
    }

    public ScoreObjective getObjectiveInDisplaySlot(int slot) {
        DisplaySlot display = DisplaySlot.BY_ID.apply(slot);
        return display == null ? null : view(modern.getDisplayObjective(display));
    }

    public ScorePlayerTeam getTeam(String name) {
        return name == null ? null : view(modern.getPlayerTeam(name));
    }

    public ScorePlayerTeam createTeam(String name) {
        if (modern.getPlayerTeam(name) != null) throw new IllegalArgumentException("A team with the name '" + name + "' already exists!");
        return view(modern.addPlayerTeam(name));
    }

    public void removeTeam(ScorePlayerTeam team) {
        if (team != null) modern.removePlayerTeam(team.modern);
    }

    public boolean func_151392_a(String player, String team) {
        PlayerTeam t = modern.getPlayerTeam(team);
        return t != null && modern.addPlayerToTeam(player, t);
    }

    public boolean removePlayerFromTeams(String player) {
        return modern.removePlayerFromTeam(player);
    }

    public void removePlayerFromTeam(String player, ScorePlayerTeam team) {
        modern.removePlayerFromTeam(player, team.modern);
    }

    public Collection getTeamNames() {
        return new ArrayList<>(modern.getTeamNames());
    }

    public Collection getTeams() {
        List<ScorePlayerTeam> out = new ArrayList<>();
        for (PlayerTeam t : modern.getPlayerTeams()) out.add(view(t));
        return out;
    }

    public ScorePlayerTeam getPlayersTeam(String player) {
        return view(modern.getPlayersTeam(player));
    }

    // change notifications: the modern scoreboard sends its own
    public void func_96522_a(ScoreObjective objective) {
    }

    public void func_96532_b(ScoreObjective objective) {
    }

    public void func_96533_c(ScoreObjective objective) {
    }

    public void func_96536_a(Score score) {
    }

    public void func_96516_a(String name) {
    }

    public void broadcastTeamCreated(ScorePlayerTeam team) {
    }

    public void broadcastTeamRemoved(ScorePlayerTeam team) {
    }

    public void func_96513_c(ScorePlayerTeam team) {
    }

    public static String getObjectiveDisplaySlot(int slot) {
        return switch (slot) {
            case 0 -> "list";
            case 1 -> "sidebar";
            case 2 -> "belowName";
            default -> null;
        };
    }

    public static int getObjectiveDisplaySlotNumber(String name) {
        return switch (name) {
            case "list" -> 0;
            case "sidebar" -> 1;
            case "belowName" -> 2;
            default -> -1;
        };
    }
}
