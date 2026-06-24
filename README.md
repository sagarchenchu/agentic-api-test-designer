# Agentic API Test Designer

A web UI for generating API test cases and BDD automation from Jira stories and Swagger/OpenAPI specifications.

## Purpose

Agentic API Test Designer helps QA engineers and automation developers turn Jira requirements and API contracts into structured test coverage, Cucumber feature files, and automation scaffolding — then execute tests and review results in one place.

## Current phase: Reference API and E2E validation harness (Phase 11)

This repository includes:

- **React frontend** — dashboard UI with form validation, tabs, agent timeline, and persisted run history
- **Spring Boot backend** — REST API under `/api/agent` with H2-backed run history, optional API token auth, and secret masking
- **Reference samples** — controlled payments API, OpenAPI spec, and target automation project for golden-path validation

Phase 11 adds a **reference API + E2E validation harness** so every merge can prove contract parsing, deterministic generation, safe file writes, and Maven preview still work without OpenAI/Jira/Git secrets. See [docs/REFERENCE_E2E.md](docs/REFERENCE_E2E.md).

Phase 10 added **persistent run history**, **optional local API token auth** (`security.enabled=false` by default), **secret masking**, **project path policy**, **risky-operation confirmation** when security is enabled, consistent `ApiErrorResponse` errors, and Docker deployment docs.

### OpenAI setup (optional)

```bash
export OPENAI_API_KEY=your-key-here
```

In `backend/src/main/resources/application.properties` or environment:

```properties
openai.enabled=true
openai.model=gpt-4.1-mini
```

By default `openai.enabled=false` and no API key is required.

### Jira setup (optional)

Jira integration is **disabled by default**. Enable it only when you want live Jira fetch/comment features:

```bash
export JIRA_ENABLED=true
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_EMAIL=qa-automation@company.com
export JIRA_API_TOKEN=your-atlassian-api-token
```

In `backend/src/main/resources/application.properties` or environment:

```properties
jira.enabled=true
jira.base-url=${JIRA_BASE_URL}
jira.email=${JIRA_EMAIL}
jira.api-token=${JIRA_API_TOKEN}
```

The API token is never logged or returned in API responses. Do not send tokens from the frontend except via the `X-Agentic-Token` header when configured.

### Security setup (optional)

Local API token auth is **disabled by default**:

```bash
export SECURITY_ENABLED=true
export AGENTIC_API_TOKEN=your-local-api-token
```

Frontend (optional):

```bash
export VITE_AGENTIC_API_TOKEN=your-local-api-token
```

When `security.enabled=true`, requests must include `X-Agentic-Token`. Risky operations also require `"confirmation": "I_UNDERSTAND"` in the request body:

- `write-generated-files`
- `run-test-execution`
- `create-git-pr`
- `jira/post-summary`
- `jira/link-pr`

### Project path policy (optional)

```properties
agent.allow-any-local-path=true
agent.allowed-project-roots=C:/repos,/Users/me/repos
```

When `agent.allow-any-local-path=false`, `projectPath` must be under one of the configured roots.

### Deployment modes

- **Local developer:** `npm run dev` + `cd backend && mvn spring-boot:run` (security/Jira/OpenAI disabled by default)
- **Docker:** copy `.env.example` to `.env`, then `docker compose up --build`
- **Production-like:** enable `SECURITY_ENABLED`, restrict `AGENT_ALLOWED_PROJECT_ROOTS`, configure Jira/OpenAI server-side only

See `.env.example` for all environment variables.

### Swagger parser limitations

- OpenAPI **3.x only** for now
- **Local `$ref`** resolution under `#/components/schemas/` only
- Focus on **`application/json`** request/response bodies
- Path matching supports exact paths and simple templates like `/api/orders/{orderId}`
- `swaggerUrl` is fetched at request time (no caching yet)

## Features

