package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.entity.player.Player;

/**
 * A modern player as a b1.7.3 {@code EntityPlayer} for block callbacks: before a callback {@link #sync} copies position, rotation, sneaking,
 * health and the 36 main inventory slots (as legacy stacks); afterwards {@link #writeBack} gives the modern player whatever the mod did to
 * those slots (a used-up item, a filled bucket). One view per player.
 */
public final class PlayerViewB173 extends legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity {
    /**
     * Keyed by identity: {@code Entity.equals} compares entity ids, and in single player the client's and the server's player have the same
     * id (a WeakHashMap gave the server the client player's view, so OpenGUI saw a client player and no menu opened).
     */
    private static final Map<Player, PlayerViewB173> VIEWS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private final Player player;
    private final ItemStack[] before = new ItemStack[36];
    private final ItemStack[] beforeArmor = new ItemStack[4];
    private int beforeHealth,beforeAir,beforeFire;
    private float beforeFallDistance;
    private boolean beforeSleeping;
    private EntityViewsB173.Placement beforePlacement;

    private PlayerViewB173(Player player) {
        super(World.mcc$wrap(player.level()));
        this.player = player;
        this.mcc$modern = player;
    }

    public static PlayerViewB173 of(Player player) {
        PlayerViewB173 view;
        synchronized (VIEWS) {
            view = VIEWS.computeIfAbsent(player, PlayerViewB173::new);
        }
        view.sync();
        if(PlayerApiB173.hasRegistered()){
            if(view.playerScreenHandler==null){
                view.playerScreenHandler=new legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler(view.inventory,!view.world.isRemote);
                view.currentScreenHandler=view.playerScreenHandler;
            }
            PlayerApiLifecycleB173.initializeView(view);
        }
        return view;
    }

    public static void release(Player player){synchronized(VIEWS){VIEWS.remove(player);}}
    /** Client owner thread only. Server identities and live, untracked client players remain cached. */
    public static void releaseDepartedClientViews(net.minecraft.world.level.Level currentLevel){
        synchronized(VIEWS){VIEWS.keySet().removeIf(player->player.level().isClientSide
                &&(player.isRemoved()||player.level()!=currentLevel));}
    }
    public static PlayerViewB173 peek(Player player){return VIEWS.get(player);}
    /** Refresh unchanged health without syncing over pending original inventory/placement writes. */
    public void mcc$prepareNativeHeal(){if(health==beforeHealth)beforeHealth=health=Math.round(player.getHealth());}
    public static void clear(){synchronized(VIEWS){VIEWS.clear();}}

    /** Client render snapshot only; gameplay callbacks keep their existing placement/write-back boundary. */
    public void mcc$prepareRenderView(){
        prevX=player.xo;prevY=player.yo+standingEyeHeight;prevZ=player.zo;
        prevYaw=player.yRotO;prevPitch=player.xRotO;
        bodyYaw=player.yBodyRot;lastBodyYaw=player.yBodyRotO;
        lastWalkAnimationSpeed=player.walkAnimation.speed(0);walkAnimationSpeed=player.walkAnimation.speed(1);
        walkAnimationProgress=player.walkAnimation.position(1);
        prevHorizontalSpeed=player.walkDistO;horizontalSpeed=player.walkDist;
        prevStepBobbingAmount=player.oBob;stepBobbingAmount=player.bob;
        capeX=player.xCloak;capeY=player.yCloak+standingEyeHeight;capeZ=player.zCloak;
        prevCapeX=player.xCloakO;prevCapeY=player.yCloakO+standingEyeHeight;prevCapeZ=player.zCloakO;
    }

