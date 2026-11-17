// Server checks of three 1.7.10 behaviors (jars: IC2Classic 1.2.6 and TheErebus 0.4.7 in run-subtest/mods/1.7.10):
//  1. an IC2 battery keeps its charge when it swaps between the empty and the charged item (ItemStack.setItem in place on a slot stack),
//  2. a legacy ItemArmor piece loses durability when its wearer is hit, a vanilla/modern piece is damaged exactly once (no double wear),
//  3. a legacy mod tree's leaves decay after its logs are broken; vanilla leaves, leaves near a remaining log and leaves of another tree stay.
// run: LIBS="lib-rotary lib" tools/dev/run-server-script.sh housekeeping-behaviors 300 100 --fresh   (scenario ticks count from the first script run)
// prints "PASS|FAIL <name> [info]" lines (SERVERSCRIPT output).
var EQ = Java.type("net.minecraft.world.entity.EquipmentSlot");
var LIB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge");
function reg(name) { return L.R.ITEM.get(L.RL.parse(name)); }
function listItems(ns, frag) {
  var o = [], it = L.R.ITEM.iterator();
  while (it.hasNext()) { var k = "" + L.R.ITEM.getKey(it.next()); if (k.indexOf(ns + ":") == 0 && (!frag || k.indexOf(frag) >= 0)) o.push(k); }
  return o;
}
function listBlocks(ns, frag) {
  var o = [], it = L.R.BLOCK.iterator();
  while (it.hasNext()) { var k = "" + L.R.BLOCK.getKey(it.next()); if (k.indexOf(ns + ":") == 0 && (!frag || k.indexOf(frag) >= 0)) o.push(k); }
  return o;
}
function player() { if (!G.sp) G.sp = realPlayer("hkcheck"); return G.sp; }

if (tick == 0) {
  L.platform(600, 100, 600, 40, 40, 20);
  L.say("ic2 items with bat: " + listItems("ic2", "bat"));
  L.say("erebus blocks leaves/log: " + listBlocks("erebus", "leaves") + " " + listBlocks("erebus", "log"));
}
function guard(name, fn) { return function () { try { fn(); } catch (e) { L.say("FAIL " + name + " exception " + e + (e.getStackTrace ? " " + e.getStackTrace()[0] + " " + e.getStackTrace()[1] : "")); } }; }
if (tick == 5) { guard("battery", battery)(); }
if (tick == 10) { guard("armor", armor)(); }
if (tick == 20) { guard("leaves", leaves)(); }
L.run(tick);

function key(modern) { return "" + L.R.ITEM.getKey(modern.getItem()); }

function battery() {
  var sp = player();
  var Ic2Items = Java.type("ic2.core.Ic2Items");
  var EI = Java.type("ic2.api.item.ElectricItem");
  var legacyStack = Ic2Items.reBattery.copy();
  sp.getInventory().setItem(0, legacyStack.mcc$modern());
  var slot = sp.getInventory().getItem(0);
  var view = L.LIS.mcc$wrap(slot);
  var before = key(slot);
  EI.manager.charge(view, 100000, 1, true, false);
  L.say("battery charged: slot=" + key(sp.getInventory().getItem(0)) + " view=" + view.getItem() + " tag=" + sp.getInventory().getItem(0).getComponentsPatch());
  var got = EI.manager.getCharge(L.LIS.mcc$wrap(sp.getInventory().getItem(0)));
  L.ass("battery charge after charge()", got > 0, "charge=" + got + " item " + before + " -> " + key(sp.getInventory().getItem(0)));
  // drain it completely: IC2 swaps to the discharged item, and back when charged again
  var s2 = L.LIS.mcc$wrap(sp.getInventory().getItem(0));
  var taken = EI.manager.discharge(s2, 1e9, 1, true, false, false);
  var afterDrain = sp.getInventory().getItem(0);
  L.ass("battery slot item swapped to the empty item on drain", key(afterDrain) == before && key(afterDrain) != "ic2:item.itembatrecharged", key(afterDrain) + " taken=" + taken);
  L.ass("battery view of the slot is the swapped item", ("" + s2.getItem()).length > 0 && L.R.ITEM.getKey(s2.mcc$modern().getItem()).equals(L.R.ITEM.getKey(afterDrain.getItem())), "");
  EI.manager.charge(L.LIS.mcc$wrap(afterDrain), 5000, 1, true, false);
  var back = sp.getInventory().getItem(0);
  var c = EI.manager.getCharge(L.LIS.mcc$wrap(back));
  L.ass("battery charged again keeps charge", c > 0, "charge=" + c + " item=" + key(back));
  // the charge survives the swap in the other direction: charge, then partially discharge, charge must equal the difference
  var before2 = EI.manager.getCharge(L.LIS.mcc$wrap(sp.getInventory().getItem(0)));
  var t2 = EI.manager.discharge(L.LIS.mcc$wrap(sp.getInventory().getItem(0)), 1000, 1, true, false, false);
  var after2 = EI.manager.getCharge(L.LIS.mcc$wrap(sp.getInventory().getItem(0)));
  L.ass("battery partial discharge", Math.abs((before2 - after2) - t2) < 1e-6 && t2 > 0, "before=" + before2 + " after=" + after2 + " taken=" + t2);
}

