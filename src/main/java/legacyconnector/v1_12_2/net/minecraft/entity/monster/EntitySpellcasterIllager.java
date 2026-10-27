package legacyconnector.v1_12_2.net.minecraft.entity.monster;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 spell-casting illager (evokers, illusioners): the spell being cast, shown to clients. */
public abstract class EntitySpellcasterIllager extends AbstractIllager {
    private static final DataParameter<Byte> SPELL = EntityDataManager.createKey(EntitySpellcasterIllager.class, DataSerializers.BYTE);
    protected int spellTicks;

    public EntitySpellcasterIllager(World world) {
        super(world);
    }

    protected EntitySpellcasterIllager(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SPELL, (byte) 0);
    }

    public boolean isSpellcasting() {
        if (mcc$illager() instanceof net.minecraft.world.entity.monster.SpellcasterIllager s) return s.isCastingSpell();
        Byte b = dataManager.get(SPELL);
        return b != null && b > 0;
    }

    @Override
    public IllagerArmPose getArmPose() {
        return isSpellcasting() ? IllagerArmPose.SPELLCASTING : super.getArmPose();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("SpellTicks", spellTicks);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        spellTicks = tag.getInteger("SpellTicks");
    }
}
