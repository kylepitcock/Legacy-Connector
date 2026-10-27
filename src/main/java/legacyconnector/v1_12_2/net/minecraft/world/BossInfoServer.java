package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** 1.12.2 server boss bar: every change goes to a modern {@link net.minecraft.server.level.ServerBossEvent}, which syncs the players. */
public class BossInfoServer extends BossInfo {
    private final net.minecraft.server.level.ServerBossEvent mcc$modern;
    private final Set<EntityPlayerMP> players = new LinkedHashSet<>();
    private final Set<EntityPlayerMP> readOnlyPlayers = Collections.unmodifiableSet(players);
    private boolean visible = true;

    public BossInfoServer(ITextComponent name, Color color, Overlay overlay) {
        super(net.minecraft.util.Mth.createInsecureUUID(), name, color, overlay);
        this.mcc$modern = new net.minecraft.server.level.ServerBossEvent(mcc$text(name), color.mcc$modern(), overlay.mcc$modern());
    }

    private static net.minecraft.network.chat.Component mcc$text(ITextComponent name) {
        return name == null ? net.minecraft.network.chat.Component.empty() : name.mcc$modern();
    }

    public net.minecraft.server.level.ServerBossEvent mcc$modern() {
        return mcc$modern;
    }

    @Override
    public void setPercent(float percent) {
        super.setPercent(percent);
        mcc$modern.setProgress(Math.max(0.0F, Math.min(1.0F, percent)));
    }

    @Override
    public void setColor(Color color) {
        super.setColor(color);
        mcc$modern.setColor(color.mcc$modern());
    }

    @Override
    public void setOverlay(Overlay overlay) {
        super.setOverlay(overlay);
        mcc$modern.setOverlay(overlay.mcc$modern());
    }

    @Override
    public BossInfo setDarkenSky(boolean darkenSky) {
        super.setDarkenSky(darkenSky);
        mcc$modern.setDarkenScreen(darkenSky);
        return this;
    }

    @Override
    public BossInfo setPlayEndBossMusic(boolean playEndBossMusic) {
        super.setPlayEndBossMusic(playEndBossMusic);
        mcc$modern.setPlayBossMusic(playEndBossMusic);
        return this;
    }

    @Override
    public BossInfo setCreateFog(boolean createFog) {
        super.setCreateFog(createFog);
        mcc$modern.setCreateWorldFog(createFog);
        return this;
    }

    @Override
    public void setName(ITextComponent name) {
        super.setName(name);
        mcc$modern.setName(mcc$text(name));
    }

    public void addPlayer(EntityPlayerMP player) {
        if (player != null && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer p && players.add(player)) mcc$modern.addPlayer(p);
    }

    public void removePlayer(EntityPlayerMP player) {
        if (player != null && players.remove(player) && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer p) mcc$modern.removePlayer(p);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        mcc$modern.setVisible(visible);
    }

    public boolean isVisible() {
        return visible;
    }

    public Collection<EntityPlayerMP> getPlayers() {
        return readOnlyPlayers;
    }
}
