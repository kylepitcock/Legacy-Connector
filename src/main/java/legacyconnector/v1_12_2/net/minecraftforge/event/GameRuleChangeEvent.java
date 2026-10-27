package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.world.GameRules;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A game rule was changed by the /gamerule command. Mods' handlers link; the modern game has no rule change event to post it from. */
public class GameRuleChangeEvent extends Event {
    private final GameRules rules;
    private final String ruleName;
    private final MinecraftServer server;

    public GameRuleChangeEvent(GameRules rules, String ruleName, MinecraftServer server) {
        this.rules = rules;
        this.ruleName = ruleName;
        this.server = server;
    }

    public GameRules getRules() {
        return rules;
    }

    public String getRuleName() {
        return ruleName;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
