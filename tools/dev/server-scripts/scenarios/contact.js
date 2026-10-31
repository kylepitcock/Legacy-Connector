var Y0 = 150, X0 = 2500, Z0 = 2500;
var EntityType = Java.type("net.minecraft.world.entity.EntityType");
var MobSpawnType = Java.type("net.minecraft.world.entity.MobSpawnType");
function spawn(type, x, y, z) {
  var e = type.create(L.lvl());
  e.moveTo(x, y, z, 0, 0);
  if (e.setNoAi) e.setNoAi(true);
  L.lvl().addFreshEntity(e);
  return e;
}
if (tick == 0) {
  L.platform(X0 - 6, Y0, Z0, 30, 40, 10);
  G.g = L.rig({name: "GRINDER", X: X0, Z: Z0 + 10, Y: Y0, fuel: [["rc ethanol", 20000]]});
  G.f = L.rig({name: "FAN", X: X0, Z: Z0 + 20, Y: Y0, fuel: [["rc ethanol", 20000]]});
  G.s = L.rig({name: "SHAFT", X: X0, Z: Z0 + 30, Y: Y0, fuel: [["rc ethanol", 20000]], mid: ["SHAFT", "SHAFT"], anyYaw: true});
}
if (tick == 60) {
  // pig on the grinder, pig on the spinning shaft end, pig in front of the fan, an item in front of the fan
  var g = G.g; G.pigG = spawn(EntityType.PIG, g.mx + 0.5, Y0 + 0.3, g.mz + 0.5);
  var s = G.s; G.pigS = spawn(EntityType.PIG, s.mx + 0.5, Y0 + 0.3, s.mz + 0.5);
  var f = G.f; var fm = f.mach;
  L.say("fan fields: " + L.dump(fm).substring(0, 500));
  G.pigF = spawn(EntityType.PIG, f.mx + f.dx * 3 + 0.5, Y0 + 0.2, f.mz + f.dz * 3 + 0.5);
  var ie = new (Java.type("net.minecraft.world.entity.item.ItemEntity"))(L.lvl(), f.mx + f.dx * 2 + 0.5, Y0 + 0.5, f.mz + f.dz * 2 + 0.5, new L.IS(Java.type("net.minecraft.world.item.Items").COBBLESTONE, 1));
  ie.setDeltaMovement(0, 0, 0); L.lvl().addFreshEntity(ie); G.item = ie; G.itemPos0 = [ie.getX(), ie.getZ()];
  G.hp0 = [G.pigG.getHealth(), G.pigS.getHealth(), G.pigF.getHealth()];
  G.pos0 = [G.pigF.getX(), G.pigF.getZ()];
}
if (tick == 120) {
  var g = G.g, lv = L.lvl(), gp = new L.BP(g.mx, Y0, g.mz);
  G.pigG.setPos(g.mx + 0.5, Y0 + 0.3, g.mz + 0.5);
  lv.getBlockState(gp).entityInside(lv, gp, G.pigG);
  L.say("after explicit entityInside on grinder: pig health " + G.pigG.getHealth() + " pig pos " + G.pigG.getY().toFixed(2));
  var gt = L.te(g.mx, Y0, g.mz); L.say("grinder block now: " + L.blk(g.mx, Y0, g.mz) + " at " + g.mx + "," + g.mz + " te=" + gt + " rig mach=" + g.mach);
  var ifs = []; for (var k = gt.getClass(); k != null; k = k.getSuperclass()) Java.from(k.getInterfaces()).forEach(function (c) { ifs.push(c.getSimpleName()); });
  L.say("grinder class=" + gt.getClass().getName() + " interfaces=" + ifs.join(","));
  L.say("grinder implements DamagingContact: " + (Java.type("Reika.RotaryCraft.Auxiliary.Interfaces.DamagingContact").class.isInstance(gt)) + " dealsContactDamage=" + L.mach("GRINDER").dealsContactDamage());
}
if (tick == 160) {
  L.say("grinder pig health " + G.hp0[0] + " -> " + G.pigG.getHealth() + " alive=" + G.pigG.isAlive());
  L.say("shaft-end pig health " + G.hp0[1] + " -> " + G.pigS.getHealth() + " alive=" + G.pigS.isAlive());
  L.say("fan item moved from " + G.itemPos0[0].toFixed(1) + "," + G.itemPos0[1].toFixed(1) + " to " + G.item.getX().toFixed(1) + "," + G.item.getZ().toFixed(1) + " alive=" + G.item.isAlive());
  L.say("fan pig moved " + (Math.abs(G.pigF.getX() - G.pos0[0]) + Math.abs(G.pigF.getZ() - G.pos0[1])).toFixed(2) + " blocks (start " + G.pos0[0].toFixed(1) + "," + G.pos0[1].toFixed(1) + " now " + G.pigF.getX().toFixed(1) + "," + G.pigF.getZ().toFixed(1) + "), health " + G.hp0[2] + " -> " + G.pigF.getHealth());
}
L.run(tick);
