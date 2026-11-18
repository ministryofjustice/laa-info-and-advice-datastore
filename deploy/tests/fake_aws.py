#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys


args = sys.argv[1:]
service, operation = args[:2]


def option(name, default=None):
    return args[args.index(name) + 1] if name in args else default


state_file = Path(os.environ["FAKE_STATE"])
state = json.loads(state_file.read_text())
with open(os.environ["FAKE_CALLS"], "a") as calls:
    calls.write(json.dumps(args) + "\n")

failures = state.get("failures", {}).get(operation, [])
if failures:
    failure = failures.pop(0)
    state_file.write_text(json.dumps(state))
    if failure:
        print(
            "Waiter failed: Max attempts exceeded" if failure == "WaiterTimeout"
            else f"An error occurred ({failure}) when calling {operation}",
            file=sys.stderr,
        )
        raise SystemExit(255)

response = {}
if service != "rds":
    raise SystemExit("Unexpected AWS service")
if operation == "describe-db-instances":
    response = {"DBInstances": [state["instance"]]}
elif operation == "describe-db-snapshots":
    snapshots = state["snapshots"]
    identifier = option("--db-snapshot-identifier")
    if identifier:
        snapshots = [s for s in snapshots if s["DBSnapshotIdentifier"] == identifier]
    snapshot_type = option("--snapshot-type")
    if snapshot_type == "automated" and "late_source" in state:
        state["source_reads"] = state.get("source_reads", 0) + 1
        if state["source_reads"] == 2:
            snapshots.append(state["late_source"])
    if snapshot_type and not state.get("unfiltered_snapshots"):
        snapshots = [s for s in snapshots if s["SnapshotType"] == snapshot_type]
    start = int(option("--marker", "0"))
    end = start + state.get("page_size", 100)
    response = {"DBSnapshots": snapshots[start:end]}
    if end < len(snapshots):
        response["Marker"] = "0" if state.get("repeat_marker") else str(end)
elif operation == "list-tags-for-resource":
    arn = option("--resource-name")
    if arn == state["instance"]["DBInstanceArn"]:
        response = {"TagList": state.get("db_tags", [])}
    else:
        snapshot = next(s for s in state["snapshots"] if s["DBSnapshotArn"] == arn)
        response = {"TagList": snapshot["TagList"]}
elif operation == "delete-db-snapshot":
    identifier = option("--db-snapshot-identifier")
    state["snapshots"] = [
        s for s in state["snapshots"] if s["DBSnapshotIdentifier"] != identifier
    ]
elif operation in ("create-db-snapshot", "copy-db-snapshot"):
    name = option("--db-snapshot-identifier") or option("--target-db-snapshot-identifier")
    source_arn = option("--source-db-snapshot-identifier")
    source = next((s for s in state["snapshots"] if s["DBSnapshotArn"] == source_arn), None)
    point = {
        "DBSnapshotIdentifier": name,
        "DBSnapshotArn": state["instance"]["DBInstanceArn"].rsplit(":db:", 1)[0] + ":snapshot:" + name,
        "DBInstanceIdentifier": state["instance"]["DBInstanceIdentifier"],
        "SnapshotType": "manual",
        "Status": "creating",
        "SnapshotCreateTime": state.get("created_time", "2026-11-17T10:00:00Z"),
        "OriginalSnapshotCreateTime": (source.get("OriginalSnapshotCreateTime") or source["SnapshotCreateTime"]) if source else state.get("created_time", "2026-11-17T10:00:00Z"),
        "Encrypted": True,
        "TagList": json.loads(option("--tags")),
    }
    state["snapshots"].append(point)
    response = {"DBSnapshot": point}
    if state.pop("creation_race", False):
        state_file.write_text(json.dumps(state))
        print("An error occurred (DBSnapshotAlreadyExists) when calling create", file=sys.stderr)
        raise SystemExit(255)
elif operation == "wait":
    if args[2] != "db-snapshot-available":
        raise SystemExit("Unexpected waiter")
    name = option("--db-snapshot-identifier")
    point = next(s for s in state["snapshots"] if s["DBSnapshotIdentifier"] == name)
    point["Status"] = "available"
    point.update(state.get("wait_snapshot_changes", {}))
    tags = {tag["Key"]: tag["Value"] for tag in point["TagList"]}
    for key, value in state.get("wait_tag_changes", {}).items():
        if value is None:
            tags.pop(key, None)
        else:
            tags[key] = value
    point["TagList"] = [{"Key": key, "Value": value} for key, value in tags.items()]
else:
    raise SystemExit("Unexpected AWS operation: " + operation)

state_file.write_text(json.dumps(state))
print(json.dumps(response))
