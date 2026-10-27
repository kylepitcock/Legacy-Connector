package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import java.util.HashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 arrow (plain or tipped with potion effects). {@code new EntityTippedArrow(world, shooter)} is a modern arrow; its effects are the
 * modern arrow's. A mod's subclass keeps its own effects and applies them on hit.
 */
public class EntityTippedArrow extends EntityArrow {
    private final Set<PotionEffect> customPotionEffects = new HashSet<>();
    private int mcc$color = -1;

    public EntityTippedArrow(World world) {
        super(world);
    }

    public EntityTippedArrow(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public EntityTippedArrow(World world, EntityLivingBase shooter) {
        super(world, shooter);
    }

    protected EntityTippedArrow(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.projectile.Arrow mcc$modernArrow() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.Arrow a ? a : null;
    }

    /** Takes the effects (and colour) of a tipped arrow item; a plain arrow item clears them. */
    public void setPotionEffect(ItemStack stack) {
        var modern = Stacks1122.toModern(stack);
        var contents = modern.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        var a = mcc$modernArrow();
        if (a != null) {
            if (contents != null) for (var e : contents.getAllEffects()) a.addEffect(new net.minecraft.world.effect.MobEffectInstance(e));
            return;
        }
        customPotionEffects.clear();
        mcc$color = -1;
        if (contents != null) {
            for (var e : contents.getAllEffects()) {
                var legacy = PotionEffect.mcc$fromModern(e);
                if (legacy != null) customPotionEffects.add(legacy);
            }
            if (contents.hasEffects()) mcc$color = contents.getColor();
        }
    }

    public void addEffect(PotionEffect effect) {
        if (effect == null) return;
        var a = mcc$modernArrow();
        var modern = effect.mcc$toModern();
        if (a != null) {
            if (modern != null) a.addEffect(modern);
            return;
        }
        customPotionEffects.add(effect);
    }

    public int getColor() {
        var a = mcc$modernArrow();
        return a != null ? a.getColor() : mcc$color;
    }

    /** The colour a tipped arrow item shows (its custom colour, else its effects' mix). */
    public static int getCustomColor(ItemStack stack) {
        var contents = Stacks1122.toModern(stack).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents == null ? -1 : contents.getColor();
    }

    @Override
    protected void arrowHit(EntityLivingBase living) {
        super.arrowHit(living);
        for (PotionEffect e : customPotionEffects) living.addPotionEffect(new PotionEffect(e));
    }

    @Override
    protected ItemStack getArrowStack() {
        var a = mcc$modernArrow();
        if (a != null) return Stacks1122.fromModern(a.getPickupItemStackOrigin());
        if (customPotionEffects.isEmpty()) return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.ARROW);
        var modern = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TIPPED_ARROW);
        java.util.List<net.minecraft.world.effect.MobEffectInstance> effects = new java.util.ArrayList<>();
        for (PotionEffect e : customPotionEffects) {
            var m = e.mcc$toModern();
            if (m != null) effects.add(m);
        }
        modern.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(),
                mcc$color == -1 ? java.util.Optional.empty() : java.util.Optional.of(mcc$color), effects));
        return Stacks1122.fromModern(modern);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$modernArrow() != null) return;
        if (mcc$color != -1) tag.setInteger("Color", mcc$color);
        if (!customPotionEffects.isEmpty()) {
            var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
            for (PotionEffect e : customPotionEffects) list.appendTag(e.writeCustomPotionEffectToNBT(new NBTTagCompound()));
            tag.setTag("CustomPotionEffects", list);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$modernArrow() != null) return;
        if (tag.hasKey("Color", 99)) mcc$color = tag.getInteger("Color");
        var list = tag.getTagList("CustomPotionEffects", 10);
        for (int i = 0; i < list.tagCount(); ++i) {
            PotionEffect e = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
            if (e != null) customPotionEffects.add(e);
        }
    }
}
