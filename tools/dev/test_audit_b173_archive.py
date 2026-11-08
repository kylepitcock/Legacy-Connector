import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

import audit_b173_archive as audit
import test_b173_one as isolated
import check_mod_specific as identity_guard


def archive_bytes(entries):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, 'w') as z:
        for name, data in entries.items(): z.writestr(name, data)
    return buffer.getvalue()


class ArchiveAuditTests(unittest.TestCase):
    def test_named_beta_fixtures_do_not_exempt_production_bridges(self):
        base = 'src/main/java/org/kylepitcock/legacyconnector/vb1_7_3/'
        self.assertTrue(identity_guard.allowed(base + 'MobSelfTestB173.java'))
        self.assertTrue(identity_guard.allowed(base + 'AudioSelfTestB173.java'))
        self.assertFalse(identity_guard.allowed(base + 'MobsB173.java'))
        self.assertFalse(identity_guard.allowed(base + 'client/MobSelfTestB173.java'))
        self.assertFalse(identity_guard.allowed(base + 'MobSelfTestB173Helper.java'))

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

    def test_all_staged_entrypoints_must_construct(self):
        with tempfile.TemporaryDirectory() as temp:
            game = Path(temp)
            log = 'b1.7.3 mod Core 1.0 constructed\nB1.7.3 GUI TEST: 0 failed;\nBUILD SUCCESSFUL\n'
            (game / 'test.log').write_text(log)
            manifest = {'entrypoints': ['mod_Core', 'mod_Energy']}
            with contextlib.redirect_stdout(io.StringIO()): status = isolated.assess(game, manifest, 0)
            self.assertEqual(status, 1)
            self.assertEqual(manifest['missing_entrypoints'], ['Energy'])
            (game / 'test.log').write_text(log + 'b1.7.3 mod Energy 1.0 constructed\n')
            with contextlib.redirect_stdout(io.StringIO()): status = isolated.assess(game, manifest, 0)
            self.assertEqual(status, 0)

    def test_callback_failure_without_exception_stack_is_not_success(self):
        with tempfile.TemporaryDirectory() as temp:
            game = Path(temp)
            for failure in ['onPlaced of legacyconnector_b173.example.Block[100] failed: details',
                            'B1.7.3 BEHAVIOUR TEST: 12 legacy callbacks failed, 0 escaped',
                            'B1.7.3 ACTUAL BUCKET/BOW TEST: 0 passed, 10 failed',
                            'B1.7.3 RECIPE COUNT TEST: 1 oversized recipes, 1 failures',
                            'B1.7.3 RECIPE COUNT TEST failed for example:recipe']:
                with self.subTest(failure=failure):
                    (game / 'test.log').write_text('b1.7.3 mod Example 1.0 constructed\n' + failure +
                                                 '\nB1.7.3 GUI TEST: 0 failed;\nBUILD SUCCESSFUL\n')
                    manifest = {'entrypoints': ['mod_Example']}
                    with contextlib.redirect_stdout(io.StringIO()): status = isolated.assess(game, manifest, 0)
                    self.assertEqual(status, 1)
                    self.assertIn(failure, manifest['problems'])


if __name__ == '__main__': unittest.main()
