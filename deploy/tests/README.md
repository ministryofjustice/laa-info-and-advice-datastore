# RDS snapshot lifecycle

[`rds-backup.sh`](../laa-info-and-advice-datastore/files/rds-backup.sh) is the
shared AWS CLI entrypoint for MEM-1679. It does not install or enable any Kubernetes
resources; ConfigMap mounting, schedules, networking and deployment gates belong
to MEM-1680. It needs `/bin/sh`, Python 3.9+, AWS CLI v2, `date`, `sleep` and the
environment's dedicated backup IRSA identity, not SQL access or database credentials.

Run the retained offline tests with `make test-backups` **from the repository
root**. These execute the real entrypoint against a fake AWS CLI and deterministic
clock, with no AWS, Kubernetes or container execution. The same command runs in
PR CI.

## Invocation and safety controls

Example for **UAT prune-only**, which deletes expired managed hourly snapshots.
Run from the repository root:

```sh
sh deploy/laa-info-and-advice-datastore/files/rds-backup.sh \
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
retention tags alone.

Missing/late sources, source snapshot activity and create/copy busy or
already-exists races get at most three attempts, 30 seconds apart. Each AWS call
also has bounded standard retries and client timeouts. The availability waiter
has a 1,900-second client bound; waiter exhaustion or timeout fails the run and
**does not mean the AWS operation stopped**. Retry with the same run identity.
Quota, permission, throttling and terminal waiter failures remain failures.

Pruning is paginated, source/tier-specific and limited to available manual
snapshots under `<db>-scheduled-<tier>-` with matching ARN, complete ownership
tags and a valid recovery/retention/expiry relationship. Foreign, pre-change,
final, restore-test and other-tier snapshots are excluded. Missing, conflicting,
unreadable or invalid metadata is not adopted: it is skipped and the job fails
for operator attention. Successful new points retain platform tags from the DB;
backup metadata is explicitly overwritten rather than relying on copy-tags.

The additional `backup-state=pending` tag identifies a complete initial creation
request. Only the exact run identity can finish its timestamp handoff to AWS's
actual original time and mark it `available`. Pending points cannot be pruned.
This permits a replacement pod to resume after a waiter failure without
adopting unowned or incompletely tagged snapshots.

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
shell command. Override that command with `/bin/sh` and the mounted script.
The script has no pip dependencies or installation step.

**Deferred by agreement:** actual image/tool versions and execution under the
non-root, read-only security context have not been verified. Before deployment,
MEM-1680 must smoke-test this digest with fake AWS responses, a numeric non-root
UID, dropped capabilities, no privilege escalation and a read-only root
filesystem. Supply only an explicitly writable `/tmp` (`TMPDIR` and `HOME`
pointing there), ensure the IRSA token is readable, and schedule on amd64 nodes.
Live snapshot permissions are exercised during controlled enablement, not by
these offline tests.
