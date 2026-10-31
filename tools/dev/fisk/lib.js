// Probe library for Fisk's Superheroes on a headless server (tools/dev/server-script.sh -f tools/dev/fisk/lib.js, once per server).
// Mod-specific development tool: nothing here ships. Globals persist in the server script engine.
var Connection = Java.type("net.minecraft.network.Connection");
var PacketFlow = Java.type("net.minecraft.network.protocol.PacketFlow");
var EmbeddedChannel = Java.type("io.netty.channel.embedded.EmbeddedChannel");
var GameProfile = Java.type("com.mojang.authlib.GameProfile");
var UUID = Java.type("java.util.UUID");
var ServerPlayer = Java.type("net.minecraft.server.level.ServerPlayer");
var ClientInformation = Java.type("net.minecraft.server.level.ClientInformation");
var Cookie = Java.type("net.minecraft.server.network.CommonListenerCookie");
var EquipmentSlot = Java.type("net.minecraft.world.entity.EquipmentSlot");
var Vec3 = Java.type("net.minecraft.world.phys.Vec3");
var BlockPos = Java.type("net.minecraft.core.BlockPos");
var Blocks = Java.type("net.minecraft.world.level.block.Blocks");
var EntityBridge = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var LItemStack = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
var Vars = Java.type("com.fiskmods.heroes.common.data.var.Vars");
var Hero = Java.type("com.fiskmods.heroes.common.hero.Hero");
var HeroTracker = Java.type("com.fiskmods.heroes.common.hero.HeroTracker");
var SHPlayerData = Java.type("com.fiskmods.heroes.common.data.SHPlayerData");
var HeroIteration = Java.type("com.fiskmods.heroes.common.hero.HeroIteration");

var R = []; // result lines
function out(s) { R.push("[" + tick + "] " + s); }
function flush() { var s = R.join("\n"); R = []; return s; }

