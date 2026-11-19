from copy import deepcopy
from datetime import datetime
import json

from botocore.exceptions import ClientError, ReadTimeoutError, WaiterError
from botocore.session import get_session
from botocore.validate import validate_parameters


class RDSStub:
    def __init__(self, state, calls):
        self.state = deepcopy(state)
        self.calls = calls
        self.model = get_session().get_service_model("rds")

    def __getattr__(self, name):
        return lambda **params: self.call(name.replace("_", "-"), **params)

    def get_paginator(self, name):
        if name != "describe_db_snapshots":
            raise AssertionError(f"Unexpected paginator: {name}")
        return self

    def paginate(self, **params):
        points = self.call("describe-db-snapshots", **params)["DBSnapshots"]
        size = self.state.get("page_size", 100)
        for start in range(0, max(1, len(points)), size):
            yield {"DBSnapshots": points[start:start + size]}

    def get_waiter(self, name):
        if name != "db_snapshot_available":
            raise AssertionError(f"Unexpected waiter: {name}")
        return self

    def wait(self, **params):
        self.call("wait", **params)

    def call(self, operation, **params):
        api_name = "".join("DB" if part == "db" else part.title() for part in operation.split("-"))
        if operation != "wait":
            validate_parameters(params, self.model.operation_model(api_name).input_shape)
        with self.calls.open("a") as calls:
            calls.write(json.dumps(["rds", operation, params]) + "\n")
        failures = self.state.get("failures", {}).get(operation, [])
        failure = failures.pop(0) if failures else None
        if failure:
            if failure == "ReadTimeout":
                raise ReadTimeoutError(endpoint_url="https://rds.eu-west-2.amazonaws.com")
            if operation == "wait":
                raise WaiterError(
                    name="db_snapshot_available",
                    reason="Max attempts exceeded" if failure == "WaiterTimeout" else "Terminal failure",
                    last_response={},
                )
            raise ClientError({"Error": {"Code": failure}}, api_name)

        response = {}
        if operation == "describe-db-instances":
            response = {"DBInstances": [self.state["instance"]]}
        elif operation == "describe-db-snapshots":
            points = self.state["snapshots"]
            if params.get("DBSnapshotIdentifier"):
                points = [p for p in points if p["DBSnapshotIdentifier"] == params["DBSnapshotIdentifier"]]
            tier = params.get("SnapshotType")
            if tier == "automated" and "late_source" in self.state:
                self.state["source_reads"] = self.state.get("source_reads", 0) + 1
                if self.state["source_reads"] == 2:
                    points.append(self.state["late_source"])
            if tier and not self.state.get("unfiltered_snapshots"):
                points = [p for p in points if p["SnapshotType"] == tier]
            response = {"DBSnapshots": points}
        elif operation == "list-tags-for-resource":
            arn = params["ResourceName"]
            if arn == self.state["instance"]["DBInstanceArn"]:
                response = {"TagList": self.state.get("db_tags", [])}
            else:
                point = next(p for p in self.state["snapshots"] if p["DBSnapshotArn"] == arn)
                response = {"TagList": point["TagList"]}
        elif operation == "delete-db-snapshot":
            self.state["snapshots"] = [
                p for p in self.state["snapshots"] if p["DBSnapshotIdentifier"] != params["DBSnapshotIdentifier"]
            ]
        elif operation in ("create-db-snapshot", "copy-db-snapshot"):
            name = params.get("DBSnapshotIdentifier") or params["TargetDBSnapshotIdentifier"]
            source = next((
                p for p in self.state["snapshots"]
                if p["DBSnapshotArn"] == params.get("SourceDBSnapshotIdentifier")
            ), None)
            created = self.state.get("created_time", "2026-11-17T10:00:00Z")
            point = {
                "DBSnapshotIdentifier": name,
                "DBSnapshotArn": self.state["instance"]["DBInstanceArn"].rsplit(":db:", 1)[0] + ":snapshot:" + name,
                "DBInstanceIdentifier": self.state["instance"]["DBInstanceIdentifier"],
                "SnapshotType": "manual",
                "Status": "creating",
                "SnapshotCreateTime": created,
                "OriginalSnapshotCreateTime": (
                    source.get("OriginalSnapshotCreateTime") or source["SnapshotCreateTime"]
                ) if source else created,
                "Encrypted": True,
                "TagList": params["Tags"],
            }
            self.state["snapshots"].append(point)
            response = {"DBSnapshot": point}
            if self.state.pop("creation_race", False):
                raise ClientError({"Error": {"Code": "DBSnapshotAlreadyExists"}}, api_name)
        elif operation == "wait":
            point = next(
                p for p in self.state["snapshots"] if p["DBSnapshotIdentifier"] == params["DBSnapshotIdentifier"]
            )
            point["Status"] = "available"
            point.update(self.state.get("wait_snapshot_changes", {}))
            tags = {tag["Key"]: tag["Value"] for tag in point["TagList"]}
            tags.update(self.state.get("wait_tag_changes", {}))
            point["TagList"] = [
                {"Key": key, "Value": value} for key, value in tags.items() if value is not None
            ]
        else:
            raise AssertionError(f"Unexpected RDS operation: {operation}")

        response = deepcopy(response)
        points = response.get("DBSnapshots", [])
        if "DBSnapshot" in response:
            points.append(response["DBSnapshot"])
        for point in points:
            for key in ("SnapshotCreateTime", "OriginalSnapshotCreateTime"):
                if isinstance(point.get(key), str):
                    try:
                        point[key] = datetime.fromisoformat(point[key].replace("Z", "+00:00"))
                    except ValueError:
                        pass
        return response