- Single-page dashboard with agent input form and tabbed workspace
- **API Contract** tab — operation summary, headers, parameters, request body fields, responses, warnings
- **Extract API Contract** — parses Swagger/OpenAPI JSON or URL for the selected endpoint
- Dynamic test matrix generation from extracted contract fields
- **Test Generation Mode** — Deterministic Swagger Rules or AI-assisted
- **Generate Test Matrix** calls `/api/agent/generate-test-matrix` or `/api/agent/generate-ai-test-matrix` based on mode
- **Generate Automation Package** — AI-assisted BDD + automation scaffold files in one call
- **Generate BDD from Test Matrix** and **Generate Automation Files** buttons in workspace tabs
- **File Write Preview** tab with per-file action/status/diff and write summary
- **Preview Write to Project** and **Write Files to Project** buttons in Generated Files tab
- Overwrite existing files and create backup checkboxes in the left panel
- **Test Execution** tab with Maven command preview/run, summary, report paths, failed scenarios, and log tail
- **Preview Test Execution** and **Run Maven Tests** buttons with Test Tag, Maven Profile, and Timeout controls
- **Git / PR** tab with branch/commit/PR preview, command log, commit SHA, PR URL, and warnings/errors
- **Preview Git PR** and **Create Pull Request** buttons with Base Branch, New Branch, Commit Message, PR Title, and Remote controls
- **Fetch Jira Story**, **Post Jira Summary**, and **Link PR to Jira** buttons near the Jira Story Key field
- Jira config status shown in the left panel (enabled/configured only — no secrets)
- Fetched Jira story populates story text, acceptance criteria, and requirement summary fields
- **Run History** tab with persisted runs, open/detail, artifacts, and delete actions
- Optional `VITE_AGENTIC_API_TOKEN` sends `X-Agentic-Token` when API security is enabled
- AI matrix and automation generation show source, warnings, and assumptions when available
- Inline form validation (frontend and backend)
- Agent timeline with execution-mode-aware step control
- Dynamic BDD preview based on Jira key, HTTP method, and endpoint
- Copy-to-clipboard and download for BDD feature files
- Light/dark theme toggle
- Responsive layout for desktop and mobile

## How to run

### Frontend

```bash
npm install
npm run dev
```

Open the URL shown by Vite (default `http://localhost:5173`).

```bash
npm run build   # production build
npm run lint    # oxlint
npm run preview # preview production build
```

### Backend

Requires Java 17+ and Maven.

```bash
cd backend
mvn spring-boot:run
```

Backend listens on `http://localhost:8080`.

### Run both together

1. Start backend: `cd backend && mvn spring-boot:run`
2. In another terminal: `npm run dev`
3. Open `http://localhost:5173` — footer shows backend connection status

Optional: set `VITE_API_BASE_URL` to point at a different backend URL.

## Example API calls

### Extract contract

```bash
curl -X POST http://localhost:8080/api/agent/extract-contract \
  -H "Content-Type: application/json" \
  -d '{
    "jiraStoryKey": "PAY-1234",
    "swaggerUrl": "https://qa-api.company.com/swagger.json",
    "baseApiUrl": "https://qa-api.company.com",
    "endpointPath": "/api/payments",
    "httpMethod": "POST"
  }'
```

### Generate AI test matrix

```bash
curl -X POST http://localhost:8080/api/agent/generate-ai-test-matrix \
  -H "Content-Type: application/json" \
  -d '{
    "jiraStoryKey": "PAY-1234",
    "jiraStoryText": "Amount should not exceed daily limit of 5000",
    "swaggerJson": "{ ... }",
    "baseApiUrl": "https://qa-api.company.com",
    "endpointPath": "/api/payments",
    "httpMethod": "POST",
    "frameworkType": "restassured-cucumber-serenity"
  }'
```

### Preview file write

```bash
curl -X POST http://localhost:8080/api/agent/preview-file-write \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/repos/api-automation-framework",
    "overwriteExisting": false,
    "createBackup": true,
    "files": [
      {
        "path": "src/test/resources/features/payment/create_payment.feature",
        "content": "Feature: Create payment API",
        "language": "gherkin"
      }
    ]
  }'
```

### Write generated files

```bash
curl -X POST http://localhost:8080/api/agent/write-generated-files \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/repos/api-automation-framework",
    "overwriteExisting": true,
    "createBackup": true,
    "files": [
      {
        "path": "src/test/resources/features/payment/create_payment.feature",
        "content": "Feature: Create payment API",
        "language": "gherkin"
      }
    ]
  }'
```

### Preview test execution

```bash
curl -X POST http://localhost:8080/api/agent/preview-test-execution \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/repos/api-automation-framework",
    "mavenCommand": "mvn clean verify",
    "testTag": "@PAY-1234",
    "profile": "qa",
    "timeoutSeconds": 300,
    "environment": "QA"
  }'
```

### Run test execution

```bash
curl -X POST http://localhost:8080/api/agent/run-test-execution \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/repos/api-automation-framework",
    "mavenCommand": "mvn clean verify",
    "testTag": "@PAY-1234",
    "profile": "qa",
    "timeoutSeconds": 300,
    "environment": "QA"
  }'
```

