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
The chart's jobs-enabled switch is separate: PR/branch releases must
deploy neither mode against shared UAT.

The daily backup cycle is the UTC calendar day at each process start; replacement
pods re-evaluate it, including date changes across midnight. Sources must
belong to this DB/account/Region, be automated and available, and have an original
recovery time in that cycle, not in the future. Yesterday's point and existing
manual copies are never fallbacks. Expiry is seven days from the source's
`OriginalSnapshotCreateTime` (or `SnapshotCreateTime` when absent), not the
destination's copy time; `SnapshotDatabaseTime` is not used.
The copied destination must expose `OriginalSnapshotCreateTime` for authoritative
verification; otherwise it is retained and the run fails rather than trusting
the destination's copy time. Hourly points use AWS's original snapshot time,
falling back to `SnapshotCreateTime` when the original field is absent.

Each process makes one lifecycle attempt. Missing/late sources, snapshot
activity and create/copy errors fail that run without pruning. The chart sets
Job-level `backoffLimit: 2` and pod-level `restartPolicy: Never` on both CronJobs,
allowing an initial run and up to two replacement runs within the shared deadline.
Kubernetes uses its own backoff, not a 30-second application delay, and retries
all failed runs, including permission/quota and cleanup failures, rather than
only selected source/busy errors. A replacement pod re-reads AWS state and resumes
the same owned identity; it must not generate a new hourly run ID.
RDS requests retain SDK standard retries (at most three total attempts),
a 10-second connect timeout and
a 30-second read timeout. These are per-request limits, not the former CLI's
90-second whole-process limit.
The availability waiter polls at 30-second intervals for at most 60 attempts.
Elapsed time is owned by Kubernetes, not a Python alarm: both CronJobs set
`spec.jobTemplate.spec.activeDeadlineSeconds: 1900`. This is a
whole-Job budget, including retries, creation, waiting and cleanup, not a fresh
1,900 seconds for each wait or pod. All environments remain disabled by default; direct
CLI execution has neither whole-run retries nor an overall elapsed-time limit.
Waiter exhaustion fails the run; an expired Job deadline terminates its pods.
**Neither means the AWS operation stopped.** Retry with the same run identity.
Deadline termination can interrupt cleanup and omit the script's final JSON log;
monitor the Job's `DeadlineExceeded` and exhausted-retry `BackoffLimitExceeded`
failures, not just application logs.
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

The image includes Python 3.13.18 and boto3/botocore 1.42.97. The chart overrides
its Lambda entrypoint with `python3 -c`: a small bootstrap derives the DB
identifier from the address secret and execs the mounted script with its CLI
arguments. There is no shell or startup installation. This is an ordinary
Kubernetes Job, not a Lambda function.
Set UID/GID 1000 explicitly; the image does not declare a non-root default user.
Keep the dedicated IRSA role and make its projected token readable by that user.

With Docker running, execute the opt-in image smoke test from the repository root:

```sh
make smoke-test-backups
```

Both lifecycle test targets run the pinned image as Linux/amd64 and UID/GID 1000, with all
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

## Helm wiring and controlled UAT enablement

The chart defaults `backups.enabled` and `backups.creationEnabled` to **false**.
UAT values temporarily enable both for the daytime rehearsal; staging and
production remain disabled. After confirming an hourly snapshot, manually
suspend both UAT CronJobs and follow up with a change disabling both UAT values.
Suspension is not automatic: do it before the database stops at 22:00 UTC.
Environment values reference the existing `ia-datastore-<env>-backups` service
account; the chart does not create its role or change application identity.
Only release `laa-info-and-advice-datastore` in its matching environment namespace
can enable backups. Branch deployments explicitly force `backups.enabled=false`,
including rollback retries. Kubernetes 1.27+ is required for `timeZone`.

| Settings | Result |
|---|---|
| `backups.enabled=false` | No backup ConfigMap, CronJobs or NetworkPolicy |
| Enabled, creation off (UAT/staging only) | Both schedules prune; **even the last expired managed point can be deleted** |
| Enabled, creation on | Hourly creation and daily copying, verified before pruning |

Jobs use UTC schedules (`0 * * * *` / `30 9 * * *`), `Forbid` concurrency, a
five-minute missed-start window, one successful/three failed history entries and
a one-day finished-Job TTL. Each Job has the shared deadline/retry policy above.
Pods run on amd64 as UID/GID 1000, with a read-only root/script and a bounded
memory-backed `/tmp`. Kubernetes API token automount is disabled; the separate
IRSA web-identity projection must be verified during UAT enablement.
Only `rds_instance_address` from `rds-postgresql-instance-output` is exposed,
not database credentials. The Job-name label supplies the stable run identity.

The backup NetworkPolicy denies ingress and permits DNS (TCP/UDP 53) and IPv4
HTTPS (TCP 443). **This is not an AWS-host-specific allowlist.** Its selector
does not match application pods. Existing chart policies exclude backup pods,
including policies rendered by PR releases. NetworkPolicies are additive:
inspect namespace policies, including older PR releases, before relying on
effective isolation. The application receives no new HTTPS allowance.

With Helm (validated with 4.3.0) and Python available:

```sh
python3 -m pip install -r deploy/tests/requirements.txt
make test-backup-chart
```

This runs offline render/command/deployment-gate tests and lints all three
environments with backups explicitly disabled. Enabled configurations render under the
canonical release in tests; `helm lint` uses its own noncanonical release name.
The deployment tests expose only a stub Helm on PATH, including rollback paths;
they cannot fall back to the real deployment command. The bootstrap test stubs
process handoff rather than connecting to RDS. These checks also run in PR CI.

An enabled **render only**, with no cluster changes:

```sh
helm template laa-info-and-advice-datastore deploy/laa-info-and-advice-datastore \
  --namespace laa-info-and-advice-datastore-uat \
  --values deploy/laa-info-and-advice-datastore/values/uat.yaml \
  --set backups.enabled=true --set backups.creationEnabled=true
```

Before any live UAT run, confirm IAM tag-policy changes are applied, image
admission/pull and vulnerability checks pass, and the projected IRSA token is
readable. Verify effective DNS/STS/RDS access and preserve the snapshots' KMS
key. UAT's overnight DB stop/start prevents continuous hourly creation, so the
initial rehearsal must be time-bounded during working hours.

In controlled UAT, exercise hourly create and daily copy, verify tags/encryption/
original timestamps, prove a replacement pod resumes the same point, and prune
only explicitly disposable expired managed points. Check failed/deadline-exceeded
Jobs and the absence of backup resources in PR releases. Live permissions,
controller behaviour and alert delivery are not proven by offline tests.
To stop new runs immediately, suspend both CronJobs; inspect and stop active
Jobs separately if required. `backups.enabled=false` removes chart resources;
`creationEnabled=false` is **not suspension** and continues pruning.

```sh
kubectl -n laa-info-and-advice-datastore-uat patch cronjob \
  laa-info-and-advice-datastore-backup-hourly --type merge -p '{"spec":{"suspend":true}}'
kubectl -n laa-info-and-advice-datastore-uat patch cronjob \
  laa-info-and-advice-datastore-backup-daily --type merge -p '{"spec":{"suspend":true}}'
```

Check active Jobs and avoid deleting them without assessing ongoing AWS
operations. A subsequent deployment while UAT values remain enabled may
unsuspend the CronJobs; coordinate the follow-up disablement. Retain the
snapshots and their KMS key. Alerting is not yet in place and older UAT
namespace-wide policies also allow additional ingress/egress to backup pods.
An hourly rehearsal does not prove daily copying or restore.
