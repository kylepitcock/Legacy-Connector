#!/usr/bin/env python3
"""Writes src/main/resources/legacyconnector/legacy/1.7.10/texture-renames.txt: 1.7.10 vanilla block/item texture names
(`blocks/<name>`, `items/<name>`) that 1.13+ renamed, with their modern `block/<name>` / `item/<name>`.
usage: tools/gen_texture_renames.py <client-1.7.10.jar> <1.21 client-extra.jar>"""
import sys, zipfile, re, os

old = zipfile.ZipFile(sys.argv[1]); new = zipfile.ZipFile(sys.argv[2])
def names(z, folder):
    p = f"assets/minecraft/textures/{folder}/"
    return {n[len(p):-4] for n in z.namelist() if n.startswith(p) and n.endswith('.png') and '/' not in n[len(p):]}
old_b, old_i = names(old, 'blocks'), names(old, 'items')
new_b, new_i = names(new, 'block'), names(new, 'item')

COLORS = {'silver': 'light_gray'}
def color(c): return COLORS.get(c, c)
wood = {'oak': 'oak', 'spruce': 'spruce', 'birch': 'birch', 'jungle': 'jungle', 'acacia': 'acacia', 'big_oak': 'dark_oak', 'roofed_oak': 'dark_oak'}

def block_rule(n):
    m = re.fullmatch(r'wool_colored_(\w+)', n)
    if m: return f"{color(m[1])}_wool"
    m = re.fullmatch(r'hardened_clay_stained_(\w+)', n)
    if m: return f"{color(m[1])}_terracotta"
    m = re.fullmatch(r'glass_pane_top_(\w+)', n)
    if m: return f"{color(m[1])}_stained_glass_pane_top"
    m = re.fullmatch(r'glass_(\w+)', n)
    if m and m[1] not in ('pane_top',): return f"{color(m[1])}_stained_glass"
    m = re.fullmatch(r'(log|planks|sapling)_(\w+?)(_top)?', n)
    if m and m[2] in wood:
        w = wood[m[2]]
        return {'log': f"{w}_log{'_top' if m[3] else ''}", 'planks': f"{w}_planks", 'sapling': f"{w}_sapling"}[m[1]]
    m = re.fullmatch(r'leaves_(\w+?)(_opaque)?', n)
    if m and m[1] in wood: return f"{wood[m[1]]}_leaves"
    m = re.fullmatch(r'(\w+?)_stage_(\d+)', n)
    if m: return f"{ {'wheat':'wheat','carrots':'carrots','potatoes':'potatoes','nether_wart':'nether_wart','cocoa':'cocoa'}.get(m[1], m[1]) }_stage{m[2]}"
    return None

