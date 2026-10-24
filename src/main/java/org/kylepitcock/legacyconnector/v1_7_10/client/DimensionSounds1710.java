package org.kylepitcock.legacyconnector.v1_7_10.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionPhysics1710;

/**
 * World sounds in a dimension whose provider attenuates sound (Galacticraft: {@code getSoundVolReductionAmount}, vacuum in
 * space): the volume is divided by the amount, and strongly attenuated sounds are dropped. Music and interface sounds are untouched.
 */
public final class DimensionSounds1710 {
    private DimensionSounds1710() {}

    public static void register(IEventBus gameBus) {
        // 1.7.10's PlaySoundEvent17: handlers may silence a sound
        gameBus.addListener((PlaySoundEvent e) -> {
            SoundInstance sound = e.getSound();
            var forge = legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
            if (sound == null || !forge.mcc$hasListeners(legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.PlaySoundEvent17.class)) return;
            var location = sound.getLocation();
            var legacy = new legacyconnector.v1_7_10.net.minecraft.client.audio.PositionedSoundRecord(
                    new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation(location.getNamespace(), location.getPath()),
                    sound.getVolume(), sound.getPitch(), (float) sound.getX(), (float) sound.getY(), (float) sound.getZ());
            try {
                var event = new legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.PlaySoundEvent17(location.getPath(), legacy);
                forge.post(event);
                if (event.result == null) e.setSound(null);
            } catch (RuntimeException | LinkageError ex) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Sound").warn("A PlaySoundEvent17 handler failed: {}", ex.toString());
            }
        });
        gameBus.addListener((PlaySoundEvent e) -> {
            SoundInstance sound = e.getSound();
            Minecraft mc = Minecraft.getInstance();
            if (sound == null || mc.level == null) return;
            SoundSource source = sound.getSource();
            if (source == SoundSource.MUSIC || source == SoundSource.MASTER || source == SoundSource.RECORDS) return;
            float amount = DimensionPhysics1710.soundAttenuation(mc.level);
            if (amount <= 1.0F) return;
            if (amount >= 8.0F) {
                e.setSound(null);
            } else if (!(sound instanceof TickableSoundInstance)) {
                e.setSound(new Scaled(sound, amount));
            }
        });
    }

    private record Scaled(SoundInstance base, float divisor) implements SoundInstance {
        @Override public ResourceLocation getLocation() { return base.getLocation(); }
        @Override public WeighedSoundEvents resolve(SoundManager manager) { return base.resolve(manager); }
        @Override public net.minecraft.client.resources.sounds.Sound getSound() { return base.getSound(); }
        @Override public SoundSource getSource() { return base.getSource(); }
        @Override public boolean isLooping() { return base.isLooping(); }
        @Override public boolean isRelative() { return base.isRelative(); }
        @Override public int getDelay() { return base.getDelay(); }
        @Override public float getVolume() { return base.getVolume() / divisor; }
        @Override public float getPitch() { return base.getPitch(); }
        @Override public double getX() { return base.getX(); }
        @Override public double getY() { return base.getY(); }
        @Override public double getZ() { return base.getZ(); }
        @Override public Attenuation getAttenuation() { return base.getAttenuation(); }
    }
}
