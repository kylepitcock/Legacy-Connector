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

    /** Starts (or with NONE ends) casting a spell: the modern illager's spell, which clients show as particles and the pose. */
    public void setSpellType(SpellType spellType) {
        SpellType t = spellType == null ? SpellType.NONE : spellType;
        if (mcc$illager() instanceof net.minecraft.world.entity.monster.SpellcasterIllager s) {
            org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setCastingSpell(s, t.id);
        }
        dataManager.set(SPELL, (byte) t.id);
    }

    protected SpellType getSpellType() {
        Byte b = dataManager.get(SPELL);
        return SpellType.getFromId(b == null ? 0 : b);
    }

    /** The spells and the colour of their particles. */
    public enum SpellType {
        NONE(0, 0.0D, 0.0D, 0.0D), SUMMON_VEX(1, 0.7D, 0.7D, 0.8D), FANGS(2, 0.4D, 0.3D, 0.35D), WOLOLO(3, 0.7D, 0.5D, 0.2D),
        DISAPPEAR(4, 0.3D, 0.3D, 0.8D), BLINDNESS(5, 0.1D, 0.1D, 0.2D);

        private final int id;
        private final double[] particleSpeed;

        SpellType(int idIn, double xParticleSpeed, double yParticleSpeed, double zParticleSpeed) {
            this.id = idIn;
            this.particleSpeed = new double[] {xParticleSpeed, yParticleSpeed, zParticleSpeed};
        }

        public static SpellType getFromId(int idIn) {
            for (SpellType t : values()) if (idIn == t.id) return t;
            return NONE;
        }
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
