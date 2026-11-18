from contextlib import redirect_stderr, redirect_stdout
from datetime import datetime
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from fake_rds import RDSStub


SCRIPT = Path(__file__).resolve().parents[1] / "laa-info-and-advice-datastore/files/rds-backup.py"
DB = "laa-info-and-advice-datastore-uat"
ARN_PREFIX = "arn:aws:rds:eu-west-2:123456789012"


def snapshot(suffix="old", tier="hourly", **overrides):
    name = f"{DB}-scheduled-{tier}-{suffix}"
    recovery = "2026-11-16T09:00:00Z" if tier == "hourly" else "2026-11-01T02:00:00Z"
    tags = {
        "service": "laa-info-and-advice-datastore",
        "env": "uat",
        "backup-type": "scheduled",
        "backup-tier": tier,
        "retention-hours": "24" if tier == "hourly" else "168",
        "managed-by": "datastore-backup-automation",
    }
    if tier == "daily":
        tags["source-snapshot"] = "rds:" + DB + "-2026-11-01-02-00"
    return {
        "DBSnapshotIdentifier": name,
        "DBSnapshotArn": ARN_PREFIX + ":snapshot:" + name,
        "DBInstanceIdentifier": DB,
        "SnapshotType": "manual",
        "Status": "available",
        "SnapshotCreateTime": recovery,
        "OriginalSnapshotCreateTime": recovery,
        "Encrypted": True,
        "TagList": [{"Key": key, "Value": value} for key, value in tags.items()],
        **overrides,
    }


def automated(day="17"):
    name = f"rds:{DB}-2026-11-{day}-02-00"
    return snapshot(
        DBSnapshotIdentifier=name,
        DBSnapshotArn=ARN_PREFIX + ":snapshot:" + name,
        SnapshotType="automated",
        SnapshotCreateTime=f"2026-11-{day}T02:00:00Z",
        OriginalSnapshotCreateTime=f"2026-11-{day}T02:00:00Z",
        TagList=[],
    )


def tagged(point, **changes):
    tags = {tag["Key"]: tag["Value"] for tag in point["TagList"]}
    tags.update(changes)
    point["TagList"] = [
        {"Key": key, "Value": value} for key, value in tags.items() if value is not None
    ]
    return point


class SnapshotLifecycleTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.work = Path(self.directory.name)
        self.calls = self.work / "calls.jsonl"
        self.now = "2026-11-17T10:00:00Z"
        self.clock = lambda: datetime.fromisoformat(self.now.replace("Z", "+00:00"))
        self.state_file = self.work / "state.json"
        self.state = {
            "instance": {
                "DBInstanceIdentifier": DB,
                "DBInstanceArn": ARN_PREFIX + ":db:" + DB,
                "DBInstanceStatus": "available",
                "StorageEncrypted": True,
            },
            "snapshots": [],
        }

    def run_script(self, **overrides):
        options = {
            "db-instance-identifier": "laa-info-and-advice-datastore-uat",
            "region": "eu-west-2",
            "env": "uat",
            "tier": "hourly",
            "creation-enabled": "false",
            "run-id": "backup-hourly-123",
            **overrides,
        }
        args = [part for key, value in options.items() for part in ("--" + key, value)]
        spec = importlib.util.spec_from_file_location("rds_backup", SCRIPT)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        rds = RDSStub(self.state, self.calls)
        stdout, stderr = io.StringIO(), io.StringIO()
        with (
            patch("boto3.client", return_value=rds),
            patch.object(module, "utc_now", side_effect=self.clock),
            patch.object(module.time, "sleep"),
            patch.object(sys, "argv", [str(SCRIPT), *args]),
            redirect_stdout(stdout),
            redirect_stderr(stderr),
        ):
            try:
                code = module.main()
            except SystemExit as error:
                code = error.code
        self.state_file.write_text(json.dumps(rds.state))
        return subprocess.CompletedProcess(args, code, stdout.getvalue(), stderr.getvalue())

    def operations(self):
        if not self.calls.exists():
            return []
        return [json.loads(line)[1] for line in self.calls.read_text().splitlines()]

    def saved_snapshots(self):
        return json.loads(self.state_file.read_text())["snapshots"]

    def test_production_cannot_accidentally_run_prune_only(self):
        result = self.run_script(
            env="prod", **{"db-instance-identifier": "laa-info-and-advice-datastore-prod"}
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("prune-only is not allowed in prod", result.stderr)
        self.assertFalse(self.calls.exists(), "Invalid configuration must not contact AWS")

    def test_prune_only_deletes_the_last_expired_owned_point_for_each_tier(self):
        for tier in ("hourly", "daily"):
            with self.subTest(tier=tier):
                self.state["snapshots"] = [snapshot(tier=tier)]
                result = self.run_script(tier=tier)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(json.loads(self.state_file.read_text())["snapshots"], [])
                self.assertNotIn("create-db-snapshot", self.operations())
                self.assertNotIn("copy-db-snapshot", self.operations())

    def test_pruning_uses_aws_recovery_times_not_legacy_timestamp_or_state_tags(self):
        for tier in ("hourly", "daily"):
            with self.subTest(tier=tier):
                expired = tagged(snapshot("expired", tier), **{
                    "recovery-point-time": None, "expires-at": None, "backup-state": "pending",
                })
                recent = tagged(
                    snapshot(
                        "recent", tier, SnapshotCreateTime="2026-11-17T09:30:00Z",
                        OriginalSnapshotCreateTime="2026-11-17T09:30:00Z",
                    ),
                    **{
                        "recovery-point-time": "invalid", "expires-at": "2000-01-01T00:00:00Z",
                        "backup-state": "pending",
                        **({"source-snapshot": f"rds:{DB}-2026-11-17-09-30"} if tier == "daily" else {}),
                    },
                )
                self.state["snapshots"] = [expired, recent]
                result = self.run_script(tier=tier)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(self.saved_snapshots(), [recent])

    def test_hourly_creates_and_verifies_a_tagged_point_before_pruning(self):
        self.state["snapshots"] = [snapshot()]
        self.state["db_tags"] = [{"Key": "business-unit", "Value": "LAA"}]
        result = self.run_script(**{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        remaining = self.saved_snapshots()
        self.assertEqual(len(remaining), 1)
        self.assertTrue(remaining[0]["DBSnapshotIdentifier"].startswith(DB + "-scheduled-hourly-"))
        self.assertEqual(remaining[0]["Status"], "available")
        tags = {tag["Key"]: tag["Value"] for tag in remaining[0]["TagList"]}
        self.assertFalse({"recovery-point-time", "expires-at", "backup-state"} & tags.keys())
        self.assertEqual(tags["retention-hours"], "24")
        self.assertEqual(tags["business-unit"], "LAA")
        self.assertNotIn("add-tags-to-resource", self.operations())
        self.assertLess(self.operations().index("create-db-snapshot"), self.operations().index("wait"))
        self.assertLess(self.operations().index("wait"), self.operations().index("delete-db-snapshot"))

    def test_daily_copy_keeps_source_recovery_time_and_deduplicates_across_jobs(self):
        source = automated()
        self.state["snapshots"] = [snapshot(tier="daily"), automated("16"), source]
        result = self.run_script(tier="daily", **{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        copies = [s for s in self.saved_snapshots() if s["SnapshotType"] == "manual"]
        self.assertEqual(len(copies), 1)
        tags = {tag["Key"]: tag["Value"] for tag in copies[0]["TagList"]}
        self.assertEqual(tags["source-snapshot"], source["DBSnapshotIdentifier"])
        self.assertFalse({"recovery-point-time", "expires-at", "backup-state"} & tags.keys())
        self.assertNotIn("add-tags-to-resource", self.operations())
        self.state = json.loads(self.state_file.read_text())
        result = self.run_script(
            tier="daily",
            **{"creation-enabled": "true", "run-id": "different-daily-job"},
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.operations().count("copy-db-snapshot"), 1)

    def test_waiter_timeout_retains_previous_point_and_reports_operation_may_continue(self):
        old = snapshot()
        self.state["snapshots"] = [old]
        self.state["failures"] = {"wait": ["WaiterTimeout"]}
        result = self.run_script(**{"creation-enabled": "true"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("waiter exhausted", result.stderr)
        self.assertIn("may still be running", result.stderr)
        self.assertIn(old, self.saved_snapshots())
        self.assertNotIn("delete-db-snapshot", self.operations())

    def test_creation_does_not_repair_or_adopt_changed_ownership_or_held_points(self):
        for changes in ({"env": "prod"}, {"backup-type": None}, {"incident-hold": "false"}):
            with self.subTest(changes=changes):
                old = snapshot()
                self.state["snapshots"] = [old]
                self.state["wait_tag_changes"] = changes
                result = self.run_script(**{"creation-enabled": "true"})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(old, self.saved_snapshots())
                self.assertNotIn("add-tags-to-resource", self.operations())
                self.assertNotIn("delete-db-snapshot", self.operations())

                self.state = json.loads(self.state_file.read_text())
                previous_calls = len(self.operations())
                result = self.run_script(**{"creation-enabled": "true"})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(old, self.saved_snapshots())
                resumed_operations = self.operations()[previous_calls:]
                self.assertNotIn("wait", resumed_operations)
                self.assertNotIn("add-tags-to-resource", resumed_operations)
                self.assertNotIn("delete-db-snapshot", resumed_operations)

    def test_hourly_retry_resumes_inflight_run_and_retains_from_actual_aws_start_time(self):
        self.state["snapshots"] = [snapshot()]
        self.state["created_time"] = "2026-11-17T10:00:01Z"
        self.state["failures"] = {"wait": ["WaiterTimeout"]}
        result = self.run_script(**{"creation-enabled": "true"})
        self.assertNotEqual(result.returncode, 0)
        self.state = json.loads(self.state_file.read_text())
        self.now = "2026-11-17T10:01:00Z"
        result = self.run_script(**{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.operations().count("create-db-snapshot"), 1)
        self.assertEqual(len(self.saved_snapshots()), 1)
        self.state = json.loads(self.state_file.read_text())
        self.now = "2026-11-18T10:00:00Z"
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(self.saved_snapshots()), 1)
        self.state = json.loads(self.state_file.read_text())
        self.now = "2026-11-18T10:00:01Z"
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.saved_snapshots(), [])

    def test_daily_waits_boundedly_for_current_cycle_and_never_uses_manual_source(self):
        manual = snapshot(
            suffix="recent-copy", tier="daily",
            SnapshotCreateTime="2026-11-17T09:00:00Z",
        )
        self.state["snapshots"] = [snapshot(tier="daily"), automated("16"), manual]
        self.state["late_source"] = automated()
        self.state["unfiltered_snapshots"] = True
        result = self.run_script(tier="daily", **{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = [json.loads(line) for line in self.calls.read_text().splitlines()]
        copies = [call for call in calls if call[1] == "copy-db-snapshot"]
        self.assertEqual(len(copies), 1)
        self.assertEqual(
            copies[0][2]["SourceDBSnapshotIdentifier"],
            automated()["DBSnapshotArn"],
        )
        self.assertIn("retry", result.stdout)

    def test_cross_tier_busy_races_retry_with_the_same_destination(self):
        for tier, operation in (("hourly", "create-db-snapshot"), ("daily", "copy-db-snapshot")):
            with self.subTest(tier=tier):
                self.state["snapshots"] = [snapshot(tier=tier), automated()]
                self.state["failures"] = {operation: ["InvalidDBInstanceState", None]}
                result = self.run_script(tier=tier, **{"creation-enabled": "true"})
                self.assertEqual(result.returncode, 0, result.stderr)
                calls = [json.loads(line) for line in self.calls.read_text().splitlines()]
                writes = [call for call in calls if call[1] == operation]
                self.assertEqual(len(writes), 2)
                self.assertEqual(writes[0], writes[1])

    def test_paginated_pruning_preserves_foreign_held_unexpired_and_invalid_points(self):
        for tier, day in (("hourly", "16"), ("daily", "10")):
            with self.subTest(tier=tier):
                boundary = tagged(
                    snapshot(
                        suffix="boundary", tier=tier,
                        SnapshotCreateTime=f"2026-11-{day}T10:00:00Z",
                        OriginalSnapshotCreateTime=f"2026-11-{day}T10:00:00Z",
                    ),
                    **{
                        "recovery-point-time": f"2026-11-{day}T10:00:00Z",
                        "expires-at": "2026-11-17T10:00:00Z",
                        **({"source-snapshot": f"rds:{DB}-2026-11-10-10-00"} if tier == "daily" else {}),
                    },
                )
                preserved = [
                    tagged(
                        snapshot(
                            "future", tier, SnapshotCreateTime="2026-11-17T09:00:00Z",
                            OriginalSnapshotCreateTime="2026-11-17T09:00:00Z",
                        ),
                        **{
                            "recovery-point-time": "2026-11-17T09:00:00Z",
                            "expires-at": f"2026-11-{'18' if tier == 'hourly' else '24'}T09:00:00Z",
                            **({"source-snapshot": f"rds:{DB}-2026-11-17-09-00"} if tier == "daily" else {}),
                        },
                    ),
                    tagged(snapshot("held", tier), **{"incident-hold": "false"}),
                    tagged(snapshot("missing", tier), env=None),
                    tagged(snapshot("foreign-env", tier), env="staging"),
                    tagged(snapshot("wrong-retention", tier), **{"retention-hours": "1"}),
                    snapshot("bad-time", tier, OriginalSnapshotCreateTime="yesterday"),
                    snapshot("busy", tier, Status="creating"),
                    snapshot("another-db", tier, DBInstanceIdentifier=DB + "-metabase"),
                    snapshot("foreign-arn", tier, DBSnapshotArn="arn:aws:rds:eu-west-2:999999999999:snapshot:other"),
                    snapshot("automated", tier, SnapshotType="automated"),
                    snapshot(tier="daily" if tier == "hourly" else "hourly"),
                ]
                for purpose in ("pre-change", "final", "restore-test"):
                    name = f"{DB}-{purpose}"
                    preserved.append(snapshot(
                        tier=tier, DBSnapshotIdentifier=name,
                        DBSnapshotArn=ARN_PREFIX + ":snapshot:" + name,
                    ))
                self.state["snapshots"] = [boundary, *preserved, snapshot(tier=tier)]
                self.state["page_size"] = 2
                self.state["unfiltered_snapshots"] = True
                result = self.run_script(tier=tier)
                self.assertNotEqual(result.returncode, 0)
                self.assertCountEqual(self.saved_snapshots(), preserved)
                self.assertIn("cleanup-failed", result.stdout)
                self.assertIn("held", result.stdout)

    def test_creation_failures_never_allow_pruning_and_log_context_and_duration(self):
        failures = (
            {"create-db-snapshot": ["SnapshotQuotaExceeded"]},
            {"create-db-snapshot": ["AccessDenied"]},
            {"create-db-snapshot": ["Throttling"]},
            {"create-db-snapshot": ["ReadTimeout"]},
            {"wait": ["WaiterTerminalFailure"]},
            {"list-tags-for-resource": [None, "AccessDenied"]},
        )
        for failure in failures:
            with self.subTest(failure=failure):
                old = snapshot()
                self.state["snapshots"] = [old]
                self.state["failures"] = failure
                result = self.run_script(**{"creation-enabled": "true"})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(old, self.saved_snapshots())
                self.assertNotIn("delete-db-snapshot", self.operations())
                events = [json.loads(line) for line in result.stdout.splitlines()]
                final = events[-1]
                self.assertEqual(final["event"], "failed")
                self.assertEqual(final["database"], DB)
                self.assertEqual(final["tier"], "hourly")
                self.assertEqual(final["env"], "uat")
                self.assertGreaterEqual(final["duration_seconds"], 0)

    def test_daily_uses_original_time_not_snapshot_copy_time_or_identifier_minute(self):
        source = automated()
        source["SnapshotCreateTime"] = "2026-11-17T02:05:00Z"
        source["OriginalSnapshotCreateTime"] = "2026-11-17T02:01:42Z"
        self.state["snapshots"] = [source]
        result = self.run_script(tier="daily", **{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        point = next(s for s in self.saved_snapshots() if s["SnapshotType"] == "manual")
        events = [json.loads(line) for line in result.stdout.splitlines()]
        available = next(event for event in events if event["event"] == "available")
        self.assertEqual(available["recovery_point_time"], "2026-11-17T02:01:42Z")
        self.state = json.loads(self.state_file.read_text())
        self.now = "2026-11-24T02:01:41Z"
        result = self.run_script(tier="daily")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(point, self.saved_snapshots())
        self.state = json.loads(self.state_file.read_text())
        self.now = "2026-11-24T02:01:42Z"
        result = self.run_script(tier="daily")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn(point, self.saved_snapshots())

    def test_daily_verifies_persisted_source_identity_before_pruning(self):
        old = snapshot(tier="daily")
        self.state["snapshots"] = [old, automated()]
        self.state["wait_tag_changes"] = {"source-snapshot": automated("16")["DBSnapshotIdentifier"]}
        result = self.run_script(tier="daily", **{"creation-enabled": "true"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(old, self.saved_snapshots())
        self.assertNotIn("delete-db-snapshot", self.operations())

        self.state = json.loads(self.state_file.read_text())
        previous_calls = len(self.operations())
        result = self.run_script(tier="daily", **{"creation-enabled": "true"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(old, self.saved_snapshots())
        resumed_operations = self.operations()[previous_calls:]
        self.assertNotIn("wait", resumed_operations)
        self.assertNotIn("delete-db-snapshot", resumed_operations)

    def test_invalid_or_expired_aws_recovery_times_never_allow_pruning(self):
        cases = (
            ("hourly", {"created_time": "2026-11-18T10:00:00Z"}),
            ("hourly", {"created_time": "2026-11-16T09:00:00Z"}),
            ("daily", {"wait_snapshot_changes": {"OriginalSnapshotCreateTime": None}}),
            ("daily", {"wait_snapshot_changes": {"OriginalSnapshotCreateTime": "yesterday"}}),
            ("daily", {"wait_snapshot_changes": {"OriginalSnapshotCreateTime": "2026-11-17T03:00:00Z"}}),
        )
        for tier, scenario in cases:
            with self.subTest(tier=tier, scenario=scenario):
                old = snapshot(tier=tier)
                self.state = {
                    "instance": self.state["instance"],
                    "snapshots": [old, automated()],
                    **scenario,
                }
                result = self.run_script(tier=tier, **{"creation-enabled": "true"})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(old, self.saved_snapshots())
                self.assertNotIn("delete-db-snapshot", self.operations())

    def test_source_retry_refreshes_the_clock_instead_of_rejecting_newer_points(self):
        source = automated()
        source["SnapshotCreateTime"] = "2026-11-17T10:00:30Z"
        source["OriginalSnapshotCreateTime"] = "2026-11-17T10:00:30Z"
        self.state["snapshots"] = [automated("16")]
        self.state["late_source"] = source
        self.state["created_time"] = "2026-11-17T10:01:00Z"
        times = iter([
            datetime.fromisoformat(value) for value in (
                "2026-11-17T10:00:00+00:00",
                "2026-11-17T10:01:00+00:00",
                "2026-11-17T10:01:00+00:00",
                "2026-11-17T10:01:00+00:00",
            )
        ])
        self.clock = lambda: next(times)
        result = self.run_script(tier="daily", **{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.operations().count("copy-db-snapshot"), 1)

    def test_missing_stale_manual_or_foreign_sources_fail_after_three_attempts(self):
        for sources in (
            [],
            [automated("16")],
            [snapshot("recent-manual", tier="daily", SnapshotCreateTime="2026-11-17T09:00:00Z")],
            [automated() | {"DBInstanceIdentifier": DB + "-metabase"}],
            [automated() | {"Status": "creating"}],
        ):
            with self.subTest(sources=sources):
                self.calls.unlink(missing_ok=True)
                old = snapshot(tier="daily")
                self.state["snapshots"] = [old, *sources]
                self.state["unfiltered_snapshots"] = True
                result = self.run_script(tier="daily", **{"creation-enabled": "true"})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("after 3 attempts", result.stderr)
                self.assertEqual(self.operations().count("describe-db-snapshots"), 3)
                self.assertNotIn("copy-db-snapshot", self.operations())
                self.assertIn(old, self.saved_snapshots())

    def test_surviving_cross_tier_activity_defers_without_any_writes(self):
        old = snapshot()
        self.state["snapshots"] = [old, snapshot("in-flight", tier="daily", Status="copying")]
        result = self.run_script(**{"creation-enabled": "true"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("after 3 attempts", result.stderr)
        self.assertEqual(self.saved_snapshots(), self.state["snapshots"])
        self.assertNotIn("create-db-snapshot", self.operations())
        self.assertNotIn("delete-db-snapshot", self.operations())

    def test_already_exists_race_resumes_the_owned_identity(self):
        self.state["snapshots"] = [snapshot()]
        self.state["creation_race"] = True
        result = self.run_script(**{"creation-enabled": "true"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.operations().count("create-db-snapshot"), 1)
        self.assertEqual(len(self.saved_snapshots()), 1)

    def test_pruning_includes_all_pages_from_the_sdk_paginator(self):
        self.state["snapshots"] = [snapshot("one"), snapshot("two")]
        self.state["page_size"] = 1
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.saved_snapshots(), [])

    def test_unreadable_tags_or_failed_delete_leave_expired_point_and_report_failure(self):
        for operation in ("list-tags-for-resource", "delete-db-snapshot"):
            with self.subTest(operation=operation):
                self.state["snapshots"] = [snapshot()]
                self.state["failures"] = {operation: ["AccessDenied"]}
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(self.saved_snapshots(), self.state["snapshots"])
                self.assertIn("cleanup-failed", result.stdout)

    def test_invalid_configuration_fails_before_aws_calls(self):
        for invalid in (
            {"creation-enabled": "yes"},
            {"db-instance-identifier": DB + "-metabase"},
            {"run-id": " "},
        ):
            with self.subTest(invalid=invalid):
                self.calls.unlink(missing_ok=True)
                result = self.run_script(**{"creation-enabled": "true", **invalid})
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(self.operations(), [])

    def test_only_hourly_points_can_fall_back_to_aws_snapshot_create_time(self):
        for tier in ("hourly", "daily"):
            with self.subTest(tier=tier):
                point = snapshot(
                    tier=tier, SnapshotCreateTime="2026-11-17T09:30:00Z",
                    OriginalSnapshotCreateTime=None,
                )
                self.state["snapshots"] = [point]
                result = self.run_script(tier=tier)
                self.assertEqual(result.returncode, 0 if tier == "hourly" else 1, result.stderr)
                self.assertEqual(self.saved_snapshots(), [point])
                self.assertNotIn("delete-db-snapshot", self.operations())


if __name__ == "__main__":
    unittest.main()
