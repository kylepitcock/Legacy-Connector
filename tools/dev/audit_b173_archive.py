#!/usr/bin/env python3
"""Read-only Beta archive inventory. No classes are executed or extracted.

Class identity comes from this_class, not the zip filename. A replacement means
different bytes from both original client/server copies, not a semantic diff.
Nested archives and install roots are separate units; never auto-install a bundle.
"""
import argparse
import collections
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import subprocess
import tempfile
import zipfile

REPO = Path(__file__).resolve().parents[2]
API_ARCHIVES = {
    'ModLoader': r'^ModLoader_B',
    'ModLoaderMP': r'^ModLoaderMp_',
    'Forge': r'^minecraftforge-',
    'Reforged': r'^reforged',
    'ShockAhPI': r'^ShockAhPI_',
    'Player API': r'^Player.?API',
    'AudioMod': r'^AudioMod_',
    'GuiAPI': r'^GuiAPI',
    'ModOptionsAPI': r'^ModOptionsAPI',
    'ScotTools': r'^ScotTools',
    'ItemSpriteAPI': r'^ItemSpriteAPI',
    'OverrideAPI': r'^OverrideAPI',
    'InfSprites': r'^InfSprites',
    # Record dependencies even when outside the implementation scope.
    'PropertyReader (outside scope)': r'^PropertyReader',
    'TMT (outside scope)': r'^turbomodelthingy',
}
KNOWN = {
    'ModLoader': {'BaseMod', 'ModLoader', 'MLProp', 'ModTextureAnimation', 'ModTextureStatic'},
    'ModLoaderMP': {'BaseModMp', 'ModLoaderMp', 'Packet230ModLoader', 'ISpawnable'},
    'ShockAhPI': {'SAPI', 'DimensionBase', 'ACPage', 'ToolBase', 'Loc', 'IInterceptBlockSet', 'IReach'},
    'Player API': {'PlayerAPI', 'PlayerBase', 'PlayerBaseSorting'},
    'GuiAPI': {'ModSettings', 'ModSettingScreen', 'GuiModScreen', 'GuiApiHelper'},
}
POPULAR = re.compile(r'aether|buildcraft|industrialcraft|nethercraft|portal.?gun|redpower|minefactory|minefractory|mocreatures|morecreeps|millenaire|equivalentexchange|twilightforest|plasticcraft|betterthanwolves', re.I)


def class_info(data):
    if data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('not a class file')
    count = struct.unpack_from('>H', data, 8)[0]
    cp, pos, i = [None] * count, 10, 1
    while i < count:
        tag = data[pos]; pos += 1
        if tag == 1:
            size = struct.unpack_from('>H', data, pos)[0]; pos += 2
            cp[i] = data[pos:pos + size].decode('utf-8', 'replace'); pos += size
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, struct.unpack_from('>H', data, pos)[0]); pos += 2
        elif tag == 12:
            cp[i] = (tag, *struct.unpack_from('>HH', data, pos)); pos += 4
        elif tag in (3, 4, 9, 10, 11, 17, 18): pos += 4
        elif tag in (5, 6): pos += 8; i += 1
        elif tag == 15: pos += 3
        else: raise ValueError(f'unknown constant tag {tag}')
        i += 1
    _, this, parent = struct.unpack_from('>HHH', data, pos)
    name = cp[cp[this][1]]
    refs = {cp[x[1]] for x in cp if isinstance(x, tuple) and x[0] == 7}
    descriptors = [cp[x[2]] for x in cp if isinstance(x, tuple) and x[0] == 12]
    descriptors += [cp[x[1]] for x in cp if isinstance(x, tuple) and x[0] == 16]
    # Declared field/method descriptors may not have a NameAndType constant.
    pos += 6
    interfaces = struct.unpack_from('>H', data, pos)[0]; pos += 2 + 2 * interfaces
    for _ in range(2):
        members = struct.unpack_from('>H', data, pos)[0]; pos += 2
        for _ in range(members):
            _, _, descriptor, attributes = struct.unpack_from('>HHHH', data, pos); pos += 8
            descriptors.append(cp[descriptor])
            for _ in range(attributes):
                size = struct.unpack_from('>I', data, pos + 2)[0]; pos += 6 + size
    for value in descriptors:
        refs.update(re.findall(r'L([\w$/]+);', value))
    refs.discard(name)
    strings = {cp[x[1]] for x in cp if isinstance(x, tuple) and x[0] == 8}
    reflective = {s.replace('.', '/') for s in strings if re.fullmatch(r'(?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*', s)}
    return name, cp[cp[parent][1]] if parent else '', refs, reflective


