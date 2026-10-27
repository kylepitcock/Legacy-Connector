package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The player's abilities (flying, creative, speeds). */
public class SPacketPlayerAbilities implements Packet<INetHandlerPlayClient> {
    private boolean invulnerable;
    private boolean flying;
    private boolean allowFlying;
    private boolean creativeMode;
    private float flySpeed;
    private float walkSpeed;

    public SPacketPlayerAbilities() {
    }

    public SPacketPlayerAbilities(legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities capabilities) {
        this.invulnerable = legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(capabilities);
        this.flying = legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isFlying(capabilities);
        this.allowFlying = legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$allowFlying(capabilities);
        this.creativeMode = legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(capabilities);
        this.flySpeed = capabilities.getFlySpeed();
        this.walkSpeed = capabilities.getWalkSpeed();
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isAllowFlying() {
        return allowFlying;
    }

    public boolean isCreativeMode() {
        return creativeMode;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        byte b = buf.readByte();
        invulnerable = (b & 1) > 0;
        flying = (b & 2) > 0;
        allowFlying = (b & 4) > 0;
        creativeMode = (b & 8) > 0;
        flySpeed = buf.readFloat();
        walkSpeed = buf.readFloat();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        byte b = 0;
        if (invulnerable) b = (byte) (b | 1);
        if (flying) b = (byte) (b | 2);
        if (allowFlying) b = (byte) (b | 4);
        if (creativeMode) b = (byte) (b | 8);
        buf.writeByte(b);
        buf.writeFloat(flySpeed);
        buf.writeFloat(walkSpeed);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var abilities = new net.minecraft.world.entity.player.Abilities();
        abilities.invulnerable = invulnerable;
        abilities.flying = flying;
        abilities.mayfly = allowFlying;
        abilities.instabuild = creativeMode;
        abilities.setFlyingSpeed(flySpeed);
        abilities.setWalkingSpeed(walkSpeed);
        return new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(abilities);
    }
}
