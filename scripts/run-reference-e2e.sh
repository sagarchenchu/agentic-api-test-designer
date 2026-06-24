#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REFERENCE_API_DIR="${ROOT_DIR}/samples/reference-api"
AUTOMATION_DIR="${ROOT_DIR}/samples/reference-automation-project"
BACKEND_DIR="${ROOT_DIR}/backend"
OPENAPI_FILE="${REFERENCE_API_DIR}/openapi/reference-api.yaml"

REFERENCE_API_PORT="${REFERENCE_API_PORT:-9090}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
REFERENCE_API_URL="http://localhost:${REFERENCE_API_PORT}"
BACKEND_URL="http://localhost:${BACKEND_PORT}"

REFERENCE_API_PID=""
BACKEND_PID=""

cleanup() {
  if [[ -n "${BACKEND_PID}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
    kill "${BACKEND_PID}" || true
  fi
  if [[ -n "${REFERENCE_API_PID}" ]] && kill -0 "${REFERENCE_API_PID}" 2>/dev/null; then
    kill "${REFERENCE_API_PID}" || true
  fi
}
trap cleanup EXIT

wait_for_url() {
  local url="$1"
  local label="$2"
  for _ in $(seq 1 90); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "${label} is ready at ${url}"
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for ${label} at ${url}" >&2
  return 1
}

agent_request_json() {
  python3 - "$OPENAPI_FILE" "$REFERENCE_API_URL" <<'PY'
import json
import pathlib
import sys

openapi = pathlib.Path(sys.argv[1]).read_text()
print(json.dumps({
    "jiraStoryKey": "PAY-REF-001",
    "swaggerJson": openapi,
    "baseApiUrl": sys.argv[2],
    "endpointPath": "/api/payments",
    "httpMethod": "POST",
    "credentialRef": "reference_api_user",
}))
PY
}

echo "==> Level 1 checks (no secrets)"
mvn -f "${REFERENCE_API_DIR}/pom.xml" -q test
mvn -f "${AUTOMATION_DIR}/pom.xml" -q test
mvn -f "${BACKEND_DIR}/pom.xml" -q test -Dtest=ReferenceHarnessIntegrationTest

echo "==> Starting reference API on port ${REFERENCE_API_PORT}"
mvn -f "${REFERENCE_API_DIR}/pom.xml" -q spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${REFERENCE_API_PORT}" &
REFERENCE_API_PID=$!
wait_for_url "${REFERENCE_API_URL}/openapi/reference-api.yaml" "Reference API"

echo "==> Starting agent backend on port ${BACKEND_PORT}"
mvn -f "${BACKEND_DIR}/pom.xml" -q spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT}" &
BACKEND_PID=$!
wait_for_url "${BACKEND_URL}/api/agent/health" "Agent backend"

echo "==> Extract contract (OpenAPI file mode)"
curl -fsS -X POST "${BACKEND_URL}/api/agent/extract-contract" \
  -H 'Content-Type: application/json' \
  -d "$(agent_request_json)" \
  | tee /tmp/reference-extract-contract.json >/dev/null

echo "==> Generate deterministic test matrix"
curl -fsS -X POST "${BACKEND_URL}/api/agent/generate-test-matrix" \
  -H 'Content-Type: application/json' \
  -d "$(agent_request_json)" \
  | tee /tmp/reference-test-matrix.json >/dev/null

echo "==> Preview file write into sample automation project"
python3 - "$AUTOMATION_DIR" <<'PY' | curl -fsS -X POST "${BACKEND_URL}/api/agent/preview-file-write" \
  -H 'Content-Type: application/json' -d @- | tee /tmp/reference-file-write-preview.json >/dev/null
import json
import pathlib
import sys

root = sys.argv[1]
print(json.dumps({
    "projectPath": root,
    "overwriteExisting": True,
    "createBackup": False,
    "files": [{
        "path": "src/test/resources/features/payment/reference_e2e.feature",
        "content": "Feature: Reference E2E\n  Scenario: Smoke\n    Given reference harness is running\n",
        "type": "gherkin",
    }],
}))
PY

echo "==> Preview Maven test execution"
python3 - "$AUTOMATION_DIR" <<'PY' | curl -fsS -X POST "${BACKEND_URL}/api/agent/preview-test-execution" \
  -H 'Content-Type: application/json' -d @- | tee /tmp/reference-test-execution-preview.json >/dev/null
import json
import sys

print(json.dumps({
    "projectPath": sys.argv[1],
    "commandType": "MAVEN",
    "mavenCommand": "mvn test",
    "testTag": "@PAY-REF-001",
    "profile": "qa",
    "timeoutSeconds": 300,
    "environment": "QA",
    "dryRun": True,
}))
PY

echo "==> Run lightweight Maven smoke test"
mvn -f "${AUTOMATION_DIR}/pom.xml" -q test

cat <<EOF

Reference E2E script completed successfully.

Swagger/OpenAPI options for the UI:
  URL mode:  ${REFERENCE_API_URL}/v3/api-docs
  File mode: ${OPENAPI_FILE}

Target automation project:
  ${AUTOMATION_DIR}

Optional Level 3 (manual, secrets required):
  OpenAI, Jira, GitHub CLI, security.enabled=true

Artifacts:
  /tmp/reference-extract-contract.json
  /tmp/reference-test-matrix.json
  /tmp/reference-file-write-preview.json
  /tmp/reference-test-execution-preview.json
EOF
