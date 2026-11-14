#!/usr/bin/env python3
"""Refill src/main/resources/legacyconnector/b173/vanilla_sprites from a b1.7.3 terrain.png (a minecraft.jar's, 256x256, 16x16 tiles).

Only the tiles the layer cannot take from a modern texture are cut out: the chest (25 top, 26 side, 27 front) and the fluids (205/206 water,
237/238 lava). The source image is the caller's own; nothing but these tiles is kept in the repository.

usage: extract_b173_sprites.py <terrain.png>
"""
import sys
from pathlib import Path
from PIL import Image

TILES = {25: 'chest_top', 26: 'chest_side', 27: 'chest_front', 205: 'water_still', 206: 'water_flow', 237: 'lava_still', 238: 'lava_flow'}
OUT = Path(__file__).resolve().parents[2] / 'src/main/resources/legacyconnector/b173/vanilla_sprites'

def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    sheet = Image.open(sys.argv[1]).convert('RGBA')
    if sheet.width != sheet.height or sheet.width % 16:
        sys.exit('terrain.png must be square with 16x16 tiles')
    tile = sheet.width // 16
    OUT.mkdir(parents=True, exist_ok=True)
    for index, name in TILES.items():
        x, y = (index % 16) * tile, (index // 16) * tile
        sheet.crop((x, y, x + tile, y + tile)).save(OUT / (name + '.png'))
        print('wrote', name)

if __name__ == '__main__':
    main()
