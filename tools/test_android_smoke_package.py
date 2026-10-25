import json
from pathlib import Path
import tempfile
import unittest

from run_video_android_smoke import apk_package


class SmokePackageTest(unittest.TestCase):
    def package(self, app_id, test_id):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            paths = [root / "app" / "app.apk", root / "tests" / "tests.apk"]
            for path, identity in zip(paths, [app_id, test_id]):
                path.parent.mkdir()
                (path.parent / "output-metadata.json").write_text(json.dumps({"applicationId": identity}))
            return apk_package(paths)

    def test_normal_and_recovery_select_their_own_runner(self):
        for app_id in ("vea.raceremote", "vea.raceremote.recovery"):
            self.assertEqual(app_id, self.package(app_id, app_id + ".test"))

    def test_mixed_artifacts_and_unknown_application_are_rejected(self):
        with self.assertRaises(RuntimeError):
            self.package("vea.raceremote.recovery", "vea.raceremote.test")
        with self.assertRaises(RuntimeError):
            self.package("another.app", "another.app.test")


if __name__ == "__main__":
    unittest.main()
