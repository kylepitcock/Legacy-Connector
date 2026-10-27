package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/** One player's advancements: a view over the modern player's (granting and revoking criteria, reading progress). */
public class PlayerAdvancements {
    private EntityPlayerMP player;

    /** 1.12.2's constructor (the save file is the modern player's own). */
    public PlayerAdvancements(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server, java.io.File file, EntityPlayerMP player) {
        this.player = player;
    }

    public PlayerAdvancements(EntityPlayerMP player) {
        this.player = player;
    }

    public net.minecraft.server.PlayerAdvancements mcc$modern() {
        var sp = player == null ? null : player.mcc$serverPlayer();
        return sp == null ? null : sp.getAdvancements();
    }

    public EntityPlayerMP mcc$player() {
        return player;
    }

    public void setPlayer(EntityPlayerMP player) {
        this.player = player;
    }

    public boolean grantCriterion(Advancement advancement, String criterionName) {
        var modern = mcc$modern();
        return modern != null && advancement != null && modern.award(advancement.mcc$modern(), criterionName);
    }

    public boolean revokeCriterion(Advancement advancement, String criterionName) {
        var modern = mcc$modern();
        return modern != null && advancement != null && modern.revoke(advancement.mcc$modern(), criterionName);
    }

    public AdvancementProgress getProgress(Advancement advancement) {
        var modern = mcc$modern();
        if (modern == null || advancement == null) return new AdvancementProgress();
        return new AdvancementProgress(modern.getOrStartProgress(advancement.mcc$modern()));
    }

    /** Sends what changed to the player. */
    public void flushDirty(EntityPlayerMP player) {
        var modern = mcc$modern();
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (modern != null && sp != null) modern.flushDirty(sp);
    }

    public void save() {
        var modern = mcc$modern();
        if (modern != null) modern.save();
    }

    public void reload() {
        var modern = mcc$modern();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (modern != null && server != null) modern.reload(server.getAdvancements());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayerAdvancements p && java.util.Objects.equals(p.mcc$modern(), mcc$modern());
    }

    @Override
    public int hashCode() {
        var modern = mcc$modern();
        return modern == null ? 0 : System.identityHashCode(modern);
    }
}