var P = null, conn = null, chan = null;
var FAKES = [];          // every fake client: {p, conn, chan, out: [packet summaries since last read]}
var KeepAliveS = Java.type("net.minecraft.network.protocol.common.ServerboundKeepAlivePacket");
var KeepAliveC = Java.type("net.minecraft.network.protocol.common.ClientboundKeepAlivePacket");
var CCPP = Java.type("net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket");
var LP = Java.type("org.kylepitcock.legacyconnector.core.network.LegacyPayload");
var BundleP = Java.type("net.minecraft.network.protocol.game.ClientboundBundlePacket");
function summarize(m) {
  if (m instanceof BundleP) { var o = [], it = m.subPackets().iterator(); while (it.hasNext()) o.push(summarize(it.next())); return o.join(", "); }
  if (m instanceof CCPP && m.payload() instanceof LP) { var pl = m.payload(); return "legacy:" + pl.channel() + "#" + pl.discriminator() + "(" + pl.data().length + ")"; }
  return String(m.getClass().getSimpleName());
}
function pump() { // answers keep-alives (a silent fake client is dropped after 15 s) and records what the server sent
  FAKES.forEach(function (f) {
    var m; while ((m = f.chan.readOutbound()) != null) {
      if (m instanceof KeepAliveC) { try { f.p.connection.handleKeepAlive(new KeepAliveS(m.getId())); } catch (e) {} }
      summarize(m).split(", ").forEach(function (k) { f.out.push(k); });
    }
  });
}
function join(name) {
  conn = new Connection(PacketFlow.SERVERBOUND);
  chan = new EmbeddedChannel(conn);
  var prof = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), name);
  P = new ServerPlayer(server, server.overworld(), prof, ClientInformation.createDefault());
  server.getConnection().getConnections().add(conn); // the connection ticks, and with it the player (PlayerTickEvent)
  Java.type("net.neoforged.neoforge.network.registration.ChannelAttributes").getOrCreateAdHocChannels(conn).addAll(Java.type("java.util.List").of(Java.type("net.minecraft.resources.ResourceLocation").parse("legacyconnector:legacy"), Java.type("net.minecraft.resources.ResourceLocation").parse("neoforge:advanced_open_screen")));
  server.getPlayerList().placeNewPlayer(conn, P, Cookie.createInitial(prof, false));
  FAKES.push({ p: P, conn: conn, chan: chan, out: [] });
  return P;
}
function fake(p) { for (var i = 0; i < FAKES.length; i++) if (FAKES[i].p === p) return FAKES[i]; return null; }
function sentSummary() { // what the server sent to the current fake client since the last call: legacy payloads as channel#discriminator(bytes), the rest by name, counted
  pump(); var f = fake(P), cnt = {}; f.out.forEach(function (k) { cnt[k] = (cnt[k] || 0) + 1; }); f.out = [];
  var r = []; for (var k in cnt) r.push(k + (cnt[k] > 1 ? " x" + cnt[k] : "")); return r.join(", ");
}
var EntityType = Java.type("net.minecraft.world.entity.EntityType");
var BuiltInRegistries = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var RL = Java.type("net.minecraft.resources.ResourceLocation");
function spawn(id, x, y, z) {
  var t = BuiltInRegistries.ENTITY_TYPE.get(RL.parse(id));
  var e = t.create(server.overworld()); e.moveTo(x, y, z, 0, 0);
  if (e.setNoAi) e.setNoAi(true);
  server.overworld().addFreshEntity(e); return e;
}
function L() { return EntityBridge.wrap(P); }          // the legacy EntityPlayer of the player
function hero(name) { return Hero.REGISTRY.getObject(name.indexOf(":") < 0 ? "fiskheroes:" + name : name); }
function wear(name) {
  var h = hero(name);
  var st = h.getDefault().createArmorStacks();
  var slots = [EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET];
  for (var i = 0; i < 4; i++) P.setItemSlot(slots[i], st[i] == null ? Java.type("net.minecraft.world.item.ItemStack").EMPTY : LItemStack.mcc$toModern(st[i]));
  return h;
}
function strip() {
  var E = Java.type("net.minecraft.world.item.ItemStack").EMPTY;
  [EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND].forEach(function (s) { P.setItemSlot(s, E); });
}
function worn() { var w = SHPlayerData.getData(L()); return w == null ? "nodata" : String(w.getWornSuit()); }
function tp(x, y, z) { // a fake client never sends its move packets, so the chunk map is told where the player is
  P.teleportTo(server.overworld(), x, y, z, 0, 0); server.overworld().getChunkSource().move(P);
  P.connection.handleAcceptTeleportPacket(new (Java.type("net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket"))(P.connection.awaitingTeleport));
}
function ticking() { return server.overworld().isPositionEntityTicking(P.blockPosition()); }
function flat(x0, z0, y) { // stone platform with air above
  var lv = server.overworld();
  for (var x = x0 - 6; x <= x0 + 6; x++) for (var z = z0 - 6; z <= z0 + 6; z++) {
    lv.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3);
    for (var h = 1; h < 8; h++) lv.setBlock(new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState(), 3);
  }
}
function pos() { return P.position().x.toFixed(2) + "," + P.position().y.toFixed(2) + "," + P.position().z.toFixed(2); }
function vel() { var d = P.getDeltaMovement(); return d.x.toFixed(3) + "," + d.y.toFixed(3) + "," + d.z.toFixed(3); }
// scenario: array of steps; a step is a function (may return a number of ticks to wait afterwards)
var STEPS = [], WAIT = 0, DONE = false;
function scenario(steps) { STEPS = steps.slice(); WAIT = 0; DONE = false; R = []; }
function onTick(n) { // always on: keeps the fake clients alive, then runs the scenario
  pump(); stepScenario(); return false;
}
function stepScenario() {
  if (WAIT > 0) { WAIT--; return false; }
  if (STEPS.length == 0) { DONE = true; return false; }
  var s = STEPS.shift();
  try { var w = s(); if (typeof w === "number") WAIT = w; }
  catch (e) { out("STEP ERROR " + e); }
  return false;
}
"lib loaded";
