#!/bin/sh
set -eu

exec python3 - "$@" <<'PY'
import argparse
from datetime import datetime, timedelta
import hashlib
import json
import os
import re
import subprocess
import sys
import time


CONTEXT = {}
STARTED = time.monotonic()


class BackupError(Exception):
    pass


class RetryableError(BackupError):
    pass


def timestamp(value):
    try:
        result = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if result.utcoffset() != timedelta(0):
            raise ValueError()
        return result
    except (AttributeError, TypeError, ValueError):
        raise BackupError("invalid UTC timestamp") from None


def log(event, **fields):
    print(json.dumps({"event": event, **CONTEXT, **fields}), flush=True)


def utc(value):
    return value.isoformat().replace("+00:00", "Z")


def original_time(point):
    return timestamp(point.get("OriginalSnapshotCreateTime") or point["SnapshotCreateTime"])


class Lifecycle:
    def __init__(self, args):
        self.args = args
        self.now = timestamp(subprocess.check_output(
            ["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"], text=True
        ).strip())
        self.cycle_start = self.now.replace(hour=0, minute=0, second=0, microsecond=0)
        self.retention = timedelta(hours=args.retention_hours)
        self.prefix = f"{args.db_instance_identifier}-scheduled-{args.tier}-"
        self.owner = {
            "service": args.service,
            "env": args.env,
            "backup-type": "scheduled",
            "backup-tier": args.tier,
            "retention-hours": str(args.retention_hours),
            "managed-by": "datastore-backup-automation",
        }
        self.source = None
        instances = self.aws(
            "describe-db-instances", "--db-instance-identifier", args.db_instance_identifier
        )["DBInstances"]
        if len(instances) != 1:
            raise BackupError("expected exactly one source database")
        self.db = instances[0]
        self.db_arn = self.db["DBInstanceArn"]
        pattern = rf"arn:aws:rds:{re.escape(args.region)}:\d{{12}}:db:{re.escape(args.db_instance_identifier)}"
        if self.db["DBInstanceIdentifier"] != args.db_instance_identifier or not re.fullmatch(pattern, self.db_arn):
            raise BackupError("unexpected source database ARN")
        self.snapshot_arn_prefix = self.db_arn.rsplit(":db:", 1)[0] + ":snapshot:"

    def aws(self, operation, *options, timeout=90):
        env = {
            **os.environ,
            "AWS_PAGER": "",
            "AWS_MAX_ATTEMPTS": "3",
            "AWS_RETRY_MODE": "standard",
            "AWS_STS_REGIONAL_ENDPOINTS": "regional",
            "AWS_EC2_METADATA_DISABLED": "true",
        }
        try:
            result = subprocess.run(
                ["aws", "rds", operation, *options, "--region", self.args.region,
                 "--output", "json", "--cli-connect-timeout", "10", "--cli-read-timeout", "30"],
                env=env, text=True, capture_output=True, timeout=timeout,
            )
        except subprocess.TimeoutExpired:
            raise BackupError(f"{operation}: client timeout; AWS operation may still be running") from None
        if result.returncode:
            if operation == "wait" and "Max attempts exceeded" in result.stderr:
                raise BackupError("waiter exhausted; AWS operation may still be running")
            code = re.search(r"\(([A-Za-z0-9._-]+)\)", result.stderr)
            code = code.group(1) if code else "AWS CLI failure"
            error = RetryableError if operation in ("create-db-snapshot", "copy-db-snapshot") and code in (
                "InvalidDBInstanceState", "InvalidDBInstanceStateFault",
                "InvalidDBSnapshotState", "InvalidDBSnapshotStateFault",
                "DBSnapshotAlreadyExists", "DBSnapshotAlreadyExistsFault",
            ) else BackupError
            raise error(f"{operation}: {code}")
        try:
            return json.loads(result.stdout or "{}")
        except ValueError:
            raise BackupError(f"{operation}: invalid JSON response") from None

    def snapshots(self, snapshot_type=None):
        options = ["--db-instance-identifier", self.args.db_instance_identifier,
                   "--no-paginate", "--max-records", "100"]
        if snapshot_type:
            options += ["--snapshot-type", snapshot_type]
        snapshots, markers = [], set()
        while True:
            page = self.aws("describe-db-snapshots", *options)
            snapshots.extend(page["DBSnapshots"])
            marker = page.get("Marker")
            if not marker:
                return snapshots
            if marker in markers:
                raise BackupError("snapshot pagination repeated a marker")
            markers.add(marker)
            options = [*options[:options.index("--marker")], "--marker", marker] if "--marker" in options else [*options, "--marker", marker]

    def tags(self, arn):
        raw = self.aws("list-tags-for-resource", "--resource-name", arn)["TagList"]
        tags = {tag["Key"]: tag["Value"] for tag in raw}
        if len(tags) != len(raw):
            raise BackupError("duplicate tag keys")
        return tags

    def owned(self, point):
        return (
            point["DBInstanceIdentifier"] == self.args.db_instance_identifier
            and point["SnapshotType"] == "manual"
            and point["DBSnapshotIdentifier"].startswith(self.prefix)
            and point["DBSnapshotArn"] == self.snapshot_arn_prefix + point["DBSnapshotIdentifier"]
        )

    def verify_tags(self, point, tags, pending=False):
        if any(tags.get(key) != value for key, value in self.owner.items()):
            raise BackupError("missing or conflicting ownership tags")
        if tags.get("backup-state") not in (None, "available") and not pending:
            raise BackupError("snapshot metadata has not been finalised")
        recovery = timestamp(tags.get("recovery-point-time"))
        expiry = timestamp(tags.get("expires-at"))
        if recovery > self.now or expiry != recovery + self.retention:
            raise BackupError("invalid recovery-point/expiry relationship")
        if self.args.tier == "daily":
            source = re.fullmatch(
                rf"rds:{re.escape(self.args.db_instance_identifier)}-(\d{{4}}-\d{{2}}-\d{{2}}-\d{{2}}-\d{{2}})",
                tags.get("source-snapshot", ""),
            )
            if not source:
                raise BackupError("missing or conflicting daily source identity")
        original = point.get("OriginalSnapshotCreateTime")
        if self.args.tier == "daily" and not original:
            raise BackupError("AWS original snapshot time unavailable; refusing to trust copy time or tags")
        if self.args.tier == "hourly" and not original:
            original = point["SnapshotCreateTime"]
        if original and not pending and timestamp(original) != recovery:
            raise BackupError("recovery-point tag conflicts with AWS snapshot time")
        if timestamp(point["SnapshotCreateTime"]) < recovery:
            raise BackupError("snapshot predates its claimed recovery point")
        return recovery, expiry

    def destination_tags(self, platform_tags, recovery):
        inherited = {
            key: value for key, value in platform_tags.items()
            if not key.startswith("aws:")
            and key not in {*self.owner, "recovery-point-time", "expires-at", "source-snapshot", "incident-hold", "backup-state"}
        }
        tags = {
            **inherited, **self.owner,
            "recovery-point-time": utc(recovery),
            "expires-at": utc(recovery + self.retention),
            "backup-state": "available",
        }
        if self.source:
            tags["source-snapshot"] = self.source["DBSnapshotIdentifier"]
        return tags

    def by_name(self, name):
        points = self.aws(
            "describe-db-snapshots", "--db-snapshot-identifier", name, "--no-paginate"
        )["DBSnapshots"]
        if len(points) != 1 or points[0]["DBSnapshotIdentifier"] != name or not self.owned(points[0]):
            raise BackupError("new/resumed snapshot does not match the owned run identity")
        return points[0]

    def create(self):
        for attempt in range(1, 4):
            try:
                self.create_attempt()
                return
            except RetryableError as error:
                if attempt == 3:
                    raise BackupError(f"creation deferred after {attempt} attempts: {error}") from None
                log("retry", attempt=attempt, reason=str(error))
                subprocess.run(["sleep", "30"], check=True)

    def create_attempt(self):
        self.now = timestamp(subprocess.check_output(
            ["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"], text=True
        ).strip())
        self.source = None
        if self.args.tier == "daily":
            sources = [
                point for point in self.snapshots("automated")
                if point["SnapshotType"] == "automated"
                and point["DBInstanceIdentifier"] == self.args.db_instance_identifier
                and point["Status"] == "available"
                and re.fullmatch(
                    re.escape(self.snapshot_arn_prefix + "rds:" + self.args.db_instance_identifier)
                    + r"-\d{4}-\d{2}-\d{2}-\d{2}-\d{2}", point["DBSnapshotArn"]
                )
                and point["DBSnapshotArn"] == self.snapshot_arn_prefix + point["DBSnapshotIdentifier"]
                and self.cycle_start <= original_time(point) <= min(
                    self.now, self.cycle_start + timedelta(days=1) - timedelta(microseconds=1)
                )
            ]
            if not sources:
                raise RetryableError("no available automated snapshot in the current UTC backup cycle")
            self.source = max(sources, key=original_time)
            log("source-selected", snapshot=self.source["DBSnapshotIdentifier"],
                recovery_point_time=utc(original_time(self.source)))
        identity = self.source["DBSnapshotArn"] if self.source else self.args.run_id
        name = self.prefix + hashlib.sha256(identity.encode()).hexdigest()[:24]
        existing = [p for p in self.snapshots("manual") if p["DBSnapshotIdentifier"] == name]
        platform_tags = self.tags(self.db_arn)
        if existing:
            point = self.by_name(name)
            tags = self.tags(point["DBSnapshotArn"])
            if "incident-hold" in tags:
                raise BackupError("current run snapshot is held")
            self.verify_tags(point, tags, pending=tags.get("backup-state") == "pending")
        else:
            if not self.source and self.db["DBInstanceStatus"] != "available":
                self.db["DBInstanceStatus"] = self.aws(
                    "describe-db-instances", "--db-instance-identifier", self.args.db_instance_identifier
                )["DBInstances"][0]["DBInstanceStatus"]
                if self.db["DBInstanceStatus"] != "available":
                    raise RetryableError("source database is not available for a fresh snapshot")
            if any(p["Status"] in ("creating", "copying") for p in self.snapshots()):
                raise RetryableError("source database has snapshot activity")
            recovery = original_time(self.source) if self.source else self.now
            tags = self.destination_tags(platform_tags, recovery)
            tags["backup-state"] = "pending"
            operation, options = (
                ("copy-db-snapshot", [
                    "--source-db-snapshot-identifier", self.source["DBSnapshotArn"],
                    "--target-db-snapshot-identifier", name,
                ])
                if self.source else
                ("create-db-snapshot", [
                    "--db-instance-identifier", self.args.db_instance_identifier,
                    "--db-snapshot-identifier", name,
                ])
            )
            self.aws(operation, *options, "--tags",
                     json.dumps([{"Key": key, "Value": value} for key, value in tags.items()]))
        self.aws("wait", "db-snapshot-available", "--db-snapshot-identifier", name, timeout=1900)
        point = self.by_name(name)
        if point["Status"] != "available":
            raise BackupError("snapshot waiter returned without an available point")
        observed = self.tags(point["DBSnapshotArn"])
        contract = {*self.owner, "recovery-point-time", "expires-at", "source-snapshot", "backup-state"}
        if "incident-hold" in observed or any(observed.get(key) != tags.get(key) for key in contract):
            raise BackupError("snapshot metadata changed or is incomplete/held; refusing to retag or prune")
        recovery = original_time(self.source or point)
        if self.db.get("StorageEncrypted") and point.get("Encrypted") is not True:
            raise BackupError("new/resumed snapshot must remain encrypted")
        tags = self.destination_tags(platform_tags, recovery)
        self.aws(
            "add-tags-to-resource", "--resource-name", point["DBSnapshotArn"], "--tags",
            json.dumps([{"Key": key, "Value": value} for key, value in tags.items()]),
        )
        self.now = timestamp(subprocess.check_output(
            ["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"], text=True
        ).strip())
        verified = self.tags(point["DBSnapshotArn"])
        if "incident-hold" in verified or any(verified.get(key) != tags.get(key) for key in contract):
            raise BackupError("snapshot tag verification failed or snapshot is held")
        _, expiry = self.verify_tags(point, verified)
        if expiry <= self.now:
            raise BackupError("run snapshot is already expired; retaining previous points")
        log("available", snapshot=name, recovery_point_time=utc(recovery),
            duration_seconds=round(time.monotonic() - STARTED, 3))

    def prune(self):
        failed = False
        for point in self.snapshots("manual"):
            if not self.owned(point) or point["Status"] != "available":
                continue
            try:
                tags = self.tags(point["DBSnapshotArn"])
                if "incident-hold" in tags:
                    log("held", snapshot=point["DBSnapshotIdentifier"])
                    continue
                recovery, expiry = self.verify_tags(point, tags)
                if expiry > self.now:
                    continue
                self.aws("delete-db-snapshot", "--db-snapshot-identifier", point["DBSnapshotIdentifier"])
                log("deleted", snapshot=point["DBSnapshotIdentifier"],
                    recovery_point_time=tags["recovery-point-time"], expires_at=tags["expires-at"])
            except BackupError as error:
                failed = True
                log("cleanup-failed", snapshot=point["DBSnapshotIdentifier"], reason=str(error))
        if failed:
            raise BackupError("cleanup incomplete; inspect skipped or failed snapshots")


