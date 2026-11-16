"""Protected instance methods of Minecraft 1.21.1 classes (net/minecraft/world/**, server/level, core): class TAB name+descriptor."""
import re, subprocess, sys, zipfile

# usage: python tools/dev/gen_protected_members_1201.py [merged-jar] [out.tsv.gz]   (JDK 21 javap on PATH or JAVA_HOME)
import gzip, os
jar = sys.argv[1] if len(sys.argv) > 1 else 'build/moddev/artifacts/neoforge-21.1.258-merged.jar'
out_path = sys.argv[2] if len(sys.argv) > 2 else 'loader/src/main/resources/legacyconnector/forward/1.20.1/mc-protected.tsv.gz'
JAVAP = os.path.join(os.environ.get('JAVA_HOME', ''), 'bin', 'javap') if os.environ.get('JAVA_HOME') else 'javap'
names = []
with zipfile.ZipFile(jar) as z:
    for n in z.namelist():
        if n.endswith('.class') and (n.startswith('net/minecraft/world/') or n.startswith('net/minecraft/server/level/') or n.startswith('net/minecraft/network/syncher/')):
            names.append(n[:-6].replace('/', '.'))
rows = []
for i in range(0, len(names), 250):
    chunk = names[i:i + 250]
    p = subprocess.run([JAVAP, '-p', '-s', '-cp', jar] + chunk, capture_output=True, text=True)
    cur = None
    lines = p.stdout.splitlines()
    for j, l in enumerate(lines):
        m = re.match(r'^(?:public |protected |private |abstract |final |static |sealed |non-sealed )*(?:class|interface|enum|record) ([\w.$]+)', l)
        if m:
            cur = m.group(1).replace('.', '/')
            continue
        if cur is None:
            continue
        m = re.match(r'^  protected (?!static)(?:(?:abstract|final|synchronized|native) )*.*?(\w+)\(.*\)(?: throws [\w., ]+)?;$', l)
        if m and j + 1 < len(lines) and lines[j + 1].strip().startswith('descriptor: '):
            name = m.group(1)
            desc = lines[j + 1].strip()[len('descriptor: '):]
            if name == '<init>':
                continue
            rows.append('%s\t%s%s' % (cur, name, desc))
with gzip.open(out_path, 'wt', encoding='utf-8', newline='\n') as f:
    f.write('# Protected instance methods of Minecraft 1.21.1 classes (class TAB name+descriptor), made by tools/dev/gen_protected_members_1201.py; see ProtectedCalls1201.\n')
    f.write('\n'.join(sorted(set(rows))) + '\n')
print(len(rows), 'rows from', len(names), 'classes')
