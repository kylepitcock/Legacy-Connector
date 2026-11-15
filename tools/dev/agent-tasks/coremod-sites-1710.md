# 1.7.10 coremod hook sites found in local jars (round 2)

Produced with `./gradlew probeCoremodHooks -q -Psites=true` over the 1.7.10 jars that have a coremod (`FMLCorePlugin`) in `test-mods*`, `run-*` (read-only).
Keys are Minecraft sites (`Class.srgMethod#kind`), `@n` repeats folded. "mapped" = a place exists in `CoremodSites` (this round added: POTION_PARTICLE_NAME, ARMOR_VISIBILITY,
PLAYER_HURT_SOUND, LIQUID_ADDED, FIREWORK_LIFT, LIMB_SWING, FLYING_TRAVEL_END, SQUID_TICK, BOW_DRAW_TIME). Next round: start from the unmapped lists below.

Left out: mods on the `OBSOLETE` list (gtnhmixins loaders, ArchaicFix, Hodgepodge, BugTorch, CoreTweaks, Angelica, NEID, ...), the GTNH builds whose plugin needs `gtnhmixins` (AppleCore, Avaritia, EtFuturum, GregTech, RandomThings, ServerUtilities, Thaumic Tinkerer 2.11, Witching Gadgets, ... load as mixin plugins, not transformers),
events served under `compat/` (DragonAPI crafting/smelting hooks: its 149 sites are listed but not worked on), and earlier-tested OpenBlocks, OpenModsLib, EnderCore, Logistics Pipes, Factorization, CodeChickenCore.

## Round 3 state (server places, Fisk's Heroes first)

New places (all generic, keyed by the 1.7.10 site; `CoremodSites` + a `*HookMixin`): PRESSURE_ENTITIES + PRESSURE_IGNORED (`BasePressurePlateBlock.getEntityCount`), SET_BLOCK_REPLACE + SET_BLOCK_LIMIT (`Level.setBlock`), TARGET_CANDIDATES (`TargetingConditions.test`), CLOSEST_VULNERABLE (`NearestAttackableTargetGoal.findTarget`, player targets), USE_ENTITY_REACH (`Player.entityInteractionRange`, server), TRACKING_MAX (`ChunkMap$TrackedEntity.updatePlayer` cap), FENCE_BOXES + PANE_IGNORED (`CrossCollisionBlock.getCollisionShape`), PLATE_TICK (`BasePressurePlateBlock.tick`), STEP_DOWN (`Entity.collide` result), NOCLIP_MOVE (`Entity.move` with noPhysics), ARROW_TARGET (`ServerGamePacketListenerImpl$1.onAttack` instanceof), FURNACE_TAKEN_OVER (`AbstractFurnaceBlockEntity.serverTick`).
Verified with `tools/dev/coremod-sites-1710-probe.js` on a headless server (Fisk + RotaryCraft + DragonAPI, `-PcoremodReport`): every mixin applies (the probe loads each target class; a failing injection throws), no "A coremod hook failed". Effect checked for: pressure plate with a pig (powered, so the count path runs), setBlock and targeting (hooks called, vanilla answers unchanged for non-hero entities). "Applies" only: the other places (need a Fisk hero / a player to change the answer).
Fisk server-side count (`-PcoremodReport`, 44 hooks visible on a server): replayed 20 hook methods before, 34 after; one place left without equivalent.

State of the remaining Fisk sites:
- **no equivalent** `BlockButton.func_150046_n#after:ldc EntityArrow` (getButtonPressClass): modern `ButtonBlock.checkPressed` asks only wood-type buttons for `AbstractArrow` (typed result, gated by `BlockSetType.canButtonBeActivatedByArrows`); a legacy class cannot be substituted without rewriting the button's press logic. Affects: trick arrows pressing metal buttons (minor).
- Client sites (`ASMHooksClient`, `ModelHelper`, `SHRenderHooks`, `SHParticlesClient`): **open**, not done in this session. Gating visible features: `EntityRenderer.func_78479_a` orientCameraPre/Post and `func_78475_f` view bobbing/distance walked (camera and FPS bob when scaled/flying), `RenderPlayer.func_82441_a` first-person arm and `ItemRenderer.func_78440_a` (first-person suit/hand rendering), `ModelBiped` pose pre (suit animations: ANGLES_PRE/RENDER_PRE exist; the Fisk `ModelHelper` hooks are replayed there), `Minecraft.func_71407_l` key substitution (ability keys), `GuiInventory.func_147046_a` (preview scale), `FlightMouseManager.shouldOverride` (flight mouse). Cosmetic: `Entity.func_70053_R` shadow height, `Entity.func_70070_b` brightness Y, `SoundManager` range, `EffectRenderer` particle count/binding, `RenderGlobal` spell particle scale, `World/ChunkCache.getSkyBlockTypeBrightness`, bow icon times (`EntityPlayer.func_70620_b`), `GuiContainerCreative` scroll, sneak offset `const:0.125`/`ldc 0.2`.
- Not in this list because not probed as sites: Fisk's block overrides (liquid, leaf, bed collision) are method overrides, not coremod hooks.

Other mods in this round:
- RotaryCraft `TileEntityFurnace.updateEntity#head-cancel`: **mapped** (FURNACE_TAKEN_OVER; applies).
- CoFHCore (all five): **no equivalent needed**, they are 1.7.10 engine fixes/optimisations the modern game does natively (item stacking `EntityItem.searchForOtherItemsNearby`, `Container.mergeItemStack` speed, `Entity.moveEntity` collision box collection, texture tick, held-item equality for the re-equip animation).
- NEI `NBTTagCompound/NBTTagList.toString` fixTrailingCommaList: **no place needed** (modern NBT text has no trailing comma). `ContainerWorkbench.transferStackInSlot` (`InventoryUtils.insertItem`): open.
- Betweenlands `ServerConfigurationManager.func_148545_a` (createPlayerForUser): **no equivalent**, the hook builds the player object (a mod subclass of EntityPlayerMP), modern `PlayerList.getPlayerForLogin` always creates a `ServerPlayer`.
- SpaceCore `Packet.processPacket` x93: **no equivalent** (packet class heads; legacy packets of mods travel in `LegacyPayload`, vanilla ones are modern types).
- PlayerAPI: structural, unchanged.
- 1Legends: server place `World.func_72856_b` **mapped** (CLOSEST_VULNERABLE; same hook family as Fisk). Its client sites: open (same classes as Fisk's).
- Open (jar not available locally or semantics not determined): Anti-Gravity (jar not found; `Item.getMovingObjectPosition`, `NetHandlerPlayServer.processPlayer` kick checks, client camera), ChromatiCraft (`World.getCollidingBoundingBoxes` interception: replaced call is `expand`, the hook returns a box list, insertion point not determinable from the key; `EntityLivingBase.moveEntityWithHeading#after:ifeq` stopSlowFall; End island bias; client reach/chunk updates/icon load), Galacticraft (`ChunkProviderServer.populate` prevent-generate; client sky/fog colour, camera, footprints, liquid overlays), BL client perspective hooks, DragonAPI (served under `compat/`).

### Follow-up (reach check, client sites)

- **Reach (`USE_ENTITY_REACH`) checked, no change needed.** 1.7.10's `processUseEntity` compares the SQUARED distance with `d0 = 36.0` (9.0 when the entity is not seen); Fisk's `getReachDistanceSq` returns `d * f * f` with `f = getReachDistance(player, sqrt(d))`, which for a player without reach modifiers is `sqrt(d)` itself, so the answer is `d * d` = 1296 (reach 36 blocks) for everybody in the original mod too. The place converts that squared limit back to a distance multiplier (`sqrt(answer / 36)` = x6 at the default) on the server-side `Player.entityInteractionRange` only. It does not change who a player can hit: the vanilla client picks the target with its own 3-block range, the server limit only decides whether the packet is accepted (as in 1.7.10). Modern distance in, modern distance out; only the 36 constant is legacy.
- **Fisk client sites that need no new place**: `renderFirstPersonArm`/`Post` and `orientCameraPre` are served by `compat/fiskheroes/FiskHeroesClient` (NeoForge `RenderArmEvent` and `ViewportEvent.ComputeCameraAngles`). The hooks draw through the mod's own render helpers (`RenderPlayerHand`, `CameraHandler.orient(GLProxy)`), not through GL calls a generic place could capture, so a generic replacement would only move the same code out of `compat/` without changing the arm angle the play test saw (that is the mod's `RenderPlayerHand` maths against the modern hand pose, to be fixed in the compat class with a client at hand).
- **Left to the Ant-Man/size worker (scale-dependent)**: `getViewBobbing`, `getDistanceWalked`, `getInventoryPlayerScale/Offset/OffsetPost`, `getScaledSneakOffset`, `getShadowHeight`, `getModifiedEntityScale` (OtherPlayerMP), `getBrightnessY`, `applyPlayerRenderTranslation`.
- **No equivalent** (concrete): `SoundManager.func_148611_c ldc 16.0` (modern attenuation distance is fixed per `Sound` entry, no per-instance range); `Minecraft.func_71407_l` left/right click key substitution (modern `Minecraft.handleKeybinds` reads `KeyMapping` objects directly, the substitute is a different binding object with the hook's own state); `GuiContainerCreative` scroll (modern creative screen has no per-tab scroll hook; the hero tab's pages are a mod GUI feature); bow icon time (`EntityPlayer.getItemIcon`: modern bow pull models are item properties of the model JSON, not code).
- **Still open**: `FlightMouseManager.shouldOverride` (`EntityRenderer.func_78480_b`), `onPickBlock`, `ItemRenderer.func_78440_a` held item and tail, sky-light brightness, `EffectRenderer` particle hooks, potion particle scale; ChromatiCraft, Galacticraft and Betweenlands client hooks; NEI `ContainerWorkbench.transferStackInSlot`. Reason: each needs a real client run per batch (RAM was below 6 GB) and none could be verified blind.