    private void sync() {
        mcc$syncSleeping(player.isSleeping());beforeSleeping=sleeping;sleepTimer=player.getSleepTimer();
        sleepingPos=player.getSleepingPos().map(p->new legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i(p.getX(),p.getY(),p.getZ())).orElse(null);
        world = World.mcc$wrap(player.level());
        dimensionId=DimensionsB173.worldId(player.level());
        age = player.tickCount;
        prevX = x = player.getX();
        y = player.getY() + standingEyeHeight;
        prevY = y;
        prevZ = z = player.getZ();
        yaw = prevYaw = player.getYRot();
        pitch = prevPitch = player.getXRot();
        onGround = player.onGround();
        EntityViewsB173.syncCollisions(this,player);
        handSwinging=player.swinging;
        mcc$environment(PlayerAbilitiesB173.nativeWaterBaseline(player));
        var motion=player.getDeltaMovement();velocityX=motion.x;velocityY=motion.y;velocityZ=motion.z;
        mcc$movementInputs(player.xxa,player.zza,EntityFieldsB173.jumpInput(player));
        // Modern passengers do not integrate walking input into their own motion. Beta mounts read that rider motion.
        // Preserve real motion (knockback, swimming, other integrations); supply only the missing horizontal intent.
        if(player.getVehicle() instanceof B173EntityHost&&velocityX==0&&velocityZ==0)
            moveNonSolid(player.xxa*.98F,player.zza*.98F,onGround?.1F:.02F);
        health = Math.round(player.getHealth());
        beforeHealth=health;beforeAir=air=player.getAirSupply();beforeFire=fireTicks=player.getRemainingFireTicks();
        beforeFallDistance=fallDistance=player.fallDistance;
        name = player.getGameProfile().getName();
        dead = !player.isAlive();
        inventory.selectedSlot = player.getInventory().selected;
        for (int i = 0; i < 36; i++) {
            ItemStack legacy = StacksB173.toLegacy(player.getInventory().items.get(i));
            inventory.main[i] = legacy;
            before[i] = legacy == null ? null : legacy.copy();
        }
        for(int i=0;i<4;i++){
            ItemStack stack=StacksB173.toLegacy(player.getInventory().armor.get(i));
            inventory.armor[i]=stack;beforeArmor[i]=stack==null?null:stack.copy();
        }
        beforePlacement=EntityViewsB173.Placement.of(this);
    }

    /** A native world transfer keeps the original player and inventory; refresh only its placement boundary. */
    public void mcc$afterTravel(){
        world=World.mcc$wrap(player.level());dimensionId=DimensionsB173.worldId(player.level());
        setPosition(player.getX(),player.getY()+standingEyeHeight,player.getZ());
        prevX=x;prevY=y;prevZ=z;prevYaw=yaw=player.getYRot();prevPitch=pitch=player.getXRot();
        onGround=player.onGround();beforePlacement=EntityViewsB173.Placement.of(this);
    }

    /** Applies the mod's changes of the main inventory slots to the modern player. */
    public void writeBack() {
        if(beforePlacement!=null)beforePlacement.apply(this,player);
        beforePlacement=EntityViewsB173.Placement.of(this);
        writeSleepBack();
        if(health!=beforeHealth){PlayerHealthCapacityB173.commit(player,beforeHealth,health);player.setHealth(health);beforeHealth=health;}
        if(air!=beforeAir){player.setAirSupply(air);beforeAir=air;}
        if(fireTicks!=beforeFire){player.setRemainingFireTicks(fireTicks);beforeFire=fireTicks;}
        if(fallDistance!=beforeFallDistance){player.fallDistance=fallDistance;beforeFallDistance=fallDistance;}
        for (int i = 0; i < 36; i++) {
            ItemStack now = inventory.main[i];
            ItemStack was = before[i];
            if (same(now, was)) continue;
            player.getInventory().items.set(i, now == null || now.count <= 0 ? net.minecraft.world.item.ItemStack.EMPTY : StacksB173.toModern(now));
            before[i] = now == null ? null : now.copy();
        }
        for(int i=0;i<4;i++){
            ItemStack now=inventory.armor[i];if(same(now,beforeArmor[i]))continue;
            player.getInventory().armor.set(i,now==null||now.count<=0?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(now));
            beforeArmor[i]=now==null?null:now.copy();
        }
    }

    private void writeSleepBack(){
        boolean changed=sleeping!=beforeSleeping;
        if(!changed)return;
        BedsB173.writeBack(this,player,beforeSleeping);beforeSleeping=player.isSleeping();
        sleeping=beforeSleeping;
        if(changed&&beforeSleeping){
            setPosition(player.getX(),player.getY()+standingEyeHeight,player.getZ());
            beforePlacement=EntityViewsB173.Placement.of(this);
        }
    }
    /** Original direct-field sleepers call this before their block callback returns. */
    public static void updateSleepingPlayers(net.minecraft.world.level.Level level){
        for(var view:VIEWS.values())if(view.player.level()==level)view.writeSleepBack();
        if(level instanceof net.minecraft.server.level.ServerLevel server)server.updateSleepingPlayerList();
    }

    private static boolean same(ItemStack a, ItemStack b) {
        if (a == null || a.count <= 0) return b == null || b.count <= 0;
        return b != null && a.itemId == b.itemId && a.count == b.count && a.getDamage() == b.getDamage() && StacksB173.sameComponents(a,b);
    }

    @Override
    public boolean isSneaking() {
        return player.isShiftKeyDown();
    }

}
