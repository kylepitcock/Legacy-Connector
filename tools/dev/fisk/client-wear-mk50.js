// Control-session helper (SCENARIO=control tools/dev/quick-shots.sh, or CONTROL=<dir> for a -PshotDir copy): puts the Iron Man Mk50 suit on the integrated-server player so the client key probe (probe-client-keys.js) and
// "holdkey suitAbility1..5 <ticks>" (C aim, V blade, G nanite transform) can be driven. Findings 2026-11-01 (singleplayer, with and without the perf mods): transform, blade toggle and aiming all work through the real keyPress path, no key-list flicker.
// Mod-specific development tool: nothing here ships.
var EquipmentSlot = Java.type("net.minecraft.world.entity.EquipmentSlot");
var LItemStack = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
var Hero = Java.type("com.fiskmods.heroes.common.hero.Hero");
var sp = server.getPlayerList().getPlayers().get(0);
var h = Hero.REGISTRY.getObject("fiskheroes:iron_man_mk50");
var st = h.getDefault().createArmorStacks();
var slots = [EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET];
for (var i = 0; i < 4; i++) if (st[i] != null) sp.setItemSlot(slots[i], LItemStack.mcc$toModern(st[i]));
"worn " + h