## Summary

| mod | hook sites | mapped | unmapped |
|---|---|---|---|
| 1.7.10-SpaceCore-0.7.14.jar | 94 | 0 | 94 |
| 1Legends-1.7.10-8.6.2.jar | 37 | 10 | 27 |
| Anti-Gravity-Mod-1.7.10.jar | 11 | 3 | 8 |
| ChromatiCraft 1.7.10 V33a.jar | 6 | 0 | 6 |
| CoFHCore-[1.7.10]3.1.4-329.jar | 5 | 0 | 5 |
| DragonAPI 1.7.10 V33b.jar | 127 | 4 | 123 |
| FiskHeroes-1.7.10-2.4.0.jar | 97 | 35 | 62 |
| Galacticraft-1.7-3.0.12.504.jar | 13 | 5 | 8 |
| NotEnoughItems-1.7.10-1.0.5.120-universal.jar | 3 | 0 | 3 |
| PlayerAPI-1.5.0.jar | 35 | 0 | 35 |
| RotaryCraft 1.7.10 V33a.jar | 1 | 0 | 1 |
| TheBetweenlands-1.0.6-alpha-universal.jar | 13 | 2 | 11 |

Mods probed that put no hook calls into Minecraft classes (structural patches of their own or other mods' classes only, or the plugin could not run offline): InventoryTweaks (container/GuiTextField structure), AE2 (`@Integration` stripping), OpenComputers, Steve's Addons, Thaumic Machina, Thaumic Energistics, Thaumic Exploration, BrandonsCore, Aroma1997Core, ForgeRelocation, MrTJPCore, WR-CBE, Thaumcraft 4, GalaxySpace (HookLib: transformer is `PrimaryClassTransformer`, hooks come from its own container; 0 recorded), Galacticraft (plugin ran with a created `mods` dir).
INpureCore failed to run offline (log4j filter NPE).

## 1.7.10-SpaceCore-0.7.14.jar (94 sites, 0 mapped, 94 unmapped)

93 of the unmapped sites are `Packet.processPacket` heads (`#after:aload 0` in every vanilla packet class: its packet interceptor, hook `PacketInterceptTransformer`); one place would need a per-packet hook on the modern network layer. Not mapped.

- **unmapped** `client/renderer/texture/TextureManager.func_110577_a(Lutil/ResourceLocation;)V#head` -> `com/spacechase0/minecraft/spacecore/client/render/TextureOverrides.intercept(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/util/Resou`

## 1Legends-1.7.10-8.6.2.jar (37 sites, 10 mapped, 27 unmapped)

- **unmapped** `client/entity/EntityOtherPlayerMP.func_70071_h_()V#after:invokestatic util/MathHelper.func_76133_a(D)F` -> `com/tihyo/legends/management/asm/ASMMethods.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- **unmapped** `client/entity/EntityPlayerSP.func_70636_d()V#after:ldc 0.2` -> `com/tihyo/legends/management/asm/ASMMethods.getScaledSneakOffset(Lnet/minecraft/entity/Entity;D)D`
- mapped `client/entity/EntityPlayerSP.func_71151_f()F#replace:func_75094_b()F` -> `com/tihyo/legends/management/asm/ASMClientMethods.getScaledWalkSpeedForFOV(Lnet/minecraft/entity/player/PlayerCapabilities;Lnet/minecraft/cl`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#after:invokestatic org/lwjgl/opengl/GL11.glScalef(FFF)V` -> `com/tihyo/legends/management/asm/ASMClientMethods.getInventoryPlayerOffsetPost(IILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#before:iload 2` -> `com/tihyo/legends/management/asm/ASMClientMethods.getInventoryPlayerScale(ILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#before:invokestatic org/lwjgl/opengl/GL11.glScalef(FFF)V` -> `com/tihyo/legends/management/asm/ASMClientMethods.getInventoryPlayerScale(ILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#before:ldc 50.0` -> `com/tihyo/legends/management/asm/ASMClientMethods.getInventoryPlayerOffset(IILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/model/ModelBiped.func_78088_a(Lentity/Entity;FFFFFF)V#after:aload 0` -> `com/tihyo/legends/management/PlayerRenderHelper.renderBipedPre(Lnet/minecraft/client/model/ModelBiped;Lnet/minecraft/entity/Entity;FFFFFF)V`
- **unmapped** `client/model/ModelBiped.func_78088_a(Lentity/Entity;FFFFFF)V#tail` -> `com/tihyo/legends/management/PlayerRenderHelper.renderBipedPost(Lnet/minecraft/client/model/ModelBiped;Lnet/minecraft/entity/Entity;FFFFFF)V`
- **unmapped** `client/particle/EffectRenderer.func_78869_b()Ljava/lang/String;#replace:size()I` -> `com/tihyo/legends/client/particles/LegendsParticlesClient.getParticlesInWorld([Ljava/util/List;)I`
- **unmapped** `client/particle/EffectRenderer.func_78874_a(Lentity/Entity;F)V#after:invokestatic org/lwjgl/opengl/GL11.glAlphaFunc(IF)V` -> `com/tihyo/legends/management/asm/TransformerEffectRenderer.plus(I)I`
- **unmapped** `client/particle/EffectRenderer.func_78874_a(Lentity/Entity;F)V#after:invokevirtual client/renderer/Tessellator.func_78382_b()V` -> `com/tihyo/legends/client/particles/LegendsParticlesClient.bindParticleTextures(I)V`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:aload 2` -> `com/tihyo/legends/management/asm/ASMMethods.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:getfield entity/player/EntityPlayer.field_70140_Q F` -> `com/tihyo/legends/management/asm/ASMMethods.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:getfield entity/player/EntityPlayer.field_70141_P F` -> `com/tihyo/legends/management/asm/ASMMethods.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:invokestatic java/lang/Math.abs(F)F` -> `com/tihyo/legends/management/asm/ASMClientMethods.getViewBobbingMultiplier(F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:ldc 0.5` -> `com/tihyo/legends/management/asm/ASMClientMethods.getViewBobbingMultiplier(F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78479_a(FI)V#after:aload 0` -> `com/tihyo/legends/management/asm/ASMClientMethods.orientCameraPost(Lnet/minecraft/client/renderer/EntityRenderer;F)V`
- **unmapped** `client/renderer/EntityRenderer.func_78479_a(FI)V#after:fload 1` -> `com/tihyo/legends/management/asm/ASMClientMethods.orientCameraPre(Lnet/minecraft/client/renderer/EntityRenderer;F)V`
- **unmapped** `client/renderer/ItemRenderer.func_78440_a(F)V#tail` -> `com/tihyo/legends/management/LegendsRenderHelper.renderItemInFirstPerson(Lnet/minecraft/client/renderer/ItemRenderer;F)V`
- **unmapped** `client/renderer/entity/RenderBiped.func_76986_a(Lentity/EntityLiving;DDDFF)V#const:0.125` -> `com/tihyo/legends/management/asm/ASMMethods.getScaledSneakOffset(Lnet/minecraft/entity/Entity;D)D`
- **unmapped** `client/renderer/entity/RenderPlayer.func_76986_a(Lclient/entity/AbstractClientPlayer;DDDFF)V#const:0.125` -> `com/tihyo/legends/management/asm/ASMMethods.getScaledSneakOffset(Lnet/minecraft/entity/Entity;D)D`
- **unmapped** `client/renderer/entity/RenderPlayer.func_77039_a(Lentity/EntityLivingBase;DDD)V#replace:func_77039_a(Lclient/entity/AbstractClientPlayer;DDD)V` -> `com/tihyo/legends/management/asm/ASMClientMethods.applyPlayerRenderTranslation(Lnet/minecraft/client/renderer/entity/RenderPlayer;Lnet/minec`
- **unmapped** `client/renderer/entity/RenderPlayer.func_77043_a(Lclient/entity/AbstractClientPlayer;FFF)V#tail` -> `com/tihyo/legends/management/PlayerRenderHelper.rotateCorpse(Lnet/minecraft/entity/player/EntityPlayer;F)V`
- **unmapped** `client/renderer/entity/RenderPlayer.func_82441_a(Lentity/player/EntityPlayer;)V#head-cancel` -> `com/tihyo/legends/management/asm/ASMClientMethods.renderFirstPersonArm(Lnet/minecraft/client/renderer/entity/RenderPlayer;Lnet/minecraft/ent`
- mapped `entity/Entity.func_70030_z()V#after:invokevirtual entity/Entity.func_70051_ag()Z` -> `com/tihyo/legends/management/asm/ASMMethods.shouldProduceSprintParticles(Lnet/minecraft/entity/Entity;)Z`
- **unmapped** `entity/Entity.func_70053_R()F#after:getfield entity/Entity.field_70131_O F` -> `com/tihyo/legends/management/asm/ASMMethods.getEntityScale(Lnet/minecraft/entity/Entity;)F`
- **unmapped** `entity/Entity.func_70070_b(F)I#replace:func_76128_c(D)I` -> `com/tihyo/legends/management/asm/ASMClientMethods.getBrightnessForRender(Lnet/minecraft/entity/Entity;)I`
- mapped `entity/EntityLivingBase.func_70612_e(FF)V#after:invokestatic util/MathHelper.func_76133_a(D)F` -> `com/tihyo/legends/management/asm/ASMMethods.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- mapped `entity/EntityLivingBase.func_70612_e(FF)V#head-cancel` -> `com/tihyo/legends/management/asm/ASMMethods.moveEntityWithHeading(Lnet/minecraft/entity/EntityLivingBase;FF)Z`
- mapped `entity/EntityLivingBase.func_70679_bo()V#const:"mobSpell"` -> `com/tihyo/legends/management/asm/ASMMethods.getPotionParticleName(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Ljava/lang/String;`
- mapped `entity/EntityLivingBase.func_70679_bo()V#const:"mobSpellAmbient"` -> `com/tihyo/legends/management/asm/ASMMethods.getPotionParticleName(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Ljava/lang/String;`
- mapped `entity/player/EntityPlayer.func_71059_n(Lentity/Entity;)V#replace:func_70097_a(Lutil/DamageSource;F)Z` -> `com/tihyo/legends/management/asm/ASMMethods.attackEntityFrom(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;F)Z`
- mapped `entity/player/EntityPlayer.func_82243_bO()F#after:fdiv` -> `com/tihyo/legends/management/asm/ASMMethods.getArmorVisibilityMultiplier(Lnet/minecraft/entity/player/EntityPlayer;)F`
- mapped `server/MinecraftServer.run()V#const:50` -> `com/tihyo/legends/management/asm/ASMMethods.getMiliSecondsPerTick()J`
- mapped `util/FoodStats.func_75118_a(Lentity/player/EntityPlayer;)V#const:80` -> `com/tihyo/legends/management/asm/ASMMethods.getHealRate(Lnet/minecraft/entity/player/EntityPlayer;I)I`
- **unmapped** `world/World.func_72856_b(Lentity/Entity;D)Lentity/player/EntityPlayer;#replace:func_72846_b(DDDD)Lentity/player/EntityPlayer;` -> `com/tihyo/legends/management/asm/ASMMethods.getClosestVulnerablePlayer(Lnet/minecraft/entity/Entity;DDDD)Lnet/minecraft/entity/player/Entity`

## Anti-Gravity-Mod-1.7.10.jar (11 sites, 3 mapped, 8 unmapped)

- mapped `block/BlockLiquid.func_149726_b(Lworld/World;III)V#cancel-after:iload 4` -> `jp/mc/ancientred/starminer/core/TransformServerHelper.blockLiquidOnBlockAddedHook(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;III)`
- mapped `client/entity/EntityPlayerSP.func_70636_d()V#after:aload 0` -> `jp/mc/ancientred/starminer/core/TransformClientHelper.setFlyMovementByGravity(Lnet/minecraft/entity/player/EntityPlayer;)V`
- **unmapped** `client/renderer/EntityRenderer.func_78467_g(F)V#after:dstore 10` -> `jp/mc/ancientred/starminer/core/TransformClientHelper.roatate3rdPersonViewByGravity(DF)Z`
- **unmapped** `client/renderer/EntityRenderer.func_78467_g(F)V#after:getfield client/settings/GameSettings.field_74325_U Z` -> `jp/mc/ancientred/starminer/core/TransformClientHelper.orientCameraByGravity(F)V`
- **unmapped** `client/renderer/EntityRenderer.func_78467_g(F)V#cancel-after:ifeq` -> `jp/mc/ancientred/starminer/core/TransformClientHelper.rotateSleepingViewByGravity(F)Z`
- **unmapped** `client/renderer/entity/RendererLivingEntity.func_76986_a(Lentity/EntityLivingBase;DDDFF)V#replace:func_77043_a(Lentity/EntityLivingBase;FFF)V` -> `jp/mc/ancientred/starminer/core/TransformClientHelper.rotateCorpseProxy(Lnet/minecraft/client/renderer/entity/RendererLivingEntity;Lnet/mine`
- **unmapped** `client/renderer/entity/RendererLivingEntity.func_77043_a(Lentity/EntityLivingBase;FFF)V#head` -> `jp/mc/ancientred/starminer/core/TransformClientHelper.rotateCorpseByGravity(Lnet/minecraft/entity/EntityLivingBase;FFF)V`
- mapped `entity/item/EntityItem.func_70071_h_()V#const:0.03999999910593033` -> `jp/mc/ancientred/starminer/core/TransformServerHelper.pullGravityYInGravity(Lnet/minecraft/entity/Entity;)D`
- **unmapped** `item/Item.func_77621_a(Lworld/World;Lentity/player/EntityPlayer;Z)Lutil/MovingObjectPosition;#head` -> `jp/mc/ancientred/starminer/core/TransformServerHelper.getMovingObjectPositionFromPlayerByGravity(Lnet/minecraft/world/World;Lnet/minecraft/e`
- **unmapped** `network/NetHandlerPlayServer.func_147347_a(Lnetwork/play/client/C03PacketPlayer;)V#after:aload 0` -> `jp/mc/ancientred/starminer/core/TransformServerHelper.jumpOverKickFloatTooLong(Lnet/minecraft/network/NetHandlerPlayServer;)Z`
- **unmapped** `network/NetHandlerPlayServer.func_147347_a(Lnetwork/play/client/C03PacketPlayer;)V#after:ifge` -> `jp/mc/ancientred/starminer/core/TransformServerHelper.jumpOverKickIllegalStance()Z`

## ChromatiCraft 1.7.10 V33a.jar (6 sites, 0 mapped, 6 unmapped)

- **unmapped** `client/multiplayer/PlayerControllerMP.func_78757_d()F#tail` -> `Reika/ChromatiCraft/Auxiliary/ChromaAux.getReachDistanceClient(FLnet/minecraft/client/multiplayer/PlayerControllerMP;)F`
- **unmapped** `client/multiplayer/WorldClient.func_72835_b()V#replace:func_73156_b()Z` -> `Reika/ChromatiCraft/Auxiliary/ChromaAux.interceptClientChunkUpdates(Lnet/minecraft/client/multiplayer/ChunkProviderClient;)Z`
- **unmapped** `client/renderer/texture/TextureAtlasSprite.func_147964_a([Ljava/awt/image/BufferedImage;Lclient/resources/data/AnimationMetadataSection;Z)V#tail` -> `Reika/ChromatiCraft/Auxiliary/ChromaAux.onIconLoad(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V`
- **unmapped** `entity/EntityLivingBase.func_70612_e(FF)V#after:ifeq` -> `Reika/ChromatiCraft/World/Dimension/SkyRiverManagerClient.stopSlowFall()Z`
- **unmapped** `world/World.func_72945_a(Lentity/Entity;Lutil/AxisAlignedBB;)Ljava/util/List;#replace:func_72314_b(DDD)Lutil/AxisAlignedBB;` -> `Reika/ChromatiCraft/Auxiliary/ChromaAux.interceptEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util`
- **unmapped** `world/gen/ChunkProviderEnd.func_73187_a([DIIIIII)[D#after:fload 22` -> `Reika/ChromatiCraft/Auxiliary/ChromaAux.getIslandBias(Lnet/minecraft/world/World;FFFII)F`

## CoFHCore-[1.7.10]3.1.4-329.jar (5 sites, 0 mapped, 5 unmapped)

- **unmapped** `client/Minecraft.func_71407_l()V#replace:func_110550_d()V` -> `cofh/asmhooks/HooksCore.tickTextures(Lnet/minecraft/client/renderer/texture/ITickable;)V`
- **unmapped** `client/multiplayer/PlayerControllerMP.func_85182_a(III)Z#replace:func_77973_b()Litem/Item;` -> `cofh/asmhooks/HooksCore.areItemsEqualHook(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z`
- **unmapped** `entity/Entity.func_70091_d(DDD)V#replace:func_72945_a(Lentity/Entity;Lutil/AxisAlignedBB;)Ljava/util/List;` -> `cofh/asmhooks/HooksCore.getEntityCollisionBoxes(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)L`
- **unmapped** `entity/item/EntityItem.func_85054_d()V#replace:func_72314_b(DDD)Lutil/AxisAlignedBB;` -> `cofh/asmhooks/HooksCore.stackItems(Lnet/minecraft/entity/item/EntityItem;)V`
- **unmapped** `inventory/Container.func_75135_a(Litem/ItemStack;IIZ)Z#const:1` -> `cofh/lib/util/helpers/InventoryHelper.mergeItemStack(Ljava/util/List;Lnet/minecraft/item/ItemStack;IIZZ)Z`

## DragonAPI 1.7.10 V33b.jar

Served under `compat/` (crafting/smelting hooks); not listed per key. 127 sites.

## FiskHeroes-1.7.10-2.4.0.jar (97 sites, 35 mapped, 62 unmapped)

- **unmapped** `block/Block.func_149646_a(Lworld/IBlockAccess;IIII)Z#tail` -> `com/fiskmods/heroes/asm/ASMHooksClient.shouldSideBeRendered(ZLnet/minecraft/block/Block;Lnet/minecraft/world/IBlockAccess;IIII)Z`
- **unmapped** `block/BlockBasePressurePlate.func_149674_a(Lworld/World;IIILjava/util/Random;)V#tail` -> `com/fiskmods/heroes/asm/ASMHooks.unpressPressurePlate(Lnet/minecraft/block/BlockBasePressurePlate;Lnet/minecraft/world/World;III)V`
- **unmapped** `block/BlockButton.func_150046_n(Lworld/World;III)V#after:ldc Lentity/projectile/EntityArrow;` -> `com/fiskmods/heroes/asm/ASMHooks.getButtonPressClass(Ljava/lang/Class;Z)Ljava/lang/Class;`
- **unmapped** `block/BlockFence.func_149743_a(Lworld/World;IIILutil/AxisAlignedBB;Ljava/util/List;Lentity/Entity;)V#cancel-after:iload 4` -> `com/fiskmods/heroes/asm/ASMHooks.addFenceCollisionBoxesToList(Lnet/minecraft/block/BlockFence;Lnet/minecraft/world/World;IIILnet/minecraft/u`
- **unmapped** `block/BlockPane.func_149743_a(Lworld/World;IIILutil/AxisAlignedBB;Ljava/util/List;Lentity/Entity;)V#head-cancel` -> `com/fiskmods/heroes/asm/ASMHooks.ignorePaneCollisionBox(Lnet/minecraft/block/BlockPane;Lnet/minecraft/entity/Entity;)Z`
- **unmapped** `block/BlockPressurePlate.func_150065_e(Lworld/World;III)I#after:invokevirtual entity/Entity.func_145773_az()Z` -> `com/fiskmods/heroes/asm/ASMHooks.doesEntityNotTriggerPressurePlate(ZLnet/minecraft/entity/Entity;Lnet/minecraft/block/BlockPressurePlate$Sen`
- **unmapped** `block/BlockPressurePlate.func_150065_e(Lworld/World;III)I#replace:func_72872_a(Ljava/lang/Class;Lutil/AxisAlignedBB;)Ljava/util/List;` -> `com/fiskmods/heroes/asm/ASMHooks.getAllowedPressureEntitiesWithinAABB(Lnet/minecraft/world/World;Ljava/lang/Class;Lnet/minecraft/util/AxisAl`
- **unmapped** `client/Minecraft.func_147115_a(Z)V#head-cancel` -> `com/fiskmods/heroes/asm/ASMHooksClient.disableLeftClick()Z`
- **unmapped** `client/Minecraft.func_147116_af()V#head-cancel` -> `com/fiskmods/heroes/asm/ASMHooksClient.disableLeftClick()Z`
- **unmapped** `client/Minecraft.func_147121_ag()V#const:4` -> `com/fiskmods/heroes/asm/ASMHooksClient.getRightClickDelay(I)I`
- **unmapped** `client/Minecraft.func_71407_l()V#after:getfield client/settings/GameSettings.field_74312_F Lclient/settings/KeyBinding;` -> `com/fiskmods/heroes/asm/ASMHooksClient.getLeftClickKey(Lnet/minecraft/client/settings/KeyBinding;)Lnet/minecraft/client/settings/KeyBinding;`
- **unmapped** `client/Minecraft.func_71407_l()V#after:getfield client/settings/GameSettings.field_74313_G Lclient/settings/KeyBinding;` -> `com/fiskmods/heroes/asm/ASMHooksClient.getRightClickKey(Lnet/minecraft/client/settings/KeyBinding;)Lnet/minecraft/client/settings/KeyBinding`
- **unmapped** `client/Minecraft.func_71407_l()V#after:invokespecial client/Minecraft.func_147112_ai()V` -> `com/fiskmods/heroes/asm/ASMHooksClient.onPickBlock()V`
- **unmapped** `client/audio/SoundManager.func_148611_c(Lclient/audio/ISound;)V#after:ldc 16.0` -> `com/fiskmods/heroes/asm/ASMHooksClient.getSoundRange(FLnet/minecraft/client/audio/ISound;)F`
- **unmapped** `client/entity/EntityOtherPlayerMP.func_70071_h_()V#after:invokestatic util/MathHelper.func_76133_a(D)F` -> `com/fiskmods/heroes/asm/ASMHooks.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- mapped `client/entity/EntityPlayerSP.func_70031_b(Z)V#cancel-after:iload 1` -> `com/fiskmods/heroes/asm/ASMHooksClient.disableSprinting(Lnet/minecraft/client/entity/EntityPlayerSP;Z)Z`
- mapped `client/entity/EntityPlayerSP.func_70636_d()V#after:aload 0` -> `com/fiskmods/heroes/asm/ASMHooksClient.updatePlayerMovementInput(Lnet/minecraft/client/entity/EntityPlayerSP;)V`
- **unmapped** `client/entity/EntityPlayerSP.func_70636_d()V#after:ldc 0.2` -> `com/fiskmods/heroes/asm/ASMHooksClient.getScaledSneakOffset(Lnet/minecraft/entity/EntityLivingBase;D)D`
- mapped `client/entity/EntityPlayerSP.func_71151_f()F#replace:func_75094_b()F` -> `com/fiskmods/heroes/asm/ASMHooksClient.getScaledWalkSpeedForFOV(Lnet/minecraft/entity/player/PlayerCapabilities;Lnet/minecraft/client/entity`
- **unmapped** `client/gui/inventory/GuiContainerCreative.func_146274_d()V#after:invokestatic org/lwjgl/input/Mouse.getEventDWheel()I` -> `com/fiskmods/heroes/asm/ASMHooksClient.handleCreativeTabScroll(ILnet/minecraft/client/gui/inventory/GuiContainerCreative;)I`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#after:invokestatic org/lwjgl/opengl/GL11.glScalef(FFF)V` -> `com/fiskmods/heroes/asm/ASMHooksClient.getInventoryPlayerOffsetPost(IILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#before:iload 2` -> `com/fiskmods/heroes/asm/ASMHooksClient.getInventoryPlayerScale(ILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#before:invokestatic org/lwjgl/opengl/GL11.glScalef(FFF)V` -> `com/fiskmods/heroes/asm/ASMHooksClient.getInventoryPlayerScale(ILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/gui/inventory/GuiInventory.func_147046_a(IIIFFLentity/EntityLivingBase;)V#before:ldc 50.0` -> `com/fiskmods/heroes/asm/ASMHooksClient.getInventoryPlayerOffset(IILnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/model/ModelBiped.func_78087_a(FFFFFFLentity/Entity;)V#after:aload 0` -> `com/fiskmods/heroes/client/model/ModelHelper.setRotationAnglesPre(Lnet/minecraft/client/model/ModelBiped;FFFFFFLnet/minecraft/entity/Entity;`
- **unmapped** `client/model/ModelBiped.func_78087_a(FFFFFFLentity/Entity;)V#tail` -> `com/fiskmods/heroes/client/model/ModelHelper.setRotationAngles(Lnet/minecraft/client/model/ModelBiped;FFFFFFLnet/minecraft/entity/Entity;)V`
- **unmapped** `client/model/ModelBiped.func_78088_a(Lentity/Entity;FFFFFF)V#after:aload 0` -> `com/fiskmods/heroes/client/model/ModelHelper.renderBipedPre(Lnet/minecraft/client/model/ModelBiped;Lnet/minecraft/entity/Entity;FFFFFF)V`
- mapped `client/multiplayer/PlayerControllerMP.func_78751_a(IIII)Z#head` -> `com/fiskmods/heroes/asm/ASMHooksClient.disableBlockDestruction()Z`
- **unmapped** `client/particle/EffectRenderer.func_78869_b()Ljava/lang/String;#replace:size()I` -> `com/fiskmods/heroes/client/particle/SHParticlesClient.getParticlesInWorld([Ljava/util/List;)I`
- **unmapped** `client/particle/EffectRenderer.func_78874_a(Lentity/Entity;F)V#after:invokestatic org/lwjgl/opengl/GL11.glAlphaFunc(IF)V` -> `com/fiskmods/heroes/asm/transformers/CTEffectRenderer.plus(I)I`
- **unmapped** `client/particle/EffectRenderer.func_78874_a(Lentity/Entity;F)V#after:invokevirtual client/renderer/Tessellator.func_78382_b()V` -> `com/fiskmods/heroes/client/particle/SHParticlesClient.bindParticleTextures(I)V`
- mapped `client/renderer/EntityRenderer.func_78472_g(F)V#after:invokevirtual client/entity/EntityClientPlayerMP.func_70644_a(Lpotion/Potion;)Z` -> `com/fiskmods/heroes/asm/ASMHooksClient.hasNightVisionOverride(Z)Z`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:aload 2` -> `com/fiskmods/heroes/asm/ASMHooksClient.getDistanceWalked(FLnet/minecraft/entity/Entity;F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:getfield entity/player/EntityPlayer.field_70140_Q F` -> `com/fiskmods/heroes/asm/ASMHooksClient.getDistanceWalked(FLnet/minecraft/entity/Entity;F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:getfield entity/player/EntityPlayer.field_70141_P F` -> `com/fiskmods/heroes/asm/ASMHooksClient.getDistanceWalked(FLnet/minecraft/entity/Entity;F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:invokestatic java/lang/Math.abs(F)F` -> `com/fiskmods/heroes/asm/ASMHooksClient.getViewBobbing(FLnet/minecraft/entity/Entity;F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78475_f(F)V#after:ldc 0.5` -> `com/fiskmods/heroes/asm/ASMHooksClient.getViewBobbing(FLnet/minecraft/entity/Entity;F)F`
- **unmapped** `client/renderer/EntityRenderer.func_78479_a(FI)V#after:aload 0` -> `com/fiskmods/heroes/asm/ASMHooksClient.orientCameraPost(Lnet/minecraft/client/renderer/EntityRenderer;F)V`
- **unmapped** `client/renderer/EntityRenderer.func_78479_a(FI)V#after:fload 1` -> `com/fiskmods/heroes/asm/ASMHooksClient.orientCameraPre(Lnet/minecraft/client/renderer/EntityRenderer;F)V`
- **unmapped** `client/renderer/EntityRenderer.func_78480_b(F)V#after:ifeq` -> `com/fiskmods/heroes/client/FlightMouseManager.shouldOverride()Z`
- mapped `client/renderer/EntityRenderer.func_82830_a(Lentity/player/EntityPlayer;F)F#after:aload 1` -> `com/fiskmods/heroes/asm/ASMHooksClient.hasNightVisionPotion(Lnet/minecraft/entity/player/EntityPlayer;)Z`
- mapped `client/renderer/EntityRenderer.func_82830_a(Lentity/player/EntityPlayer;F)F#tail` -> `com/fiskmods/heroes/asm/ASMHooksClient.getNightVisionAmount(FLnet/minecraft/entity/player/EntityPlayer;F)F`
- **unmapped** `client/renderer/ItemRenderer.func_78440_a(F)V#after:getfield client/renderer/ItemRenderer.field_78453_b Litem/ItemStack;` -> `com/fiskmods/heroes/asm/ASMHooksClient.getFPHeldItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/it`
- **unmapped** `client/renderer/ItemRenderer.func_78440_a(F)V#tail` -> `com/fiskmods/heroes/client/SHRenderHooks.renderItemInFirstPerson(Lnet/minecraft/client/renderer/ItemRenderer;F)V`
- **unmapped** `client/renderer/RenderGlobal.func_72726_b(Ljava/lang/String;DDDDDD)Lclient/particle/EntityFX;#after:invokespecial client/particle/EntitySpellParticleFX.<init>(Lworld/World;DDDDDD)V` -> `com/fiskmods/heroes/asm/ASMHooksClient.getPotionParticleScale(Ljava/lang/String;)F`
- **unmapped** `client/renderer/entity/RenderBiped.func_76986_a(Lentity/EntityLiving;DDDFF)V#const:0.125` -> `com/fiskmods/heroes/asm/ASMHooksClient.getScaledSneakOffset(Lnet/minecraft/entity/EntityLivingBase;D)D`
- **unmapped** `client/renderer/entity/RenderPlayer.func_76986_a(Lclient/entity/AbstractClientPlayer;DDDFF)V#const:0.125` -> `com/fiskmods/heroes/asm/ASMHooksClient.getScaledSneakOffset(Lnet/minecraft/entity/EntityLivingBase;D)D`
- **unmapped** `client/renderer/entity/RenderPlayer.func_77039_a(Lentity/EntityLivingBase;DDD)V#replace:func_77039_a(Lclient/entity/AbstractClientPlayer;DDD)V` -> `com/fiskmods/heroes/asm/ASMHooksClient.applyPlayerRenderTranslation(Lnet/minecraft/client/renderer/entity/RenderPlayer;Lnet/minecraft/client`
- **unmapped** `client/renderer/entity/RenderPlayer.func_77043_a(Lclient/entity/AbstractClientPlayer;FFF)V#tail` -> `com/fiskmods/heroes/client/model/ModelHelper.rotateCorpse(Lnet/minecraft/entity/player/EntityPlayer;F)V`
- **unmapped** `client/renderer/entity/RenderPlayer.func_82441_a(Lentity/player/EntityPlayer;)V#head` -> `com/fiskmods/heroes/asm/ASMHooksClient.renderFirstPersonArm(Lnet/minecraft/client/renderer/entity/RenderPlayer;Lnet/minecraft/entity/player/`
- **unmapped** `client/renderer/entity/RenderPlayer.func_82441_a(Lentity/player/EntityPlayer;)V#tail` -> `com/fiskmods/heroes/asm/ASMHooksClient.renderFirstPersonArmPost(Lnet/minecraft/client/renderer/entity/RenderPlayer;Lnet/minecraft/entity/pla`
- **unmapped** `client/renderer/entity/RendererLivingEntity.func_76986_a(Lentity/EntityLivingBase;DDDFF)V#after:fadd` -> `com/fiskmods/heroes/client/model/ModelHelper.getLimbSwingAmount(FLnet/minecraft/entity/EntityLivingBase;)F`
- **unmapped** `client/renderer/entity/RendererLivingEntity.func_76986_a(Lentity/EntityLivingBase;DDDFF)V#after:fsub` -> `com/fiskmods/heroes/client/model/ModelHelper.getLimbSwing(FLnet/minecraft/entity/EntityLivingBase;)F`
- mapped `entity/Entity.func_70030_z()V#after:invokevirtual entity/Entity.func_70051_ag()Z` -> `com/fiskmods/heroes/asm/ASMHooks.shouldProduceSprintParticles(ZLnet/minecraft/entity/Entity;)Z`
- **unmapped** `entity/Entity.func_70053_R()F#tail` -> `com/fiskmods/heroes/asm/ASMHooksClient.getShadowHeight(FLnet/minecraft/entity/Entity;)F`
- **unmapped** `entity/Entity.func_70070_b(F)I#replace:func_76128_c(D)I` -> `com/fiskmods/heroes/asm/ASMHooksClient.getBrightnessY(Lnet/minecraft/entity/Entity;)I`
- **unmapped** `entity/Entity.func_70091_d(DDD)V#after:aload 0` -> `com/fiskmods/heroes/asm/ASMHooks.moveEntityStepDown(Lnet/minecraft/entity/Entity;DD)D`
- mapped `entity/Entity.func_70091_d(DDD)V#const:1` -> `com/fiskmods/heroes/asm/ASMHooks.getStepDistance(Lnet/minecraft/entity/Entity;I)F`
- **unmapped** `entity/Entity.func_70091_d(DDD)V#replace:func_72317_d(DDD)Lutil/AxisAlignedBB;` -> `com/fiskmods/heroes/asm/ASMHooks.noClipCollide(Lnet/minecraft/entity/Entity;DDD)V`
- mapped `entity/EntityFlying.func_70612_e(FF)V#tail` -> `com/fiskmods/heroes/asm/ASMHooks.applyEntityGravity(Lnet/minecraft/entity/Entity;)V`
- mapped `entity/EntityLivingBase.func_70612_e(FF)V#after:invokestatic util/MathHelper.func_76133_a(D)F` -> `com/fiskmods/heroes/asm/ASMHooks.getModifiedEntityScale(Lnet/minecraft/entity/Entity;)F`
- mapped `entity/EntityLivingBase.func_70612_e(FF)V#after:ldc 0.08` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/EntityLivingBase.func_70612_e(FF)V#head-cancel` -> `com/fiskmods/heroes/asm/ASMHooks.moveEntityWithHeading(Lnet/minecraft/entity/EntityLivingBase;FF)Z`
- mapped `entity/EntityLivingBase.func_70679_bo()V#const:"mobSpell"` -> `com/fiskmods/heroes/asm/ASMHooks.getPotionParticleName(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Ljava/lang/String;`
- mapped `entity/EntityLivingBase.func_70679_bo()V#const:"mobSpellAmbient"` -> `com/fiskmods/heroes/asm/ASMHooks.getPotionParticleName(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Ljava/lang/String;`
- mapped `entity/EntityLivingBase.func_82166_i()I#tail` -> `com/fiskmods/heroes/asm/ASMHooks.getArmSwingAnimationEnd(ILnet/minecraft/entity/EntityLivingBase;)I`
- **unmapped** `entity/EntityTracker.func_72785_a(Lentity/Entity;IIZ)V#after:getfield entity/EntityTracker.field_72792_d I` -> `com/fiskmods/heroes/asm/ASMHooks.getMaxEntityTrackingRange(ILnet/minecraft/entity/Entity;)I`
- mapped `entity/EntityTracker.func_72785_a(Lentity/Entity;IIZ)V#after:iload 2` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityTrackingRange(ILnet/minecraft/entity/Entity;I)I`
- **unmapped** `entity/ai/EntityAINearestAttackableTarget.func_75250_a()Z#replace:func_82733_a(Ljava/lang/Class;Lutil/AxisAlignedBB;Lcommand/IEntitySelector;)Ljava/util/List;` -> `com/fiskmods/heroes/asm/ASMHooks.targetableEntitiesWithinAABB(Lnet/minecraft/entity/EntityCreature;Ljava/lang/Class;Lnet/minecraft/util/Axis`
- mapped `entity/item/EntityFallingBlock.func_70071_h_()V#after:ldc 0.03999999910593033` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/item/EntityFireworkRocket.func_70071_h_()V#after:ldc 0.04` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityAcceleration(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/item/EntityItem.func_70071_h_()V#after:ldc 0.03999999910593033` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/item/EntityMinecart.func_70071_h_()V#after:ldc 0.03999999910593033` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/item/EntityTNTPrimed.func_70071_h_()V#after:ldc 0.03999999910593033` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/passive/EntitySquid.func_70636_d()V#tail` -> `com/fiskmods/heroes/asm/ASMHooks.applyEntityGravity(Lnet/minecraft/entity/Entity;)V`
- **unmapped** `entity/player/EntityPlayer.func_70620_b(Litem/ItemStack;I)Lutil/IIcon;#const:13` -> `com/fiskmods/heroes/asm/ASMHooksClient.getBowIconTime(Lnet/minecraft/entity/player/EntityPlayer;I)I`
- **unmapped** `entity/player/EntityPlayer.func_70620_b(Litem/ItemStack;I)Lutil/IIcon;#const:18` -> `com/fiskmods/heroes/asm/ASMHooksClient.getBowIconTime(Lnet/minecraft/entity/player/EntityPlayer;I)I`
- mapped `entity/player/EntityPlayer.func_71059_n(Lentity/Entity;)V#replace:func_70097_a(Lutil/DamageSource;F)Z` -> `com/fiskmods/heroes/asm/ASMHooks.attackEntityFrom(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;FLnet/minecraft/entity/playe`
- mapped `entity/player/InventoryPlayer.func_70448_g()Litem/ItemStack;#tail` -> `com/fiskmods/heroes/asm/ASMHooks.getCurrentItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/InventoryPlayer;)Lnet/minecraft/i`
- mapped `entity/projectile/EntityArrow.func_70071_h_()V#after:f2d` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `entity/projectile/EntityThrowable.func_70071_h_()V#after:invokevirtual entity/projectile/EntityThrowable.func_70185_h()F` -> `com/fiskmods/heroes/asm/ASMHooks.getEntityGravity(DLnet/minecraft/entity/Entity;)D`
- mapped `item/ItemBow.func_77615_a(Litem/ItemStack;Lworld/World;Lentity/player/EntityPlayer;I)V#const:20.0` -> `com/fiskmods/heroes/asm/ASMHooks.getDrawbackTime(Lnet/minecraft/entity/player/EntityPlayer;)F`
- mapped `item/ItemStack.func_77963_c(Lnbt/NBTTagCompound;)V#tail` -> `com/fiskmods/heroes/asm/ASMHooks.readFromNBT(Lnet/minecraft/item/ItemStack;Lnet/minecraft/nbt/NBTTagCompound;)V`
- **unmapped** `network/NetHandlerPlayServer.func_147340_a(Lnetwork/play/client/C02PacketUseEntity;)V#after:dload 5` -> `com/fiskmods/heroes/asm/ASMHooks.getReachDistanceSq(Lnet/minecraft/network/NetHandlerPlayServer;D)D`
- **unmapped** `network/NetHandlerPlayServer.func_147340_a(Lnetwork/play/client/C02PacketUseEntity;)V#before:ifne` -> `com/fiskmods/heroes/asm/ASMHooks.isArrow(Lnet/minecraft/entity/Entity;)Z`
- mapped `server/MinecraftServer.run()V#const:50` -> `com/fiskmods/heroes/common/event/TickrateHandler.getModifiedTickRate(J)J`
- mapped `util/FoodStats.func_75118_a(Lentity/player/EntityPlayer;)V#const:80` -> `com/fiskmods/heroes/asm/ASMHooks.getHealRate(Lnet/minecraft/entity/player/EntityPlayer;I)I`
- **unmapped** `world/ChunkCache.func_72810_a(Lworld/EnumSkyBlock;III)I#tail` -> `com/fiskmods/heroes/asm/ASMHooksClient.getSkyBlockTypeBrightness(ILnet/minecraft/world/ChunkCache;Lnet/minecraft/world/EnumSkyBlock;III)I`
- **unmapped** `world/World.func_147465_d(IIILblock/Block;II)Z#after:aload 4` -> `com/fiskmods/heroes/asm/ASMHooks.replaceSetBlock(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;II)Lnet/minecraft/block/Block;`
- **unmapped** `world/World.func_147465_d(IIILblock/Block;II)Z#after:iload 5` -> `com/fiskmods/heroes/asm/ASMHooks.replaceSetBlock(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;II)Lnet/minecraft/block/Block;`
- **unmapped** `world/World.func_147465_d(IIILblock/Block;II)Z#after:sipush 256` -> `com/fiskmods/heroes/asm/ASMHooks.skipSetBlock(ILnet/minecraft/world/World;IIILnet/minecraft/block/Block;II)I`
- mapped `world/World.func_72835_b()V#head-cancel` -> `com/fiskmods/heroes/asm/ASMHooks.skipWeatherTick(Lnet/minecraft/world/World;)Z`
- **unmapped** `world/World.func_72856_b(Lentity/Entity;D)Lentity/player/EntityPlayer;#replace:func_72846_b(DDDD)Lentity/player/EntityPlayer;` -> `com/fiskmods/heroes/asm/ASMHooks.getClosestVulnerablePlayer(Lnet/minecraft/entity/Entity;DDDD)Lnet/minecraft/entity/player/EntityPlayer;`
- mapped `world/World.func_72864_z(III)Z#tail` -> `com/fiskmods/heroes/asm/ASMHooks.isBlockIndirectlyGettingPowered(ZLnet/minecraft/world/World;III)Z`
- mapped `world/World.func_72878_l(IIII)I#tail` -> `com/fiskmods/heroes/asm/ASMHooks.getIndirectPowerLevelTo(ILnet/minecraft/world/World;Lnet/minecraft/block/Block;III)I`
- mapped `world/World.func_72900_e(Lentity/Entity;)V#tail` -> `com/fiskmods/heroes/asm/ASMHooks.onEntityRemoved(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)V`
- **unmapped** `world/World.func_72925_a(Lworld/EnumSkyBlock;III)I#tail` -> `com/fiskmods/heroes/asm/ASMHooksClient.getSkyBlockTypeBrightness(ILnet/minecraft/world/World;Lnet/minecraft/world/EnumSkyBlock;III)I`

## Galacticraft-1.7-3.0.12.504.jar (13 sites, 5 mapped, 8 unmapped)

- mapped `client/gui/GuiSleepMP.func_146418_g()V#tail` -> `micdoodle8/mods/miccore/MicdoodlePlugin.onSleepCancelled()V`
- **unmapped** `client/particle/EffectRenderer.func_78874_a(Lentity/Entity;F)V#head` -> `micdoodle8/mods/galacticraft/core/proxy/ClientProxyCore.renderFootprints(F)V`
- **unmapped** `client/renderer/EntityRenderer.func_78466_h(F)V#after:astore 5` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getSkyColorHook(Lnet/minecraft/world/World;)Lnet/minecraft/util/Vec3;`
- **unmapped** `client/renderer/EntityRenderer.func_78466_h(F)V#after:astore 9` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getFogColorHook(Lnet/minecraft/world/World;)Lnet/minecraft/util/Vec3;`
- **unmapped** `client/renderer/EntityRenderer.func_78467_g(F)V#tail` -> `micdoodle8/mods/galacticraft/core/proxy/ClientProxyCore.orientCamera(F)V`
- **unmapped** `client/renderer/EntityRenderer.func_78472_g(F)V#after:fstore 15` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getColorRed(Lnet/minecraft/world/World;)F`
- **unmapped** `client/renderer/ItemRenderer.func_78447_b(F)V#after:sipush 3008` -> `micdoodle8/mods/galacticraft/core/proxy/ClientProxyCore.renderLiquidOverlays(F)V`
- **unmapped** `client/renderer/entity/RendererLivingEntity.func_77036_a(Lentity/EntityLivingBase;FFFFFF)V#tail` -> `micdoodle8/mods/galacticraft/core/client/render/entities/RenderPlayerGC.renderModelS(Lnet/minecraft/client/renderer/entity/RendererLivingEnt`
- mapped `entity/EntityLivingBase.func_70612_e(FF)V#const:0.08` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getGravityForEntity(Lnet/minecraft/entity/Entity;)D`
- mapped `entity/item/EntityItem.func_70071_h_()V#const:0.03999999910593033` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getItemGravity(Lnet/minecraft/entity/item/EntityItem;)D`
- mapped `entity/projectile/EntityArrow.func_70071_h_()V#const:0.05` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getArrowGravity(Lnet/minecraft/entity/projectile/EntityArrow;)F`
- mapped `world/World.func_72867_j(F)F#after:aload 0` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.getRainStrength(Lnet/minecraft/world/World;F)F`
- **unmapped** `world/gen/ChunkProviderServer.func_73153_a(Lworld/chunk/IChunkProvider;II)V#after:invokeinterface world/chunk/IChunkProvider.func_73153_a(Lworld/chunk/IChunkProvider;II)V` -> `micdoodle8/mods/galacticraft/core/util/WorldUtil.otherModPreventGenerate(IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvi`

## NotEnoughItems-1.7.10-1.0.5.120-universal.jar (3 sites, 0 mapped, 3 unmapped)

- **unmapped** `inventory/ContainerWorkbench.func_82846_b(Lentity/player/EntityPlayer;I)Litem/ItemStack;#const:1` -> `codechicken/lib/inventory/InventoryUtils.insertItem(Lcodechicken/lib/inventory/InventoryRange;Lnet/minecraft/item/ItemStack;Z)I`
- **unmapped** `nbt/NBTTagCompound.toString()Ljava/lang/String;#after:invokevirtual java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;` -> `codechicken/nei/NEIServerUtils.fixTrailingCommaList(Ljava/lang/StringBuilder;)Ljava/lang/StringBuilder;`
- **unmapped** `nbt/NBTTagList.toString()Ljava/lang/String;#after:invokevirtual java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;` -> `codechicken/nei/NEIServerUtils.fixTrailingCommaList(Ljava/lang/StringBuilder;)Ljava/lang/StringBuilder;`

## PlayerAPI-1.5.0.jar (35 sites, 0 mapped, 35 unmapped)

PlayerAPI replaces the player classes' method bodies (`#replace:` of whole methods, constructors, `EntityPlayerMP.<init>` overrides); not a site list, needs the player class swap (structural). Not mapped.


## RotaryCraft 1.7.10 V33a.jar (1 sites, 0 mapped, 1 unmapped)

- **unmapped** `tileentity/TileEntityFurnace.func_145845_h()V#head-cancel` -> `Reika/RotaryCraft/TileEntities/Auxiliary/TileEntityFurnaceHeater.isHijacked(Lnet/minecraft/tileentity/TileEntityFurnace;)Z`

## TheBetweenlands-1.0.6-alpha-universal.jar (13 sites, 2 mapped, 11 unmapped)

- **unmapped** `client/Minecraft.func_147115_a(Z)V#before:aload 0` -> `thebetweenlands/forgeevent/BLForgeHooksClient.handleBlockBreakingInput(ZI)I`
- **unmapped** `client/Minecraft.func_147116_af()V#replace:func_71038_i()V` -> `thebetweenlands/forgeevent/BLForgeHooksClient.handlePlayerAttackInput(I)I`
- mapped `client/Minecraft.func_71384_a()V#tail` -> `thebetweenlands/event/debugging/DebugHandlerClient.onMinecraftFinishedStarting()V`
- **unmapped** `client/Minecraft.func_71407_l()V#const:1` -> `thebetweenlands/client/perspective/Perspective.cyclePerspective()I`
- **unmapped** `client/Minecraft.func_71411_J()V#const:0` -> `thebetweenlands/client/perspective/Perspective.getInsideOpaqueBlockView()I`
- **unmapped** `client/renderer/ActiveRenderInfo.func_74583_a(Lentity/player/EntityPlayer;Z)V#replace:func_76134_b(F)F` -> `thebetweenlands/client/perspective/Perspective.updateRenderInfo(FFLjava/nio/FloatBuffer;Ljava/nio/FloatBuffer;Ljava/nio/IntBuffer;Ljava/nio/`
- **unmapped** `client/renderer/EntityRenderer.func_78467_g(F)V#replace:glRotatef(FFFF)V` -> `thebetweenlands/client/perspective/Perspective.orient(F)Z`
- **unmapped** `client/renderer/EntityRenderer.func_78473_a(F)V#cancel-after:putfield client/Minecraft.field_147125_j Lentity/Entity;` -> `thebetweenlands/forgeevent/BLForgeHooksClient.getMouseOverHook()Z`
- **unmapped** `client/renderer/EntityRenderer.func_78476_b(FI)V#tail` -> `thebetweenlands/forgeevent/BLForgeHooksClient.postRenderHandEvent(FI)V`
- **unmapped** `client/renderer/entity/RenderManager.func_147938_a(Lworld/World;Lclient/renderer/texture/TextureManager;Lclient/gui/FontRenderer;Lentity/EntityLivingBase;Lentity/Entity;Lclient/settings/GameSettings;F)V#tail` -> `thebetweenlands/client/perspective/Perspective.cacheActiveRenderInfo()V`
- **unmapped** `entity/Entity.func_70082_c(FF)V#const:0.15` -> `thebetweenlands/client/perspective/Perspective.setAngles(Lnet/minecraft/entity/Entity;FF)V`
- mapped `entity/player/EntityPlayer.func_70621_aR()Ljava/lang/String;#const:"game.player.hurt"` -> `thebetweenlands/forgeevent/BLForgeHooks.onPlayerGetHurtSound(Lnet/minecraft/entity/player/EntityPlayer;)Ljava/lang/String;`
- **unmapped** `server/management/ServerConfigurationManager.func_148545_a(Lcom/mojang/authlib/GameProfile;)Lentity/player/EntityPlayerMP;#replace:<init>(Lserver/MinecraftServer;Lworld/WorldServer;Lcom/mojang/authlib/GameProfile;Lserver/management/ItemInWorldManager;)V` -> `thebetweenlands/forgeevent/BLForgeHooks.createPlayerForUser(Lnet/minecraft/server/management/ServerConfigurationManager;Lcom/mojang/authlib/`

## Notes for the next round

- Shared and still open: `EntityLivingBase.func_70612_e` (travel) has several additional hooks (after `ifeq`, `sqrt`); `World.func_72856_b` (closest vulnerable player, 1Legends + Fisk); `Entity.func_70070_b`/`ChunkCache.func_72810_a`/`World.func_72925_a` (brightness, client light); render sites (`EntityRenderer`, `RenderPlayer`, `ModelBiped`, `GuiInventory`) need the legacy GL state.
- CoFH: `Entity.moveEntity` x5 `#replace:getCollidingBoundingBoxes`, `EntityItem.searchForOtherItemsNearby` (item stacking), `Container.mergeItemStack#const:1` (CoFH `InventoryHelper.mergeItemStack`, 6 args): the semantics of the replaced calls are not documented by the key; read `cofh.asm.HooksCore` before mapping.
- RotaryCraft `TileEntityFurnace.updateEntity#head-cancel`: the shim has a furnace view (`TileEntityFurnace(AbstractFurnaceBlockEntity)`), a mixin on `AbstractFurnaceBlockEntity.serverTick` could call it once the view carries world and position.
- NEI `NBTTagCompound/NBTTagList.toString` `fixTrailingCommaList`: needs no place (the modern NBT `toString` has no trailing comma).
- Galacticraft `ChunkProviderServer.populate` and ChromatiCraft `ChunkProviderEnd` (island bias) hook terrain generation of vanilla chunk providers: no direct modern place.
- Anti-Gravity `Item.getMovingObjectPositionFromPlayer#head` needs a `MovingObjectPosition` to `BlockHitResult` conversion; `NetHandlerPlayServer.processPlayer` hooks (kick checks) have no modern counterpart.
