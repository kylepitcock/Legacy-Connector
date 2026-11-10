package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Explicit original health writes can establish capacity without owning native bases or foreign modifiers. */
public final class PlayerHealthCapacityB173 {
    static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath("legacyconnector","beta_health_capacity");
    static final String FLOOR="legacyconnector:beta_health_capacity_floor";
    private static boolean installed;
    private PlayerHealthCapacityB173(){}
    static void commit(Player player,int before,int requested){
        if(player.level().isClientSide||before==requested)return;
        commit(player.getPersistentData(),player.getAttribute(Attributes.MAX_HEALTH),before,requested,PlayerApiB173.hasRegistered());
    }
    static void commit(CompoundTag data,AttributeInstance attribute,int before,int requested,boolean enabled){
        if(attribute==null)return;
        if(enabled&&before!=requested&&requested>attribute.getValue())
            data.putDouble(FLOOR,Math.max(floor(data),attribute.getAttribute().value().sanitizeValue(requested)));
        reconcile(data,attribute,enabled);
    }
    static double floor(CompoundTag data){double value=data.getDouble(FLOOR);return Double.isFinite(value)&&value>0?value:0;}
    static void reconcile(CompoundTag data,AttributeInstance attribute,boolean enabled){
        if(attribute==null)return;
        double floor=enabled?floor(data):0;
        if(!enabled)data.remove(FLOOR);
        double additive=attribute.getBaseValue(),factor=1,total=1;
        for(var modifier:attribute.getModifiers()){
            if(modifier.id().equals(MODIFIER))continue;
            switch(modifier.operation()){
                case ADD_VALUE->additive+=modifier.amount();
                case ADD_MULTIPLIED_BASE->factor+=modifier.amount();
                case ADD_MULTIPLIED_TOTAL->total*=1+modifier.amount();
            }
        }
        double multiplier=factor*total,underlying=additive*multiplier;
        // A foreign zero/negative multiplier remains authoritative. Never remove it to manufacture capacity.
        double amount=floor>attribute.getAttribute().value().sanitizeValue(underlying)&&multiplier>0&&Double.isFinite(multiplier)
                ?floor/multiplier-additive:0;
        var old=attribute.getModifier(MODIFIER);
        if(old!=null&&old.operation()==AttributeModifier.Operation.ADD_VALUE&&Double.compare(old.amount(),amount)==0)return;
        if(old!=null)attribute.removeModifier(MODIFIER);
        if(amount>0&&Double.isFinite(amount))attribute.addPermanentModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void reconcile(Player player){
        if(!player.level().isClientSide)reconcile(player.getPersistentData(),player.getAttribute(Attributes.MAX_HEALTH),PlayerApiB173.hasRegistered());
    }
    static void copyFloor(CompoundTag original,CompoundTag copy){double floor=floor(original);if(floor>0)copy.putDouble(FLOOR,floor);else copy.remove(FLOOR);}
    /** restoreFrom applies native health before Clone exposes our capacity; retain any earlier foreign health decision. */
    static float cloneHealth(float oldHealth,float nativeHealth,float nativeMax,float restoredMax,boolean death){
        if(death)return Float.compare(nativeHealth,nativeMax)==0?restoredMax:nativeHealth;
        float expected=Math.max(0,Math.min(oldHealth,nativeMax));
        return Float.compare(nativeHealth,expected)==0?Math.max(0,Math.min(oldHealth,restoredMax)):nativeHealth;
    }
    public static synchronized void install(){
        if(installed)return;installed=true;
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event)->{if(event.getEntity() instanceof Player player)reconcile(player);});
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event)->reconcile(event.getEntity()));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event)->{
            if(event.getEntity().level().isClientSide)return;
            var player=event.getEntity();float nativeHealth=player.getHealth(),nativeMax=player.getMaxHealth();
            copyFloor(event.getOriginal().getPersistentData(),event.getEntity().getPersistentData());
            reconcile(event.getEntity());
            if(floor(player.getPersistentData())>0){
                float health=cloneHealth(event.getOriginal().getHealth(),nativeHealth,nativeMax,player.getMaxHealth(),event.isWasDeath());
                if(Float.compare(health,player.getHealth())!=0)player.setHealth(health);
            }
        });
    }
}
