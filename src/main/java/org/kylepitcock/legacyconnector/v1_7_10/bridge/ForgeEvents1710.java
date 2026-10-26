package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;

/**
 * More NeoForge game events fed into the 1.7.10 Forge bus (see {@link EventBridge1710} for the main set): item expiry,
 * commands, fuel, entity sounds, attacks, harvest checks, entity tracking, name formatting, sleeping, experience
 * pickup, anvils, note blocks, potential spawns, attack targets, despawning, lightning and minecart updates. Legacy
 * results are written back into the modern event; legacy event objects are only built when a legacy listener exists.
 */
public final class ForgeEvents1710 {
    private ForgeEvents1710() {}

    private static EventBus forge() {
        return MinecraftForge.EVENT_BUS;
    }

    private static boolean wants(Class<? extends Event> type) {
        return forge().mcc$hasListeners(type);
    }

    private static EntityPlayer player(Player p) {
        return EntityBridge.wrap(p) instanceof EntityPlayer legacy ? legacy : null;
    }

    public static void register(IEventBus game) {
        game.addListener((net.neoforged.neoforge.event.entity.item.ItemExpireEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.item.ItemExpireEvent.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof EntityItem item)) return;
            ItemStack stack = item.getEntityItem();
            int life = stack == null || stack.getItem() == null ? 6000 : stack.getItem().getEntityLifespan(stack, item.mcc$world());
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.item.ItemExpireEvent(item, life);
            if (forge().post(legacy) && legacy.extraLife > 0) e.addExtraLife(legacy.extraLife);
        });

        game.addListener(EventPriority.LOW, (net.neoforged.neoforge.event.CommandEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.CommandEvent.class)) return;
            var parse = e.getParseResults();
            String line = parse.getReader().getString();
            if (line.startsWith("/")) line = line.substring(1);
            String[] parts = line.trim().split(" +");
            if (parts.length == 0 || parts[0].isEmpty()) return;
            var source = parse.getContext().getSource();
            String[] args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.CommandEvent(command(source.getServer(), parts[0]),
                    CommandBridge1710.sender(source), args);
            if (forge().post(legacy)) {
                e.setCanceled(true);
                if (legacy.exception != null) e.setException(legacy.exception);
            }
        });

        game.addListener(EventPriority.LOW, (net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.FuelBurnTimeEvent.class) || e.getItemStack().isEmpty()) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.FuelBurnTimeEvent(ItemStack.mcc$wrap(e.getItemStack()));
            legacy.burnTime = e.getBurnTime();
            forge().post(legacy);
            if (legacy.getResult() == Event.Result.ALLOW) e.setBurnTime(Math.max(0, legacy.burnTime));
            else if (legacy.getResult() == Event.Result.DENY) e.setBurnTime(0);
        });

        game.addListener((net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.PlaySoundAtEntityEvent.class) || e.getSound() == null) return;
            var entity = EntityBridge.wrap(e.getEntity());
            if (entity == null) return;
            var key = e.getSound().value().getLocation();
            String name = key.getNamespace().equals("minecraft") ? key.getPath() : key.toString();
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.PlaySoundAtEntityEvent(entity, name, e.getOriginalVolume(), e.getOriginalPitch());
            if (forge().post(legacy) || legacy.name == null || legacy.name.isEmpty()) {
                e.setCanceled(true);
                return;
            }
            if (!legacy.name.equals(name)) {
                var replaced = SoundBridge.sound(legacy.name);
                if (replaced != null) e.setSound(net.minecraft.core.Holder.direct(replaced));
            }
            e.setNewVolume(legacy.newVolume);
            e.setNewPitch(legacy.newPitch);
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.AttackEntityEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.AttackEntityEvent.class)) return;
            EntityPlayer p = player(e.getEntity());
            var target = EntityBridge.wrap(e.getTarget());
            if (p == null || target == null) return;
            if (forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.AttackEntityEvent(p, target))) e.setCanceled(true);
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.HarvestCheck e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.HarvestCheck.class)) return;
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.HarvestCheck(p,
                    BlockBridge.legacyBlock(e.getTargetBlock()), e.canHarvest());
            forge().post(legacy);
            e.setCanHarvest(legacy.success);
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.StartTracking.class)) return;
            EntityPlayer p = player(e.getEntity());
            var target = EntityBridge.wrap(e.getTarget());
            if (p != null && target != null) forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.StartTracking(p, target));
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StopTracking e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.StopTracking.class)) return;
            EntityPlayer p = player(e.getEntity());
            var target = EntityBridge.wrap(e.getTarget());
            if (p != null && target != null) forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.StopTracking(p, target));
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.NameFormat e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.NameFormat.class)) return;
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            String current = e.getDisplayname().getString();
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.NameFormat(p, e.getUsername().getString());
            legacy.displayname = current;
            forge().post(legacy);
            if (legacy.displayname != null && !legacy.displayname.equals(current)) e.setDisplayname(net.minecraft.network.chat.Component.literal(legacy.displayname));
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerSleepInBedEvent.class)) return;
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var pos = e.getPos();
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerSleepInBedEvent(p, pos.getX(), pos.getY(), pos.getZ());
            forge().post(legacy);
            if (legacy.result == null) return;
            e.setProblem(switch (legacy.result) {
                case OK -> null;
                case NOT_POSSIBLE_HERE -> Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
                case NOT_POSSIBLE_NOW -> Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
                case TOO_FAR_AWAY -> Player.BedSleepingProblem.TOO_FAR_AWAY;
                case NOT_SAFE -> Player.BedSleepingProblem.NOT_SAFE;
                case OTHER_PROBLEM -> Player.BedSleepingProblem.OTHER_PROBLEM;
            });
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerXpEvent.PickupXp e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerPickupXpEvent.class)) return;
            EntityPlayer p = player(e.getEntity());
            if (p == null || !(EntityBridge.wrap(e.getOrb()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.item.EntityXPOrb orb)) return;
            if (forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerPickupXpEvent(p, orb))) e.setCanceled(true);
        });

        game.addListener((net.neoforged.neoforge.event.entity.player.AnvilRepairEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.AnvilRepairEvent.class)) return;
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.AnvilRepairEvent(p, ItemStack.mcc$wrap(e.getOutput()),
                    ItemStack.mcc$wrap(e.getLeft()), ItemStack.mcc$wrap(e.getRight()));
            legacy.breakChance = e.getBreakChance();
            forge().post(legacy);
            e.setBreakChance(legacy.breakChance);
        });

        game.addListener((net.neoforged.neoforge.event.level.NoteBlockEvent.Play e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.world.NoteBlockEvent.Play.class)) return;
            if (!(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            var pos = e.getPos();
            int instrument = switch (e.getInstrument()) {
                case BASEDRUM -> 1;
                case SNARE -> 2;
                case HAT -> 3;
                case BASS -> 4;
                default -> 0;
            };
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.world.NoteBlockEvent.Play(World.mcc$wrap(level), pos.getX(), pos.getY(),
                    pos.getZ(), BlockBridge.legacyMeta(e.getState()), e.getVanillaNoteId(), instrument);
            if (forge().post(legacy)) {
                e.setCanceled(true);
                return;
            }
            if (legacy.getVanillaNoteId() != e.getVanillaNoteId()) {
                int id = legacy.getVanillaNoteId();
                e.setNote(net.neoforged.neoforge.event.level.NoteBlockEvent.Note.values()[id % 12],
                        net.neoforged.neoforge.event.level.NoteBlockEvent.Octave.values()[Math.min(id / 12, 2)]);
            }
            if (legacy.instrument != null && legacy.instrument.ordinal() != instrument) {
                e.setInstrument(switch (legacy.instrument) {
                    case BASSDRUM -> net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM;
                    case SNARE -> net.minecraft.world.level.block.state.properties.NoteBlockInstrument.SNARE;
                    case CLICKS -> net.minecraft.world.level.block.state.properties.NoteBlockInstrument.HAT;
                    case BASSGUITAR -> net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS;
                    default -> net.minecraft.world.level.block.state.properties.NoteBlockInstrument.HARP;
                });
            }
        });

        game.addListener((net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns e) -> potentialSpawns(e));

        game.addListener(EventPriority.LOWEST, (net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent e) -> {
            if (e.isCanceled() || !wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof EntityLivingBase living)) return;
            var target = e.getNewAboutToBeSetTarget() == null ? null : EntityBridge.wrap(e.getNewAboutToBeSetTarget());
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent(living,
                    target instanceof EntityLivingBase t ? t : null));
        });

        game.addListener((net.neoforged.neoforge.event.entity.living.MobDespawnEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof EntityLivingBase living)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn(living);
            forge().post(legacy);
            if (legacy.getResult() == Event.Result.ALLOW) e.setResult(net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.ALLOW);
            else if (legacy.getResult() == Event.Result.DENY) e.setResult(net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.DENY);
        });

        game.addListener((net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent e) -> {
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityStruckByLightningEvent.class)) return;
            var entity = EntityBridge.wrap(e.getEntity());
            if (entity == null || !(EntityBridge.wrap(e.getLightning()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt bolt)) return;
            if (forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityStruckByLightningEvent(entity, bolt))) e.setCanceled(true);
        });

        game.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) -> {
            if (!(e.getEntity() instanceof net.minecraft.world.entity.vehicle.AbstractMinecart cart) || cart.level().isClientSide()) return;
            if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartUpdateEvent.class)) return;
            if (!(EntityBridge.wrap(cart) instanceof legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart legacy)) return;
            int x = net.minecraft.util.Mth.floor(cart.getX()), y = net.minecraft.util.Mth.floor(cart.getY()), z = net.minecraft.util.Mth.floor(cart.getZ());
            if (net.minecraft.world.level.block.BaseRailBlock.isRail(cart.level(), new net.minecraft.core.BlockPos(x, y - 1, z))) y--;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartUpdateEvent(legacy, x, y, z));
        });
    }

    /** The legacy command a command line runs: a mod's own, or a stand-in for a modern command. */
    private static legacyconnector.v1_7_10.net.minecraft.command.ICommand command(net.minecraft.server.MinecraftServer server, String name) {
        var manager = legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.mcc$wrap(server).getCommandManager();
        Object found = manager.getCommands().get(name);
        if (found instanceof legacyconnector.v1_7_10.net.minecraft.command.ICommand c) return c;
        return new ModernCommand(name);
    }

    /** A vanilla or modern mod command, as a legacy command (its name; running it runs the modern command). */
    static final class ModernCommand extends legacyconnector.v1_7_10.net.minecraft.command.CommandBase {
        private final String name;

        ModernCommand(String name) {
            this.name = name;
        }

        @Override
        public String getCommandName() {
            return name;
        }

        @Override
        public String getCommandUsage(legacyconnector.v1_7_10.net.minecraft.command.ICommandSender sender) {
            return "/" + name;
        }

        @Override
        public void processCommand(legacyconnector.v1_7_10.net.minecraft.command.ICommandSender sender, String[] args) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            String line = name + (args.length == 0 ? "" : " " + String.join(" ", args));
            var source = sender instanceof legacyconnector.v1_7_10.net.minecraft.entity.Entity e && e.mcc$entity() != null
                    ? e.mcc$entity().createCommandSourceStack() : server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, line);
        }

        @Override
        public int compareTo(Object o) {
            return o instanceof legacyconnector.v1_7_10.net.minecraft.command.ICommand c ? name.compareTo(c.getCommandName()) : 0;
        }
    }

    /** Mobs that may spawn at a position: legacy listeners add and remove entries (Extra Utilities' siege, HBM's pollution). */
    private static void potentialSpawns(net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns e) {
        if (!wants(legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent.PotentialSpawns.class)) return;
        legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type = null;
        for (var t : legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType.values()) if (t.mcc$modern() == e.getMobCategory()) type = t;
        if (type == null) return;
        Map<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry, net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData> origin = new IdentityHashMap<>();
        List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> list = new ArrayList<>();
        for (var data : e.getSpawnerDataList()) {
            Class<?> cls = SpawnClasses.legacyClass(data.type);
            var entry = new legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry(cls, data.getWeight().asInt(), data.minCount, data.maxCount);
            origin.put(entry, data);
            list.add(entry);
        }
        var pos = e.getPos();
        World world = e.getLevel() instanceof net.minecraft.world.level.LevelAccessor l ? World.mcc$wrap(l) : null;
        var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent.PotentialSpawns(world, type, pos.getX(), pos.getY(), pos.getZ(), list);
        if (forge().post(legacy)) {
            e.setCanceled(true);
            return;
        }
        java.util.Set<Object> kept = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object o : legacy.list) {
            if (!(o instanceof legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry entry)) continue;
            var data = origin.get(entry);
            if (data != null && entry.itemWeight == data.getWeight().asInt() && entry.minGroupCount == data.minCount && entry.maxGroupCount == data.maxCount) {
                kept.add(entry);
                continue;
            }
            var modernType = SpawnClasses.modernType(entry.entityClass);
            if (modernType == null || entry.itemWeight <= 0) continue;
            e.addSpawnerData(new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(modernType, entry.itemWeight,
                    Math.max(1, entry.minGroupCount), Math.max(Math.max(1, entry.minGroupCount), entry.maxGroupCount)));
        }
        for (var o : origin.entrySet()) if (!kept.contains(o.getKey())) e.removeSpawnerData(o.getValue());
    }
}
