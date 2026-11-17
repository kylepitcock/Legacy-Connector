var CT = Java.type("net.minecraft.world.inventory.ClickType");
var BP = Java.type("net.minecraft.core.BlockPos");
function menu() { return mc.player.containerMenu; }
function click(slot, btn, type) { mc.gameMode.handleInventoryMouseClick(menu().containerId, slot, btn, type || CT.PICKUP, mc.player); }
// find the menu slot index (in the player-inventory part of the open menu) holding an item with this registry id fragment
function findSlot(frag) {
  var m = menu();
  for (var i = m.slots.size() - 1; i >= 0; i--) { var s = m.getSlot(i); if (s.container == mc.player.getInventory() && String(s.getItem()).indexOf(frag) >= 0) return i; }
  return -1;
}
// place one of item `frag` into each grid slot listed (crafting menu: grid slots 1-9)
function put(frag, gridSlots) {
  var src = findSlot(frag);
  if (src < 0) return "missing " + frag;
  click(src, 0);
  for (var k = 0; k < gridSlots.length; k++) click(gridSlots[k], 1);
  click(src, 0);
  return "ok";
}
function takeResult() { click(0, 0, CT.QUICK_MOVE); return String(menu().getSlot(0).getItem()); }
function inv() {
  var p = server.getPlayerList().getPlayers().get(0); var s = ""; var iv = p.getInventory();
  for (var i = 0; i < iv.getContainerSize(); i++) { var it = iv.getItem(i); if (!it.isEmpty()) s += i + ":" + it + (it.getDamageValue() ? "(d" + it.getDamageValue() + ")" : "") + ", "; }
  return s;
}
function bs(x, y, z) { return String(mc.level.getBlockState(new BP(x, y, z))); }