def digest(data):
    return hashlib.sha256(data).hexdigest()


def as_zip(blob):
    """Some historical .zip downloads are actually RAR; read through 7z stdout."""
    if not blob.startswith(b'Rar!'): return blob
    with tempfile.NamedTemporaryFile(suffix='.rar', delete=False) as temp:
        temp.write(blob); path = Path(temp.name)
    try:
        listing = subprocess.check_output(['7z', 'l', '-slt', '-ba', str(path)], text=True, encoding='utf-8', errors='replace')
        result = io.BytesIO()
        with zipfile.ZipFile(result, 'w', zipfile.ZIP_DEFLATED) as z:
            for record in listing.split('\n\n'):
                fields = dict(line.split(' = ', 1) for line in record.splitlines() if ' = ' in line)
                name = fields.get('Path')
                if not name or 'D' in fields.get('Attributes', ''): continue
                if int(fields.get('Size', '0')) > 128 * 1024 * 1024: raise ValueError('RAR entry exceeds size limit')
                data = subprocess.check_output(['7z', 'x', '-so', '-spd', str(path), name])
                z.writestr(name.replace('\\', '/'), data)
        return result.getvalue()
    finally:
        path.unlink()


def collect(blob, label, records, errors, depth=0):
    if depth > 6:
        errors.append({'archive': label, 'error': 'nested archive depth limit'}); return
    groups = collections.defaultdict(list)
    notes = []
    try:
        with zipfile.ZipFile(io.BytesIO(as_zip(blob))) as z:
            for entry in z.infolist():
                if entry.is_dir() or entry.filename.startswith('__MACOSX/') or Path(entry.filename).name.startswith('._'): continue
                if entry.file_size > 128 * 1024 * 1024:
                    errors.append({'archive': label, 'entry': entry.filename, 'error': 'entry exceeds 128 MiB safety limit'}); continue
                if entry.filename.lower().endswith(('.zip', '.jar')):
                    collect(z.read(entry), label + '!' + entry.filename, records, errors, depth + 1)
                elif entry.filename.lower().endswith(('.txt', '.md', '.html')) and entry.file_size < 2 * 1024 * 1024:
                    text = z.read(entry).decode('utf-8', 'replace')
                    for line in text.splitlines():
                        if re.search(r'modloader|forge|shockah|player.?api|audiomod|guiapi|modoptions|scottools|itemsprite|overrideapi|infsprites|propertyreader|turbo.?model', line, re.I):
                            notes.append({'file': entry.filename, 'text': line.strip()[:500]})
                elif entry.filename.lower().endswith('.class'):
                    try:
                        data = z.read(entry)
                        name, parent, refs, reflective = class_info(data)
                        suffix = name + '.class'
                        root = entry.filename[:-len(suffix)] if entry.filename.endswith(suffix) else '?unmatched-path/'
                        groups[root].append({'name': name, 'parent': parent, 'refs': sorted(refs), 'reflective_candidates': sorted(reflective), 'sha256': digest(data), 'entry': entry.filename})
                    except Exception as exc:
                        errors.append({'archive': label, 'entry': entry.filename, 'error': str(exc)})
    except Exception as exc:
        errors.append({'archive': label, 'error': str(exc)})
    for root, classes in groups.items():
        records.append({'archive': label, 'root': root, 'classes': classes, 'documentation_mentions': notes[:100]})
    if not groups and notes:
        records.append({'archive': label, 'root': '', 'classes': [], 'documentation_mentions': notes[:100]})


