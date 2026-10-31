var Y0 = 150, X0 = 2300, Z0 = 2300;
var MB = Java.type("net.minecraft.world.level.block.Blocks");
var IR = Java.type("Reika.RotaryCraft.Registry.ItemRegistry");
var AABB = Java.type("net.minecraft.world.phys.AABB");
function setv(x, y, z, b) { L.lvl().setBlock(new L.BP(x, y, z), b.defaultBlockState(), 3); }
if (tick == 0) {
  L.platform(X0, Y0, Z0, 20, 20, 8);
  // farmland (hydrated) with a water block beside
  for (var x = 2; x < 8; x++) { L.lvl().setBlock(new L.BP(X0 + x, Y0 - 1, Z0 + 2), MB.FARMLAND.defaultBlockState().setValue(Java.type("net.minecraft.world.level.block.FarmBlock").MOISTURE, 7), 3); }
  setv(X0 + 1, Y0 - 1, Z0 + 2, MB.WATER);
  var seed = IR.CANOLA.getStackOf();
  L.say("canola stack: " + seed + " item=" + seed.getItem().getClass().getSimpleName());
  var r = L.place(new L.LIS(seed.getItem(), 4, seed.getItemDamage()), X0 + 3, Y0, Z0 + 2, null, 0);
  L.say("plant seeds on farmland: " + r.r + " block=" + L.blk(X0 + 3, Y0, Z0 + 2));
  // a second one on plain dirt (should fail)
  var r2 = L.place(new L.LIS(seed.getItem(), 4, seed.getItemDamage()), X0 + 12, Y0, Z0 + 2, null, 0);
  L.say("plant on stone floor: " + r2.r + " block=" + L.blk(X0 + 12, Y0, Z0 + 2));
}
if (tick > 5 && tick <= 400 && tick % 40 == 0) {
  var lv = L.lvl(), pos = new L.BP(X0 + 3, Y0, Z0 + 2), st = lv.getBlockState(pos);
  for (var i = 0; i < 300; i++) { st = lv.getBlockState(pos); if (st.isRandomlyTicking()) st.randomTick(lv, pos, lv.random); }
  L.say("t" + tick + " canola " + L.blk(X0 + 3, Y0, Z0 + 2) + " farmland-below=" + L.blk(X0 + 3, Y0 - 1, Z0 + 2));
}
if (tick == 420) {
  var lv = L.lvl(), pos = new L.BP(X0 + 3, Y0, Z0 + 2);
  var before = L.blk(X0 + 3, Y0, Z0 + 2);
  lv.destroyBlock(pos, true);
  var ents = lv.getEntities(null, new AABB(X0, Y0 - 1, Z0, X0 + 10, Y0 + 3, Z0 + 6)), drops = [];
  Java.from(ents.toArray()).forEach(function (e) { if (("" + e.getClass().getSimpleName()).indexOf("ItemEntity") >= 0) drops.push(e.getItem().getCount() + "x " + e.getItem().getItem()); });
  L.say("harvest of " + before + " dropped: " + drops.join(", "));
}
L.run(tick);
