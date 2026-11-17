package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * The abilities of a player (invulnerable, flying, may fly, creative, fly and walk speed). Sending it to a player applies them to that
 * player and tells the client, as the 1.7.10 server did for a mod that built the packet by hand.
 */
public class S39PacketPlayerAbilities extends Packet {
    private boolean invulnerable;
    private boolean flying;
    private boolean allowFlying;
    private boolean creative;
    private float flySpeed;
    private float walkSpeed;

    public S39PacketPlayerAbilities() {
    }

    public S39PacketPlayerAbilities(PlayerCapabilities capabilities) {
        setInvulnerable(PlayerCapabilities.mcc$get$disableDamage(capabilities));
        setFlying(PlayerCapabilities.mcc$get$isFlying(capabilities));
        setAllowFlying(PlayerCapabilities.mcc$get$allowFlying(capabilities));
        setCreativeMode(PlayerCapabilities.mcc$get$isCreativeMode(capabilities));
        setFlySpeed(capabilities.getFlySpeed());
        setWalkSpeed(capabilities.getWalkSpeed());
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        byte flags = data.readByte();
        setInvulnerable((flags & 1) > 0);
        setFlying((flags & 2) > 0);
        setAllowFlying((flags & 4) > 0);
        setCreativeMode((flags & 8) > 0);
        setFlySpeed(data.readFloat());
        setWalkSpeed(data.readFloat());
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        int flags = 0;
        if (isInvulnerable()) flags |= 1;
        if (isFlying()) flags |= 2;
        if (isAllowFlying()) flags |= 4;
        if (isCreativeMode()) flags |= 8;
        data.writeByte(flags);
        data.writeFloat(flySpeed);
        data.writeFloat(walkSpeed);
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean v) {
        invulnerable = v;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean v) {
        flying = v;
    }

    public boolean isAllowFlying() {
        return allowFlying;
    }

    public void setAllowFlying(boolean v) {
        allowFlying = v;
    }

    public boolean isCreativeMode() {
        return creative;
    }

    public void setCreativeMode(boolean v) {
        creative = v;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public void setFlySpeed(float v) {
        flySpeed = v;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(float v) {
        walkSpeed = v;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var abilities = player.getAbilities();
        abilities.invulnerable = invulnerable;
        abilities.mayfly = allowFlying;
        abilities.flying = flying && allowFlying;
        abilities.instabuild = creative;
        abilities.setFlyingSpeed(flySpeed);
        abilities.setWalkingSpeed(walkSpeed);
        player.onUpdateAbilities();
    }
}
