import json
import os
from pathlib import Path
import platform
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "laa-info-and-advice-datastore/files/rds-backup.py"


class RuntimeSmokeTests(unittest.TestCase):
    def test_restricted_runtime_and_read_only_script(self):
        self.assertEqual((os.getuid(), os.getgid()), (1000, 1000))
        status = dict(
            line.split(":", 1) for line in Path("/proc/self/status").read_text().splitlines()
        )
        self.assertEqual(int(status["CapEff"].strip(), 16), 0)
        self.assertEqual(status["NoNewPrivs"].strip(), "1")
        self.assertEqual(status["Seccomp"].strip(), "2")
        active_networks = []
        for flags_file in Path("/sys/class/net").glob("*/flags"):
            flags = int(flags_file.read_text().strip(), 16)
            if flags & 0x1 and not flags & 0x8:
                active_networks.append(flags_file.parent.name)
        self.assertEqual(active_networks, [])
        self.assertTrue(os.statvfs("/").f_flag & os.ST_RDONLY)
        self.assertTrue(os.statvfs(SCRIPT).f_flag & os.ST_RDONLY)
        self.assertFalse(os.statvfs("/tmp").f_flag & os.ST_RDONLY)
        self.assertEqual(Path.home(), Path("/tmp"))
        with tempfile.TemporaryFile(dir="/tmp") as file:
            file.write(b"writable temporary storage")

    def test_installed_tools_and_original_snapshot_time_model(self):
        self.assertGreaterEqual(sys.version_info, (3, 9))
        version = subprocess.run(
            ["aws", "--version"], check=True, capture_output=True, text=True, timeout=30
        )
        print(
            f"Runtime: Python {platform.python_version()}, "
            f"{(version.stdout or version.stderr).strip()}",
            flush=True,
        )
        model = subprocess.run(
            [
                "aws", "rds", "describe-db-snapshots", "--region", "eu-west-2",
                "--generate-cli-skeleton", "output", "--no-sign-request",
            ],
            check=True, capture_output=True, text=True, timeout=30,
        )
        self.assertIn("OriginalSnapshotCreateTime", json.loads(model.stdout)["DBSnapshots"][0])


if __name__ == "__main__":
    suite = unittest.TestSuite([
        unittest.defaultTestLoader.loadTestsFromTestCase(RuntimeSmokeTests),
        unittest.defaultTestLoader.discover(str(Path(__file__).parent), pattern="test_rds_backup.py"),
    ])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
