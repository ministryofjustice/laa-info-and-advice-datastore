import os
from pathlib import Path
import platform
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import boto3
import botocore


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
        self.assertTrue(os.statvfs("/tmp").f_flag & os.ST_NOEXEC)
        self.assertEqual(Path.home(), Path("/tmp"))
        with tempfile.TemporaryFile(dir="/tmp") as file:
            file.write(b"writable temporary storage")

    def test_sdk_models_and_irsa_provider(self):
        self.assertGreaterEqual(sys.version_info, (3, 13))
        print(
            f"Runtime: Python {platform.python_version()}, "
            f"boto3 {boto3.__version__}, botocore {botocore.__version__}",
            flush=True,
        )
        with tempfile.TemporaryDirectory() as work:
            token = Path(work) / "token"
            token.write_text("offline-probe-not-a-real-token")
            with patch.dict(os.environ, {
                "AWS_ROLE_ARN": "arn:aws:iam::000000000000:role/offline-probe",
                "AWS_WEB_IDENTITY_TOKEN_FILE": str(token),
            }):
                session = boto3.Session(region_name="eu-west-2")
                self.assertEqual(session.get_credentials().method, "assume-role-with-web-identity")
                rds = session.client("rds")
                self.assertIn(
                    "OriginalSnapshotCreateTime", rds.meta.service_model.shape_for("DBSnapshot").members
                )
                rds.get_paginator("describe_db_snapshots")
                rds.get_waiter("db_snapshot_available")
                rds.close()

    def test_subprocess_cli_entrypoint(self):
        help_result = subprocess.run(
            [sys.executable, str(SCRIPT), "--help"],
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(help_result.returncode, 0, help_result.stderr)
        rejected = subprocess.run(
            [
                sys.executable, str(SCRIPT),
                "--db-instance-identifier", "laa-info-and-advice-datastore-prod",
                "--region", "eu-west-2", "--env", "prod", "--tier", "hourly",
                "--creation-enabled", "false", "--run-id", "offline-probe",
            ],
            capture_output=True, text=True, timeout=30,
        )
        self.assertNotEqual(rejected.returncode, 0)
        self.assertIn("prune-only is not allowed in prod", rejected.stderr)
        failed = subprocess.run(
            [
                sys.executable, str(SCRIPT),
                "--db-instance-identifier", "laa-info-and-advice-datastore-uat",
                "--region", "eu-west-2", "--env", "uat", "--tier", "hourly",
                "--creation-enabled", "false", "--run-id", "offline-probe",
            ],
            env={
                **os.environ,
                "AWS_SHARED_CREDENTIALS_FILE": "/tmp/no-credentials",
                "AWS_CONFIG_FILE": "/tmp/no-config",
            },
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(failed.returncode, 1, failed.stderr)
        self.assertIn("NoCredentialsError", failed.stderr)
        self.assertIn('"event": "failed"', failed.stdout)


if __name__ == "__main__":
    suite = unittest.TestSuite([
        unittest.defaultTestLoader.loadTestsFromTestCase(RuntimeSmokeTests),
        unittest.defaultTestLoader.discover(str(Path(__file__).parent), pattern="test_rds_backup.py"),
    ])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
