# RDS snapshot lifecycle

[`rds-backup.py`](../laa-info-and-advice-datastore/files/rds-backup.py) is the
shared AWS CLI entrypoint for MEM-1679. It does not install or enable any Kubernetes
resources; ConfigMap mounting, schedules, networking and deployment gates belong
to MEM-1680. It needs Python 3.9+, AWS CLI v2, `date`, `sleep` and the
environment's dedicated backup IRSA identity, not SQL access or database credentials.

Run the retained offline tests with `make test-backups` **from the repository
root**. These execute the real entrypoint against a fake AWS CLI and deterministic
clock, with no AWS, Kubernetes or container execution. The same command runs in
PR CI.

## Invocation and safety controls

Example for **UAT prune-only**, which deletes expired managed hourly snapshots.
Run from the repository root:

```sh
python3 deploy/laa-info-and-advice-datastore/files/rds-backup.py \
  --db-instance-identifier laa-info-and-advice-datastore-uat \
  --region eu-west-2 \
  --service laa-info-and-advice-datastore \
  --env uat \
  --tier hourly \
  --retention-hours 24 \
  --creation-enabled false \
  --run-id '<stable-job-name>'
```

Use the Kubernetes Job name for `--run-id`, unchanged across replacement pods.
An hourly snapshot's name hashes this identity; daily names hash the automated
source ARN, so different Jobs copying the same source resume the same point.
Do not generate a new hourly identity on every retry.

| Tier | Creation enabled | Retention |
|---|---|---|
| `hourly` | Create a fresh manual snapshot | `--retention-hours 24` |
| `daily` | Copy the latest available automated snapshot of this DB | `--retention-hours 168` |

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
already-exists races get at most three attempts, 30 seconds apart. Each AWS call
also has bounded standard retries and client timeouts. The availability waiter
has a 1,900-second client bound; waiter exhaustion or timeout fails the run and
**does not mean the AWS operation stopped**. Retry with the same run identity.
Quota, permission, throttling and terminal waiter failures remain failures.

AWS CLI handles snapshot pagination automatically. Pruning remains
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

Pin the existing Cloud Platform tools image, with no new image build pipeline:

```text
ministryofjustice/cloud-platform-tools:2.12.0@sha256:0e6f63db6144437b9daf755653dbec9179a7197c58e38f7c33c87ee22ec165ae
```

Its registry config/history was inspected without executing it: Linux/amd64,
Python 3 and coreutils installation, AWS CLI copied into `/usr/local`, and a
shell command. Override that command with `python3` and the mounted `.py` script.
The script has no pip dependencies or installation step.

With Docker running, execute the opt-in image smoke test from the repository root:

```sh
make smoke-test-backups
```

It runs the pinned Linux/amd64 image as UID/GID 1000, with all capabilities
dropped, no privilege escalation, default seccomp, no networking and a read-only
root filesystem. Only `/tmp` is explicitly writable (`TMPDIR` and `HOME` point
there); the script and tests are mounted read-only. CPU, memory and process
limits bound the container, which is removed when it exits. No host AWS
credentials, Docker socket or Kubernetes configuration are mounted.
The temporary filesystem permits execution so the tests can run their fake
CLI helpers; the tests fail immediately if those helpers cannot take precedence
over the real tools.

The smoke test verifies these restrictions, reports the actual Python/AWS CLI
versions, checks the real AWS CLI's offline snapshot output model includes
`OriginalSnapshotCreateTime`, and runs the existing lifecycle tests with fake
AWS responses. The initial image download needs registry access; test execution
has networking disabled. This target is separate from the normal offline tests
and does not run automatically in PR CI.

Verified locally: `make smoke-test-backups` passed all 25 tests
(two runtime checks plus the 23 lifecycle tests), using amd64 emulation on
Docker Desktop's ARM64 engine. The image contains Python 3.10.14 and AWS CLI
2.7.20; the snapshot output model includes `OriginalSnapshotCreateTime`.

Before deployment, MEM-1680 must also ensure the IRSA token is readable and
schedule on amd64 nodes. Live identity/network access and snapshot permissions
are exercised during controlled enablement, not by these offline tests.
