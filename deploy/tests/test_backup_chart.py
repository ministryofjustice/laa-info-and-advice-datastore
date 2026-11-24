import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]
CHART = ROOT / "deploy/laa-info-and-advice-datastore"
SERVICE = "laa-info-and-advice-datastore"


def render(environment="uat", release=SERVICE, namespace=None, kube_version="1.30.0", extra_args=(), **settings):
    env = "prod" if environment == "production" else environment
    command = [
        "helm", "template", release, str(CHART),
        "--namespace", namespace or f"{SERVICE}-{env}",
        "--kube-version", kube_version,
        "--values", str(CHART / "values" / f"{environment}.yaml"),
    ]
    for key, value in settings.items():
        command.extend(["--set", f"{key}={str(value).lower() if isinstance(value, bool) else value}"])
    command.extend(extra_args)
    return subprocess.run(command, capture_output=True, text=True, timeout=30)


def selects(selector, labels):
    if any(labels.get(key) != value for key, value in selector.get("matchLabels", {}).items()):
        return False
    for expression in selector.get("matchExpressions", []):
        if expression["operator"] != "NotIn":
            raise AssertionError(f"Unexpected selector operator: {expression}")
        if labels.get(expression["key"]) in expression["values"]:
            return False
    return True


class BackupChartTests(unittest.TestCase):
    def resources(self, **settings):
        result = render(**settings)
        self.assertEqual(result.returncode, 0, result.stderr)
        return [resource for resource in yaml.safe_load_all(result.stdout) if resource]

    def test_chart_defaults_do_not_enable_backups(self):
        values = yaml.safe_load((CHART / "values/uat.yaml").read_text())
        values["backups"].pop("enabled")
        values["backups"].pop("creationEnabled")
        with tempfile.TemporaryDirectory() as work:
            path = Path(work) / "values.yaml"
            path.write_text(yaml.safe_dump(values))
            result = subprocess.run(
                ["helm", "template", SERVICE, str(CHART), "--namespace", f"{SERVICE}-uat", "--values", str(path)],
                capture_output=True, text=True, timeout=30,
            )
        self.assertEqual(result.returncode, 0, result.stderr)
        resources = [resource for resource in yaml.safe_load_all(result.stdout) if resource]
        self.assertFalse(any(r["metadata"].get("labels", {}).get("app.kubernetes.io/component") == "backup"
                             for r in resources))

    def test_backups_are_absent_in_other_environments_and_pr_releases(self):
        for environment in ("staging", "production"):
            resources = self.resources(environment=environment)
            self.assertFalse(any(r["metadata"].get("labels", {}).get("app.kubernetes.io/component") == "backup"
                                 for r in resources))
        for environment in ("uat", "staging", "production"):
            for release in (SERVICE, "feature-example"):
                with self.subTest(environment=environment, release=release):
                    resources = self.resources(environment=environment, release=release, **{"backups.enabled": False})
                    backups = [
                        resource for resource in resources
                        if resource["metadata"].get("labels", {}).get("app.kubernetes.io/component") == "backup"
                    ]
                    self.assertEqual(backups, [])

    def test_uat_canonical_values_enable_both_creation_jobs(self):
        jobs = [r for r in self.resources() if r["kind"] == "CronJob"]
        self.assertEqual({job["metadata"]["labels"]["backup-tier"] for job in jobs}, {"hourly", "daily"})
        for job in jobs:
            args = job["spec"]["jobTemplate"]["spec"]["template"]["spec"]["containers"][0]["args"]
            self.assertEqual(args[args.index("--creation-enabled") + 1], "true")

    def test_unsafe_enablement_is_rejected(self):
        for settings, reason in (
            ({"release": "feature-example"}, "canonical release"),
            ({"namespace": f"{SERVICE}-staging"}, "namespace"),
            ({"environment": "production"}, "prune-only"),
            ({"backups.environment": "unknown"}, "environment"),
            ({"backups.serviceAccountName": ""}, "dedicated service account"),
            ({"backups.serviceAccountName": "cd-serviceaccount"}, "dedicated service account"),
        ):
            with self.subTest(settings=settings):
                result = render(**{"backups.enabled": True, **settings})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(reason, result.stderr)

    def test_enabled_backups_require_stable_cronjob_timezone_support(self):
        result = render(kube_version="1.26.0", **{"backups.enabled": True})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Kubernetes 1.27", result.stderr)
        self.assertEqual(render(kube_version="1.26.0", **{"backups.enabled": False}).returncode, 0)

    def test_string_false_cannot_accidentally_enable_backup_jobs(self):
        result = render(extra_args=["--set-string", "backups.enabled=false"])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("boolean", result.stderr)

    def test_enabled_uat_renders_two_prune_only_jobs(self):
        resources = self.resources(**{"backups.enabled": True, "backups.creationEnabled": False})
        jobs = [resource for resource in resources if resource["kind"] == "CronJob"]
        self.assertEqual(len(jobs), 2)
        for job in jobs:
            spec = job["spec"]
            tier = job["metadata"]["labels"]["backup-tier"]
            self.assertEqual(spec["schedule"], {"hourly": "0 * * * *", "daily": "30 9 * * *"}[tier])
            self.assertEqual(spec["timeZone"], "Etc/UTC")
            self.assertEqual(spec["concurrencyPolicy"], "Forbid")
            job_spec = spec["jobTemplate"]["spec"]
            self.assertEqual(job_spec["activeDeadlineSeconds"], 1900)
            self.assertEqual(job_spec["backoffLimit"], 2)
            pod = job_spec["template"]["spec"]
            self.assertEqual(pod["restartPolicy"], "Never")
            self.assertEqual(pod["serviceAccountName"], "ia-datastore-uat-backups")
            args = pod["containers"][0]["args"]
            self.assertEqual(args[args.index("--tier") + 1], tier)
            self.assertEqual(args[args.index("--creation-enabled") + 1], "false")

    def test_creation_enabled_preserves_application_and_uses_environment_identity(self):
        for environment, env in (("uat", "uat"), ("staging", "staging"), ("production", "prod")):
            with self.subTest(environment=environment):
                baseline = self.resources(environment=environment, **{"backups.enabled": False})
                enabled = self.resources(environment=environment, **{
                    "backups.enabled": True, "backups.creationEnabled": True,
                })
                self.assertEqual(
                    [r for r in baseline if r["kind"] in ("Deployment", "Service", "ServiceAccount")],
                    [r for r in enabled if r["kind"] in ("Deployment", "Service", "ServiceAccount")],
                )
                jobs = [r for r in enabled if r["kind"] == "CronJob"]
                self.assertEqual(len(jobs), 2)
                for job in jobs:
                    pod = job["spec"]["jobTemplate"]["spec"]["template"]["spec"]
                    self.assertEqual(pod["serviceAccountName"], f"ia-datastore-{env}-backups")
                    args = pod["containers"][0]["args"]
                    self.assertEqual(args[args.index("--env") + 1], env)
                    self.assertEqual(args[args.index("--creation-enabled") + 1], "true")

    def test_mounted_script_and_command_pass_only_required_source_identification(self):
        resources = self.resources(**{"backups.enabled": True, "backups.creationEnabled": False})
        config = next(r for r in resources if r["kind"] == "ConfigMap")
        self.assertEqual(config["data"]["rds-backup.py"], (CHART / "files/rds-backup.py").read_text())
        job = next(r for r in resources if r["kind"] == "CronJob")
        pod = job["spec"]["jobTemplate"]["spec"]["template"]["spec"]
        container = pod["containers"][0]
        self.assertNotIn("envFrom", container)
        variables = {entry["name"]: entry for entry in container["env"]}
        secrets = [entry["valueFrom"]["secretKeyRef"] for entry in container["env"]
                   if "secretKeyRef" in entry.get("valueFrom", {})]
        self.assertEqual(secrets, [{"name": "rds-postgresql-instance-output", "key": "rds_instance_address"}])
        self.assertEqual(
            variables["BACKUP_RUN_ID"]["valueFrom"]["fieldRef"]["fieldPath"],
            "metadata.labels['batch.kubernetes.io/job-name']",
        )
        self.assertEqual(container["args"][-2:], ["--run-id", "$(BACKUP_RUN_ID)"])
        self.assertEqual(container["command"][:2], ["python3", "-c"])
        capture = (
            "import json, os\n"
            "os.execv = lambda executable, argv: print(json.dumps([executable, argv]))\n"
            + container["command"][2]
        )
        result = subprocess.run(
            [sys.executable, "-c", capture, *container["args"]],
            env={**os.environ, "RDS_ADDRESS": f"{SERVICE}-uat.example.eu-west-2.rds.amazonaws.com"},
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        executable, argv = json.loads(result.stdout)
        self.assertEqual(executable, sys.executable)
        self.assertEqual(argv[:4], [sys.executable, "/backups/rds-backup.py", "--db-instance-identifier", f"{SERVICE}-uat"])
        self.assertEqual(argv[4:], [
            "--region", "eu-west-2", "--env", "uat", "--tier", "hourly",
            "--creation-enabled", "false", "--run-id", "$(BACKUP_RUN_ID)",
        ])
        script_volume = next(v for v in pod["volumes"] if v["name"] == "script")
        self.assertEqual(script_volume["configMap"]["name"], config["metadata"]["name"])
        self.assertTrue(next(v for v in container["volumeMounts"] if v["name"] == "script")["readOnly"])

    def test_backup_pods_are_restricted_and_not_selected_as_application_pods(self):
        resources = self.resources(**{"backups.enabled": True})
        deployment = next(r for r in resources if r["kind"] == "Deployment")
        for job in (r for r in resources if r["kind"] == "CronJob"):
            template = job["spec"]["jobTemplate"]["spec"]["template"]
            pod = template["spec"]
            labels = template["metadata"]["labels"]
            self.assertFalse(all(
                labels.get(key) == value for key, value in deployment["spec"]["selector"]["matchLabels"].items()
            ))
            for service in (r for r in resources if r["kind"] == "Service"):
                self.assertFalse(all(labels.get(k) == v for k, v in service["spec"]["selector"].items()))
            self.assertFalse(pod["automountServiceAccountToken"])
            self.assertEqual(pod["nodeSelector"]["kubernetes.io/arch"], "amd64")
            self.assertTrue(pod["securityContext"]["runAsNonRoot"])
            self.assertEqual(pod["securityContext"]["runAsUser"], 1000)
            self.assertEqual(pod["securityContext"]["fsGroup"], 1000)
            self.assertEqual(pod["securityContext"]["seccompProfile"]["type"], "RuntimeDefault")
            container = pod["containers"][0]
            self.assertRegex(container["image"], r"^public\.ecr\.aws/lambda/python:3\.13@sha256:[a-f0-9]{64}$")
            self.assertTrue(container["securityContext"]["readOnlyRootFilesystem"])
            self.assertFalse(container["securityContext"]["allowPrivilegeEscalation"])
            self.assertEqual(container["securityContext"]["capabilities"]["drop"], ["ALL"])
            self.assertIn("limits", container["resources"])
            self.assertEqual(next(v for v in pod["volumes"] if v["name"] == "tmp")["emptyDir"]["sizeLimit"], "64Mi")

    def test_backup_network_rules_do_not_widen_application_or_pr_pod_egress(self):
        resources = self.resources(**{"backups.enabled": True})
        job = next(r for r in resources if r["kind"] == "CronJob")
        backup_labels = job["spec"]["jobTemplate"]["spec"]["template"]["metadata"]["labels"]
        application_labels = [
            r["spec"]["template"]["metadata"]["labels"] for r in resources if r["kind"] == "Deployment"
        ]
        policies = [r for r in resources if r["kind"] == "NetworkPolicy"]
        backup_policies = [p for p in policies if selects(p["spec"]["podSelector"], backup_labels)]
        self.assertEqual(len(backup_policies), 1)
        policy = backup_policies[0]["spec"]
        self.assertEqual(set(policy["policyTypes"]), {"Ingress", "Egress"})
        self.assertEqual(policy["ingress"], [])
        ports = {(port["port"], port["protocol"]) for rule in policy["egress"] for port in rule["ports"]}
        self.assertEqual(ports, {(53, "UDP"), (53, "TCP"), (443, "TCP")})
        for labels in application_labels:
            self.assertFalse(selects(policy["podSelector"], labels))
        for release_resources in (resources, self.resources(release="feature-example", **{"backups.enabled": False})):
            for existing in (r for r in release_resources if r["kind"] == "NetworkPolicy"
                             and r["metadata"]["labels"].get("app.kubernetes.io/component") not in ("backup", "metabase")):
                selector = existing["spec"]["podSelector"]
                self.assertFalse(selects(selector, backup_labels))
                for labels in [*application_labels, {}]:
                    self.assertTrue(selects(selector, labels))


class DeploymentScriptTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.work = Path(directory.name)
        self.calls = self.work / "calls.jsonl"
        for name in ("tr", "sed", "cut"):
            (self.work / name).symlink_to(shutil.which(name))
        helm = self.work / "helm"
        helm.write_text(f"#!{sys.executable}\n" + """
import json
import os
from pathlib import Path
import sys
calls = Path(os.environ["HELM_CALLS"])
previous = [json.loads(line) for line in calls.read_text().splitlines()] if calls.exists() else []
with calls.open("a") as output:
    output.write(json.dumps(sys.argv[1:]) + "\\n")
if os.environ.get("FAIL_FIRST_UPGRADE") == "1" and sys.argv[1] == "upgrade":
    if not any(call[0] == "upgrade" for call in previous):
        sys.exit(1)
""")
        helm.chmod(0o755)

    def deploy(self, branch, fail_first=False):
        self.calls.unlink(missing_ok=True)
        result = subprocess.run(
            ["/bin/bash", str(ROOT / "deploy/scripts/deploy.sh"), "uat"],
            cwd=ROOT, capture_output=True, text=True, timeout=30,
            env={
                "PATH": str(self.work), "HOME": str(self.work),
                "HELM_CALLS": str(self.calls),
                "GITHUB_REF_NAME": branch, "GITHUB_HEAD_REF": "feature/backups",
                "GITHUB_SHA": "offline-test", "K8S_NAMESPACE": f"{SERVICE}-uat",
                "FAIL_FIRST_UPGRADE": "1" if fail_first else "0",
            },
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        return [json.loads(line) for line in self.calls.read_text().splitlines()]

    def test_branch_deploy_forces_backups_off_including_rollback_retry(self):
        for fail_first in (False, True):
            with self.subTest(fail_first=fail_first):
                calls = self.deploy("feature/backups", fail_first)
                upgrades = [call for call in calls if call[0] == "upgrade"]
                self.assertEqual(len(upgrades), 2 if fail_first else 1)
                for args in upgrades:
                    self.assertIn("backups.enabled=false", args)

    def test_main_deploy_leaves_backup_enablement_to_canonical_values(self):
        calls = self.deploy("main")
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0][:2], ["upgrade", SERVICE])
        self.assertFalse(any(arg.startswith("backups.") for arg in calls[0]))


