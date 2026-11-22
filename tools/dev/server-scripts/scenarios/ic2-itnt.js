// IC2 Classic industrial TNT / nuke: the ways a player ignites them (punch it, flint and steel, redstone torch) and what happens (1.7.10).
var Y0 = 150, X0 = 2600, Z0 = 2600;
var GM = Java.type("net.minecraft.world.level.GameType");
function count(r) {
  var n = 0;
  for (var x = X0 - r; x <= X0 + r; x++) for (var y = Y0 - r; y < Y0 + r; y++) for (var z = Z0 - r; z <= Z0 + r; z++)
    if (!L.lvl().getBlockState(new L.BP(x, y, z)).isAir()) n++;
  return n;
}
function ents() {
  var AABB = Java.type("net.minecraft.world.phys.AABB");
  var l = L.lvl().getEntities(null, new AABB(X0 - 30, Y0 - 30, Z0 - 30, X0 + 30, Y0 + 30, Z0 + 30));
  var o = []; for (var i = 0; i < l.size(); i++) o.push("" + l.get(i).getType().toString().replace("entity.", "")); return o.join(",");
}
function setup() {
  for (var x = X0 - 8; x <= X0 + 8; x++) for (var y = Y0; y < Y0 + 8; y++) for (var z = Z0 - 8; z <= Z0 + 8; z++) L.lvl().setBlock(new L.BP(x, y, z), L.Blocks.STONE.defaultBlockState(), 2);
}
if (tick == 0) {
  L.platform(X0 - 14, Y0, Z0 - 14, 28, 28, 14);
  setup();
  G.n0 = count(10);
  G.itnt = L.R.BLOCK.get(L.RL.parse("ic2:industrialtnt"));
  G.nuke = L.R.BLOCK.get(L.RL.parse("ic2:nuke"));
}
if (tick == 20) {
  var p = L.fp(); p.setGameMode(GM.SURVIVAL);
  var pos = new L.BP(X0, Y0 + 8, Z0);
  L.lvl().setBlock(pos, G.itnt.defaultBlockState(), 3);
  p.teleportTo(L.lvl(), X0 + 0.5, Y0 + 9, Z0 + 3.5, 180, 0);
  var ok = p.gameMode.destroyBlock(pos);
  L.say("punch ITNT: destroy=" + ok + " now " + L.blk(X0, Y0 + 8, Z0) + " ents " + ents());
}
if (tick == 22) {
  // flint and steel
  var p = L.fp(), pos = new L.BP(X0, Y0 + 8, Z0);
  L.lvl().setBlock(pos, G.itnt.defaultBlockState(), 3);
  var fs = new L.IS(L.R.ITEM.get(L.RL.parse("minecraft:flint_and_steel")), 1);
  p.setItemInHand(L.Hand.MAIN_HAND, fs);
  var res = L.lvl().getBlockState(pos).useItemOn(fs, L.lvl(), p, L.Hand.MAIN_HAND, new L.BHR(new L.Vec3(X0 + 0.5, Y0 + 8.5, Z0 + 1), L.Dir.SOUTH, pos, false));
  L.say("flint and steel on ITNT: " + res + " now " + L.blk(X0, Y0 + 8, Z0) + " ents " + ents());
  var res2 = L.lvl().getBlockState(pos).useWithoutItem(L.lvl(), p, new L.BHR(new L.Vec3(X0 + 0.5, Y0 + 8.5, Z0 + 1), L.Dir.SOUTH, pos, false));
  L.say("empty-hand use: " + res2 + " now " + L.blk(X0, Y0 + 8, Z0));
}
if (tick == 24) {
  // redstone torch next to it
  var pos = new L.BP(X0, Y0 + 8, Z0);
  L.lvl().setBlock(pos, G.itnt.defaultBlockState(), 3);
  L.lvl().setBlock(new L.BP(X0, Y0 + 9, Z0), L.Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
  L.say("torch above ITNT placed: now " + L.blk(X0, Y0 + 8, Z0));
  L.lvl().setBlock(new L.BP(X0 + 2, Y0 + 8, Z0), L.Blocks.STONE.defaultBlockState(), 3);
  L.lvl().setBlock(new L.BP(X0 + 1, Y0 + 8, Z0), L.Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
  L.lvl().setBlock(new L.BP(X0 + 2, Y0 + 9, Z0), L.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
}
if (tick == 28) L.say("after torch/wire: ITNT block " + L.blk(X0, Y0 + 8, Z0) + " ents " + ents());
if (tick == 200) L.say("tick 200 ents " + ents() + " blocks " + G.n0 + " -> " + count(10));
L.run(tick);
