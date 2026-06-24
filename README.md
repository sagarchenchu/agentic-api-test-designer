# Agentic API Test Designer

A web UI for generating API test cases and BDD automation from Jira stories and Swagger/OpenAPI specifications.

## Purpose

Agentic API Test Designer helps QA engineers and automation developers turn Jira requirements and API contracts into structured test coverage, Cucumber feature files, and automation scaffolding — then execute tests and review results in one place.

## Current phase: Maven test execution and report parsing (Phase 7)

This repository includes:

- **React frontend** — dashboard UI with form validation, tabs, and agent timeline
- **Spring Boot backend** — REST API under `/api/agent` with Swagger/OpenAPI parsing and optional OpenAI integration

Phase 7 adds **safe Maven/Serenity test execution** with preview, ProcessBuilder-based command execution, timeout handling, log capture, and Surefire/Failsafe/Cucumber/Serenity report parsing. Git commit/PR automation is **not implemented yet**.

Phase 6 added safe generated-file writes. Phase 5 added AI-assisted BDD and automation file generation. OpenAI is **optional**.

There is **no real integration** yet with Jira or Git/PR automation.

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
5. If AI is disabled or fails, deterministic BDD and scaffold files are used automatically

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
- No Maven execution, git commits, or writes outside `projectPath`

Future phases can execute tests and return real reports:

```json
{
  "jiraStory": "...",
  "apiContract": { "structured contract" },
  "testCases": [ "selected cases" ],
  "framework": "RestAssured + Cucumber + Serenity"
}
```

## Next phase

- Maven/test execution and real execution reports
- Real Jira integration

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
    service/              # OpenApiParserService, FileWriteService, AutomationGenerationService
```

## License

Private — internal QA tooling prototype.
