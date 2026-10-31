var LBlock = Java.type("legacyconnector.v1_7_10.net.minecraft.block.Block");
var LBlocks = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Blocks");
var LItem = Java.type("legacyconnector.v1_7_10.net.minecraft.item.Item");
var LItemStack = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
var LWorld = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World");
var SM = Java.type("thaumcraft.common.lib.research.ScanManager");
var BU = Java.type("thaumcraft.common.lib.utils.BlockUtils");
var ScanResult = Java.type("thaumcraft.api.research.ScanResult");
var BlockPos = Java.type("net.minecraft.core.BlockPos");
var MB = Java.type("net.minecraft.world.level.block.Blocks");
var EntityBridge = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var Connection = Java.type("net.minecraft.network.Connection");
var PacketFlow = Java.type("net.minecraft.network.protocol.PacketFlow");
var EmbeddedChannel = Java.type("io.netty.channel.embedded.EmbeddedChannel");
var GameProfile = Java.type("com.mojang.authlib.GameProfile");
var UUID = Java.type("java.util.UUID");
var ServerPlayer = Java.type("net.minecraft.server.level.ServerPlayer");
var ClientInformation = Java.type("net.minecraft.server.level.ClientInformation");
var Cookie = Java.type("net.minecraft.server.network.CommonListenerCookie");
var KeepAliveS = Java.type("net.minecraft.network.protocol.common.ServerboundKeepAlivePacket");
var KeepAliveC = Java.type("net.minecraft.network.protocol.common.ClientboundKeepAlivePacket");
var lvl = server.overworld();
var w = LWorld.mcc$wrap(lvl);
var FAKE = null;
function pump() {
  if (FAKE == null) return;
  var m; while ((m = FAKE.chan.readOutbound()) != null) {
    if (m instanceof KeepAliveC) { try { FAKE.p.connection.handleKeepAlive(new KeepAliveS(m.getId())); } catch (e) {} }
  }
}
function onTick(n) { pump(); return false; }
function join(name) {
  var conn = new Connection(PacketFlow.SERVERBOUND);
  var chan = new EmbeddedChannel(conn);
  var prof = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), name);
  var P = new ServerPlayer(server, server.overworld(), prof, ClientInformation.createDefault());
  server.getConnection().getConnections().add(conn);
  Java.type("net.neoforged.neoforge.network.registration.ChannelAttributes").getOrCreateAdHocChannels(conn).addAll(Java.type("java.util.List").of(Java.type("net.minecraft.resources.ResourceLocation").parse("legacyconnector:legacy"), Java.type("net.minecraft.resources.ResourceLocation").parse("neoforge:advanced_open_screen")));
  server.getPlayerList().placeNewPlayer(conn, P, Cookie.createInitial(prof, false));
  FAKE = { p: P, conn: conn, chan: chan };
  return P;
}
function aspStr(asp) {
  var al = "";
  if (asp != null) { var as = asp.getAspects(); for (var k = 0; k < as.length; k++) if (as[k] != null) al += as[k].getTag() + asp.getAmount(as[k]) + ","; }
  return al;
}
// what Thaumcraft's Thaumometer (ItemThaumometer.doScan) computes for the block at x,y,z
function scanAt(x, y, z) {
  var b = w.getBlock(x, y, z);
  var r = { block: String(b) };
  r.meta = w.getBlockMetadata(x, y, z);
  r.dv = b.getDamageValue(w, x, y, z);
  var pick = b.getPickBlock(null, w, x, y, z);
  var s = pick;
  if (s == null) s = BU.createStackedBlock(b, r.dv);
  var id, d;
  if (s == null) { id = LBlock.getIdFromBlock(b); d = r.dv; } else { id = LItem.getIdFromItem(s.getItem()); d = s.getItemDamage(); }
  r.id = id; r.d = d;
  var it = LItem.getItemById(id);
  r.item = String(it);
  r.name = it == null ? "NULLITEM" : new LItemStack(it, 1, d).getDisplayName();
  r.hash = it == null ? "-" : SM.generateItemHash(it, d);
  var sr = new ScanResult(1, id, d, null, "");
  r.sr = sr;
  r.aspects = aspStr(SM.getScanAspects(sr, w));
  return r;
}
function fmt(label, r) { return label + ": block=" + r.block + " m=" + r.meta + " dv=" + r.dv + " -> id=" + r.id + ":" + r.d + " item=" + r.item + " name='" + r.name + "' hash=" + r.hash + " aspects=" + r.aspects; }
