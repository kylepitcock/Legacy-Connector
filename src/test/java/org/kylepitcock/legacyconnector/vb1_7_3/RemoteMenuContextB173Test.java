package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RemoteMenuContextB173Test {
    @BeforeAll static void bootstrap(){
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        BlockEntity.create(Tile.class,"remote_menu_fixture");
    }
    public static class Tile extends BlockEntity implements Inventory {
        ItemStack stack;int progress,reads;
        @Override public void writeNbt(NbtCompound tag){super.writeNbt(tag);tag.putInt("progress",progress);}
        @Override public void readNbt(NbtCompound tag){super.readNbt(tag);progress=tag.getInt("progress");reads++;}
        public int size(){return 1;}public ItemStack getStack(int index){return stack;}
        public ItemStack removeStack(int index,int count){var result=stack;stack=null;return result;}
        public void setStack(int index,ItemStack stack){this.stack=stack;}
        public String getName(){return "fixture";}public int getMaxCountPerStack(){return 64;}
        public boolean canPlayerUse(PlayerEntity player){return true;}
    }
    static class Handler extends ScreenHandler {
        Handler(PlayerInventory inventory,Tile tile){addSlot(new Slot(tile,0,56,17));addSlot(new Slot(inventory,9,8,84));}
        @Override public boolean canUse(PlayerEntity player){return true;}
    }
    public static class Gui extends HandledScreen {
        final Tile tile;
        public Gui(PlayerInventory inventory,Tile tile){super(new Handler(inventory,tile));this.tile=tile;}
        @Override public void drawBackground(float partial){}
    }
    public static class PrivateGui extends HandledScreen {
        private PrivateGui(PlayerInventory inventory,Tile tile){super(new Handler(inventory,tile));}
        @Override public void drawBackground(float partial){}
    }
    private static final class ClientWorld extends World {
        final Map<String,BlockEntity> tiles=new HashMap<>();
        ClientWorld(){isRemote=true;}
        @Override public BlockEntity getBlockEntity(int x,int y,int z){return tiles.get(x+":"+y+":"+z);}
        void put(Tile tile){tile.world=this;tiles.put(tile.x+":"+tile.y+":"+tile.z,tile);}
    }
    private static Tile tile(int x,int progress){var tile=new Tile();tile.x=x;tile.y=64;tile.z=7;tile.progress=progress;return tile;}
    private static PlayerInventory inventory(){return new PlayerEntity(null){}.inventory;}
    private static int[][] layout(){return new int[][]{{1,0,56,17},{0,9,8,84}};}
    @Test void reconstructsOriginalPublicConstructorWithCanonicalClientTileAndInitialNbt()throws Exception{
        var inventory=inventory();var serverTile=tile(3,47);var serverGui=new Gui(inventory,serverTile);
        var context=RemoteMenuContextB173.capture(serverGui.container);
        assertEquals(1,context.getList("tiles",Tag.TAG_COMPOUND).size());
        var world=new ClientWorld();var clientTile=tile(3,0);world.put(clientTile);
        var restored=(Gui)RemoteMenuContextB173.construct(Gui.class,context,inventory,world,layout());
        assertNotNull(restored);assertSame(clientTile,restored.tile);assertSame(clientTile,restored.container.slots.getFirst().inventory);
        assertSame(inventory,restored.container.slots.get(1).inventory);assertEquals(47,clientTile.progress);assertEquals(1,clientTile.reads);
        RemoteMenuContextB173.forward(restored,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE,3));
        assertNotNull(clientTile.stack);assertEquals(1,clientTile.stack.itemId);assertEquals(3,clientTile.stack.count);
        RemoteMenuContextB173.forward(restored,0,net.minecraft.world.item.ItemStack.EMPTY);assertNull(clientTile.stack);
    }
    @Test void rejectsMissingMismatchedAmbiguousOrPrivateConstructorContexts()throws Exception{
        var inventory=inventory();var source=tile(3,1);var context=RemoteMenuContextB173.capture(new Gui(inventory,source).container);
        var world=new ClientWorld();assertNull(RemoteMenuContextB173.construct(Gui.class,context,inventory,world,layout()));
        var canonical=tile(3,0);world.put(canonical);
        assertNull(RemoteMenuContextB173.construct(PrivateGui.class,context,inventory,world,layout()));assertEquals(0,canonical.reads);
        var wrongId=context.copy();wrongId.getList("tiles",Tag.TAG_COMPOUND).getCompound(0).putString("type","other");
        assertNull(RemoteMenuContextB173.construct(Gui.class,wrongId,inventory,world,layout()));assertEquals(0,canonical.reads);
        var duplicate=context.copy();duplicate.getList("tiles",Tag.TAG_COMPOUND).add(duplicate.getList("tiles",Tag.TAG_COMPOUND).getCompound(0).copy());
        assertNull(RemoteMenuContextB173.construct(Gui.class,duplicate,inventory,world,layout()));assertEquals(0,canonical.reads);
        var wrongLayout=layout();wrongLayout[0][2]++;
        assertNull(RemoteMenuContextB173.construct(Gui.class,context,inventory,world,wrongLayout));
    }
    @Test void validatesSlotLayoutCountsAndInstalledJarOwnership(){
        var handler=new Handler(inventory(),tile(3,0));assertTrue(RemoteMenuContextB173.matches(handler,layout()));
        assertFalse(RemoteMenuContextB173.matches(handler,new int[0][]));
        assertFalse(RemoteMenuContextB173.matches(handler,new int[RemoteMenuContextB173.MAX_SLOTS+1][]));
        assertTrue(RemoteMenuContextB173.owned(Gui.class,getClass()));assertFalse(RemoteMenuContextB173.owned(Gui.class,String.class));
    }
    @Test void captureBoundsRegisteredTileContextCount(){
        var inventory=inventory();var handler=new Handler(inventory,tile(3,0));
        for(int i=1;i<=RemoteMenuContextB173.MAX_TILES;i++)handler.slots.add(new Slot(tile(i+3,i),0,0,0));
        assertTrue(RemoteMenuContextB173.capture(handler).isEmpty());
    }
    @Test void malformedOptionalContextPreservesHandoffPendingAndPlainPanelFallbacks(){
        var malformed=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            malformed.writeByte(Tag.TAG_COMPOUND);var context=RemoteMenuContextB173.read(malformed);assertNull(context);
            Object handoff=new Object(),pending=new Object();
            var selected=RemoteMenuContextB173.select(handoff,()->{throw new AssertionError("handoff has precedence");},()->{throw new AssertionError("handoff has precedence");});
            assertSame(handoff,selected.gui());assertFalse(selected.forward());
            selected=RemoteMenuContextB173.select(null,()->pending,()->{throw new AssertionError("pending has precedence");});
            assertSame(pending,selected.gui());assertTrue(selected.forward());
            selected=RemoteMenuContextB173.select(null,()->null,()->context==null?null:new Object());
            assertNull(selected.gui());assertFalse(selected.forward());
        }finally{malformed.release();}
    }
    @Test void optionalContextDecoderBoundsBytesAndAcceptsOnlyCompoundRoots(){
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            var original=RemoteMenuContextB173.capture(new Handler(inventory(),tile(3,4)));
            buffer.writeNbt(original);assertEquals(original,RemoteMenuContextB173.read(buffer));
            buffer.clear();buffer.writeNbt(new net.minecraft.nbt.ListTag());assertNull(RemoteMenuContextB173.read(buffer));
            buffer.clear();buffer.writeZero(RemoteMenuContextB173.MAX_BYTES+1);int before=buffer.readerIndex();
            assertNull(RemoteMenuContextB173.read(buffer));assertEquals(before,buffer.readerIndex(),"oversized context is rejected before NBT allocation");
        }finally{buffer.release();}
    }
    public static class InterfaceGui extends HandledScreen {
        final String title;
        public InterfaceGui(Inventory upper,Inventory lower,int tier){
            super(new Handler((PlayerInventory)upper,(Tile)lower));
            title=switch(tier){case 17->"seventeen";case 42->"forty-two";default->"other";};
        }
        @Override public void drawBackground(float partial){}
    }
    public static class UnsupportedGui extends HandledScreen {
        public UnsupportedGui(Inventory upper,Inventory lower,Object arbitrary){super(new Handler((PlayerInventory)upper,(Tile)lower));}
        @Override public void drawBackground(float partial){}
    }
    @Test void recordedInventoryInterfacesAndComputedPrimitiveStateReconstructCanonicalInventories()throws Exception{
        var player=inventory();var source=tile(3,47);var gui=new InterfaceGui(player,source,42);
        MenuConstructorStateB173.record(gui,InterfaceGui.class,"(Llegacyconnector/vb1_7_3/net/minecraft/inventory/Inventory;Llegacyconnector/vb1_7_3/net/minecraft/inventory/Inventory;I)V",new Object[]{player,source,42});
        var context=RemoteMenuContextB173.capture(gui);assertTrue(context.contains("constructor",Tag.TAG_COMPOUND));
        assertEquals(42,context.getCompound("constructor").getList("arguments",Tag.TAG_COMPOUND).getCompound(2).getInt("value"));
        var world=new ClientWorld();var canonical=tile(3,0);world.put(canonical);
        var restored=(InterfaceGui)RemoteMenuContextB173.construct(InterfaceGui.class,context,player,world,layout());
        assertNotNull(restored);assertEquals("forty-two",restored.title);assertSame(player,restored.container.slots.get(1).inventory);assertSame(canonical,restored.container.slots.getFirst().inventory);assertEquals(47,canonical.progress);
        var bad=context.copy();bad.getCompound("constructor").putString("descriptor","(Ljava/lang/Object;)V");int reads=canonical.reads;
        assertNull(RemoteMenuContextB173.construct(InterfaceGui.class,bad,player,world,layout()));assertEquals(reads,canonical.reads);
        bad=context.copy();bad.getCompound("constructor").getList("arguments",Tag.TAG_COMPOUND).getCompound(2).putString("kind","java.lang.String");
        assertNull(RemoteMenuContextB173.construct(InterfaceGui.class,bad,player,world,layout()));assertEquals(reads,canonical.reads);
    }
    @Test void unsupportedArgumentsAndMissingCaptureKeepFallbackWithoutGuessingMetadata()throws Exception{
        var player=inventory();var source=tile(3,1);var world=new ClientWorld();world.put(tile(3,0));
        var gui=new InterfaceGui(player,source,17);
        assertFalse(RemoteMenuContextB173.capture(gui).contains("constructor"));
        assertNull(RemoteMenuContextB173.construct(InterfaceGui.class,RemoteMenuContextB173.capture(gui),player,world,layout()));
        var unsupported=new UnsupportedGui(player,source,new Object());
        MenuConstructorStateB173.record(unsupported,UnsupportedGui.class,"(Llegacyconnector/vb1_7_3/net/minecraft/inventory/Inventory;Llegacyconnector/vb1_7_3/net/minecraft/inventory/Inventory;Ljava/lang/Object;)V",new Object[]{player,source,new Object()});
        assertFalse(RemoteMenuContextB173.capture(unsupported).contains("constructor"));
        assertNull(RemoteMenuContextB173.construct(UnsupportedGui.class,RemoteMenuContextB173.capture(unsupported),player,world,layout()));
        var machine=new Gui(player,source);assertNotNull(RemoteMenuContextB173.construct(Gui.class,RemoteMenuContextB173.capture(machine),player,world,layout()));
    }

}