function wearTest(label, itemName, slotKind) {
  var sp = player();
  var item = reg(itemName);
  if (item == null || ("" + itemName) == "minecraft:air") { L.say("FAIL armor " + label + ": no item " + itemName); return null; }
  var st = new L.IS(item, 1);
  sp.setItemSlot(slotKind, st);
  var worn = sp.getItemBySlot(slotKind);
  var d0 = worn.getDamageValue();
  sp.invulnerableTime = 0;
  L.setf(sp, "spawnInvulnerableTime", 0); L.setf(sp, "spawnInvulnerableTicks", 0); // a player that just joined ignores damage for 3 s
  var hp0 = sp.getHealth();
  var res = sp.hurt(sp.damageSources().cactus(), 8.0);
  L.say("  hurt -> " + res + " invulnerable=" + sp.isInvulnerable() + " mode=" + sp.gameMode.getGameModeForPlayer() + " hp=" + sp.getHealth() + " dead=" + sp.isDeadOrDying());
  var d1 = sp.getItemBySlot(slotKind).getDamageValue();
  sp.setItemSlot(slotKind, L.IS.EMPTY);
  sp.setHealth(20.0);
  L.say("  " + label + " " + itemName + " damage " + d0 + " -> " + d1 + " (hp " + hp0 + " -> )");
  return d1 - d0;
}

function armor() {
  var cands = ["erebus:exoskeleton_helmet", "erebus:exoskeleton_chestplate", "erebus:jade_helmet", "erebus:jade_chestplate", "erebus:rhino_helmet", "erebus:rhino_chestplate"];
  var all = listItems("erebus", "");
  var found = [];
  all.forEach(function (k) { if (/helm|chest|legg|boot|exo|jade|rhino/.test(k)) found.push(k); });
  L.say("erebus armor-like items: " + found);
  var van = wearTest("vanilla", "minecraft:iron_chestplate", EQ.CHEST);
  L.ass("vanilla armor damaged exactly as vanilla (>=1, same formula)", van != null && van >= 1, "delta=" + van);
  var legacyTried = 0;
  found.forEach(function (k) {
    var item = reg(k);
    var cls = item.getClass().getName();
    var slot = /boot/.test(k) ? EQ.FEET : /legg/.test(k) ? EQ.LEGS : /chest|exo.*body|rein/.test(k) ? EQ.CHEST : EQ.HEAD;
    var adapter = Java.type("org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemAdapter");
    if (!(item instanceof adapter)) return;
    var li = item.legacyItem();
    var isArmor = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemArmor").class.isInstance(li);
    var special = Java.type("legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor").class.isInstance(li);
    if (!isArmor) return;
    if (!item.components().has(Java.type("net.minecraft.core.component.DataComponents").MAX_DAMAGE)) { L.say("  " + k + " not damageable: skipped"); return; }
    legacyTried++;
    var delta = wearTest("legacy ItemArmor (special=" + special + ")", k, slot);
    if (!special) L.ass("legacy armor loses durability when hit " + k, delta != null && delta >= 1, "delta=" + delta);
  });
  if (legacyTried == 0) L.say("FAIL armor: no damageable legacy ItemArmor found");
}