def run(archive, out):
    vanilla = collections.defaultdict(set)
    for side in ('client', 'server'):
        with zipfile.ZipFile(REPO / f'build/devtools/b1.7.3/{side}.jar') as z:
            for name in z.namelist():
                if name.endswith('.class'):
                    data = z.read(name)
                    vanilla[class_info(data)[0]].add(digest(data))
    units, errors, files = [], [], []
    for p in sorted(archive.rglob('*')):
        if not p.is_file(): continue
        label = p.relative_to(archive).as_posix()
        if p.suffix.lower() in ('.jar', '.zip'):
            data = p.read_bytes(); files.append({'path': label, 'sha256': digest(data), 'bytes': len(data), 'format': 'rar' if data.startswith(b'Rar!') else 'zip' if zipfile.is_zipfile(io.BytesIO(data)) else 'invalid-or-empty'})
            collect(data, label, units, errors)
        elif p.suffix.lower() == '.class':
            try:
                data = p.read_bytes(); name, parent, refs, reflective = class_info(data)
                units.append({'archive': label, 'root': '', 'classes': [{'name': name, 'parent': parent, 'refs': sorted(refs), 'reflective_candidates': sorted(reflective), 'sha256': digest(data), 'entry': label}]})
            except Exception as exc: errors.append({'archive': label, 'error': str(exc)})

    providers, api_patches = collections.defaultdict(set), collections.defaultdict(set)
    for api, names in KNOWN.items():
        for name in names: providers[name].add(api)
    for unit in units:
        families = [api for api, pattern in API_ARCHIVES.items() if re.search(pattern, Path(unit['archive'].split('!')[0]).name, re.I)]
        unit['api_distribution'] = families
        for cls in unit['classes']:
            for api in families:
                if cls['name'] in vanilla: api_patches[(cls['name'], cls['sha256'])].add(api)
                elif not cls['name'].startswith(('java/', 'org/', 'com/', 'de/', 'paulscode/')):
                    providers[cls['name']].add(api)
    # Pin stable public API names; incidental helper copies don't imply dependencies.
    for api, names in KNOWN.items():
        for name in names: providers[name] = {api}
    class_providers = collections.defaultdict(set)
    for unit in units:
        for cls in unit['classes']: class_providers[cls['name']].add(unit['archive'])

    rows = []
    for unit in units:
        own = {c['name'] for c in unit['classes']}
        refs = set().union(*(set(c['refs']) for c in unit['classes']))
        reflective = set().union(*(set(c['reflective_candidates']) for c in unit['classes'])) - own
        parent_notes = []
        parents = unit['archive'].split('!')
        for n in range(1, len(parents)):
            for parent in units:
                if parent['archive'] == '!'.join(parents[:n]):
                    for note in parent.get('documentation_mentions', []):
                        if note not in parent_notes: parent_notes.append(note)
        dependencies = collections.defaultdict(set)
        for ref in refs:
            families = set(providers.get(ref, ()))
            if ref.startswith('forge/'): families.add('Forge')
            if ref.startswith('reforged/'): families.add('Reforged')
            if ref.startswith('overrideapi/'): families.add('OverrideAPI')
            if ref.startswith('modoptionsapi/'): families.add('ModOptionsAPI')
            if ref.startswith('net/mine_diver/infsprites/'): families.add('InfSprites')
            if ref.startswith('de/matthiasmann/twl/'): families.add('GuiAPI')
            for family in families: dependencies[family].add(ref)
        changed, known, unchanged = [], [], []
        for cls in unit['classes']:
            if cls['name'] not in vanilla: continue
            if cls['sha256'] in vanilla[cls['name']]: unchanged.append(cls['name'])
            elif (cls['name'], cls['sha256']) in api_patches:
                known.append({'class': cls['name'], 'apis': sorted(api_patches[(cls['name'], cls['sha256'])])})
            else: changed.append(cls['name'])
        entrypoints = sorted(n for n in own if n.rsplit('/', 1)[-1].startswith('mod_') and '$' not in n)
        unresolved = sorted(r for r in refs - own if r not in vanilla and r not in providers and not r.startswith(('java/', 'javax/', 'sun/', 'org/lwjgl/', 'org/apache/', 'org/objectweb/', 'paulscode/', 'argo/', '[', 'forge/', 'modoptionsapi/', 'de/matthiasmann/')))
        unit_errors = [e for e in errors if e['archive'].split('!')[0] == unit['archive'].split('!')[0]]
        status = ('review-errors' if unit_errors or unit['root'].startswith('?') else
                  'loose-class-review' if unit['archive'].endswith('.class') else
                  'api-distribution' if unit['api_distribution'] else
                  'base-class-replacements' if changed else
                  'bundled-api-patches' if known else
                  'no-base-class-replacements' if entrypoints else 'library-or-resource-classes')
        rows.append({'archive': unit['archive'], 'root': unit['root'], 'status': status,
                     'priority_review': bool(POPULAR.search(unit['archive'])),
                     'entrypoints': entrypoints, 'class_count': len(own),
                     'base_replacements': sorted(changed), 'known_api_patches': known,
                     'unchanged_vanilla': sorted(unchanged), 'api_distribution': unit['api_distribution'],
                     'documentation_mentions': unit.get('documentation_mentions', []),
                     'container_documentation_mentions': parent_notes,
                     'reflective_class_candidates': sorted(reflective),
                     'reflective_provider_candidates': {r: sorted(class_providers[r] - {unit['archive']}) for r in reflective if class_providers[r] - {unit['archive']}},
                     'dynamic_patch_references': sorted(r for r in refs if r.startswith(('org/objectweb/asm/', 'javassist/', 'java/lang/instrument/'))),
                     'api_evidence': {k: sorted(v) for k, v in sorted(dependencies.items())},
                     'external_classes': unresolved,
                     'possible_dependency_archives': {r: sorted(class_providers[r] - {unit['archive']}) for r in unresolved if class_providers[r] - {unit['archive']}},
                     'class_sha256': {c['name']: c['sha256'] for c in unit['classes']}})
    out.mkdir(parents=True, exist_ok=True)
    summary = {'input': str(archive), 'archive_files': len(files), 'units': len(rows), 'statuses': dict(collections.Counter(r['status'] for r in rows)), 'errors': len(errors)}
    (out / 'inventory.json').write_text(json.dumps({'summary': summary, 'files': files, 'units': rows, 'errors': errors}, indent=2), encoding='utf-8')
    columns = ['archive', 'root', 'status', 'priority_review', 'class_count', 'entrypoints', 'apis', 'base_replacements', 'known_api_patches', 'external_classes']
    for filename, selected in [('inventory.csv', rows), ('priority-review.csv', [r for r in rows if r['priority_review']])]:
        with (out / filename).open('w', newline='', encoding='utf-8-sig') as f:
            writer = csv.DictWriter(f, fieldnames=columns); writer.writeheader()
            for row in selected:
                record = {k: row.get(k, '') for k in columns}; record['apis'] = list(row['api_evidence'])
                writer.writerow({k: '; '.join(map(str, v)) if isinstance(v, list) else v for k, v in record.items()})
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archive', type=Path)
    parser.add_argument('--out', type=Path, default=REPO / 'build/b173-archive-audit')
    args = parser.parse_args()
    run(args.archive.resolve(), args.out.resolve())
