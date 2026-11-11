package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.entity.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta hostile creature superclass; original subclasses retain their own targeting and attack overrides. */
public abstract class MonsterEntity extends PathAwareEntity implements Monster {
    protected int attackDamage=2;
    public MonsterEntity(World world){super(world);health=20;}
    @Override public void tickMovement(){if(getBrightnessAtEyes(1)>.5F)despawnCounter+=2;super.tickMovement();}
    @Override public void tick(){super.tick();if(world!=null&&!world.isRemote&&world.mcc$level()!=null&&world.mcc$level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)markDead();}
    @Override protected Entity getTargetInRange(){Entity player=org.kylepitcock.legacyconnector.vb1_7_3.CombatTargetsB173.monsterClosest(this,16);return player!=null&&canSee(player)?player:null;}
    @Override public boolean damage(Entity attacker,int amount){
        if(!super.damage(attacker,amount))return false;
        if(attacker!=passenger&&attacker!=vehicle&&attacker!=this){
            Entity retaliation=org.kylepitcock.legacyconnector.vb1_7_3.CombatTargetsB173.retaliation(this,attacker);
            if(attacker==null||retaliation!=null)target=retaliation;
        }
        return true;
    }
    @Override protected void attack(Entity victim,float distance){
        if(org.kylepitcock.legacyconnector.vb1_7_3.CombatTargetsB173.allowed(this,victim)&&attackCooldown<=0&&distance<2&&victim.boundingBox.maxY>boundingBox.minY&&victim.boundingBox.minY<boundingBox.maxY){attackCooldown=20;victim.damage(this,attackDamage);}
    }
    @Override protected float getPathfindingFavor(int x,int y,int z){return .5F-world.getLightLevel(x,y,z)/15F;}
    @Override public boolean canSpawn(){
        if(world==null||world.mcc$level()==null)return false;
        var level=world.mcc$access();var pos=net.minecraft.core.BlockPos.containing(x,boundingBox.minY,z);
        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos)>random.nextInt(32))return false;
        int light=level.getMaxLocalRawBrightness(pos,world.mcc$level().isThundering()?10:level.getSkyDarken());
        return light<=random.nextInt(8)&&super.canSpawn();
    }
}
