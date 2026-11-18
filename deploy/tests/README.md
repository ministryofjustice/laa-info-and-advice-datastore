# RDS snapshot lifecycle

[`rds-backup.py`](../laa-info-and-advice-datastore/files/rds-backup.py) is the
shared Python/boto3 entrypoint for MEM-1679. It does not install or enable any
Kubernetes resources; ConfigMap mounting, schedules, networking and deployment gates belong
to MEM-1680. It uses boto3's RDS client, paginators and waiter, with native Python
UTC timestamps and delays. The pinned Linux runtime below includes Python and
boto3; AWS CLI and shell helpers are not required. Live execution uses the
environment's dedicated backup IRSA identity, not SQL access or database credentials.

Run the retained offline tests with `make test-backups` **from the repository
root**, with Docker running. They run in the pinned SDK image, so no host Python
or pip setup is needed. The CLI's `main()` runs in-process against a stubbed RDS
client and fixed clock; request parameters are checked against the real SDK's
model. No AWS or Kubernetes calls occur. The same command runs in PR CI.

## Invocation and safety controls

Example for **UAT prune-only**, which deletes expired managed hourly snapshots.
Run from the repository root in a Python environment with boto3:

```sh
python3 deploy/laa-info-and-advice-datastore/files/rds-backup.py \
  --db-instance-identifier laa-info-and-advice-datastore-uat \
  --region eu-west-2 \
  --env uat \
  --tier hourly \
  --creation-enabled false \
  --run-id '<stable-job-name>'
```

Use the Kubernetes Job name for `--run-id`, unchanged across replacement pods.
An hourly snapshot's name hashes this identity; daily names hash the automated
source ARN, so different Jobs copying the same source resume the same point.
Do not generate a new hourly identity on every retry.

| Tier | Creation enabled | Retention |
|---|---|---|
| `hourly` | Create a fresh manual snapshot | 24 hours |
| `daily` | Copy the latest available automated snapshot of this DB | 168 hours (seven days) |

The service is fixed to `laa-info-and-advice-datastore`, and retention is derived
from the tier; neither is a CLI option.

`--creation-enabled true` must create/resume, wait and verify the point's
availability, encryption and persisted tags before pruning. Any failure stops
the run without falling back to prune-only. `false` is allowed only in UAT or
staging: it performs no creation or automated-source discovery, and **can delete
the last expired managed point**. Production rejects prune-only.
The future chart's jobs-enabled switch is separate: PR/branch releases must
deploy neither mode against shared UAT.

The daily backup cycle is the UTC calendar day at process start. Sources must
belong to this DB/account/Region, be automated and available, and have an original
recovery time in that cycle, not in the future. Yesterday's point and existing
manual copies are never fallbacks. Expiry is seven days from the source's
`OriginalSnapshotCreateTime` (or `SnapshotCreateTime` when absent), not the
destination's copy time; `SnapshotDatabaseTime` is not used.
The copied destination must expose `OriginalSnapshotCreateTime` for authoritative
verification; otherwise it is retained and the run fails rather than trusting
the destination's copy time. Hourly points use AWS's original snapshot time,
falling back to `SnapshotCreateTime` when the original field is absent.

Missing/late sources, source snapshot activity and create/copy busy or
already-exists races get at most three attempts, 30 seconds apart. RDS requests
use SDK standard retries (three total attempts), a 10-second connect timeout and
a 30-second read timeout. These are per-request limits, not the former CLI's
90-second whole-process limit.
The availability waiter polls at 30-second intervals for at most 60 attempts.
Elapsed time is owned by Kubernetes, not a Python alarm: both CronJobs must set
`spec.jobTemplate.spec.activeDeadlineSeconds: 1900` in MEM-1680. This is a
whole-Job budget, including retries, creation, waiting and cleanup, not a fresh
1,900 seconds for each wait or pod. The CronJobs are not implemented yet; direct
CLI execution has no overall elapsed-time limit.
Waiter exhaustion fails the run; an expired Job deadline terminates its pods.
**Neither means the AWS operation stopped.** Retry with the same run identity.
Deadline termination can interrupt cleanup and omit the script's final JSON log;
monitor the Job's `DeadlineExceeded` failure, not just application logs.
Quota, permission, throttling and terminal waiter failures remain failures.

