#!/bin/bash
# Runs ClaimsApiGatewayUatIntegrationTest inside a throwaway pod in the UAT namespace, using the
# in-cluster claims API URL and the same API key secret as the deployed app. Proves our client
# works against the real claims API from where it actually runs.
set -euo pipefail

NAMESPACE=${K8S_NAMESPACE:?K8S_NAMESPACE must be set}
BASE_URL=${CLAIMS_API_URL:-http://main-data-claims-api.laa-data-claims-api-uat.svc.cluster.local:8080}
SECRET_NAME=${CLAIMS_API_SECRET:-laa-info-and-advice-datastore-claims-api}
IMAGE=${TEST_RUNNER_IMAGE:-eclipse-temurin:25-jre}
POD_NAME="claims-api-test-${GITHUB_RUN_ID:-$$}"
TEST_CLASS=uk.gov.justice.laa.ia.datastore.gateway.ClaimsApiGatewayUatIntegrationTest
BUNDLE_DIR=info-and-advice-datastore-service/build/claims-api-pod-test

./gradlew :info-and-advice-datastore-service:claimsApiPodTestBundle -q

OVERRIDES=$(cat <<EOF
{
  "spec": {
    "securityContext": {"runAsNonRoot": true, "runAsUser": 1000, "seccompProfile": {"type": "RuntimeDefault"}},
    "containers": [{
      "name": "${POD_NAME}",
      "image": "${IMAGE}",
      "command": ["sleep", "900"],
      "env": [
        {"name": "CLAIMS_API_URL", "value": "${BASE_URL}"},
        {"name": "CLAIMS_API_KEY", "valueFrom": {"secretKeyRef": {"name": "${SECRET_NAME}", "key": "CLAIMS_API_KEY"}}}
      ],
      "resources": {"requests": {"cpu": "100m", "memory": "512Mi"}, "limits": {"memory": "1Gi"}},
      "securityContext": {"allowPrivilegeEscalation": false, "capabilities": {"drop": ["ALL"]}}
    }]
  }
}
EOF
)

trap 'kubectl --namespace "$NAMESPACE" delete pod "$POD_NAME" --ignore-not-found --wait=false' EXIT

kubectl --namespace "$NAMESPACE" run "$POD_NAME" --restart=Never --image="$IMAGE" --overrides="$OVERRIDES"
kubectl --namespace "$NAMESPACE" wait --for=condition=Ready "pod/$POD_NAME" --timeout=180s

COPYFILE_DISABLE=1 tar --no-xattrs -czf - -C "$BUNDLE_DIR" . |
  kubectl --namespace "$NAMESPACE" exec -i "$POD_NAME" -- sh -c 'mkdir -p /tmp/bundle && tar xzf - -C /tmp/bundle'

kubectl --namespace "$NAMESPACE" exec "$POD_NAME" -- sh -c "cd /tmp/bundle && java -cp 'classes:lib/*:launcher/*' \
  org.junit.platform.console.ConsoleLauncher execute --select-class ${TEST_CLASS} \
  --details=tree --disable-banner --fail-if-no-tests"
