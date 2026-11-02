// AbyssalCraft 1.12.2 machines: places them with setBlock, loads inputs and prints the tile fields over time (ticking check).
var Y0 = 150, X0 = 1300, Z0 = 1300;
var LItem = Java.type("legacyconnector.v1_12_2.net.minecraft.item.Item");
var LIS = Java.type("legacyconnector.v1_12_2.net.minecraft.item.ItemStack");
var MACH = ["transmutator", "crystallizer", "engraver", "materializer"];
function it(n, c, m) { var i = LItem.getByNameOrId(n); return i == null ? null : new LIS(i, c || 1, m || 0); }
function put(t, slot, st) { if (st == null) { L.say("missing item for slot " + slot); return; } t.setInventorySlotContents(slot, st); }
if (tick == 0) {
  L.platform(X0, Y0, Z0, 20, 6, 6);
  MACH.forEach(function (n, i) {
    var b = L.R.BLOCK.get(L.RL.fromNamespaceAndPath("abyssalcraft", n));
    L.lvl().setBlock(new L.BP(X0 + 2 + i * 3, Y0, Z0 + 2), b.defaultBlockState(), 3);
  });
  L.fp().teleportTo(L.lvl(), X0 + 6, Y0, Z0 + 5, 0, 0);
}
if (tick == 20) {
  MACH.forEach(function (n, i) {
    var t = L.te(X0 + 2 + i * 3, Y0, Z0 + 2);
    L.say(n + " -> " + (t ? t.getClass().getName() : "NO TILE") + " " + L.blk(X0 + 2 + i * 3, Y0, Z0 + 2));
  });
  var t0 = L.te(X0 + 2, Y0, Z0 + 2);
  if (t0) { put(t0, 0, it("abyssalcraft:antibeef")); put(t0, 1, it("minecraft:blaze_powder")); }
  var t1 = L.te(X0 + 5, Y0, Z0 + 2);
  if (t1) { put(t1, 0, it("minecraft:iron_ingot")); put(t1, 1, it("minecraft:coal")); put(t1, 2, it("minecraft:blaze_powder")); }
  var t2 = L.te(X0 + 8, Y0, Z0 + 2);
  if (t2) { put(t2, 0, it("minecraft:iron_ingot")); put(t2, 1, it("minecraft:coal")); }
  var t3 = L.te(X0 + 11, Y0, Z0 + 2);
  if (t3) { put(t3, 0, it("abyssalcraft:crystalshard")); }
}
if (tick == 21 || tick == 100 || tick == 200 || tick == 500) {
  MACH.forEach(function (n, i) {
    var t = L.te(X0 + 2 + i * 3, Y0, Z0 + 2);
    if (t) L.say("t" + tick + " " + n + ": " + L.dump(t) + " | " + L.slots(t));
  });
}
L.run(tick);
