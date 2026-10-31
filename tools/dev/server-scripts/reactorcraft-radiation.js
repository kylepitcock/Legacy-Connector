// run: ./gradlew runServerSubTest -PautostopTicks=600 -PserverScriptFrom=100 -PserverScript=<abs path of this file>
// needs the Reika jars (DragonAPI, RotaryCraft, ElectriCraft, ReactorCraft) + CoFHCore in run-subtest/mods/1.7.10; see docs/wiki/Development.md
var __p = String(java.lang.System.getProperty("legacyconnector.serverScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib.js");
var Y = 100, X = 300, Z = 300;
var RE = Java.type("Reika.ReactorCraft.Auxiliary.RadiationEffects");
var RIn = Java.type("Reika.ReactorCraft.Auxiliary.RadiationEffects$RadiationIntensity").class.getEnumConstants();
var RI = Java.type("Reika.ReactorCraft.Registry.ReactorItems");
var EB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var ET = Java.type("net.minecraft.world.entity.EntityType");
var EquipmentSlot = Java.type("net.minecraft.world.entity.EquipmentSlot");
var out = "";
if (tick == 0) { flat(X - 4, Y, Z - 4, 16, 16, 8); }
function spawn(x, z) { var e = ET.PIG.create(lvl()); e.moveTo(x + 0.5, Y, z + 0.5); lvl().addFreshEntity(e); return e; }
if (tick == 3) {
  var p1 = spawn(X, Z), p2 = spawn(X + 4, Z);
  G("pigs", [p1, p2]);
  // hazmat on pig 2
  var items = [RI.HAZHELMET, RI.HAZCHEST, RI.HAZLEGS, RI.HAZBOOTS].map(function (i) { return i.getStackOf().mcc$modern(); });
  var slots = [EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET];
  slots.forEach(function (s, i) { p2.setItemSlot(s, items[i]); });
  out += "intensities=" + Java.from(RIn).map(function (x) { return "" + x; }).join(",") + " hazmat(pig1)=" + RE.instance.hasHazmatSuit(EB.wrap(p1)) + " hazmat(pig2)=" + RE.instance.hasHazmatSuit(EB.wrap(p2));
}
if (tick >= 5 && tick < 700 && tick % 5 == 0) {
  var pigs = G("pigs");

  pigs.forEach(function (p) { if (!p.isDeadOrDying()) RE.instance.applyEffects(EB.wrap(p), RIn[RIn.length - 1]); });
}
if (tick % 60 == 0 && tick > 0) {
  var pigs = G("pigs");
  out += "T" + tick + " pig1 hp=" + pigs[0].getHealth() + " effects=" + pigs[0].getActiveEffects().size() + " alive=" + pigs[0].isAlive() + " | pig2(hazmat) hp=" + pigs[1].getHealth() + " effects=" + pigs[1].getActiveEffects().size() + " alive=" + pigs[1].isAlive();
}
out;
