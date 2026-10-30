package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.world.GameType;

/** A change of the tab list: players added or removed, or their game mode, ping or display name; sent as the modern player info packet. */
public class SPacketPlayerListItem implements Packet<INetHandlerPlayClient> {
    private Action action;
    private final List<AddPlayerData> players = new ArrayList<>();
    private final List<net.minecraft.server.level.ServerPlayer> mcc$players = new ArrayList<>();

    public SPacketPlayerListItem() {
    }

    public SPacketPlayerListItem(Action actionIn, EntityPlayerMP... playersIn) {
        this(actionIn, java.util.Arrays.asList(playersIn));
    }

    public SPacketPlayerListItem(Action actionIn, Iterable<EntityPlayerMP> playersIn) {
        this.action = actionIn;
        for (EntityPlayerMP p : playersIn) {
            var sp = p == null ? null : p.mcc$serverPlayer();
            if (sp == null) continue;
            mcc$players.add(sp);
            players.add(new AddPlayerData(sp.getGameProfile(), sp.connection == null ? 0 : sp.connection.latency(), GameType.mcc$of(sp.gameMode.getGameModeForPlayer()),
                    sp.getTabListDisplayName() == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(sp.getTabListDisplayName().getString())));
        }
    }

    public List<AddPlayerData> getEntries() {
        return players;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (action == null || mcc$players.isEmpty()) return null;
        return switch (action) {
            case ADD_PLAYER -> net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(mcc$players);
            case REMOVE_PLAYER -> new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(
                    mcc$players.stream().map(net.minecraft.world.entity.Entity::getUUID).toList());
            case UPDATE_GAME_MODE -> new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                    java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE), mcc$players);
            case UPDATE_LATENCY -> new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                    java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), mcc$players);
            case UPDATE_DISPLAY_NAME -> new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                    java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), mcc$players);
        };
    }

    public enum Action {
        ADD_PLAYER, UPDATE_GAME_MODE, UPDATE_LATENCY, UPDATE_DISPLAY_NAME, REMOVE_PLAYER
    }

    public class AddPlayerData {
        private final int ping;
        private final GameType gamemode;
        private final GameProfile profile;
        private final ITextComponent displayName;

        public AddPlayerData(GameProfile profileIn, int latencyIn, GameType gameModeIn, ITextComponent displayNameIn) {
            this.profile = profileIn;
            this.ping = latencyIn;
            this.gamemode = gameModeIn;
            this.displayName = displayNameIn;
        }

        public GameProfile getProfile() {
            return profile;
        }

        public int getPing() {
            return ping;
        }

        public GameType getGameMode() {
            return gamemode;
        }

        public ITextComponent getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return "AddPlayerData{latency=" + ping + ", gameMode=" + gamemode + ", profile=" + profile + ", displayName=" + displayName + "}";
        }
    }
}