Boto3 handles snapshot pagination automatically. Pruning remains
source/tier-specific and limited to available manual
snapshots under `<db>-scheduled-<tier>-` with matching ARN, complete ownership
tags and valid AWS recovery timestamps. Expiry is calculated from that recovery
time plus the fixed tier retention, never from mutable timestamp tags. Foreign, pre-change,
final, restore-test and other-tier snapshots are excluded. Missing, conflicting,
unreadable or invalid metadata is not adopted: it is skipped and the job fails
for operator attention. Successful new points retain platform tags from the DB;
backup metadata is explicitly overwritten rather than relying on copy-tags.

Creation/copy requests carry the owner, tier and retention tags, plus
`source-snapshot` for daily copies. There is no post-creation retagging or
`backup-state` handshake: AWS's availability and timestamps are authoritative.
Legacy `recovery-point-time`, `expires-at` and `backup-state` tags are ignored,
not removed. A replacement pod resumes only the exact, fully owned run identity;
creating/copying points are never pruned.

**Deployment prerequisite:** apply the matching backup IAM policy changes in
`cloud-platform-environments` before using this version. The previously deployed
policies require timestamp tags on creation/tagging/deletion; the updated policies
retain ownership, tier, retention and daily-source conditions without requiring
the removed timestamp tags. No IAM deployment is performed by these tests.

To hold incident evidence, add an `incident-hold` tag to the snapshot. **Any tag
value, including `false`, holds it.** Remove the tag using an authorised operator
identity to release it; automation never removes holds. A hold on the current
run's point also stops pruning. JSON logs include DB/environment/tier/run,
availability/recovery time, cleanup outcomes and duration; nonzero exit means
the lifecycle or cleanup is incomplete. Alert delivery belongs to MEM-1681.

## Selected runtime for MEM-1680

Pin the existing AWS Python image, with no custom build or startup installation:

```text
public.ecr.aws/lambda/python:3.13@sha256:eac388575a9ff5c116d32b9620675341f998def5fe224fc3752186986afa9a4a
```

The image includes Python 3.13.18 and boto3/botocore 1.42.97. Override its Lambda
entrypoint with `command: ["python3"]` and pass the mounted script path and CLI
arguments. This runs an ordinary Kubernetes Job, not a Lambda function.
Set UID/GID 1000 explicitly; the image does not declare a non-root default user.
Keep the dedicated IRSA role and make its projected token readable by that user.

With Docker running, execute the opt-in image smoke test from the repository root:

```sh
make smoke-test-backups
```

Both test targets run the pinned image as Linux/amd64 and UID/GID 1000, with all
capabilities dropped, no privilege escalation, default seccomp, no networking and a read-only
root filesystem. Only `/tmp` is explicitly writable (`TMPDIR` and `HOME` point
there); the script and tests are mounted read-only. CPU, memory and process
limits bound the container, which is removed when it exits. No host AWS
credentials, Docker socket or Kubernetes configuration are mounted.
The temporary filesystem is mounted `noexec`; SDK stubs and the fixed clock do
not need executable helper files.

The smoke test also verifies these restrictions, reports Python/SDK versions,
checks the real SDK's snapshot model, paginator and waiter, and confirms its
web-identity credential provider is selected with a dummy role/token. No STS
exchange is attempted. A subprocess check exercises the actual script's help,
production prune-only rejection and missing-credentials failure/exit status,
alongside the in-process lifecycle tests.
The initial image download needs registry access; test execution has networking
disabled. The extra smoke checks are opt-in and do not run automatically in PR CI.

Verified locally on 2026-11-18: `make smoke-test-backups` passed all 26 tests
(three runtime/entrypoint checks plus the 23 lifecycle tests), using amd64
emulation on Docker Desktop's ARM64 engine.

Before deployment, MEM-1680 must also check Cloud Platform registry/admission
compatibility, image vulnerabilities and IRSA token readability, and schedule on
the tested amd64 architecture (or separately smoke-test arm64). Live
identity/network access and snapshot permissions are exercised during controlled
enablement, not by these offline tests.
