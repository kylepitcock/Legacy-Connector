package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

/**
 * Stands in for {@code DustParticleOptionsBase} (1.20.1; 1.21.1 has no common base of DustParticleOptions and DustColorTransitionOptions): the colour and
 * scale holder with the command/network helpers that mods' own dust-like particle options extend. The write methods stay plain methods the mod overrides.
 */
public abstract class LegacyDustParticleOptionsBase implements ParticleOptions {
    public static final float MIN_SCALE = 0.01f;
    public static final float MAX_SCALE = 4.0f;

    protected final Vector3f color;
    protected final float scale;

    public LegacyDustParticleOptionsBase(Vector3f color, float scale) {
        this.color = color;
        this.scale = net.minecraft.util.Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public static Vector3f readVector3f(StringReader reader) throws CommandSyntaxException {
        reader.expect(' ');
        float x = reader.readFloat();
        reader.expect(' ');
        float y = reader.readFloat();
        reader.expect(' ');
        float z = reader.readFloat();
        return new Vector3f(x, y, z);
    }

    public static Vector3f readVector3f(FriendlyByteBuf buffer) {
        return new Vector3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(color.x());
        buffer.writeFloat(color.y());
        buffer.writeFloat(color.z());
        buffer.writeFloat(scale);
    }

    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(getType()),
                color.x(), color.y(), color.z(), scale);
    }

    public Vector3f getColor() {
        return color;
    }

    public float getScale() {
        return scale;
    }
}