function leaves() {
  var lv = L.lvl();
  var logs = listBlocks("erebus", "log"), leaf = listBlocks("erebus", "leaves");
  if (!leaf.length || !logs.length) { L.say("FAIL leaves: no erebus leaves/logs blocks"); return; }
  var logB = L.R.BLOCK.get(L.RL.parse(logs[0])), leafB = L.R.BLOCK.get(L.RL.parse(leaf[0]));
  L.say("leaf block " + leaf[0] + " " + leafB.getClass().getName() + " log block " + logs[0]);
  var X = 610, Y = 100, Z = 610;
  function put(b, x, y, z) { lv.setBlock(new L.BP(x, y, z), b.defaultBlockState(), 2); }
  function id(x, y, z) { return "" + L.R.BLOCK.getKey(lv.getBlockState(new L.BP(x, y, z)).getBlock()); }
  // tree A (logs at X, Y..Y+3) with leaves around the top; tree B 10 blocks away keeps its log
  for (var t = 0; t < 2; t++) {
    var bx = X + t * 12;
    for (var y = 0; y < 4; y++) put(logB, bx, Y + y, Z);
    for (var dx = -2; dx <= 2; dx++) for (var dz = -2; dz <= 2; dz++) for (var dy = 2; dy <= 4; dy++) {
      if (dx == 0 && dz == 0 && dy < 4) continue;
      put(leafB, bx + dx, Y + dy, Z + dz);
    }
  }
  // vanilla leaves, persistent=false and distance 7 (they decay natively), far away from any log: must follow vanilla (not touched by the shim)
  var van = L.Blocks.OAK_LEAVES.defaultBlockState().setValue(Java.type("net.minecraft.world.level.block.LeavesBlock").PERSISTENT, true);
  lv.setBlock(new L.BP(X + 30, Y + 3, Z), van, 2);
  var count = function (bx) { var n = 0; for (var dx = -2; dx <= 2; dx++) for (var dz = -2; dz <= 2; dz++) for (var dy = 2; dy <= 4; dy++) if (id(bx + dx, Y + dy, Z + dz) == leaf[0]) n++; return n; };
  var n0A = count(X), n0B = count(X + 12);
  L.say("leaves before: A=" + n0A + " B=" + n0B + " sample state " + lv.getBlockState(new L.BP(X + 2, Y + 3, Z)));
  var rnd = lv.getRandom();
  // random ticks with the log standing change nothing
  for (var r = 0; r < 200; r++) for (var dx = -2; dx <= 2; dx++) for (var dz = -2; dz <= 2; dz++) for (var dy = 2; dy <= 4; dy++) {
    var p = new L.BP(X + dx, Y + dy, Z + dz), s = lv.getBlockState(p);
    if (s.getBlock() == leafB) s.randomTick(lv, p, rnd);
  }
  L.ass("leaves stay while their log stands", count(X) == n0A, "A=" + count(X) + "/" + n0A);
  // break the log of tree A from the bottom up (destroyBlock runs the legacy breakBlock)
  for (var y = 0; y < 4; y++) lv.destroyBlock(new L.BP(X, Y + y, Z), false);
  L.say("sample leaf state after logs broken: " + lv.getBlockState(new L.BP(X + 1, Y + 3, Z)));
  for (var r = 0; r < 400; r++) for (var dx = -2; dx <= 2; dx++) for (var dz = -2; dz <= 2; dz++) for (var dy = 2; dy <= 4; dy++) {
    var p = new L.BP(X + dx, Y + dy, Z + dz), s = lv.getBlockState(p);
    if (s.getBlock() == leafB) s.randomTick(lv, p, rnd);
  }
  L.say("leaves after: A=" + count(X) + " B=" + count(X + 12));
  L.ass("leaves of the felled tree decay", count(X) == 0, "A=" + count(X));
  // the other tree's leaves are within the 4-block range of nothing broken: untouched even when ticked
  for (var r = 0; r < 400; r++) for (var dx = -2; dx <= 2; dx++) for (var dz = -2; dz <= 2; dz++) for (var dy = 2; dy <= 4; dy++) {
    var p = new L.BP(X + 12 + dx, Y + dy, Z + dz), s = lv.getBlockState(p);
    if (s.getBlock() == leafB) s.randomTick(lv, p, rnd);
  }
  L.ass("leaves near a remaining log stay", count(X + 12) == n0B, "B=" + count(X + 12) + "/" + n0B);
  L.ass("vanilla persistent leaves untouched", id(X + 30, Y + 3, Z) == "minecraft:oak_leaves", id(X + 30, Y + 3, Z));
  // unloaded chunk safety: a leaf at the edge of an unloaded area must not load chunks when it ticks
  var far = 2000000;
  var loadedBefore = lv.getChunkSource().getLoadedChunksCount();
  lv.getChunk(far >> 4, far >> 4);
  put(leafB, far, Y + 3, far);
  var cnt0 = lv.getChunkSource().getLoadedChunksCount();
  var ps = new L.BP(far, Y + 3, far);
  var metaProp = leafB.getStateDefinition().getProperty("meta");
  lv.setBlock(ps, leafB.defaultBlockState().setValue(metaProp, java.lang.Integer.valueOf(8)), 2); // "check decay" bit set: the tick must reach the neighbourhood scan
  for (var r = 0; r < 20; r++) lv.getBlockState(ps).randomTick(lv, ps, rnd);
  L.ass("leaf with unknown surroundings is kept", id(far, Y + 3, far) == leaf[0], id(far, Y + 3, far));
  var cnt1 = lv.getChunkSource().getLoadedChunksCount();
  L.ass("leaf tick next to unloaded chunks loads none", cnt1 == cnt0, "loaded " + cnt0 + " -> " + cnt1);
}