### Preview Git PR

```bash
curl -X POST http://localhost:8080/api/agent/preview-git-pr \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/repos/api-automation-framework",
    "jiraStoryKey": "PAY-1234",
    "baseBranch": "main",
    "newBranchName": "feature/PAY-1234-api-tests",
    "commitMessage": "Add API automation tests for PAY-1234",
    "prTitle": "PAY-1234 Add API automation tests",
    "prBody": "Generated API tests from Jira + Swagger.",
    "remoteName": "origin",
    "filesToCommit": [
      "src/test/resources/features/payment/create_payment.feature"
    ],
    "dryRun": false
  }'
```

### Create Git PR

```bash
curl -X POST http://localhost:8080/api/agent/create-git-pr \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/repos/api-automation-framework",
    "jiraStoryKey": "PAY-1234",
    "baseBranch": "main",
    "newBranchName": "feature/PAY-1234-api-tests",
    "commitMessage": "Add API automation tests for PAY-1234",
    "prTitle": "PAY-1234 Add API automation tests",
    "prBody": "Generated API tests from Jira + Swagger.",
    "remoteName": "origin",
    "filesToCommit": [
      "src/test/resources/features/payment/create_payment.feature"
    ],
    "dryRun": false
  }'
```

### Generate AI automation package

```bash
curl -X POST http://localhost:8080/api/agent/generate-ai-automation-package \
  -H "Content-Type: application/json" \
  -d '{
    "agentRequest": {
      "jiraStoryKey": "PAY-1234",
      "jiraStoryText": "Create payment with valid account",
      "swaggerJson": "{ ... }",
      "baseApiUrl": "https://qa-api.company.com",
      "endpointPath": "/api/payments",
      "httpMethod": "POST",
      "frameworkType": "restassured-cucumber-serenity",
      "testGenerationMode": "ai-assisted"
    },
    "testCases": [
      {
        "id": "TC_001",
        "scenarioName": "Create payment with valid request",
        "type": "Positive",
        "inputVariation": "Valid body",
        "expectedStatus": "201",
        "expectedValidation": "Payment is created",
        "priority": "High",
        "automationStatus": "Ready",
        "source": "JIRA+SWAGGER"
      }
    ]
  }'
```

### Fetch Jira story

```bash
curl -X POST http://localhost:8080/api/agent/jira/fetch-story \
  -H "Content-Type: application/json" \
  -d '{ "jiraStoryKey": "PAY-1234" }'
```

### Post Jira summary

```bash
curl -X POST http://localhost:8080/api/agent/jira/post-summary \
  -H "Content-Type: application/json" \
  -d '{
    "jiraStoryKey": "PAY-1234",
    "testCaseCount": 12,
    "bddGenerated": true,
    "filesWritten": 7,
    "executionStatus": "PASSED",
    "passed": 12,
    "failed": 0,
    "prUrl": "https://github.com/org/repo/pull/99",
    "serenityReportPath": "target/site/serenity/index.html"
  }'
```

### Link PR to Jira

```bash
curl -X POST http://localhost:8080/api/agent/jira/link-pr \
  -H "Content-Type: application/json" \
  -d '{
    "jiraStoryKey": "PAY-1234",
    "prUrl": "https://github.com/org/repo/pull/99"
  }'
```

### Run agent

```bash
curl -X POST http://localhost:8080/api/agent/run \
  -H "Content-Type: application/json" \
  -d '{
    "jiraStoryKey": "PAY-1234",
    "swaggerUrl": "https://qa-api.company.com/swagger.json",
    "baseApiUrl": "https://qa-api.company.com",
    "endpointPath": "/api/payments",
    "httpMethod": "POST",
    "headers": [
      { "key": "Authorization", "value": "Bearer {{token}}" }
    ],
    "executionMode": "generate-execute"
  }'
```