class MetabaseGoogleAuthChartTests(unittest.TestCase):
    def resources(self, environment, **settings):
        result = render(environment=environment, **settings)
        self.assertEqual(result.returncode, 0, result.stderr)
        return [resource for resource in yaml.safe_load_all(result.stdout) if resource]

    def test_google_sign_in_remains_disabled_outside_uat(self):
        for environment in ("staging", "production"):
            with self.subTest(environment=environment):
                resources = self.resources(environment)
                self.assertFalse(any(r["metadata"]["name"].endswith("-metabase-google-auth") for r in resources))
                deployment = next(r for r in resources if r["kind"] == "Deployment"
                                  and r["metadata"].get("labels", {}).get("app.kubernetes.io/component") == "metabase")
                names = {entry["name"] for entry in deployment["spec"]["template"]["spec"]["containers"][0]["env"]}
                self.assertNotIn("MB_GOOGLE_AUTH_CLIENT_ID", names)

    def test_enabled_sign_in_uses_secret_and_limits_egress_to_metabase(self):
        resources = self.resources("uat")
        deployment = next(r for r in resources if r["kind"] == "Deployment"
                          and r["metadata"].get("labels", {}).get("app.kubernetes.io/component") == "metabase")
        pod = deployment["spec"]["template"]
        env = {entry["name"]: entry for entry in pod["spec"]["containers"][0]["env"]}
        self.assertEqual(env["MB_GOOGLE_AUTH_CLIENT_ID"]["valueFrom"]["secretKeyRef"], {
            "name": "laa-info-and-advice-datastore-metabase-google", "key": "client_id",
        })
        self.assertEqual(env["MB_GOOGLE_AUTH_ENABLED"]["value"], "true")
        self.assertNotIn("MB_GOOGLE_AUTH_AUTO_CREATE_ACCOUNTS_DOMAIN", env)
        policy = next(r for r in resources if r["kind"] == "NetworkPolicy"
                      and r["metadata"]["name"].endswith("-metabase-google-auth"))["spec"]
        self.assertTrue(selects(policy["podSelector"], pod["metadata"]["labels"]))
        for other in (r for r in resources if r["kind"] == "Deployment" and r is not deployment):
            self.assertFalse(selects(policy["podSelector"], other["spec"]["template"]["metadata"]["labels"]))
        self.assertEqual(policy["policyTypes"], ["Egress"])
        self.assertEqual(policy["egress"], [{"ports": [{"protocol": "TCP", "port": 443}]}])


if __name__ == "__main__":
    unittest.main()
