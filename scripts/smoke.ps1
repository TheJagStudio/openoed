# End-to-end check. The stack must already be up (.\dev.ps1 up).
$ErrorActionPreference = 'Stop'
$api = 'http://localhost:8087'
$platform = 'http://localhost:8090'

$health = Invoke-RestMethod "$api/actuator/health"
if ($health.status -ne 'UP') { throw "api health $($health.status)" }
$status = Invoke-RestMethod "$platform/status"
if ($null -eq $status.ledgerItems) { throw 'platform status missing ledgerItems' }

$login = Invoke-RestMethod -Method Post -Uri "$api/api/v1/auth/login" -ContentType 'application/json' -Body '{"email":"oed@openoed.local","password":"Oed#12345"}'
$h = @{ Authorization = "Bearer $($login.accessToken)" }
$apps = Invoke-RestMethod -Uri "$api/api/v1/applications" -Headers $h
$lumen = $apps | Where-Object { $_.mark -eq 'AURORA LUMEN' } | Select-Object -First 1
if (-not $lumen -or $lumen.riskBand -ne 'CRITICAL') { throw 'Aurora Lumen is not critical' }
$detail = Invoke-RestMethod -Uri "$api/api/v1/applications/$($lumen.id)" -Headers $h
if ($detail.findings.Count -lt 3) { throw 'expected several rule findings' }

$ig = Invoke-RestMethod -Method Post -Uri "$api/api/v1/auth/login" -ContentType 'application/json' -Body '{"email":"ig@openoed.local","password":"Inspector#12345"}'
$audit = Invoke-RestMethod -Uri "$api/api/v1/audit?days=2" -Headers @{ Authorization = "Bearer $($ig.accessToken)" }
Write-Host ("smoke ok: lumen {0} {1}, audit rows {2}, schedules {3}" -f $lumen.riskScore, $lumen.riskBand, $audit.Count, $status.schedules)
