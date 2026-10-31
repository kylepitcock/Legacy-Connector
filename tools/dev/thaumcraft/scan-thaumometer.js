// Thaumometer scan audit (Thaumcraft 4.2.3.5). Needs a server with -PserverScript=run-subtest/probe -PserverScriptMod=thaumcraft; submit lib.js first, then this file
// (tools/dev/server-script.sh -f tools/dev/thaumcraft/lib.js). Runs the real ItemThaumometer.doScan against a placed block under a fake player looking down
// and prints PASS/FAIL against what 1.7.10 shows (item name of the scan result; aspects where Thaumcraft registers some).
var R = [];
if (typeof P == "undefined" || P == null) { P = join("scanner"); }
var LP = EntityBridge.wrap(P);
var TH = Java.type("thaumcraft.common.items.relics.ItemThaumometer");
var Items = Java.type("thaumcraft.common.config.ConfigItems");
var thaum = new LItemStack(Items.itemThaumometer, 1, 0);
var m = TH.class.getDeclaredMethod("doScan", LItemStack.class, LWorld.class, Java.type("legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer").class, Java.type("int").class);
m.setAccessible(true);
function scanLook(label, setup) {
  var y = 110;
  // stone floor and clear air above
  for (var dx = 6; dx <= 10; dx++) for (var dz = 6; dz <= 10; dz++) { lvl.setBlock(new BlockPos(dx, y - 1, dz), MB.STONE.defaultBlockState(), 3); for (var dy = 0; dy < 4; dy++) lvl.setBlock(new BlockPos(dx, y + dy, dz), MB.AIR.defaultBlockState(), 3); }
  setup(8, y, 8);
  P.setPos(8.5, y + 2.0, 8.5);
  P.setXRot(90); P.setYRot(0);
  var sr;
  try { sr = m.invoke(Items.itemThaumometer, thaum, w, LP, 0); } catch (e) { R.push(label + ": doScan EXC " + (e.getCause ? e.getCause() : e)); return; }
  if (sr == null) { R.push(label + ": doScan -> null"); return; }
  var it = LItem.getItemById(sr.id);
  var nm = it == null ? "NULLITEM" : new LItemStack(it, 1, sr.meta).getDisplayName();
  var a = ""; try { a = aspStr(SM.getScanAspects(sr, w)); } catch (e2) { a = "EXC " + e2; }
  var done = ""; try { done = SM.completeScan(LP, sr, "@"); } catch (e3) { done = "completeScan EXC " + e3; }
  R.push(label + ": type=" + sr.type + " id=" + sr.id + " meta=" + sr.meta + " phen='" + sr.phenomena + "' -> '" + nm + "' aspects=" + a + " completeScan=" + done);
}
function put(b, meta) { return function (x, y, z) { w.setBlock(x, y, z, b, meta, 3); }; }
[["oak", 0], ["spruce", 1], ["birch", 2], ["jungle", 3], ["spruce-x", 5], ["birch-z", 10], ["jungle-bark", 15]].forEach(function (e) { scanLook("log " + e[0], put(LBlocks.log, e[1])); });
[["acacia", 0], ["darkoak", 1], ["darkoak-z", 9]].forEach(function (e) { scanLook("log2 " + e[0], put(LBlocks.log2, e[1])); });
scanLook("stone", put(LBlocks.stone, 0));
scanLook("granite", put(LBlocks.stone, 1));
scanLook("water", function (x, y, z) { lvl.setBlock(new BlockPos(x, y, z), MB.WATER.defaultBlockState(), 3); });
scanLook("lava", function (x, y, z) { lvl.setBlock(new BlockPos(x, y, z), MB.LAVA.defaultBlockState(), 3); });
scanLook("node", put(LBlock.getBlockFromName("Thaumcraft:blockAiry"), 0));
scanLook("frosted_ice", function (x, y, z) { lvl.setBlock(new BlockPos(x, y, z), MB.FROSTED_ICE.defaultBlockState(), 3); });
scanLook("end_gateway", function (x, y, z) { lvl.setBlock(new BlockPos(x, y, z), MB.END_GATEWAY.defaultBlockState(), 3); });
scanLook("air", function (x, y, z) { });
var WANT = {"log oak":"Oak Log","log spruce":"Spruce Log","log birch":"Birch Log","log jungle":"Jungle Log","log spruce-x":"Spruce Log","log birch-z":"Birch Log","log jungle-bark":"Jungle Log",
  "log2 acacia":"Acacia Log","log2 darkoak":"Dark Oak Log","log2 darkoak-z":"Dark Oak Log","stone":"Stone","granite":"Granite","water":"Water","lava":"Lava","frosted_ice":"Frosted Ice"};
R = R.map(function (line) { var label = line.substring(0, line.indexOf(":")); var want = WANT[label]; return (line.indexOf("doScan -> null") >= 0 ? "SKIP (already scanned by this player; restart the server to repeat) " : want == null ? "     " : (line.indexOf("-> '" + want + "'") >= 0 ? "PASS " : "FAIL ")) + line; });
R.join(String.fromCharCode(10));
