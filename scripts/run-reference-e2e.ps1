#requires -Version 5.1
$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$ReferenceApiDir = Join-Path $RootDir "samples/reference-api"
$AutomationDir = Join-Path $RootDir "samples/reference-automation-project"
$BackendDir = Join-Path $RootDir "backend"
$OpenApiFile = Join-Path $ReferenceApiDir "openapi/reference-api.yaml"

$ReferenceApiPort = if ($env:REFERENCE_API_PORT) { $env:REFERENCE_API_PORT } else { "9090" }
$BackendPort = if ($env:BACKEND_PORT) { $env:BACKEND_PORT } else { "8080" }
$ReferenceApiUrl = "http://localhost:$ReferenceApiPort"
$BackendUrl = "http://localhost:$BackendPort"

function Wait-ForUrl {
    param(
        [string]$Url,
        [string]$Label,
        [int]$Attempts = 90
    )
    for ($i = 0; $i -lt $Attempts; $i++) {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing | Out-Null
            Write-Host "$Label is ready at $Url"
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    throw "Timed out waiting for $Label at $Url"
}

function New-AgentRequestJson {
    $openapi = Get-Content -Raw -Path $OpenApiFile
    return (@{
        jiraStoryKey = "PAY-REF-001"
        swaggerJson = $openapi
        baseApiUrl = $ReferenceApiUrl
        endpointPath = "/api/payments"
        httpMethod = "POST"
        credentialRef = "reference_api_user"
    } | ConvertTo-Json -Depth 6 -Compress)
}

Write-Host "==> Level 1 checks (no secrets)"
& mvn -f (Join-Path $ReferenceApiDir "pom.xml") -q test
& mvn -f (Join-Path $AutomationDir "pom.xml") -q test
& mvn -f (Join-Path $BackendDir "pom.xml") -q test "-Dtest=ReferenceHarnessIntegrationTest"

Write-Host "==> Starting reference API on port $ReferenceApiPort"
$referenceApi = Start-Process -FilePath "mvn" -ArgumentList @(
    "-f", (Join-Path $ReferenceApiDir "pom.xml"), "-q", "spring-boot:run",
    "-Dspring-boot.run.arguments=--server.port=$ReferenceApiPort"
) -PassThru -NoNewWindow

Write-Host "==> Starting agent backend on port $BackendPort"
$backend = Start-Process -FilePath "mvn" -ArgumentList @(
    "-f", (Join-Path $BackendDir "pom.xml"), "-q", "spring-boot:run",
    "-Dspring-boot.run.arguments=--server.port=$BackendPort"
) -PassThru -NoNewWindow

try {
    Wait-ForUrl -Url "$ReferenceApiUrl/openapi/reference-api.yaml" -Label "Reference API"
    Wait-ForUrl -Url "$BackendUrl/api/agent/health" -Label "Agent backend"

    $agentRequest = New-AgentRequestJson

    Write-Host "==> Extract contract (OpenAPI file mode)"
  Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/agent/extract-contract" `
        -ContentType "application/json" -Body $agentRequest | Out-Null

    Write-Host "==> Generate deterministic test matrix"
    Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/agent/generate-test-matrix" `
        -ContentType "application/json" -Body $agentRequest | Out-Null

    $previewWrite = @{
        projectPath = $AutomationDir
        overwriteExisting = $true
        createBackup = $false
        files = @(@{
            path = "src/test/resources/features/payment/reference_e2e.feature"
            content = "Feature: Reference E2E`n  Scenario: Smoke`n    Given reference harness is running`n"
            type = "gherkin"
        })
    } | ConvertTo-Json -Depth 6 -Compress

    Write-Host "==> Preview file write into sample automation project"
    Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/agent/preview-file-write" `
        -ContentType "application/json" -Body $previewWrite | Out-Null

    $previewExecution = @{
        projectPath = $AutomationDir
        commandType = "MAVEN"
        mavenCommand = "mvn test"
        testTag = "@PAY-REF-001"
        profile = "qa"
        timeoutSeconds = 300
        environment = "QA"
        dryRun = $true
    } | ConvertTo-Json -Compress

    Write-Host "==> Preview Maven test execution"
    Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/agent/preview-test-execution" `
        -ContentType "application/json" -Body $previewExecution | Out-Null

    Write-Host "==> Run lightweight Maven smoke test"
    & mvn -f (Join-Path $AutomationDir "pom.xml") -q test

    Write-Host ""
    Write-Host "Reference E2E script completed successfully."
    Write-Host "Swagger URL mode: $ReferenceApiUrl/v3/api-docs"
    Write-Host "OpenAPI file mode: $OpenApiFile"
    Write-Host "Target automation project: $AutomationDir"
}
finally {
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force -ErrorAction SilentlyContinue }
    if ($referenceApi -and -not $referenceApi.HasExited) { Stop-Process -Id $referenceApi.Id -Force -ErrorAction SilentlyContinue }
}
