"""Drops the entries of an agent task list that the shim now declares (class, member and descriptor, through superclasses).

usage: python prune_done.py <task list> <shim-index.txt> [version prefix, default legacyconnector/v1_12_2/]
Entries implemented through Synth.addInstanceMethod/addStaticMethod are not detected; remove those by hand.
"""
import re
import sys

task, index = sys.argv[1], sys.argv[2]
prefix = sys.argv[3] if len(sys.argv) > 3 else 'legacyconnector/v1_12_2/'

classes = {}
cur = None
for line in open(index, encoding='utf-8'):
    parts = line.split()
    if not parts:
        continue
    if parts[0] == 'C':
        cur = {'super': parts[2], 'ifaces': [] if parts[3] == '-' else parts[3].split(','), 'members': set()}
        classes[parts[1]] = cur
    elif cur is not None and parts[0] in ('M', 'F'):
        cur['members'].add(parts[1] + ' ' + parts[2])


def shim_type(desc):
    def rep(m):
        name = m.group(1).replace('.', '/')
        if name.startswith('synth/'):
            name = name[len('synth/'):]
        if name.startswith('net/minecraft') or name.startswith('net/minecraftforge'):
            return 'L' + prefix + name + ';'
        return 'L' + name + ';'
    return re.sub(r'L([\w.$/]+);', rep, desc)


def has(owner, key, seen=None):
    seen = seen or set()
    if owner in seen or owner not in classes:
        return False
    seen.add(owner)
    c = classes[owner]
    if key in c['members']:
        return True
    for parent in [c['super']] + c['ifaces']:
        if has(parent, key, seen):
            return True
    return False


kept, dropped = [], 0
for line in open(task, encoding='utf-8'):
    sig = line.split('#')[0].strip()
    m = re.match(r'(?:synth\.)?([\w.$]+)\.([\w<>$]+)(\(.*?\)\S*|:\S+)', sig)
    if not m:
        kept.append(line)
        continue
    owner = prefix + m.group(1).replace('.', '/')
    name, rest = m.group(2), m.group(3)
    if rest.startswith(':'):
        desc = shim_type(rest[1:])
        ok = has(owner, name + ' ' + desc) or has(owner, 'mcc$get$' + name + ' (L' + owner + ';)' + desc) \
            or any(k.startswith('mcc$get$' + name + ' ') for k in classes.get(owner, {}).get('members', ()))
    else:
        desc = shim_type(rest.split(' ')[0])
        ok = has(owner, name + ' ' + desc)
    if ok and '[UNRESOLVED]' not in line:
        dropped += 1
    else:
        kept.append(line)
open(task, 'w', encoding='utf-8').writelines(kept)
print('dropped', dropped, 'kept', len(kept))