def main():
    parser = argparse.ArgumentParser(description="Create/copy and prune owned RDS snapshots")
    parser.add_argument("--db-instance-identifier", required=True)
    parser.add_argument("--region", required=True)
    parser.add_argument("--service", required=True)
    parser.add_argument("--env", required=True, choices=("uat", "staging", "prod"))
    parser.add_argument("--tier", required=True, choices=("hourly", "daily"))
    parser.add_argument("--retention-hours", required=True, type=int)
    parser.add_argument("--creation-enabled", required=True, choices=("true", "false"))
    parser.add_argument("--run-id", required=True)
    args = parser.parse_args()
    if not args.run_id.strip():
        parser.error("scheduled-run identity must not be blank")
    if args.env == "prod" and args.creation_enabled == "false":
        parser.error("prune-only is not allowed in prod")
    if args.retention_hours != (24 if args.tier == "hourly" else 168):
        parser.error("retention must be 24 hours for hourly or 168 hours for daily")
    if args.db_instance_identifier != f"{args.service}-{args.env}" or args.service != "laa-info-and-advice-datastore":
        parser.error("database identifier must match the service and environment")
    CONTEXT.update(database=args.db_instance_identifier, env=args.env, tier=args.tier,
                   run_id=args.run_id, creation_enabled=args.creation_enabled)
    log("started")
    lifecycle = Lifecycle(args)
    if args.creation_enabled == "true":
        lifecycle.create()
    lifecycle.prune()
    log("completed", cleanup="complete", duration_seconds=round(time.monotonic() - STARTED, 3))


if __name__ == "__main__":
    try:
        main()
    except (BackupError, KeyError, TypeError, ValueError, OSError) as error:
        log("failed", reason=str(error), duration_seconds=round(time.monotonic() - STARTED, 3))
        print(f"backup failed: {error}", file=sys.stderr)
        sys.exit(1)
PY
