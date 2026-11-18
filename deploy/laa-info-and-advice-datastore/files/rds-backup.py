#!/usr/bin/env python3
import argparse
from datetime import datetime, timedelta
import hashlib
import json
import os
import re
import subprocess
import sys
import time


SERVICE = "laa-info-and-advice-datastore"
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
        retention_hours = 24 if args.tier == "hourly" else 168
        self.retention = timedelta(hours=retention_hours)
        self.prefix = f"{args.db_instance_identifier}-scheduled-{args.tier}-"
        self.owner = {
            "service": SERVICE,
            "env": args.env,
            "backup-type": "scheduled",
            "backup-tier": args.tier,
            "retention-hours": str(retention_hours),
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
        options = ["--db-instance-identifier", self.args.db_instance_identifier]
        if snapshot_type:
            options += ["--snapshot-type", snapshot_type]
        return self.aws("describe-db-snapshots", *options)["DBSnapshots"]

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

    def verify_tags(self, tags, source=None):
        if any(tags.get(key) != value for key, value in self.owner.items()):
            raise BackupError("missing or conflicting ownership tags")
        if self.args.tier == "daily":
            valid_source = re.fullmatch(
                rf"rds:{re.escape(self.args.db_instance_identifier)}-(\d{{4}}-\d{{2}}-\d{{2}}-\d{{2}}-\d{{2}})",
                tags.get("source-snapshot", ""),
            )
            if not valid_source or (source and tags["source-snapshot"] != source["DBSnapshotIdentifier"]):
                raise BackupError("missing or conflicting daily source identity")

    def recovery_times(self, point):
        if self.args.tier == "daily" and not point.get("OriginalSnapshotCreateTime"):
            raise BackupError("AWS original snapshot time unavailable; refusing to trust copy time or tags")
        recovery = original_time(point)
        if recovery > self.now or timestamp(point["SnapshotCreateTime"]) < recovery:
            raise BackupError("invalid AWS recovery-point time")
        return recovery, recovery + self.retention

    def destination_tags(self, platform_tags):
        inherited = {
            key: value for key, value in platform_tags.items()
            if not key.startswith("aws:")
            and key not in {*self.owner, "recovery-point-time", "expires-at", "source-snapshot", "incident-hold", "backup-state"}
        }
        tags = {**inherited, **self.owner}
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
            self.source = self.select_daily_source()
        name = self.create_or_resume()
        self.aws("wait", "db-snapshot-available", "--db-snapshot-identifier", name, timeout=1900)
        self.verify_available(name)

    def select_daily_source(self):
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
        source = max(sources, key=original_time)
        log("source-selected", snapshot=source["DBSnapshotIdentifier"],
            recovery_point_time=utc(original_time(source)))
        return source

    def create_or_resume(self):
        identity = self.source["DBSnapshotArn"] if self.source else self.args.run_id
        name = self.prefix + hashlib.sha256(identity.encode()).hexdigest()[:24]
        existing = [p for p in self.snapshots("manual") if p["DBSnapshotIdentifier"] == name]
        platform_tags = self.tags(self.db_arn)
        if existing:
            point = self.by_name(name)
            tags = self.tags(point["DBSnapshotArn"])
            if "incident-hold" in tags:
                raise BackupError("current run snapshot is held")
            self.verify_tags(tags, self.source)
        else:
            if not self.source and self.db["DBInstanceStatus"] != "available":
                self.db["DBInstanceStatus"] = self.aws(
                    "describe-db-instances", "--db-instance-identifier", self.args.db_instance_identifier
                )["DBInstances"][0]["DBInstanceStatus"]
                if self.db["DBInstanceStatus"] != "available":
                    raise RetryableError("source database is not available for a fresh snapshot")
            if any(p["Status"] in ("creating", "copying") for p in self.snapshots()):
                raise RetryableError("source database has snapshot activity")
            tags = self.destination_tags(platform_tags)
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
        return name

    def verify_available(self, name):
        point = self.by_name(name)
        if point["Status"] != "available":
            raise BackupError("snapshot waiter returned without an available point")
        if self.db.get("StorageEncrypted") and point.get("Encrypted") is not True:
            raise BackupError("new/resumed snapshot must remain encrypted")
        self.now = timestamp(subprocess.check_output(
            ["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"], text=True
        ).strip())
        tags = self.tags(point["DBSnapshotArn"])
        if "incident-hold" in tags:
            raise BackupError("current run snapshot is held")
        self.verify_tags(tags, self.source)
        recovery, expiry = self.recovery_times(point)
        if self.source and recovery != original_time(self.source):
            raise BackupError("copied snapshot recovery time conflicts with its source")
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
                self.verify_tags(tags)
                recovery, expiry = self.recovery_times(point)
                if expiry > self.now:
                    continue
                self.aws("delete-db-snapshot", "--db-snapshot-identifier", point["DBSnapshotIdentifier"])
                log("deleted", snapshot=point["DBSnapshotIdentifier"],
                    recovery_point_time=utc(recovery), expires_at=utc(expiry))
            except BackupError as error:
                failed = True
                log("cleanup-failed", snapshot=point["DBSnapshotIdentifier"], reason=str(error))
        if failed:
            raise BackupError("cleanup incomplete; inspect skipped or failed snapshots")


def main():
    parser = argparse.ArgumentParser(description="Create/copy and prune owned RDS snapshots")
    parser.add_argument("--db-instance-identifier", required=True)
    parser.add_argument("--region", required=True)
    parser.add_argument("--env", required=True, choices=("uat", "staging", "prod"))
    parser.add_argument("--tier", required=True, choices=("hourly", "daily"))
    parser.add_argument("--creation-enabled", required=True, choices=("true", "false"))
    parser.add_argument("--run-id", required=True)
    args = parser.parse_args()
    if not args.run_id.strip():
        parser.error("scheduled-run identity must not be blank")
    if args.env == "prod" and args.creation_enabled == "false":
        parser.error("prune-only is not allowed in prod")
    if args.db_instance_identifier != f"{SERVICE}-{args.env}":
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
