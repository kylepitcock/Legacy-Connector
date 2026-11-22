// IC2 Classic quantum helmet passive effects on a real player (1.7.10): air refill, poison removal. LIBS="lib-rotary lib".
var Y0 = 150, X0 = 2600, Z0 = 2600;
var EquipmentSlot = Java.type("net.minecraft.world.entity.EquipmentSlot");
var MobEffects = Java.type("net.minecraft.world.effect.MobEffects");
var MobEffectInstance = Java.type("net.minecraft.world.effect.MobEffectInstance");
if (tick == 0) {
  L.platform(X0 - 8, Y0, Z0 - 8, 16, 16, 6);
  var names = [];
  L.R.ITEM.keySet().forEach(function (k) { if (k.getNamespace() == "ic2" && ("" + k).indexOf("quantum") >= 0) names.push("" + k); });
  L.say("quantum items: " + names.join(" "));
  G.names = names;
}
if (tick == 20) {
  G.sp = realPlayer("Qtest");
  G.sp.teleportTo(L.lvl(), X0 + 0.5, Y0, Z0 + 0.5, 0, 0);
  var helm = null;
  G.names.forEach(function (n) { if (n.indexOf("helm") >= 0 && !helm) helm = n; });
  L.say("helmet item " + helm);
  var st = new L.IS(L.R.ITEM.get(L.RL.parse(helm)), 1);
  var lst = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack").mcc$wrap(st);
  var ElectricItem = Java.type("ic2.api.item.ElectricItem");
  L.say("charged: " + ElectricItem.manager.charge(lst, 1e9, 99, true, false));
  G.sp.setItemSlot(EquipmentSlot.HEAD, lst.mcc$modern());
  L.say("worn: " + G.sp.getItemBySlot(EquipmentSlot.HEAD));
  G.sp.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0));
  G.sp.setAirSupply(50);
  L.say("poison before: " + G.sp.hasEffect(MobEffects.POISON) + " air " + G.sp.getAirSupply());
}
if (tick > 20 && tick < 44) G.sp.doTick(); // the embedded connection is not in the server's connection list: tick the player ourselves
if (tick == 43) L.say("after 22 doTicks: poison " + G.sp.hasEffect(MobEffects.POISON) + " air " + G.sp.getAirSupply());
if (tick == 40) {
  var lp = legacy(G.sp);
  var arm = lp.inventory.armorInventory;
  L.say("armorInventory " + (arm ? arm.length : null) + ": " + (arm ? Java.from(arm).join(" | ") : ""));
  var IC2 = Java.type("ic2.core.IC2");
  L.say("IC2 instance " + IC2.instance + " platform " + IC2.platform + " simulating " + IC2.platform.isSimulating());
  var h = G.sp.getItemBySlot(EquipmentSlot.HEAD);
  L.say("helmet item class " + legacy(G.sp).inventory.armorItemInSlot(3));
}
if (tick == 450) {
  try {
    var lp = legacy(G.sp), hs = lp.inventory.armorItemInSlot(3);
    var r = "skipped";
    L.say("direct onTick -> " + r + " poison " + G.sp.hasEffect(MobEffects.POISON) + " air " + G.sp.getAirSupply());
    var evt = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent$PlayerTickEvent");
    L.say("IC2 onPlayerTick direct:");
    var ev = new evt(Java.type("legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent$Phase").END, lp);
    Java.type("ic2.core.IC2").instance.onPlayerTick(ev);
    L.say("after direct onPlayerTick: poison " + G.sp.hasEffect(MobEffects.POISON) + " air " + G.sp.getAirSupply());
  } catch (e) { L.say("ERR " + e); }
}
if (tick == 60) L.say("tick 60: poison " + G.sp.hasEffect(MobEffects.POISON) + " air " + G.sp.getAirSupply() + " helmet " + G.sp.getItemBySlot(EquipmentSlot.HEAD));
L.run(tick);
