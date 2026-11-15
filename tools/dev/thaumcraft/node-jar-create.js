// Issue #152: after node-jar.js and a modern-placed 3x3x3 (glass, node in the middle, slabs on top): createNodeJar with a silverwood wand. Result 2026-11-15: true, blockJar with TileJarNode.
var P4 = FAKE != null ? FAKE.p : join("jartest");
var Sup4 = Java.extend(Java.type("java.util.function.Supplier"), { get: function () {
  var WM = Java.type("thaumcraft.common.items.wands.WandManager");
  var IWC = Java.type("thaumcraft.common.items.wands.ItemWandCasting");
  var WR = Java.type("thaumcraft.api.wands.WandRod"), WC = Java.type("thaumcraft.api.wands.WandCap"), Asp = Java.type("thaumcraft.api.aspects.Aspect");
  var CI = Java.type("thaumcraft.common.config.ConfigItems");
  var LP = EntityBridge.wrap(P4);
  var x=2,z=2,y=0; for (var yy=-64;yy<200;yy++) if (String(w.getBlock(3,yy,3)).indexOf('blockAiry')>=0) y=yy;
  var o=[];
  var st = new LItemStack(CI.itemWandCasting, 1, 0);
  var wand = st.getItem();
  wand.setRod(st, WR.rods.get("silverwood")); wand.setCap(st, WC.caps.get("thaumium"));
  var prim = Asp.getPrimalAspects();
  for (var k=0;k<prim.size();k++) wand.addRealVis(st, prim.get(k), 100000, true);
  o.push("vis air="+wand.getVis(st, Asp.AIR)+" max="+wand.getMaxVis(st));
  o.push("blockAt node="+w.getBlock(x+1,y,z+1)+" glass="+w.getBlock(x,y+1,z));
  o.push("createNodeJar="+WM.createNodeJar(st, LP, w, x, y+1, z));
  o.push("after: "+w.getBlock(x+1,y,z+1)+" te="+w.getTileEntity(x+1,y,z+1)+" glass now="+w.getBlock(x,y+1,z)+" top="+w.getBlock(x,y+2,z));
  return o.join("\n");
}});
""+server.submit(new Sup4()).get()
