#!/usr/bin/env python3
"""Stage one audited Beta payload in a new game directory and optionally test it.

No shared configs, worlds, caches, Prism files or existing test sets are changed.
Dependencies must be reviewed explicitly; this tool never installs API jar patches.
"""
import argparse
import datetime
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import uuid
import zipfile
from audit_b173_archive import as_zip

REPO = Path(__file__).resolve().parents[2]


def stage(inventory, archive_name, root, label):
    report = json.loads(inventory.read_text(encoding='utf-8'))
    matches = [u for u in report['units'] if u['archive'] == archive_name and u['root'] == root]
    if len(matches) != 1: raise ValueError('Select exactly one archive/root from inventory.json')
    unit = matches[0]
    if unit['status'] != 'no-base-class-replacements':
        raise ValueError('Only reviewed units with no detected base-class replacements can be staged')
    if unit['dynamic_patch_references']: raise ValueError('Dynamic patch references require manual review')
    if not re.fullmatch(r'[a-z0-9-]+', label): raise ValueError('Use a lowercase label containing letters, digits and hyphens')
    outer, *nested = archive_name.split('!')
    source_root = Path(report['summary']['input']).resolve()
    source = (source_root / outer).resolve()
    if not source.is_relative_to(source_root): raise ValueError('Source outside archive')
    blob = source.read_bytes()
    expected = next(f['sha256'] for f in report['files'] if f['path'] == outer)
    if hashlib.sha256(blob).hexdigest() != expected: raise ValueError('Archive changed since audit; rescan')
    for entry in nested:
        with zipfile.ZipFile(io.BytesIO(as_zip(blob))) as z: blob = z.read(entry)
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:6]
    game = REPO / 'run-b173-single' / (label + '-' + stamp)
    mods = game / 'mods/b1.7.3'; mods.mkdir(parents=True, exist_ok=False)
    target = mods / (label + '.jar')
    written = set()
    with zipfile.ZipFile(io.BytesIO(as_zip(blob))) as z, zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as out:
        for entry in z.infolist():
            name = entry.filename
            if entry.is_dir() or not name.startswith(root): continue
            name = name[len(root):]
            if not name or name.startswith(('META-INF/', '__MACOSX/')) or Path(name).name.startswith('._'): continue
            if name.lower().endswith(('.zip', '.jar')): continue
            if '\\' in name or name.startswith('/') or '..' in name.split('/') or ':' in name:
                raise ValueError('Unsafe archive resource path: ' + name)
            if name in written: raise ValueError('Duplicate archive path: ' + name)
            if name.endswith('.class') and name[:-6] not in unit['class_sha256']:
                # Other install roots, source trees and optional patches are not this unit.
                continue
            data = z.read(entry)
            if name.endswith('.class') and hashlib.sha256(data).hexdigest() != unit['class_sha256'][name[:-6]]:
                raise ValueError('Class changed since audit')
            out.writestr(name, data); written.add(name)
    missing = {name + '.class' for name in unit['class_sha256']} - written
    if missing: raise ValueError('Selected payload did not include every audited class: ' + str(sorted(missing)))
    (game / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\n', encoding='utf-8')
    manifest = {'source': archive_name, 'root': root, 'entrypoints': unit['entrypoints'],
                'api_evidence': unit['api_evidence'], 'documentation_mentions': unit['documentation_mentions'],
                'external_classes': unit['external_classes'],
                'jar_sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
                'git_revision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(),
                'game_directory': str(game), 'status': 'staged-not-tested',
                'limitations': 'Headless tests only. External resource/config folders are not auto-installed. Static API references do not prove mandatory dependencies.'}
    (game / 'audit-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    return game, manifest


def assess(game, manifest, exit_code):
    text = (game / 'test.log').read_text(encoding='utf-8', errors='replace')
    problems = [line for line in text.splitlines() if re.search(r'failed during construction:|failed during (?:postInit|init)|Exception caught during firing event|Encountered an unexpected exception|BUILD FAILED|[1-9]\d* failed;|^[\w.$]+(?:Exception|Error)(?::|$)', line)]
    evidence = [line for line in text.splitlines() if 'B1.7.3 ' in line and 'TEST:' in line]
    completed = any('B1.7.3 GUI TEST:' in line and '0 failed;' in line for line in evidence)
    constructed = [line for line in text.splitlines() if 'b1.7.3 mod ' in line and ' constructed' in line]
    manifest.update({'status': 'headless-smoke-passed' if exit_code == 0 and not problems and completed and constructed else 'failed-or-needs-review',
                     'exit_code': exit_code, 'problems': problems, 'constructed': constructed, 'test_evidence': evidence})
    (game / 'audit-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print(json.dumps({'status': manifest['status'], 'game_directory': str(game), 'problems': problems}, indent=2))
    return 0 if manifest['status'] == 'headless-smoke-passed' else 1


def run(game, manifest):
    command = ['gradlew.bat', 'runServerSubTest', '-PsubTestDir=' + game.relative_to(REPO).as_posix(), '-PautostopTicks=300', '--console=plain']
    with (game / 'test.log').open('w', encoding='utf-8') as out:
        result = subprocess.run(command, cwd=REPO, stdout=out, stderr=subprocess.STDOUT)
    return assess(game, manifest, result.returncode)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archive', help='Exact archive path, including !nested.zip segments, from inventory')
    parser.add_argument('--root', default='')
    parser.add_argument('--label', required=True)
    parser.add_argument('--inventory', type=Path, default=REPO / 'build/b173-archive-audit/inventory.json')
    parser.add_argument('--run', action='store_true')
    args = parser.parse_args()
    game, manifest = stage(args.inventory, args.archive, args.root, args.label)
    print(game, flush=True)
    if args.run: raise SystemExit(run(game, manifest))
