# Agentic API Test Designer

A web UI for generating API test cases and BDD automation from Jira stories and Swagger/OpenAPI specifications.

## Purpose

Agentic API Test Designer helps QA engineers and automation developers turn Jira requirements and API contracts into structured test coverage, Cucumber feature files, and automation scaffolding — then execute tests and review results in one place.

## Current phase: backend mock API skeleton

This repository includes:

- **React frontend** — dashboard UI with form validation, tabs, and agent timeline
- **Spring Boot backend** — REST API under `/api/agent` returning mock/dynamic responses

There is **no real integration** yet with OpenAI, Jira, Swagger parsing, or test execution. The UI calls the backend when available and falls back to local mock data when the backend is offline.

## Features

- Single-page dashboard with agent input form and tabbed workspace
- Inline form validation (frontend and backend)
- Key-value headers editor
- Agent timeline with execution-mode-aware step control
- Dynamic BDD preview based on Jira key, HTTP method, and endpoint
- Mock test matrix, generated files, and execution report
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

## Example API call

```bash
curl -X POST http://localhost:8080/api/agent/run \
  -H "Content-Type: application/json" \
  -d '{
    "jiraStoryKey": "PAY-1234",
    "jiraStoryText": "As a user...",
    "swaggerUrl": "https://qa-api.company.com/swagger.json",
    "swaggerJson": "",
    "baseApiUrl": "https://qa-api.company.com",
    "endpointPath": "/api/payments",
    "httpMethod": "POST",
    "headers": [
      { "key": "Authorization", "value": "Bearer {{token}}" },
      { "key": "Content-Type", "value": "application/json" }
    ],
    "credentialRef": "qa_api_user",
    "projectPath": "C:\\repos\\api-automation-framework",
    "executionMode": "generate-execute",
    "frameworkType": "restassured-cucumber-serenity"
  }'
```

## Backend endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/agent/health` | Health check |
| POST | `/api/agent/generate-test-matrix` | Returns mock test cases |
| POST | `/api/agent/generate-bdd` | Returns dynamic BDD feature |
| POST | `/api/agent/generate-files` | Returns generated file tree and BDD metadata |
| POST | `/api/agent/run` | Full agent run (mock) |
| GET | `/api/agent/runs/{runId}` | Retrieve a stored run |

## Next phase

- Parse Jira stories and Swagger/OpenAPI contracts for real
- Generate test matrices and BDD from actual API contracts
- Scaffold automation files in a target project
- Execute tests and return real reports
- Optionally create pull requests

## Tech stack

**Frontend:** React 19, TypeScript, Vite, plain CSS

**Backend:** Java 17, Spring Boot 3, Spring Validation

## Project structure

```
src/                      # React frontend
  api/agentApi.ts         # Backend API client
  components/             # UI components
  data/mockData.ts        # Local mock fallback data

backend/                  # Spring Boot backend
  src/main/java/com/agentic/api/
    controller/           # REST controllers
    model/                # DTOs
    service/              # Mock agent service
```

## License

Private — internal QA tooling prototype.
