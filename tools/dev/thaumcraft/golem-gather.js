var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel();
  var BP=Java.type("net.minecraft.core.BlockPos"), B=Java.type("net.minecraft.world.level.block.Blocks");
  var R=Java.type("net.minecraft.core.registries.BuiltInRegistries"), RL=Java.type("net.minecraft.resources.ResourceLocation");
  var IS=Java.type("net.minecraft.world.item.ItemStack"), IE=Java.type("net.minecraft.world.entity.item.ItemEntity");
  var Hit=Java.type("net.minecraft.world.phys.BlockHitResult"), V=Java.type("net.minecraft.world.phys.Vec3"), Dir=Java.type("net.minecraft.core.Direction"), Hand=Java.type("net.minecraft.world.InteractionHand");
  l.getAllEntities().forEach(function(e){ var t=(""+e.getType()).toLowerCase(); if(t.indexOf("thaumcraft")>=0&&t.indexOf("golem")>=0||t.indexOf("minecraft:item")>=0&&(""+e.getItem()).indexOf("cobble")>=0) e.discard() });
  var gy=83; p.setGameMode(Java.type("net.minecraft.world.level.GameType").SURVIVAL);
  p.teleportTo(106.5,gy,96.5);
  var placer=new IS(R.ITEM.get(RL.parse("thaumcraft:itemgolemplacer_1")));
  p.getInventory().setItem(0,placer); p.getInventory().selected=0;
  var r1=p.gameMode.useItemOn(p,l,placer,Hand.MAIN_HAND,new Hit(new V(100.5,gy+1,100.5),Dir.UP,new BP(100,gy,100),false));
  var g=null; l.getAllEntities().forEach(function(e){ var t=(""+e.getType()).toLowerCase(); if(t.indexOf("thaumcraft")>=0&&t.indexOf("golem")>=0) g=e });
  var o=["placer use="+r1+" golem="+(g==null?"none":g.position())];
  if(g!=null){
    var core=new IS(R.ITEM.get(RL.parse("thaumcraft:itemgolemcore_2"))); p.getInventory().setItem(1,core); p.getInventory().selected=1;
    var r2=p.interactOn(g,Hand.MAIN_HAND); o.push("core use="+r2+" core stack now="+p.getInventory().getItem(1));
    var stone=R.ITEM.get(RL.parse("minecraft:cobblestone"));
    for(var i=0;i<3;i++){ var e=new IE(l,106.5+i*0.4,gy+0.3,103.5,new IS(stone)); e.setDeltaMovement(0,0,0); l.addFreshEntity(e) }
  }
  return o.join("\n");
}});
""+server.submit(new Sup()).get()
