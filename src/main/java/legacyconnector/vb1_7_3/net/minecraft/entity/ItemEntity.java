package legacyconnector.vb1_7_3.net.minecraft.entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
public class ItemEntity extends Entity {
    public ItemStack stack;
    public int pickupDelay;
    public int itemAge;
    private int health=5;
    public float initialRotationAngle=(float)(Math.random()*Math.PI*2);
    private int itemTicks;
    public ItemEntity(World world) { super(world);setBoundingBoxSpacing(.25F,.25F);standingEyeHeight=height/2;setPosition(x,y,z); }
    public ItemEntity(World world,double x,double y,double z,ItemStack stack) {
        this(world);this.stack=stack;setPosition(x,y,z);yaw=(float)(Math.random()*360);
        velocityX=(float)(Math.random()*.20000000298023224-.10000000149011612);velocityY=.20000000298023224;
        velocityZ=(float)(Math.random()*.20000000298023224-.10000000149011612);
    }
    /** Original hl.w_: no modern stack merging, empty-stack expiry or extra native item travel. */
    @Override public void tick(){
        super.tick();if(pickupDelay>0)pickupDelay--;
        prevX=x;prevY=y;prevZ=z;velocityY-=.03999999910593033;
        int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z);
        if(world.getMaterial(bx,by,bz)==legacyconnector.vb1_7_3.net.minecraft.block.material.Material.LAVA){
            velocityY=.20000000298023224;velocityX=(random.nextFloat()-random.nextFloat())*.2F;velocityZ=(random.nextFloat()-random.nextFloat())*.2F;
            world.playSound(this,"random.fizz",.4F,2+random.nextFloat()*.4F);
        }
        pushOut(x,(boundingBox.minY+boundingBox.maxY)/2,z);
        move(velocityX,velocityY,velocityZ);
        float friction=.98F;
        if(onGround){
            friction=.58800006F;int id=world.getBlockId((int)Math.floor(x),(int)Math.floor(boundingBox.minY)-1,(int)Math.floor(z));
            if(id>0&&id<legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS.length&&legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[id]!=null)
                friction=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[id].slipperiness*.98F;
        }
        velocityX*=friction;velocityY*=.9800000190734863;velocityZ*=friction;if(onGround)velocityY*= -.5;
        itemTicks++;itemAge++;if(itemAge>=6000)markDead();
    }
    private void pushOut(double x,double y,double z){
        int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z);
        if(!world.shouldSuffocate(bx,by,bz))return;
        int[][] sides={{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
        double[] distances={x-bx,1-(x-bx),y-by,1-(y-by),z-bz,1-(z-bz)};int nearest=-1;double distance=9999;
        for(int i=0;i<6;i++)if(!world.shouldSuffocate(bx+sides[i][0],by+sides[i][1],bz+sides[i][2])&&distances[i]<distance){distance=distances[i];nearest=i;}
        float speed=random.nextFloat()*.2F+.1F;
        switch(nearest){case 0->velocityX=-speed;case 1->velocityX=speed;case 2->velocityY=-speed;case 3->velocityY=speed;case 4->velocityZ=-speed;case 5->velocityZ=speed;default->{}}
    }
    @Override public boolean damage(Entity attacker,int amount){scheduleVelocityUpdate();health-=amount;if(health<=0)markDead();return false;}
    @Override public void onPlayerInteraction(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){
        if(world.isRemote||stack==null)return;int count=stack.count;
        if(pickupDelay==0&&player.inventory.addStack(stack)){
            world.playSound(this,"random.pop",.2F,((random.nextFloat()-random.nextFloat())*.7F+1)*2);
            player.sendPickup(this,count);if(stack.count<=0)markDead();
        }
    }
    @Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){
        tag.putShort("Health",(short)(byte)health);tag.putShort("Age",(short)itemAge);
        if(stack!=null)tag.put("Item",stack.writeNbt(new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound()));
    }
    @Override public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){health=tag.getShort("Health")&255;itemAge=tag.getShort("Age");stack=new ItemStack(tag.getCompound("Item"));}
}
