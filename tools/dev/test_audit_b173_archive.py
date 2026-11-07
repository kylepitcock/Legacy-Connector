import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

import audit_b173_archive as audit
import test_b173_one as isolated


def archive_bytes(entries):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, 'w') as z:
        for name, data in entries.items(): z.writestr(name, data)
    return buffer.getvalue()


class ArchiveAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with zipfile.ZipFile(audit.REPO / 'build/devtools/b1.7.3/client.jar') as z:
            cls.data = z.read('net/minecraft/client/Minecraft.class')

    def test_internal_identity_nested_roots_and_mac_metadata(self):
        units, errors = [], []
        inner = archive_bytes({'minecraft/net/minecraft/client/Minecraft.class': self.data,
                               '__MACOSX/._fake.class': b'not Java'})
        audit.collect(archive_bytes({'installer/mod.jar': inner}), 'outer.zip', units, errors)
        self.assertFalse(errors)
        self.assertEqual(units[0]['archive'], 'outer.zip!installer/mod.jar')
        self.assertEqual(units[0]['root'], 'minecraft/')
        self.assertEqual(units[0]['classes'][0]['name'], 'net/minecraft/client/Minecraft')

    def test_changed_vanilla_bytes_and_corrupt_input_are_not_clean(self):
        self.assertIn(b'SourceFile', self.data)
        changed = self.data.replace(b'SourceFile', b'SourceFilx', 1)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); source = root / 'source'; source.mkdir()
            (source / 'original.zip').write_bytes(archive_bytes({'net/minecraft/client/Minecraft.class': self.data}))
            (source / 'changed.zip').write_bytes(archive_bytes({'net/minecraft/client/Minecraft.class': changed}))
            (source / 'broken.zip').write_bytes(b'')
            with contextlib.redirect_stdout(io.StringIO()): audit.run(source, root / 'out')
            result = json.loads((root / 'out/inventory.json').read_text())
            rows = {u['archive']: u for u in result['units']}
            self.assertEqual(rows['changed.zip']['status'], 'base-class-replacements')
            self.assertEqual(rows['original.zip']['unchanged_vanilla'], ['net/minecraft/client/Minecraft'])
            self.assertEqual(result['errors'][0]['archive'], 'broken.zip')

    def test_successful_gradle_does_not_hide_caught_mod_exception(self):
        with tempfile.TemporaryDirectory() as temp:
            game = Path(temp)
            (game / 'test.log').write_text('b1.7.3 mod Example constructed\n'
                'java.lang.ClassNotFoundException: example.Dependency\n'
                'B1.7.3 GUI TEST: 0 failed;\nBUILD SUCCESSFUL\n')
            manifest = {}
            with contextlib.redirect_stdout(io.StringIO()): status = isolated.assess(game, manifest, 0)
            self.assertEqual(status, 1)
            self.assertEqual(manifest['status'], 'failed-or-needs-review')

    def test_refuses_changed_source_before_staging(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            (directory / 'mod.zip').write_bytes(b'changed')
            report = {'summary': {'input': str(directory)},
                      'files': [{'path': 'mod.zip', 'sha256': 'wrong'}],
                      'units': [{'archive': 'mod.zip', 'root': '', 'status': 'no-base-class-replacements', 'dynamic_patch_references': []}]}
            inventory = directory / 'inventory.json'; inventory.write_text(json.dumps(report))
            with self.assertRaisesRegex(ValueError, 'changed since audit'):
                isolated.stage(inventory, 'mod.zip', '', 'test')


if __name__ == '__main__': unittest.main()
