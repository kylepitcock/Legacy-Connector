var LSC = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
function desc(it) { if (it.isEmpty()) return null; var l = LSC.mcc$wrap(it); return it.getCount() + "x " + it.getItem() + " d" + (l ? l.getItemDamage() : "?") + " " + it.getComponentsPatch(); }
// the chest is read on the server thread (a server-side block entity lookup from the render thread returns null)
function chest(cx, cy, cz) {
  var f = new java.util.concurrent.CompletableFuture();
  server.execute(new java.lang.Runnable(function () {
    try { var be = server.overworld().getBlockEntity(new BP(cx, cy, cz)); var s = ""; for (var j = 0; j < be.getContainerSize(); j++) { var e = desc(be.getItem(j)); if (e) s += j + "=" + e + "; "; } f.complete(s); }
    catch (e) { f.complete("err " + e); }
  }));
  return f.get(5, java.util.concurrent.TimeUnit.SECONDS);
}
function dump(cx, cy, cz) {
  var p = server.getPlayerList().getPlayers().get(0); var iv = p.getInventory(); var s = "INV: ";
  for (var i = 0; i < iv.getContainerSize(); i++) { var d = desc(iv.getItem(i)); if (d) s += i + "=" + d + "; "; }
  s += "\nCHEST: " + chest(cx, cy, cz);
  s += "\nmode=" + p.gameMode.getGameModeForPlayer() + " pos=" + p.blockPosition();
  return s;
}
// numeric ids the layer gave the mod's items: must be the same after a restart
function ids(prefix) {
  var R = Java.type("net.minecraft.core.registries.BuiltInRegistries").ITEM; var IB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge"); var s = [];
  for (var k of R.keySet().toArray()) { var n = String(k); if (n.indexOf(prefix) === 0) { var li = IB.legacyItemOf(new (Java.type("net.minecraft.world.item.ItemStack"))(R.get(k))); s.push(n + "=" + (li ? Java.type("legacyconnector.v1_7_10.net.minecraft.item.Item").getIdFromItem(li) : "?")); } }
  s.sort(); return s.join(" ");
}