EXPLICIT = {
 'brick': 'bricks', 'cobblestone_mossy': 'mossy_cobblestone', 'deadbush': 'dead_bush', 'dirt_podzol_side': 'podzol_side',
 'dirt_podzol_top': 'podzol_top', 'door_iron_lower': 'iron_door_bottom', 'door_iron_upper': 'iron_door_top',
 'door_wood_lower': 'oak_door_bottom', 'door_wood_upper': 'oak_door_top', 'farmland_dry': 'farmland', 'farmland_wet': 'farmland_moist',
 'flower_allium': 'allium', 'flower_blue_orchid': 'blue_orchid', 'flower_dandelion': 'dandelion', 'flower_houstonia': 'azure_bluet',
 'flower_oxeye_daisy': 'oxeye_daisy', 'flower_paeonia': 'peony_top', 'flower_rose': 'poppy', 'flower_tulip_orange': 'orange_tulip',
 'flower_tulip_pink': 'pink_tulip', 'flower_tulip_red': 'red_tulip', 'flower_tulip_white': 'white_tulip',
 'furnace_front_off': 'furnace_front', 'grass_side': 'grass_block_side', 'grass_side_overlay': 'grass_block_side_overlay',
 'grass_side_snowed': 'grass_block_snow', 'grass_top': 'grass_block_top', 'hardened_clay': 'terracotta', 'ice_packed': 'packed_ice',
 'mob_spawner': 'spawner', 'mushroom_block_skin_brown': 'brown_mushroom_block', 'mushroom_block_skin_red': 'red_mushroom_block',
 'mushroom_block_skin_stem': 'mushroom_stem', 'mushroom_brown': 'brown_mushroom', 'mushroom_red': 'red_mushroom',
 'nether_brick': 'nether_bricks', 'noteblock': 'note_block', 'piston_top_normal': 'piston_top', 'portal': 'nether_portal',
 'pumpkin_face_off': 'carved_pumpkin', 'pumpkin_face_on': 'jack_o_lantern', 'quartz_block_chiseled': 'chiseled_quartz_block',
 'quartz_block_chiseled_top': 'chiseled_quartz_block_top', 'quartz_block_lines': 'quartz_pillar', 'quartz_block_lines_top': 'quartz_pillar_top',
 'quartz_ore': 'nether_quartz_ore', 'rail_activator': 'activator_rail', 'rail_activator_powered': 'activator_rail_on',
 'rail_detector': 'detector_rail', 'rail_detector_powered': 'detector_rail_on', 'rail_golden': 'powered_rail',
 'rail_golden_powered': 'powered_rail_on', 'rail_normal': 'rail', 'rail_normal_turned': 'rail_corner',
 'redstone_lamp_off': 'redstone_lamp', 'redstone_torch_on': 'redstone_torch', 'reeds': 'sugar_cane', 'repeater_off': 'repeater',
 'comparator_off': 'comparator', 'sandstone_carved': 'chiseled_sandstone', 'sandstone_normal': 'sandstone', 'sandstone_smooth': 'cut_sandstone',
 'stone_slab_side': 'smooth_stone_slab_side', 'stone_slab_top': 'smooth_stone', 'stonebrick': 'stone_bricks',
 'stonebrick_carved': 'chiseled_stone_bricks', 'stonebrick_cracked': 'cracked_stone_bricks', 'stonebrick_mossy': 'mossy_stone_bricks',
 'tallgrass': 'short_grass', 'torch_on': 'torch', 'trapdoor': 'oak_trapdoor', 'trip_wire': 'tripwire', 'trip_wire_source': 'tripwire_hook',
 'waterlily': 'lily_pad', 'web': 'cobweb', 'command_block': 'command_block_front', 'dispenser_front_horizontal': 'dispenser_front',
 'dropper_front_horizontal': 'dropper_front', 'endframe_eye': 'end_portal_frame_eye', 'endframe_side': 'end_portal_frame_side',
 'endframe_top': 'end_portal_frame_top', 'fire_layer_0': 'fire_0', 'fire_layer_1': 'fire_1', 'itemframe_background': 'item_frame',
 'melon_stem_connected': 'attached_melon_stem', 'melon_stem_disconnected': 'melon_stem',
 'pumpkin_stem_connected': 'attached_pumpkin_stem', 'pumpkin_stem_disconnected': 'pumpkin_stem',
 'double_plant_fern_bottom': 'large_fern_bottom', 'double_plant_fern_top': 'large_fern_top',
 'double_plant_grass_bottom': 'tall_grass_bottom', 'double_plant_grass_top': 'tall_grass_top',
 'double_plant_paeonia_bottom': 'peony_bottom', 'double_plant_paeonia_top': 'peony_top',
 'double_plant_rose_bottom': 'rose_bush_bottom', 'double_plant_rose_top': 'rose_bush_top',
 'double_plant_sunflower_back': 'sunflower_back', 'double_plant_sunflower_bottom': 'sunflower_bottom',
 'double_plant_sunflower_front': 'sunflower_front', 'double_plant_sunflower_top': 'sunflower_top',
 'double_plant_syringa_bottom': 'lilac_bottom', 'double_plant_syringa_top': 'lilac_top',
 'anvil_base': 'anvil', 'anvil_top_damaged_0': 'anvil_top', 'anvil_top_damaged_1': 'chipped_anvil_top', 'anvil_top_damaged_2': 'damaged_anvil_top',
 'redstone_dust_cross': 'redstone_dust_dot', 'redstone_dust_line': 'redstone_dust_line0',
}
EXPLICIT_ITEMS = {
 'bucket_empty': 'bucket', 'bucket_water': 'water_bucket', 'bucket_lava': 'lava_bucket', 'bucket_milk': 'milk_bucket',
 'book_normal': 'book', 'book_enchanted': 'enchanted_book', 'book_writable': 'writable_book', 'book_written': 'written_book',
 'bow_standby': 'bow', 'brewing_stand': 'brewing_stand', 'carrot': 'carrot', 'carrot_golden': 'golden_carrot',
 'carrot_on_a_stick': 'carrot_on_a_stick', 'cauldron': 'cauldron', 'chicken_cooked': 'cooked_chicken', 'chicken_raw': 'chicken',
 'clay_ball': 'clay_ball', 'clock': 'clock_00', 'coal': 'coal', 'comparator': 'comparator', 'compass': 'compass_00',
 'dye_powder_black': 'ink_sac', 'dye_powder_blue': 'lapis_lazuli', 'dye_powder_brown': 'cocoa_beans', 'dye_powder_cyan': 'cyan_dye',
 'dye_powder_gray': 'gray_dye', 'dye_powder_green': 'green_dye', 'dye_powder_light_blue': 'light_blue_dye', 'dye_powder_lime': 'lime_dye',
 'dye_powder_magenta': 'magenta_dye', 'dye_powder_orange': 'orange_dye', 'dye_powder_pink': 'pink_dye', 'dye_powder_purple': 'purple_dye',
 'dye_powder_red': 'red_dye', 'dye_powder_silver': 'light_gray_dye', 'dye_powder_white': 'bone_meal', 'dye_powder_yellow': 'yellow_dye',
 'emerald': 'emerald', 'empty_armor_slot_boots': 'empty_armor_slot_boots', 'fireworks': 'firework_rocket', 'fireworks_charge': 'firework_star',
 'fish_cod_cooked': 'cooked_cod', 'fish_cod_raw': 'cod', 'fish_salmon_cooked': 'cooked_salmon', 'fish_salmon_raw': 'salmon',
 'fish_clownfish_raw': 'tropical_fish', 'fish_pufferfish_raw': 'pufferfish', 'fishing_rod_uncast': 'fishing_rod', 'fishing_rod_cast': 'fishing_rod_cast',
 'flint_and_steel': 'flint_and_steel', 'gold_ingot': 'gold_ingot', 'gold_nugget': 'gold_nugget', 'iron_ingot': 'iron_ingot',
 'item_frame': 'item_frame', 'map_empty': 'map', 'map_filled': 'filled_map', 'melon': 'melon_slice', 'melon_speckled': 'glistering_melon_slice',
 'minecart_chest': 'chest_minecart', 'minecart_command_block': 'command_block_minecart', 'minecart_furnace': 'furnace_minecart',
 'minecart_hopper': 'hopper_minecart', 'minecart_normal': 'minecart', 'minecart_tnt': 'tnt_minecart', 'nether_star': 'nether_star',
 'netherbrick': 'nether_brick', 'potato': 'potato', 'potato_baked': 'baked_potato', 'potato_poisonous': 'poisonous_potato',
 'potion_bottle_drinkable': 'potion', 'potion_bottle_empty': 'glass_bottle', 'potion_bottle_splash': 'splash_potion',
 'pumpkin_pie': 'pumpkin_pie', 'quartz': 'quartz', 'record_11': 'music_disc_11', 'record_13': 'music_disc_13', 'record_blocks': 'music_disc_blocks',
 'record_cat': 'music_disc_cat', 'record_chirp': 'music_disc_chirp', 'record_far': 'music_disc_far', 'record_mall': 'music_disc_mall',
 'record_mellohi': 'music_disc_mellohi', 'record_stal': 'music_disc_stal', 'record_strad': 'music_disc_strad', 'record_wait': 'music_disc_wait',
 'record_ward': 'music_disc_ward', 'redstone_dust': 'redstone', 'reeds': 'sugar_cane', 'saddle': 'saddle', 'seeds_melon': 'melon_seeds',
 'seeds_pumpkin': 'pumpkin_seeds', 'seeds_wheat': 'wheat_seeds', 'sign': 'oak_sign', 'slimeball': 'slime_ball', 'snowball': 'snowball',
 'spawn_egg': 'ghast_spawn_egg', 'spawn_egg_overlay': 'ghast_spawn_egg', 'speckled_melon': 'glistering_melon_slice',
 'spider_eye': 'spider_eye', 'spider_eye_fermented': 'fermented_spider_eye', 'stick': 'stick', 'string': 'string', 'sugar': 'sugar',
 'wheat': 'wheat', 'door_iron': 'iron_door', 'door_wood': 'oak_door', 'bed': 'red_bed', 'cake': 'cake', 'cookie': 'cookie',
 'beef_cooked': 'cooked_beef', 'beef_raw': 'beef', 'porkchop_cooked': 'cooked_porkchop', 'porkchop_raw': 'porkchop',
 'mutton_cooked': 'cooked_mutton', 'mutton_raw': 'mutton', 'rabbit_cooked': 'cooked_rabbit', 'rabbit_raw': 'rabbit',
 'rotten_flesh': 'rotten_flesh', 'ender_pearl': 'ender_pearl', 'ender_eye': 'ender_eye', 'experience_bottle': 'experience_bottle',
 'fire_charge': 'fire_charge', 'ghast_tear': 'ghast_tear', 'blaze_powder': 'blaze_powder', 'blaze_rod': 'blaze_rod', 'bone': 'bone',
 'bowl': 'bowl', 'bread': 'bread', 'brick': 'brick', 'apple': 'apple', 'apple_golden': 'golden_apple', 'arrow': 'arrow', 'diamond': 'diamond',
 'egg': 'egg', 'feather': 'feather', 'flint': 'flint', 'gunpowder': 'gunpowder', 'leather': 'leather', 'magma_cream': 'magma_cream',
 'name_tag': 'name_tag', 'paper': 'paper', 'lead': 'lead', 'hopper': 'hopper', 'repeater': 'repeater', 'glowstone_dust': 'glowstone_dust',
 'bucket_empty_': 'bucket', 'wooden_door': 'oak_door',
}
TOOL = {'wood': 'wooden', 'stone': 'stone', 'iron': 'iron', 'gold': 'golden', 'diamond': 'diamond'}
ARMOR = {'leather': 'leather', 'chainmail': 'chainmail', 'iron': 'iron', 'gold': 'golden', 'diamond': 'diamond'}
def item_rule(n):
    m = re.fullmatch(r'(wood|stone|iron|gold|diamond)_(pickaxe|axe|shovel|hoe|sword)', n)
    if m: return f"{TOOL[m[1]]}_{m[2]}"
    m = re.fullmatch(r'(leather|chainmail|iron|gold|diamond)_(helmet|chestplate|leggings|boots)', n)
    if m: return f"{ARMOR[m[1]]}_{m[2]}"
    m = re.fullmatch(r'(bow)_pulling_(\d)', n)
    if m: return f"bow_pulling_{m[2]}"
    return EXPLICIT_ITEMS.get(n)

rows, missing = [], []
for n in sorted(old_b - new_b):
    t = EXPLICIT.get(n) or block_rule(n)
    if t and t in new_b: rows.append(f"blocks/{n} block/{t}")
    else: missing.append(f"blocks/{n}")
for n in sorted(old_i - new_i):
    t = item_rule(n)
    if t and (t in new_i or t in new_b): rows.append(f"items/{n} " + (f"item/{t}" if t in new_i else f"block/{t}"))
    else: missing.append(f"items/{n}")
out = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src/main/resources/legacyconnector/legacy/1.7.10/texture-renames.txt")
open(out, 'w').write("\n".join(rows) + "\n")
print(len(rows), "renames;", len(missing), "unmapped:", missing)
