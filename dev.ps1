# OpenOED local runner.
#   .\dev.ps1 up         Postgres + platform + oed-api + Angular
#   .\dev.ps1 down       stop the processes started by up (Postgres stays up for the other projects)
#   .\dev.ps1 build      test the Java modules and build the web app
#   .\dev.ps1 status
param([Parameter(Position=0)][ValidateSet('up','down','build','status','reset-db')][string]$Cmd = 'status')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$tools = 'D:\JavaSpring\tools'
. "$tools\env.ps1" | Out-Null
$data = Join-Path $root 'data'
$pidFile = Join-Path $data 'dev-pids.txt'
$psql = Join-Path $tools 'pgsql\bin\psql.exe'
New-Item -ItemType Directory -Force $data | Out-Null

$envFile = Join-Path $root '.env'
if (Test-Path $envFile) {
  foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$' -and -not $line.TrimStart().StartsWith('#')) {
      [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"', "'"), 'Process')
    }
  }
}

function Sql($db, $sql) {
  $env:PGPASSWORD = 'postgres'
  & $psql -h localhost -p 5433 -U postgres -d $db -v ON_ERROR_STOP=1 -tAc $sql
  if ($LASTEXITCODE) { throw "psql failed: $sql" }
}

function Ensure-Databases {
  Sql 'postgres' "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'openoed') THEN CREATE ROLE openoed LOGIN PASSWORD 'openoed'; END IF; END `$`$;" | Out-Null
  foreach ($db in 'openoed', 'openoed_test') {
    if (-not (Sql 'postgres' "SELECT 1 FROM pg_database WHERE datname = '$db'")) {
      Sql 'postgres' "CREATE DATABASE $db OWNER openoed" | Out-Null
    }
  }
  Write-Host 'databases openoed + openoed_test ready'
}

function Wait-Port($port, $seconds = 180) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  while ($sw.Elapsed.TotalSeconds -lt $seconds) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { return $true }
    Start-Sleep -Milliseconds 700
  }
  return $false
}

$components = @(
  @{ name = 'platform'; port = 8090; cmd = ".\gradlew.bat :platform:bootRun" },
  @{ name = 'oed-api'; port = 8087; cmd = ".\gradlew.bat :oed-api:bootRun --args='--spring.profiles.active=dev'" }
)

switch ($Cmd) {
  'up' {
    & "$tools\pg.ps1" start
    Ensure-Databases
    $pids = @()
    foreach ($c in $components) {
      $cmdline = ". '$tools\env.ps1'; `$host.UI.RawUI.WindowTitle='$($c.name) :$($c.port)'; Set-Location '$root'; $($c.cmd)"
      $p = Start-Process powershell -ArgumentList '-NoExit', '-Command', $cmdline -PassThru
      $pids += $p.Id
      Write-Host "starting $($c.name) on :$($c.port) (pid $($p.Id))"
      if (-not (Wait-Port $c.port)) { Write-Warning "$($c.name) did not open :$($c.port) in time" }
    }
    $web = Join-Path $root 'web'
    if (-not (Test-Path (Join-Path $web 'node_modules'))) { Push-Location $web; npm install; Pop-Location }
    $p = Start-Process powershell -ArgumentList '-NoExit', '-Command', ". '$tools\env.ps1'; `$host.UI.RawUI.WindowTitle='web :4206'; Set-Location '$web'; npm start" -PassThru
    $pids += $p.Id
    $pids | Set-Content $pidFile
    Wait-Port 4206 | Out-Null
    Write-Host ''
    Write-Host 'Web app   http://localhost:4206'
    Write-Host 'API docs  http://localhost:8087/swagger-ui.html'
    Write-Host 'Platform  http://localhost:8090/status'
    Write-Host 'Dev logins'
    Write-Host '  oed@openoed.local        / Oed#12345'
    Write-Host '  examiner@openoed.local   / Examiner#12345'
    Write-Host '  applicant@openoed.local  / Applicant#12345'
    Write-Host '  ig@openoed.local         / Inspector#12345'
    Write-Host '  admin@openoed.local      / Admin#12345'
  }
  'down' {
    if (Test-Path $pidFile) {
      foreach ($id in Get-Content $pidFile) { taskkill.exe /PID $id /T /F 2>$null | Out-Null }
      Remove-Item $pidFile
    }
  }
  'build' {
    & "$tools\pg.ps1" start
    Ensure-Databases
    Push-Location $root
    .\gradlew.bat check
    if ($LASTEXITCODE) { Pop-Location; throw 'gradle check failed' }
    Pop-Location
    Push-Location (Join-Path $root 'web')
    if (-not (Test-Path node_modules)) { npm install }
    npm run build
    if ($LASTEXITCODE) { Pop-Location; throw 'web build failed' }
    npm run test:ci
    if ($LASTEXITCODE) { Pop-Location; throw 'web tests failed' }
    Pop-Location
  }
  'reset-db' {
    & "$tools\pg.ps1" start
    Sql 'postgres' "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'openoed' AND pid <> pg_backend_pid()" | Out-Null
    Sql 'postgres' 'DROP DATABASE IF EXISTS openoed' | Out-Null
    Ensure-Databases
  }
  'status' {
    foreach ($s in @(@{ p = 5433; n = 'postgres' }, @{ p = 8090; n = 'platform' }, @{ p = 8087; n = 'oed-api' }, @{ p = 4206; n = 'web' })) {
      $up = [bool](Get-NetTCPConnection -State Listen -LocalPort $s.p -ErrorAction SilentlyContinue)
      '{0,-6} {1,-12} {2}' -f $s.p, $s.n, $(if ($up) { 'UP' } else { '-' })
    }
  }
}
