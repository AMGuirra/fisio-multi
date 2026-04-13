param(
    [string]$LocalHost = "localhost",
    [int]$LocalPort = 5432,
    [string]$LocalDatabase = "fisioterapia",
    [string]$LocalUser = "postgres",
    [string]$LocalPassword = "postgres",
    [string]$EnvFile = "render-db.env",
    [string]$DumpFile = "local-fisioterapia.dump"
)

$ErrorActionPreference = "Stop"

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Comando '$Name' nao encontrado. Instale o cliente PostgreSQL e tente novamente."
    }
}

function Get-EnvMap {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        throw "Arquivo '$Path' nao encontrado. Copie render-db.env.example para render-db.env e preencha os dados do Render."
    }

    $map = @{}
    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed -split "=", 2
        if ($parts.Length -ne 2) {
            continue
        }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim().Trim("'`"")
        if ($key -and $value) {
            $map[$key] = $value
        }
    }

    return $map
}

function Parse-DatabaseUrl {
    param([string]$DatabaseUrl)
    if (-not $DatabaseUrl) {
        return $null
    }

    $normalized = $DatabaseUrl
    if ($normalized.StartsWith("postgres://")) {
        $normalized = "postgresql://" + $normalized.Substring("postgres://".Length)
    }

    if (-not $normalized.StartsWith("postgresql://")) {
        return $null
    }

    $uri = [System.Uri]$normalized
    $userInfo = $uri.UserInfo
    $username = $null
    $password = $null
    if ($userInfo) {
        $pieces = $userInfo -split ":", 2
        $username = [System.Uri]::UnescapeDataString($pieces[0])
        if ($pieces.Length -gt 1) {
            $password = [System.Uri]::UnescapeDataString($pieces[1])
        }
    }

    $database = $uri.AbsolutePath.TrimStart("/")
    return @{
        Host = $uri.Host
        Port = $(if ($uri.Port -gt 0) { $uri.Port } else { 5432 })
        Database = $database
        Username = $username
        Password = $password
        Query = $uri.Query.TrimStart("?")
    }
}

Require-Command "pg_dump"
Require-Command "pg_restore"

$envMap = Get-EnvMap -Path $EnvFile
$remote = Parse-DatabaseUrl -DatabaseUrl $envMap["DATABASE_URL"]

if (-not $remote) {
    $remote = @{
        Host = $envMap["PGHOST"]
        Port = $(if ($envMap["PGPORT"]) { [int]$envMap["PGPORT"] } else { 5432 })
        Database = $envMap["PGDATABASE"]
        Username = $envMap["PGUSER"]
        Password = $envMap["PGPASSWORD"]
        Query = "sslmode=require"
    }
}

if (-not $remote.Host -or -not $remote.Database -or -not $remote.Username -or -not $remote.Password) {
    throw "Nao foi possivel resolver as credenciais do banco Render a partir de '$EnvFile'."
}

$dumpPath = Resolve-Path "." | ForEach-Object { Join-Path $_ $DumpFile }

Write-Host "Gerando dump do banco local '$LocalDatabase'..." -ForegroundColor Cyan
$env:PGPASSWORD = $LocalPassword
& pg_dump `
    --format=custom `
    --no-owner `
    --no-privileges `
    --host $LocalHost `
    --port $LocalPort `
    --username $LocalUser `
    --dbname $LocalDatabase `
    --file $dumpPath

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar dump do banco local."
}

Write-Host "Restaurando dump no banco Render '$($remote.Database)'..." -ForegroundColor Cyan
$env:PGPASSWORD = $remote.Password

$restoreArgs = @(
    "--clean",
    "--if-exists",
    "--no-owner",
    "--no-privileges",
    "--host", $remote.Host,
    "--port", [string]$remote.Port,
    "--username", $remote.Username,
    "--dbname", $remote.Database
)

if ($remote.Query -match "sslmode=require") {
    $restoreArgs += "--sslmode=require"
}

$restoreArgs += $dumpPath

& pg_restore @restoreArgs

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao restaurar dump no banco Render."
}

Write-Host "Migracao concluida com sucesso." -ForegroundColor Green
