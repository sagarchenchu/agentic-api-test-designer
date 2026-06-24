# Agentic API Test Designer

A web UI for generating API test cases and BDD automation from Jira stories and Swagger/OpenAPI specifications.

## Purpose

Agentic API Test Designer helps QA engineers and automation developers turn Jira requirements and API contracts into structured test coverage, Cucumber feature files, and automation scaffolding — then execute tests and review results in one place.

## Current phase: Swagger parser and contract extraction (Phase 3)

This repository includes:

- **React frontend** — dashboard UI with form validation, tabs, and agent timeline
- **Spring Boot backend** — REST API under `/api/agent` with deterministic Swagger/OpenAPI parsing

Phase 3 adds a **Swagger/OpenAPI parser** that extracts structured API contracts and generates dynamic test matrices from schema fields — without AI.

There is **no real integration** yet with OpenAI, Jira, or test execution.

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
| POST | `/api/agent/generate-test-matrix` | Dynamic test cases from contract (fallback to mock) |
| POST | `/api/agent/generate-bdd` | Returns dynamic BDD feature |
| POST | `/api/agent/generate-files` | Returns file tree + BDD metadata |
| POST | `/api/agent/run` | Full agent run (mock) |
| GET | `/api/agent/runs/{runId}` | Retrieve a stored run |

## Design approach

**Swagger parser first, AI second.**

The parser produces a clean structured contract that future AI phases can consume:

```json
{
  "jiraStory": "...",
  "apiContract": { "structured contract" },
  "framework": "RestAssured + Cucumber + Serenity"
}
```

## Next phase

- OpenAI-assisted test case enrichment from structured contract + Jira story
- Real Jira and Swagger integrations
- Scaffold automation files in a target project
- Execute tests and return real reports

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
    service/              # OpenApiParserService, MockAgentService
```

## License

Private — internal QA tooling prototype.
