package legacyconnector.v1_12_2.net.minecraft.scoreboard;

import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** A team, as 1.12.2's abstract base (the scoreboard's teams are {@link ScorePlayerTeam}s). */
public abstract class Team {
    public boolean isSameTeam(Team other) {
        return other != null && this == other || other != null && getName().equals(other.getName());
    }

    public abstract String getName();

    public abstract String formatString(String input);

    public abstract boolean getSeeFriendlyInvisiblesEnabled();

    public abstract boolean getAllowFriendlyFire();

    public abstract EnumVisible getNameTagVisibility();

    public abstract TextFormatting getColor();

    public abstract Collection<String> getMembershipCollection();

    public abstract EnumVisible getDeathMessageVisibility();

    public abstract CollisionRule getCollisionRule();

    /** The legacy colour of a modern chat formatting. */
    static TextFormatting mcc$colour(net.minecraft.ChatFormatting f) {
        if (f == null) return TextFormatting.RESET;
        TextFormatting t = TextFormatting.getValueByName(f.getName());
        return t == null ? TextFormatting.RESET : t;
    }

    public enum EnumVisible {
        ALWAYS("always", 0),
        NEVER("never", 1),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams", 2),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam", 3);

        public final String internalName;
        public final int id;

        EnumVisible(String nameIn, int idIn) {
            this.internalName = nameIn;
            this.id = idIn;
        }

        public static String[] getNames() {
            String[] out = new String[values().length];
            for (int i = 0; i < out.length; i++) out[i] = values()[i].internalName;
            return out;
        }

        public static EnumVisible getByName(String nameIn) {
            for (EnumVisible v : values()) if (v.internalName.equals(nameIn)) return v;
            return null;
        }

        public net.minecraft.world.scores.Team.Visibility mcc$modern() {
            return net.minecraft.world.scores.Team.Visibility.byName(internalName);
        }

        public static EnumVisible mcc$of(net.minecraft.world.scores.Team.Visibility v) {
            EnumVisible out = v == null ? null : getByName(v.name);
            return out == null ? ALWAYS : out;
        }
    }

    public enum CollisionRule {
        ALWAYS("always", 0),
        NEVER("never", 1),
        HIDE_FOR_OTHER_TEAMS("pushOtherTeams", 2),
        HIDE_FOR_OWN_TEAM("pushOwnTeam", 3);

        public final String name;
        public final int id;

        CollisionRule(String nameIn, int idIn) {
            this.name = nameIn;
            this.id = idIn;
        }

        public static CollisionRule getByName(String nameIn) {
            for (CollisionRule r : values()) if (r.name.equals(nameIn)) return r;
            return null;
        }

        public net.minecraft.world.scores.Team.CollisionRule mcc$modern() {
            return net.minecraft.world.scores.Team.CollisionRule.byName(name);
        }

        public static CollisionRule mcc$of(net.minecraft.world.scores.Team.CollisionRule r) {
            CollisionRule out = r == null ? null : getByName(r.name);
            return out == null ? ALWAYS : out;
        }
    }
}
