#!/usr/bin/env python3
"""Supplement the global guard with Beta mod identities; tests are separate.

This is a regression guard, not proof of semantic correctness or full API coverage.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
IDENTITIES = re.compile(r'\b(?:aether|nethercraft|portalgun|minefactory|minefactoryreloaded|redpower|plasticcraft|ic2|industrialcraft|buildcraft|morecreepsandweirdos|mocreatures|millenaire|equivalentexchange|twilightforest|ItemNotchHammer|ItemSlimeEggs)\b|legacyconnector_b173[./][a-zA-Z_]+|\bmod_(?:Nether|Aether|PlasticCraft|PortalGun|MineFactory|IC2\w*|RedPower\w*)\b', re.I)


def main():
    paths = list((ROOT / 'src/main/java/legacyconnector/vb1_7_3').rglob('*.java'))
    paths += list((ROOT / 'src/main/java/org/kylepitcock/legacyconnector/vb1_7_3').rglob('*.java'))
    paths += list((ROOT / 'loader/src/main/java').rglob('*Beta*.java'))
    findings, checked = [], 0
    for path in paths:
        if 'SelfTest' in path.name: continue
        checked += 1
        text = path.read_text(encoding='utf-8')
        text = re.sub(r'/\*.*?\*/', lambda m: '\n' * m[0].count('\n'), text, flags=re.S)
        text = re.sub(r'//[^\n]*', '', text)
        for number, line in enumerate(text.splitlines(), 1):
            if IDENTITIES.search(line): findings.append(f'{path.relative_to(ROOT)}:{number}: {line.strip()}')
    print('\n'.join(findings) if findings else f'PASS: {checked} Beta production/loader source files; no listed mod-identity selectors outside tests')
    return bool(findings)


if __name__ == '__main__': sys.exit(main())
