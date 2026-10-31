// Shared helpers for -PserverScript runs: prepend this file to a script (cat lib.js script.js > run.js).
function T(n) { return Java.type(n); }
function realPlayer(name) {
  var level = server.overworld();
  var GP = T("com.mojang.authlib.GameProfile");
  var UUID = T("java.util.UUID");
  var prof = new GP(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), name);
  var CI = T("net.minecraft.server.level.ClientInformation");
  var SP = T("net.minecraft.server.level.ServerPlayer");
  var sp = new SP(server, level, prof, CI.createDefault());
  var Conn = T("net.minecraft.network.Connection");
  var PF = T("net.minecraft.network.protocol.PacketFlow");
  var conn = new Conn(PF.SERVERBOUND);
  var EC = T("io.netty.channel.embedded.EmbeddedChannel");
  var ch = new EC(conn);
  var CA = T("net.neoforged.neoforge.network.registration.ChannelAttributes");
  var RL = T("net.minecraft.resources.ResourceLocation");
  var ad = CA.getOrCreateAdHocChannels(conn);
  ["legacyconnector:legacy", "legacyconnector:legacy12", "legacyconnector:fml", "neoforge:advanced_open_screen", "neoforge:extended_server_data", "neoforge:advanced_add_entity"].forEach(function (n) { ad.add(RL.parse(n)); });
  var Cookie = T("net.minecraft.server.network.CommonListenerCookie");
  var cookie = new Cookie(prof, 0, CI.createDefault(), false);
  server.getPlayerList().placeNewPlayer(conn, sp, cookie);
  return sp;
}
function legacy(e) { return T("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge").wrap(e); }
function lworld(level) { return T("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(level); }
function P(label, fn) {
  try { var r = fn(); return label + ": " + (r === undefined ? "ok" : r) + "\n"; }
  catch (e) {
    var s = "";
    try { var je = e.getStackTrace ? e : (e.javaException ? e.javaException : null); if (je) { var st = je.getStackTrace(); for (var i = 0; i < st.length && i < 7; i++) s += "\n      at " + st[i]; } } catch (x) {}
    return label + ": EXC " + e + s + "\n";
  }
}
function placeItem(sp, level, modernStack, x, y, z) {
  // right-click the top face of block (x,y,z) with the stack, as a player would
  var Hand = T("net.minecraft.world.InteractionHand");
  var BHR = T("net.minecraft.world.phys.BlockHitResult");
  var Vec3 = T("net.minecraft.world.phys.Vec3");
  var Dir = T("net.minecraft.core.Direction");
  sp.setItemInHand(Hand.MAIN_HAND, modernStack);
  sp.setPos(x + 0.5, y + 1, z + 3.5);
  var hit = new BHR(new Vec3(x + 0.5, y + 1, z + 0.5), Dir.UP, bp(x, y, z), false);
  return sp.gameMode.useItemOn(sp, level, modernStack, Hand.MAIN_HAND, hit);
}
function modern(ls) { return T("legacyconnector.v1_7_10.net.minecraft.item.ItemStack").mcc$toModern(ls); }
function byName(n, count) {
  var RL = T("net.minecraft.resources.ResourceLocation");
  var it = T("net.minecraft.core.registries.BuiltInRegistries").ITEM.get(RL.parse(n));
  return new (T("net.minecraft.world.item.ItemStack"))(it, count || 1);
}
function placeTile(sp, level, lw, tile, x, y, z, itemName) {
  var st = itemName ? byName(itemName) : modern(tile.getCraftedProduct());
  var r = placeItem(sp, level, st, x, y - 1, z);
  return [r + " stack=" + st + " item=" + st.getItem() + " block@=" + level.getBlockState(bp(x, y, z)), lw.getTileEntity(x, y, z)];
}
function bp(x, y, z) { return T("net.minecraft.core.BlockPos").containing(x, y, z); }
