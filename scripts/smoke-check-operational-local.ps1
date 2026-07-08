param(
    [string]$DbHost = $env:OPERATIONAL_LOCAL_DB_HOST,
    [string]$DbPort = $env:OPERATIONAL_LOCAL_DB_PORT,
    [string]$DbName = $env:OPERATIONAL_LOCAL_DB_NAME,
    [string]$DbUser = $env:OPERATIONAL_LOCAL_DB_USER,
    [string]$DbPassword = $env:OPERATIONAL_LOCAL_DB_PASSWORD,
    [string]$BaseUrl = $env:OPERATIONAL_LOCAL_BASE_URL,
    [string]$From = "2026-06-23",
    [string]$To = "2026-06-23",
    [switch]$CheckHttp
)

$ErrorActionPreference = "Stop"

function Assert-SafeLocalTarget {
    param(
        [string]$HostName,
        [string]$DatabaseName
    )

    if ($env:CONFIRM_OPERATIONAL_LOCAL_DB -ne "true") {
        throw "Set CONFIRM_OPERATIONAL_LOCAL_DB=true before running smoke checks on the isolated local database."
    }

    $allowedHosts = @("localhost", "127.0.0.1")
    if ($allowedHosts -notcontains $HostName) {
        throw "Refusing to continue because host '$HostName' is not local."
    }

    if ($DatabaseName -eq "yego_integral") {
        throw "Refusing to continue because database '$DatabaseName' is protected/shared."
    }
}

function Require-Psql {
    if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
        throw "psql was not found in PATH. Install PostgreSQL client tools before running this script."
    }
}

function Invoke-LocalQuery {
    param(
        [string]$Query
    )

    & psql `
        --host $DbHost `
        --port $DbPort `
        --username $DbUser `
        --dbname $DbName `
        --set ON_ERROR_STOP=1 `
        --tuples-only `
        --no-align `
        --command $Query

    if ($LASTEXITCODE -ne 0) {
        throw "psql failed while executing smoke-check query."
    }
}

function Assert-TablesExist {
    param(
        [string[]]$TableNames
    )

    foreach ($tableName in $TableNames) {
        $query = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$tableName');"
        $exists = (Invoke-LocalQuery -Query $query).Trim()
        if ($exists -ne "t") {
            throw "Required table '$tableName' does not exist in isolated local database '$DbName'."
        }
    }
}

function Assert-RunnerClosedByDefault {
    $profilePath = Join-Path $PSScriptRoot "..\src\main\resources\application-operational-local.yml"
    $profile = Get-Content -Path $profilePath -Raw

    if ($profile -notmatch "enabled:\s*false") {
        throw "application-operational-local.yml must keep the runner disabled by default."
    }

    if ($profile -notmatch "environment:\s*local") {
        throw "application-operational-local.yml must declare operational.monitoring.runner.environment=local."
    }
}

function Invoke-HttpCheck {
    param(
        [string]$Url
    )

    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 15
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "HTTP smoke check failed for '$Url' with status $($response.StatusCode)."
    }
}

$DbHost = if ([string]::IsNullOrWhiteSpace($DbHost)) { "localhost" } else { $DbHost }
$DbPort = if ([string]::IsNullOrWhiteSpace($DbPort)) { "54329" } else { $DbPort }
$DbName = if ([string]::IsNullOrWhiteSpace($DbName)) { "yego_operational_local" } else { $DbName }
$DbUser = if ([string]::IsNullOrWhiteSpace($DbUser)) { "yego_local" } else { $DbUser }
$DbPassword = if ([string]::IsNullOrWhiteSpace($DbPassword)) { "yego_local" } else { $DbPassword }
$BaseUrl = if ([string]::IsNullOrWhiteSpace($BaseUrl)) { "http://localhost:3030/api/pro-ops/operational-monitoring" } else { $BaseUrl.TrimEnd("/") }

Assert-SafeLocalTarget -HostName $DbHost -DatabaseName $DbName
Require-Psql
Assert-RunnerClosedByDefault

$env:PGPASSWORD = $DbPassword
try {
    Assert-TablesExist -TableNames @(
        "shift_sessions",
        "module_driver_closes",
        "operational_trip_facts",
        "operational_shift_sessions",
        "operational_shift_events"
    )
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host "Local DB smoke checks passed for ${DbHost}:${DbPort}/$DbName."
Write-Host "Runner remains disabled by default in application-operational-local.yml."

if ($CheckHttp) {
    Invoke-HttpCheck -Url "$BaseUrl/trip-facts?from=$From&to=$To"
    Invoke-HttpCheck -Url "$BaseUrl/shifts?from=$From&to=$To"
    Invoke-HttpCheck -Url "$BaseUrl/events?from=$From&to=$To"
    Invoke-HttpCheck -Url "$BaseUrl/validation/coverage?from=$From&to=$To"
    Invoke-HttpCheck -Url "$BaseUrl/validation/summary?from=$From&to=$To"
    Write-Host "HTTP smoke checks passed for read-only operational endpoints."
}
else {
    Write-Host "HTTP smoke checks skipped. Re-run with -CheckHttp after starting the app with profile operational-local."
}
