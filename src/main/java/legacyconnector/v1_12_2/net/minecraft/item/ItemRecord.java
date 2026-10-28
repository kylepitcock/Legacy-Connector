package legacyconnector.v1_12_2.net.minecraft.item;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/**
 * 1.12.2 music discs: a sound and a display name, looked up by sound. Playing one in a modern jukebox needs a data-driven jukebox song,
 * which legacy discs do not register, so a jukebox does not take them.
 */
public class ItemRecord extends Item {
    private static final Map<SoundEvent, ItemRecord> RECORDS = new HashMap<>();
    private final SoundEvent sound;
    private final String displayName;

    protected ItemRecord(String recordName, SoundEvent soundIn) {
        this.displayName = "item.record." + recordName + ".desc";
        this.sound = soundIn;
        this.maxStackSize = 1;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.MISC);
        synchronized (RECORDS) {
            RECORDS.put(soundIn, this);
        }
    }

    public String getRecordNameLocal() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(displayName);
    }

    public SoundEvent getSound() {
        return sound;
    }

    public static ItemRecord getBySound(SoundEvent soundIn) {
        synchronized (RECORDS) {
            return RECORDS.get(soundIn);
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }
}
