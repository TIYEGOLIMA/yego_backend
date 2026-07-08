param(
    [string]$DbHost = $env:OPERATIONAL_LOCAL_DB_HOST,
    [string]$DbPort = $env:OPERATIONAL_LOCAL_DB_PORT,
    [string]$DbName = $env:OPERATIONAL_LOCAL_DB_NAME,
    [string]$DbUser = $env:OPERATIONAL_LOCAL_DB_USER,
    [string]$DbPassword = $env:OPERATIONAL_LOCAL_DB_PASSWORD
)

$ErrorActionPreference = "Stop"

function Require-LocalTarget {
    param(
        [string]$HostName,
        [string]$DatabaseName
    )

    if ($env:CONFIRM_OPERATIONAL_LOCAL_DB -ne "true") {
        throw "Set CONFIRM_OPERATIONAL_LOCAL_DB=true before applying migration 017 on an isolated local database."
    }

    $allowedHosts = @("localhost", "127.0.0.1")
    if ($allowedHosts -notcontains $HostName) {
        throw "Refusing to continue because host '$HostName' is not a local isolated target."
    }

    if ([string]::IsNullOrWhiteSpace($DatabaseName)) {
        throw "Database name is required."
    }

    if ($DatabaseName -eq "yego_integral") {
        throw "Refusing to continue because database '$DatabaseName' is reserved for shared/protected environments."
    }
}

function Require-Psql {
    if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
        throw "psql was not found in PATH. Install PostgreSQL client tools before running this script."
    }
}

function Invoke-PsqlFile {
    param(
        [string]$FilePath
    )

    & psql `
        --host $DbHost `
        --port $DbPort `
        --username $DbUser `
        --dbname $DbName `
        --set ON_ERROR_STOP=1 `
        --file $FilePath

    if ($LASTEXITCODE -ne 0) {
        throw "psql failed while executing $FilePath"
    }
}

function Invoke-PsqlQuery {
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
        throw "psql failed while executing verification query."
    }
}

$DbHost = if ([string]::IsNullOrWhiteSpace($DbHost)) { "localhost" } else { $DbHost }
$DbPort = if ([string]::IsNullOrWhiteSpace($DbPort)) { "54329" } else { $DbPort }
$DbName = if ([string]::IsNullOrWhiteSpace($DbName)) { "yego_operational_local" } else { $DbName }
$DbUser = if ([string]::IsNullOrWhiteSpace($DbUser)) { "yego_local" } else { $DbUser }
$DbPassword = if ([string]::IsNullOrWhiteSpace($DbPassword)) { "yego_local" } else { $DbPassword }

Require-LocalTarget -HostName $DbHost -DatabaseName $DbName
Require-Psql

$migrationPath = Join-Path $PSScriptRoot "..\src\main\resources\db\migration\017_operational_automatic_shift_mirror.sql"
$resolvedMigrationPath = (Resolve-Path $migrationPath).Path

$env:PGPASSWORD = $DbPassword
try {
    Invoke-PsqlFile -FilePath $resolvedMigrationPath

    $verificationQuery = @"
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'operational_trip_facts',
    'operational_shift_sessions',
    'operational_shift_events'
  )
ORDER BY table_name;
"@

    $tables = Invoke-PsqlQuery -Query $verificationQuery
    $missing = @(
        "operational_shift_events",
        "operational_shift_sessions",
        "operational_trip_facts"
    ) | Where-Object { $tables -notmatch $_ }

    if ($missing.Count -gt 0) {
        throw "Migration 017 did not create all required operational tables. Missing: $($missing -join ', ')"
    }

    Write-Host "Migration 017 applied successfully on isolated local database '$DbName' at ${DbHost}:${DbPort}."
    Write-Host "Verified tables: operational_trip_facts, operational_shift_sessions, operational_shift_events."
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