## Backend endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/agent/health` | Health check |
| POST | `/api/agent/extract-contract` | Parse Swagger and extract API contract |
| POST | `/api/agent/generate-test-matrix` | Deterministic test cases from contract (fallback to mock) |
| POST | `/api/agent/generate-ai-test-matrix` | AI-assisted test cases from Jira + contract |
| POST | `/api/agent/generate-ai-bdd` | AI-assisted BDD feature from Jira + contract + test cases |
| POST | `/api/agent/generate-ai-files` | AI-assisted automation scaffold files |
| POST | `/api/agent/generate-ai-automation-package` | BDD + automation files together |
| POST | `/api/agent/preview-file-write` | Preview create/update/skip/blocked actions with diff |
| POST | `/api/agent/write-generated-files` | Safely write generated files to project path |
| POST | `/api/agent/preview-test-execution` | Validate project path and preview Maven command/report paths |
| POST | `/api/agent/run-test-execution` | Run Maven safely, capture logs, parse reports |
| GET | `/api/agent/test-executions/{executionId}` | Retrieve a stored Maven execution result |
| POST | `/api/agent/preview-git-pr` | Validate git repo, branch/files, and preview PR commands (no git mutations) |
| POST | `/api/agent/create-git-pr` | Checkout base, commit allowed files, push branch, create GitHub PR via `gh` |
| GET | `/api/agent/git-pr/{operationId}` | Retrieve a stored Git PR operation result |
| GET | `/api/agent/jira/config/status` | Jira enabled/configured status (no secrets) |
| POST | `/api/agent/jira/fetch-story` | Fetch Jira issue by key and extract story details |
| POST | `/api/agent/jira/post-summary` | Post generated test/execution summary comment to Jira |
| POST | `/api/agent/jira/link-pr` | Post pull request link comment to Jira |
| GET | `/api/agent/history/runs` | List persisted run history summaries |
| GET | `/api/agent/history/runs/{runId}` | Get run history detail with artifacts and external operations |
| DELETE | `/api/agent/history/runs/{runId}` | Delete a persisted run and related records |
| GET | `/api/agent/history/runs/{runId}/artifacts` | List artifacts for a run |
| POST | `/api/agent/generate-bdd` | Returns dynamic BDD feature (legacy mock) |
| POST | `/api/agent/generate-files` | Returns file tree + BDD metadata |
| POST | `/api/agent/run` | Full agent run (mock) |
| GET | `/api/agent/runs/{runId}` | Retrieve a stored run |

## Design approach

**Swagger parser first, AI second.**

1. Phase 3 extracts a structured `ApiContractDto` from Swagger/OpenAPI
2. Phase 4 sends Jira story + contract + framework to OpenAI for smarter test cases
3. Phase 5 sends Jira story + contract + test cases to OpenAI for BDD and automation scaffold files
4. Phase 6 previews and writes generated files safely into allowed test folders under `projectPath`
5. Phase 7 runs Maven via `ProcessBuilder` argument lists (no shell), parses Surefire/Failsafe/Cucumber/Serenity reports, and stores execution results
6. Phase 8 runs git/gh via `ProcessBuilder` argument lists, stages only allowed generated test files, blocks unrelated working tree changes, and stores PR operation results
7. Phase 9 fetches Jira stories via REST API, extracts acceptance criteria from ADF/plain text, and posts ADF summary/PR comments back to Jira
8. Phase 10 persists run history in H2, masks secrets in errors/logs/history, supports optional API token auth, and centralizes project path policy
9. If AI is disabled or fails, deterministic BDD and scaffold files are used automatically

### File write safety guardrails

- `projectPath` must exist and be a directory
- Missing automation markers (`pom.xml`, `build.gradle`, `settings.gradle`, `src/test`) produce a warning
- File paths must be relative and cannot contain `..`, absolute prefixes, or drive letters
- Writes are blocked outside allowed folders:
  - `src/test/resources/features/`
  - `src/test/resources/templates/`
  - `src/test/resources/testdata/`
  - `src/test/resources/schemas/`
  - `src/test/java/steps/`
  - `src/test/java/api/`
  - `src/test/java/validators/`
- Sensitive paths are blocked (`.git/`, `.github/`, `.env`, `*.pem`, `*.key`, etc.)
- `overwriteExisting=false` skips existing files; `createBackup=true` writes `<file>.bak.<timestamp>` before updates
- `writeMode` on `FileWriteRequest` is informational only — preview and write endpoints enforce the correct mode server-side
- No git commits or writes outside `projectPath`

### Maven execution safety guardrails

