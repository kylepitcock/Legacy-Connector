package legacyconnector.v1_7_10.net.minecraft.scoreboard;

import java.util.Collection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

/** A view of a modern team; prefixes and suffixes are plain legacy-formatted strings. */
public class ScorePlayerTeam extends Team {
    private final Scoreboard scoreboard;
    final PlayerTeam modern;

    ScorePlayerTeam(Scoreboard scoreboard, PlayerTeam modern) {
        this.scoreboard = scoreboard;
        this.modern = modern;
    }

    @Override
    public String getRegisteredName() {
        return modern.getName();
    }

    public String func_96669_c() {
        return modern.getDisplayName().getString();
    }

    public void setTeamName(String name) {
        modern.setDisplayName(Component.literal(name));
    }

    public Collection getMembershipCollection() {
        return modern.getPlayers();
    }

    public String getColorPrefix() {
        return legacyText(modern.getPlayerPrefix());
    }

    public void setNamePrefix(String prefix) {
        modern.setPlayerPrefix(Component.literal(prefix == null ? "" : prefix));
    }

    public String getColorSuffix() {
        return legacyText(modern.getPlayerSuffix());
    }

    public void setNameSuffix(String suffix) {
        modern.setPlayerSuffix(Component.literal(suffix == null ? "" : suffix));
    }

    @Override
    public String formatString(String name) {
        return getColorPrefix() + name + getColorSuffix();
    }

    public static String formatPlayerName(Team team, String name) {
        return team == null ? name : team.formatString(name);
    }

    @Override
    public boolean getAllowFriendlyFire() {
        return modern.isAllowFriendlyFire();
    }

    public void setAllowFriendlyFire(boolean allow) {
        modern.setAllowFriendlyFire(allow);
    }

    @Override
    public boolean func_98297_h() {
        return modern.canSeeFriendlyInvisibles();
    }

    public void setSeeFriendlyInvisiblesEnabled(boolean see) {
        modern.setSeeFriendlyInvisibles(see);
    }

    public int func_98299_i() {
        return (getAllowFriendlyFire() ? 1 : 0) | (func_98297_h() ? 2 : 0);
    }

    public void func_98298_a(int flags) {
        setAllowFriendlyFire((flags & 1) > 0);
        setSeeFriendlyInvisiblesEnabled((flags & 2) > 0);
    }

    /** A component as text with legacy formatting codes (prefixes and suffixes were plain strings). */
    static String legacyText(Component component) {
        StringBuilder out = new StringBuilder();
        component.visit((style, text) -> {
            if (style.getColor() != null) {
                var format = net.minecraft.ChatFormatting.getByName(style.getColor().serialize());
                if (format != null) out.append('§').append(format.getChar());
            }
            if (style.isBold()) out.append("§l");
            if (style.isItalic()) out.append("§o");
            if (style.isUnderlined()) out.append("§n");
            if (style.isStrikethrough()) out.append("§m");
            if (style.isObfuscated()) out.append("§k");
            out.append(text);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out.toString();
    }

    public PlayerTeam mcc$modern() {
        return modern;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ScorePlayerTeam other && other.modern == modern;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(modern);
    }
}
