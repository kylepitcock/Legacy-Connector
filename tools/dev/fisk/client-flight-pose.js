// Control-session helper (#151): wears a flight suit, switches to the flying pose and the third-person camera so a screenshot (`shot name` command file)
// shows how the flying player model is drawn. Mod-specific development tool: nothing here ships.
var EquipmentSlot = Java.type("net.minecraft.world.entity.EquipmentSlot");
var LItemStack = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
var Hero = Java.type("com.fiskmods.heroes.common.hero.Hero");
var Vars = Java.type("com.fiskmods.heroes.common.data.var.Vars");
var EntityBridge = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var sp = server.getPlayerList().getPlayers().get(0);
var name = typeof HERO === "undefined" ? "iron_man_mk50" : HERO;
var h = Hero.REGISTRY.getObject("fiskheroes:" + name);
var st = h.getDefault().createArmorStacks();
var slots = [EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET];
for (var i = 0; i < 4; i++) if (st[i] != null) sp.setItemSlot(slots[i], LItemStack.mcc$toModern(st[i]));
var legacy = EntityBridge.wrap(sp);
sp.teleportTo(server.overworld(), sp.getX(), sp.getY() + 6, sp.getZ(), 0, 0);
Vars.FLYING.set(legacy, true).sync();
mc.execute(function () { mc.options.setCameraType(Java.type("net.minecraft.client.CameraType").THIRD_PERSON_BACK); });
"worn " + h + " flying=" + Vars.FLYING.get(legacy);
