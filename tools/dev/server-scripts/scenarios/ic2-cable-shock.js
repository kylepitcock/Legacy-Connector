// IC2 Classic (1.7.10): a burning generator next to a bare copper cable; a pig stands on the cable. The net shocks entities near cables
// carrying more energy than their insulation absorbs (EnergyNetLocal: getEntitiesWithinAABB around the path, then attackEntityFrom(electricity)).
var Y0 = 150, X0 = 2600, Z0 = 2600;
var GM = Java.type("net.minecraft.world.level.GameType");
function itemNamed(frag, not) {
  var r = [];
  L.R.ITEM.keySet().forEach(function (k) { var s = "" + k; if (k.getNamespace() == "ic2" && s.indexOf(frag) >= 0 && (!not || s.indexOf(not) < 0)) r.push(s); });
  return r;
}
function put(name, x, z) {
  var it = L.R.ITEM.get(L.RL.parse(name)), p = L.fp(), st = new L.IS(it, 1);
  p.setGameMode(GM.CREATIVE);
  p.teleportTo(L.lvl(), x - 1.5, Y0, z + 0.5, -90, 30);
  p.setItemInHand(L.Hand.MAIN_HAND, st);
  var r = st.getItem().useOn(new L.UOC(L.lvl(), p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(x + 0.5, Y0, z + 0.5), L.Dir.UP, new L.BP(x, Y0 - 1, z), false)));
  L.say(name + " at " + x + "," + z + " -> " + r + " " + L.blk(x, Y0, z));
}
if (tick == 0) {
  L.platform(X0 - 4, Y0, Z0 - 4, 12, 12, 6);
  L.say("gener: " + itemNamed("gener").join(" "));
  L.say("cable: " + itemNamed("cable").join(" "));
}
if (tick == 20) {
  var AB = Java.type("net.minecraft.world.phys.AABB"), Pg = Java.type("net.minecraft.world.entity.animal.Pig");
  var old = L.lvl().getEntitiesOfClass(Pg.class, new AB(X0 - 8, Y0 - 3, Z0 - 8, X0 + 8, Y0 + 5, Z0 + 8));
  L.say("old pigs " + old.size());
  for (var i = 0; i < old.size(); i++) old.get(i).discard();
  var gen = itemNamed("generator", "geo").filter(function (n) { return n.indexOf("solar") < 0 && n.indexOf("wind") < 0 && n.indexOf("water") < 0 && n.indexOf("thermal") < 0 && n.indexOf("semi") < 0; })[0] || "ic2:generator";
  var cab = "ic2:item.ic2cable_5";
  L.say("using " + gen + " / " + cab);
  put(gen, X0, Z0);
  put(cab, X0 + 1, Z0);
  put("ic2:blockelectric", X0 + 2, Z0);
  var t = L.te(X0, Y0, Z0);
  G.gen = t;
  L.say("gen tile " + (t ? t.getClass().getName() : null));
  G.pig = Java.type("net.minecraft.world.entity.EntityType").PIG.create(L.lvl());
  G.pig.moveTo(X0 + 1.5, Y0, Z0 + 0.5, 0, 0);
  if (typeof realPlayer !== "undefined" && typeof USE_PLAYER !== "undefined") {
    // lib.js prepended and USE_PLAYER set: a real survival player stands on the cable instead of the pig
    G.pig = realPlayer("Shocked");
    G.pig.setGameMode(GM.SURVIVAL);
    G.pig.teleportTo(L.lvl(), X0 + 1.5, Y0, Z0 + 0.5, 0, 0);
  } else L.lvl().addFreshEntity(G.pig);
  G.hp0 = G.pig.getHealth();
  // fuel
  try {
    var coal = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack").mcc$wrap(new L.IS(L.R.ITEM.get(L.RL.parse("minecraft:coal")), 16));
    t.setInventorySlotContents(1, coal);
    L.say("fuel slot: " + t.getStackInSlot(1));
  } catch (e) { L.say("fuel ERR " + e); }
}
if (tick > 20 && tick % 20 == 0 && G.gen) {
  try {
    L.say("t" + tick + " gen fuel=" + L.fs(G.gen, "fuel") + " storage=" + L.fs(G.gen, "storage") + " active=" + L.fs(G.gen, "active") + " slot1=" + G.gen.getStackInSlot(1) + " tslau=" + L.fs(G.gen, "ticksSinceLastActiveUpdate") + " invalid=" + L.call(G.gen, "isInvalid") + " entityTicking=" + L.lvl().isPositionEntityTicking(new L.BP(X0, Y0, Z0)) + " be=" + L.lvl().getBlockEntity(new L.BP(X0, Y0, Z0)) + " jv=" + L.call(G.gen, "isConverting") + " | pig hp " + G.pig.getHealth() + "/" + G.hp0);
    var cabT = L.te(X0 + 1, Y0, Z0);
    if (cabT) L.say("cable tile addedToEnergyNet=" + L.fs(cabT, "addedToEnergyNet"));
  } catch (e) { L.say("ERR " + e); }
}
if (tick == 60 && G.gen) {
  try {
    L.say("needsFuel " + L.call(G.gen, "needsFuel") + " burn(coal) " + Java.type("legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFurnace").getItemBurnTime(G.gen.getStackInSlot(1)));
    L.say("gainFuel " + L.call(G.gen, "gainFuel") + " fuel now " + L.fs(G.gen, "fuel") + " slot0 " + G.gen.getStackInSlot(1));
  } catch (e) { L.say("ERR2 " + e); }
}
if (tick == 400 && G.gen) {
  try {
    var W = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(L.lvl());
    var AABB = Java.type("legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB");
    var ELB = Java.type("legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase");
    var box = AABB.getBoundingBox(X0 + 0, Y0 - 1, Z0 - 1, X0 + 3, Y0 + 2, Z0 + 2);
    var l = W.getEntitiesWithinAABB(ELB.class, box);
    L.say("entities in box (legacy): " + l.size() + " " + l);
    if (l.size() > 0) {
      var e = l.get(0);
      var bb = Java.type("legacyconnector.v1_7_10.net.minecraft.entity.Entity").mcc$get$boundingBox(e);
      L.say("pig bb " + bb + " intersects " + bb.intersectsWith(box));
      var DS = Java.type("ic2.core.IC2DamageSource");
      L.say("DS " + DS.electricity + " hp before " + G.pig.getHealth());
      var r = e.attackEntityFrom(DS.electricity, 5.0);
      L.say("attack -> " + r + " hp after " + G.pig.getHealth());
    }
  } catch (e) { L.say("ERR3 " + e); if (e.getStackTrace) { var a = e.getStackTrace(); for (var k = 0; k < 8; k++) L.say("  at " + a[k]); } }
}
if (tick == 70 && G.gen) {
  try {
    var ct = L.te(X0 + 1, Y0, Z0);
    L.say("cable insAbs=" + L.call(ct, "getInsulationEnergyAbsorption") + " insBreak=" + L.call(ct, "getInsulationBreakdownEnergy") + " condBreak=" + L.call(ct, "getConductorBreakdownEnergy") + " type=" + L.fs(ct, "cableType") + " color=" + L.fs(ct, "color") + " foamed=" + L.fs(ct, "foamed"));
    var EN = Java.type("ic2.api.energy.EnergyNet");
    var net = EN.instance.getForWorld(Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(L.lvl()));
    L.say("net " + net.getClass().getName() + " boxes " + net.getBoxes());
  } catch (e) { L.say("ERR4 " + e); }
}
L.run(tick);