- `projectPath` must exist, be a directory, contain `pom.xml`, and must not be a filesystem root
- Maven commands are built as `ProcessBuilder` argument lists — never via `cmd /c`, `sh -c`, or shell strings
- Allowed goals: `mvn test`, `mvn verify`, `mvn clean test`, `mvn clean verify`
- Safe optional args: `-Dcucumber.filter.tags=<tag>`, `-P<profile>`, `-Denv=<environment>`
- Blocks injection patterns such as `;`, `&&`, `|`, `` ` ``, `$`, `../`, `powershell`, `cmd`, `bash`, `curl`, `rm`, etc.
- `timeoutSeconds` must be between 30 and 900; timed-out processes are killed
- Report parsing reads Surefire/Failsafe XML, Cucumber JSON failures, and detects Serenity report paths

### Git / PR safety guardrails

- `projectPath` must exist, be a directory, not be a filesystem root, and pass `git rev-parse --show-toplevel`
- Branch names allow letters, numbers, `.`, `/`, `-`, `_` only; block `..`, leading/trailing `/`, `//`, and shell metacharacters
- `remoteName` must be a configured remote name (`origin`, `upstream`, etc.) — raw URLs are rejected in Phase 8
- `commitMessage` ≤ 200 chars, `prTitle` ≤ 200 chars, `prBody` ≤ 5000 chars; null bytes are blocked
- `filesToCommit` is required; only relative paths under allowed automation folders are staged (same prefixes as file write)
- No `git add .` — only explicit `filesToCommit` paths are added
- Create is blocked when `git status` shows changes outside `filesToCommit`
- Preview runs validation and `git status` only — it does not checkout, commit, push, or create a PR
- Create uses `gh pr create`; if GitHub CLI is missing, a clear error is returned
- Git/gh commands use `ProcessBuilder` argument lists — never shell strings
- Operation results are stored in memory by `operationId`

### Jira integration safety guardrails

- `jira.enabled=false` by default; CI does not require Jira env vars
- `jira.api-token` is never logged or returned in API responses
- Frontend cannot submit raw Jira tokens — credentials are server-side only
- Jira base URL comes only from server config (no per-request arbitrary URLs in Phase 9)
- Jira keys must match `^[A-Z][A-Z0-9]+-\d+$` (normalized to uppercase when safe)
- Story fetch uses `GET /rest/api/3/issue/{issueKey}` with Basic Auth (`email:apiToken`)
- Comments use Jira Cloud ADF via `POST /rest/api/3/issue/{issueKey}/comment`
- Acceptance criteria extracted from description sections titled "Acceptance Criteria" or "AC"
- Backend tests mock the Jira client — no real Jira calls in CI

### Production hardening (Phase 10)

- Run history stored in H2 (`AgentRunHistoryEntity`, `RunArtifactEntity`, `ExternalOperationEntity`)
- Agent runs, file writes, test executions, Git PR operations, and Jira updates are recorded where practical
- `SecretMaskingService` redacts OpenAI/Jira/security tokens, Authorization headers, Bearer/Basic values, and password-like fields
- `security.enabled=false` by default; when enabled, require `X-Agentic-Token` (never logged or returned)
- `agent.allow-any-local-path=true` by default for local dev; production can restrict `AGENT_ALLOWED_PROJECT_ROOTS`
- Risky operations require `"confirmation": "I_UNDERSTAND"` when security is enabled
- API errors use consistent `ApiErrorResponse` (`error`, `message`, `code`, `details`)

## Next phase

- Deeper Serenity report parsing and optional PostgreSQL datasource profile

## Reference E2E harness

Golden-path validation for merges:

- [docs/REFERENCE_E2E.md](docs/REFERENCE_E2E.md) — reference API, OpenAPI file/URL modes, sample automation project, CI levels, local E2E scripts
- `samples/reference-api` — controlled payments API on port `9090`
- `samples/reference-automation-project` — writable Maven target scaffold
- `scripts/run-reference-e2e.sh` / `scripts/run-reference-e2e.ps1` — local Level 2 validation (no secrets)

## Tech stack

**Frontend:** React 19, TypeScript, Vite, plain CSS

**Backend:** Java 17, Spring Boot 3, Jackson JSON parsing, Spring Validation

## Project structure

```
src/                      # React frontend
  api/agentApi.ts         # Backend API client
  components/             # UI components (incl. ApiContractView)
  data/mockData.ts        # Local mock fallback data

backend/                  # Spring Boot backend
  src/main/java/com/agentic/api/
    controller/           # REST controllers
    model/                # DTOs (incl. ApiContractDto)
    service/              # RunHistoryService, SecretMaskingService, GitPrService, JiraStoryService

samples/
  reference-api/          # Golden sample payments API + OpenAPI
  reference-automation-project/  # Target automation scaffold

docs/
  REFERENCE_E2E.md        # Reference harness guide

scripts/
  run-reference-e2e.sh    # Local E2E validation (Linux/macOS)
  run-reference-e2e.ps1   # Local E2E validation (Windows)
```

## License

Private — internal QA tooling prototype.
