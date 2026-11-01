var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel(); var o=[];
  var BP=Java.type("net.minecraft.core.BlockPos"), B=Java.type("net.minecraft.world.level.block.Blocks");
  var R=Java.type("net.minecraft.core.registries.BuiltInRegistries"), RL=Java.type("net.minecraft.resources.ResourceLocation");
  var IS=Java.type("net.minecraft.world.item.ItemStack"), IE=Java.type("net.minecraft.world.entity.item.ItemEntity");
  var gy=83; var x=110,z=100;
  l.setBlock(new BP(x,gy-1,z),B.LAVA.defaultBlockState(),3); // heat source under the crucible
  var cb=R.BLOCK.get(RL.parse("thaumcraft:blockmetaldevice")); l.setBlock(new BP(x,gy,z),cb.defaultBlockState(),3);
  var W=Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(l);
  var te=W.getTileEntity(x,gy,z); o.push("tile="+te);
  var FS=Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack"), FR=Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
  o.push("fill="+te.fill(null, new FS(FR.WATER,1000), true));
  return o.join("\n");
}});
""+server.submit(new Sup()).get()
