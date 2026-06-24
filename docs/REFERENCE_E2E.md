# Reference API and E2E Validation Harness

Phase 11 adds a **golden test case** for agentic-api-test-designer: a controlled sample payments API, a checked-in OpenAPI spec, and a sample target automation project. Use this to prove the full agent workflow still works after merges.

## Purpose

Every PR can answer:

- Can the tool still parse a realistic complex API contract?
- Can it still generate deterministic test cases and automation files?
- Can it still preview/write files safely into an allowed project layout?
- Can it still preview Maven execution and run lightweight tests?
- Does the reference API itself behave deterministically for all documented status codes?

No OpenAI, Jira, GitHub CLI, or production secrets are required for CI Level 1.

## Repository layout

```
samples/
  reference-api/
    pom.xml
    openapi/reference-api.yaml      # canonical OpenAPI for file mode
    src/main/java/...               # Spring Boot payments API (port 9090)
    src/test/java/...               # status-code coverage tests

  reference-automation-project/
    pom.xml
    src/test/resources/features/
    src/test/resources/templates/
    src/test/resources/testdata/qa/
    src/test/resources/schemas/
    src/test/java/steps/
    src/test/java/api/
    src/test/java/validators/

scripts/
  run-reference-e2e.sh              # local Level 2 script (Linux/macOS)
  run-reference-e2e.ps1             # local Level 2 script (Windows)
```

## Reference API

### Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/payments` | Create payment (complex headers + nested body) |
| `GET` | `/api/payments/{paymentId}` | Fetch payment |
| `PUT` | `/api/payments/{paymentId}/status` | Update payment status |

### `POST /api/payments` requirements

**Headers**

| Header | Example | Notes |
|--------|---------|-------|
| `Authorization` | `Bearer valid-token` | `401` when missing/invalid |
| `X-Correlation-Id` | `corr-123` | required |
| `X-Client-Id` | `web-portal` | `403` for `blocked-portal` / `forbidden-client`; `500` for `trigger-500` |
| `Content-Type` | `application/json` | required |

**Body (required fields)**

- `accountId`, `amount`, `currency`
- `paymentMethod.type`, `paymentMethod.token`
- `billingAddress.line1`, `billingAddress.city`, `billingAddress.state`, `billingAddress.zip`
- `metadata.invoiceId`

**Status codes**

| Code | Trigger |
|------|---------|
| `201` | Valid request |
| `400` | Missing/invalid required fields or headers |
| `401` | Missing/invalid bearer token |
| `403` | Forbidden client id |
| `404` | Unknown `paymentId` on GET/PUT |
| `409` | Duplicate `metadata.invoiceId` |
| `422` | Unsupported currency (e.g. `JPY`) or invalid payment method type |
| `500` | `X-Client-Id: trigger-500` |

### Start the reference API

```bash
cd samples/reference-api
mvn spring-boot:run
```

Default URL: `http://localhost:9090`

OpenAPI options:

- **URL mode:** `http://localhost:9090/v3/api-docs` (springdoc)
- **Static YAML:** `http://localhost:9090/openapi/reference-api.yaml`
- **File mode:** `samples/reference-api/openapi/reference-api.yaml`

## Sample target automation project

Path:

```
samples/reference-automation-project
```

This project mirrors the folders the agent is allowed to write:

- `src/test/resources/features/`
- `src/test/resources/templates/`
- `src/test/resources/testdata/qa/`
- `src/test/resources/schemas/`
- `src/test/java/steps/`
- `src/test/java/api/`
- `src/test/java/validators/`

Use this path as **Project Path** in the UI when testing file write and Maven execution.

Smoke test:

```bash
mvn -f samples/reference-automation-project/pom.xml test
```

## Using the harness in the agent UI

1. Start the reference API (`9090`) and agent backend (`8080`).
2. In the form:
   - **Jira Story Key:** `PAY-REF-001`
   - **Swagger URL:** `http://localhost:9090/v3/api-docs` **or** paste/load `samples/reference-api/openapi/reference-api.yaml`
   - **Base API URL:** `http://localhost:9090`
   - **Endpoint Path:** `/api/payments`
   - **HTTP Method:** `POST`
   - **Project Path:** absolute path to `samples/reference-automation-project`
3. Run the normal flow:
   - Extract contract / generate test matrix
   - Generate automation package (deterministic fallback works without OpenAI)
   - Preview + write files
   - Preview + run Maven tests
4. Verify run history records appear when operations complete.

### Expected generated coverage themes

After contract extraction and matrix generation you should see cases covering:

- required headers and body fields
- nested object validation (`paymentMethod`, `billingAddress`, `metadata`)
- positive path (`201`)
- negative paths (`400`, `401`, `403`, `409`, `422`)
- schema/type checks

After file write, confirm files land only under allowed prefixes (for example `src/test/resources/features/payment/create_payment.feature`).

After Maven preview/execution, confirm a `mvn ...` command is built and report paths are detected when present.

## Validation levels

### Level 1 — CI default (no secrets)

Runs on every PR:

- frontend build + lint
- backend unit/integration tests (includes `ReferenceHarnessIntegrationTest`)
- reference API tests (`mvn -f samples/reference-api/pom.xml test`)
- reference automation project smoke test

### Level 2 — local full E2E script

```bash
./scripts/run-reference-e2e.sh
```

Windows:

```powershell
./scripts/run-reference-e2e.ps1
```

Starts reference API + backend, exercises contract extraction, matrix generation, file-write preview, Maven preview, and runs the automation project smoke test.

**Note:** Level 2 scripts call `preview-file-write` only — they do not write into `samples/reference-automation-project`. That keeps the sample project clean on repeated local runs. Actual write behavior is covered in CI by `ReferenceHarnessIntegrationTest` (writes into a temp copy). A future optional flag may enable real writes, for example:

```bash
WRITE_REFERENCE_FILES=true ./scripts/run-reference-e2e.sh
```

### Level 3 — manual secured full flow (demo/release)

Requires optional secrets and integrations:

- `security.enabled=true` + `X-Agentic-Token` + `confirmation: I_UNDERSTAND`
- OpenAI enabled
- Jira enabled
- GitHub CLI for PR creation

Run these manually; they are intentionally excluded from CI.

## Backend harness test

`ReferenceHarnessIntegrationTest` (backend) loads `samples/reference-api/openapi/reference-api.yaml` and validates:

- OpenAPI parser extraction for `createPayment`
- deterministic test matrix generation
- automation package generation (deterministic fallback)
- file write preview/write into a temp copy of the automation scaffold
- Maven execution preview

Run only the harness test:

```bash
mvn -f backend/pom.xml test -Dtest=ReferenceHarnessIntegrationTest
```
