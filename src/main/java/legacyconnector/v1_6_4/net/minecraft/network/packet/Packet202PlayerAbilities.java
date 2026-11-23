package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's player-abilities packet: invulnerable, flying, may fly and creative flags (one byte) and the two speeds. Built from a
 * {@link PlayerCapabilities}; sent to a client it is delivered as the modern abilities packet carrying exactly these values, without
 * changing the server's own abilities of the player (the server announces what it already set).
 */
public class Packet202PlayerAbilities extends Packet {
    private boolean disableDamage;
    private boolean isFlying;
    private boolean allowFlying;
    private boolean isCreativeMode;
    private float flySpeed;
    private float walkSpeed;

    public Packet202PlayerAbilities() {}

    public Packet202PlayerAbilities(PlayerCapabilities capabilities) {
        this.disableDamage = PlayerCapabilities.mcc$get$disableDamage(capabilities);
        this.isFlying = PlayerCapabilities.mcc$get$isFlying(capabilities);
        this.allowFlying = PlayerCapabilities.mcc$get$allowFlying(capabilities);
        this.isCreativeMode = PlayerCapabilities.mcc$get$isCreativeMode(capabilities);
        this.flySpeed = capabilities.getFlySpeed();
        this.walkSpeed = capabilities.getWalkSpeed();
    }

    public boolean getDisableDamage() {
        return disableDamage;
    }

    public void setDisableDamage(boolean value) {
        this.disableDamage = value;
    }

    public boolean getFlying() {
        return isFlying;
    }

    public void setFlying(boolean value) {
        this.isFlying = value;
    }

    public boolean getAllowFlying() {
        return allowFlying;
    }

    public void setAllowFlying(boolean value) {
        this.allowFlying = value;
    }

    public boolean isCreativeMode() {
        return isCreativeMode;
    }

    public void setCreativeMode(boolean value) {
        this.isCreativeMode = value;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public void setFlySpeed(float value) {
        this.flySpeed = value;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(float value) {
        this.walkSpeed = value;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        byte flags = in.readByte();
        disableDamage = (flags & 1) > 0;
        isFlying = (flags & 2) > 0;
        allowFlying = (flags & 4) > 0;
        isCreativeMode = (flags & 8) > 0;
        flySpeed = in.readFloat();
        walkSpeed = in.readFloat();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        int flags = 0;
        if (disableDamage) flags |= 1;
        if (isFlying) flags |= 2;
        if (allowFlying) flags |= 4;
        if (isCreativeMode) flags |= 8;
        out.writeByte(flags);
        out.writeFloat(flySpeed);
        out.writeFloat(walkSpeed);
    }

    @Override
    public int getPacketSize() {
        return 9;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        net.minecraft.world.entity.player.Abilities abilities = new net.minecraft.world.entity.player.Abilities();
        abilities.invulnerable = disableDamage;
        abilities.flying = isFlying;
        abilities.mayfly = allowFlying;
        abilities.instabuild = isCreativeMode;
        abilities.setFlyingSpeed(flySpeed);
        abilities.setWalkingSpeed(walkSpeed);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(abilities));
    }
}
