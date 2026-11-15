// Traincraft slope probe (#105): three tracks (straight, wooden slope, straight on top), a different stock on each; logs position/motion per tick.
// run: ./gradlew runServerSubTest -PsubTestDir=run-tc -PautostopTicks=220 -PserverTickScriptFrom=100 -PserverTickScript=<abs path>
var __p = String(java.lang.System.getProperty("legacyconnector.serverTickScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib-reika.js");
var X = 100, Y = 60, Z = 100;
var ENT = Java.type("net.minecraft.core.registries.BuiltInRegistries").ENTITY_TYPE;
var STOCK = ["tc:5plank", "tc:loco_diesel_shunter", "tc:freight_hopper_us"];
function legacyOf(e) { return Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge").wrap(e); }
function driveLoco(e) {
  var l = legacyOf(e);
  try {
    var k = CLS(l).getSuperclass();
    while (k != null && String(k.getName()) != "train.common.api.Locomotive") k = k.getSuperclass();
    var ff = k.getDeclaredField("fuelTrain"); ff.setAccessible(true); ff.setInt(l, 5000);
    l.parkingBrake = false; l.isLocoTurnedOn = true; l.forwardPressed = true;
  } catch (ex) { return "drive failed " + ex; }
}
function d2(v) { return v.toFixed(2); }
function describe(e) { return d2(e.getX()) + "," + d2(e.getY()) + " v=" + e.getDeltaMovement().x.toFixed(3) + "," + e.getDeltaMovement().y.toFixed(3); }
if (typeof CARTS === "undefined") CARTS = [];
if (tick == 0) {
  flat(X - 6, Y, Z - 6, 60, 30, 8);
  "flat"
} else if (tick == 2) {
  YAW = -90;
  var out = "";
  for (var k = 0; k < STOCK.length; k++) {
    var z = Z + k * 4;
    out += place(modItem("tc:tcrailverylongstraight"), X, Y, z, Dir.UP, Y - 1) + "/" + place(modItem("tc:tcrailslopewood"), X + 12, Y, z, Dir.UP, Y - 1) + " ";
    for (var i = 18; i < 40; i++) setBlock(X + i, Y, z, Blocks.STONE);
  }
  out;
} else if (tick == 4) {
  var out = "";
  for (var k = 0; k < STOCK.length; k++) out += place(modItem("tc:tcrailverylongstraight"), X + 18, Y + 1, Z + k * 4, Dir.UP, Y) + " ";
  out;
} else if (tick == 6) {
  for (var k = 0; k < STOCK.length; k++) {
    var e = ENT.get(RL.parse(STOCK[k])).create(lvl());
    e.setPos(X + 3.5, Y + 0.3, Z + k * 4 + 0.5);
    lvl().addFreshEntity(e);
    e.setDeltaMovement(0, 0, 0);
    CARTS.push(e);
  }
  "spawned " + CARTS.length;
} else if (tick > 6 && tick < 400 && CARTS.length) {
  var line = "";
  for (var k = 0; k < CARTS.length; k++) {
    var c = CARTS[k];
    if (k == 1 && tick > 14) driveLoco(c);
    else if (tick > 20 && c.getX() < X + 14.5) c.setDeltaMovement(0.3, c.getDeltaMovement().y, 0);
    line += STOCK[k].substring(3, 9) + " " + describe(c) + " | ";
  }
  (tick > 36 && tick % 2 == 0) ? "t" + tick + " " + line : null;
}
