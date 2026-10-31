var R = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var YAW = -90;
var RL = Java.type("net.minecraft.resources.ResourceLocation");
var BP = Java.type("net.minecraft.core.BlockPos");
var Blocks = Java.type("net.minecraft.world.level.block.Blocks");
var IS = Java.type("net.minecraft.world.item.ItemStack");
var Hand = Java.type("net.minecraft.world.InteractionHand");
var BHR = Java.type("net.minecraft.world.phys.BlockHitResult");
var Vec3 = Java.type("net.minecraft.world.phys.Vec3");
var Dir = Java.type("net.minecraft.core.Direction");
var UOC = Java.type("net.minecraft.world.item.context.UseOnContext");
var LIS = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
var Sys = Java.type("java.lang.System");
function G(k, v) { if (v !== undefined) Sys.getProperties().put(k, v); return Sys.getProperties().get(k); }
function lvl() { return server.overworld(); }
function fp() { return Java.type("net.neoforged.neoforge.common.util.FakePlayerFactory").getMinecraft(lvl()); }
function bp(x, y, z) { return new BP(x, y, z); }
function setBlock(x, y, z, b) { lvl().setBlock(bp(x, y, z), b.defaultBlockState(), 3); }
function flat(X, Y, Z, w, d, h) {
  var lv = lvl();
  for (var cx = (X - 2) >> 4; cx <= (X + w + 2) >> 4; cx++) for (var cz = (Z - 2) >> 4; cz <= (Z + d + 2) >> 4; cz++) lv.getChunk(cx, cz);
  server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "forceload add " + (X - 2) + " " + (Z - 2) + " " + (X + w + 2) + " " + (Z + d + 2));
  for (var x = X - 1; x < X + w + 1; x++) for (var z = Z - 1; z < Z + d + 1; z++) {
    for (var dy = 0; dy < (h || 5); dy++) lv.setBlock(bp(x, Y + dy, z), Blocks.AIR.defaultBlockState(), 2);
    lv.setBlock(bp(x, Y - 1, z), Blocks.STONE.defaultBlockState(), 2);
  }
}
// legacy stack -> modern stack
function ms(ls) { return new LIS(ls.getItem(), 1, ls.getItemDamage()).mcc$modern(); }
function modItem(id, n) { return new IS(R.ITEM.get(RL.parse(id)), n || 1); }
function place(st, x, Y, z, face, onY) {
  var lv = lvl(), p = fp();
  p.teleportTo(lv, x - 1.5, Y, z + 0.5, YAW, 30); p.setYRot(YAW); p.setYHeadRot(YAW); p.yBodyRot = YAW; p.setXRot(30);
  p.setItemInHand(Hand.MAIN_HAND, st);
  var r = st.getItem().useOn(new UOC(lv, p, Hand.MAIN_HAND, st, new BHR(new Vec3(x + 0.5, Y, z + 0.5), face || Dir.UP, new BP(x, (onY === undefined ? Y - 1 : onY), z), false)));
  return "" + r;
}
function te(x, y, z) { var be = lvl().getBlockEntity(bp(x, y, z)); return be && be.legacy ? be.legacy() : null; }
var MH = Java.type("java.lang.invoke.MethodHandles"), MT = Java.type("java.lang.invoke.MethodType");
var GETCLS = MH.publicLookup().findVirtual(Java.type("java.lang.Object").class, "getClass", MT.methodType(Java.type("java.lang.Class").class));
function CLS(t) { return GETCLS.invokeWithArguments(Java.to([t], "java.lang.Object[]")); }
function f(t, n) { if (t == null) return "null"; for (var k = CLS(t); k != null; k = k.getSuperclass()) { try { var x = k.getDeclaredField(n); x.setAccessible(true); return x.get(t); } catch (e) {} } return "?" + n; }
function fs(t, names) { return names.map(function (n) { return n + "=" + String(f(t, n)).split("\n").join(" ").substring(0, 160); }).join(" "); }
function giveFp(st) { fp().getInventory().add(st); }
function cmd(c) { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c); }
function rightClick(x, y, z, st, face) {
  var lv = lvl(), p = fp();
  p.teleportTo(lv, x + 0.5, y + 2, z + 0.5, 0, 0);
  p.setItemInHand(Hand.MAIN_HAND, st || IS.EMPTY);
  var hit = new BHR(new Vec3(x + 0.5, y + 0.5, z + 0.5), face || Dir.UP, bp(x, y, z), false);
  var bs = lv.getBlockState(bp(x, y, z));
  return "" + bs.useItemOn(p.getItemInHand(Hand.MAIN_HAND), lv, p, Hand.MAIN_HAND, hit);
}
// click the (cx,cy,cz) block on its `face` side; the item is placed at the neighbour (like a player clicking a wall)
function placeAt(st, cx, cy, cz, face) {
  var lv = lvl(), p = fp();
  var tx = cx + face.getStepX(), ty = cy + face.getStepY(), tz = cz + face.getStepZ();
  p.teleportTo(lv, tx - 3.5, ty - 4, tz + 0.5, YAW, 30);
  p.setYRot(YAW); p.setYHeadRot(YAW); p.yBodyRot = YAW; p.setXRot(30);
  p.setItemInHand(Hand.MAIN_HAND, st);
  var r = st.getItem().useOn(new UOC(lv, p, Hand.MAIN_HAND, st, new BHR(new Vec3(cx + 0.5 + face.getStepX() * 0.5, cy + 0.5 + face.getStepY() * 0.5, cz + 0.5 + face.getStepZ() * 0.5), face, new BP(cx, cy, cz), false)));
  return "" + r;
}

function sf(t, n, v) { for (var k = CLS(t); k != null; k = k.getSuperclass()) { try { var x = k.getDeclaredField(n); x.setAccessible(true); x.set(t, v); return true; } catch (e) {} } return false; }
