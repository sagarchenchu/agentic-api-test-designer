# Agentic API Test Designer

A web UI for generating API test cases and BDD automation from Jira stories and Swagger/OpenAPI specifications.

## Purpose

Agentic API Test Designer helps QA engineers and automation developers turn Jira requirements and API contracts into structured test coverage, Cucumber feature files, and automation scaffolding — then execute tests and review results in one place.

## Current phase: UI prototype only

This repository currently ships a **frontend-only prototype** with mock data and simulated agent progress. There is no backend or real agent integration yet.

Planned inputs:

- Jira story key and description
- Swagger/OpenAPI URL or JSON
- Base API URL, endpoint, HTTP method, headers
- Credential reference (not raw passwords)
- Local project path and framework selection

Planned outputs:

- Requirement summary
- Test case matrix
- Generated BDD feature files
- Automation file tree
- Execution report

## Features

- Single-page dashboard with agent input form and tabbed workspace
- Inline form validation
- Key-value headers editor
- Simulated agent timeline with execution-mode-aware step control
- Dynamic BDD preview based on Jira key, HTTP method, and endpoint
- Mock test matrix, generated files, and execution report
- Copy-to-clipboard and download for BDD feature files
- Light/dark theme toggle
- Responsive layout for desktop and mobile

## How to run

```bash
npm install
npm run dev
```

Other commands:

```bash
npm run build   # production build
npm run lint    # oxlint
npm run preview # preview production build
```

Open the URL shown by Vite (default `http://localhost:5173`).

## Next phase: backend agent APIs

The next phase will add backend services to:

- Parse Jira stories and Swagger/OpenAPI contracts
- Generate test matrices and BDD from real inputs
- Scaffold automation files in a target project
- Execute tests and return reports
- Optionally create pull requests

## Tech stack

- React 19
- TypeScript
- Vite
- Plain CSS

## Project structure

```
src/
  App.tsx                 # App state, validation, agent simulation
  components/             # UI components
  data/mockData.ts        # Mock data and builders
  types.ts                # Shared TypeScript types
```

## License

Private — internal QA tooling prototype.
