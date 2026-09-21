#!/usr/bin/env pwsh
# SGROAS Verification Script (EV-2)
# Equivalente a: make verify / scripts/verify.sh
# Exit code 0 = all checks passed, non-zero = failure

$ErrorActionPreference = "Continue"
$failed = 0
$py = if (Get-Command python -ErrorAction SilentlyContinue) { "python" } else { "python3" }

Write-Host "=== SGROAS Verification ===" -ForegroundColor Cyan
Write-Host ""

# [P1] Hardcoded secrets (application*.properties, docker-compose.yml,
# render.yaml, todos los .sql) -- misma logica que make/verify.sh, un
# solo script para que los tres no puedan desincronizarse otra vez.
Write-Host "[P1] Checking hardcoded secrets..." -ForegroundColor Yellow
& $py scripts/check-hardcoded-secrets.py
if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: hardcoded secrets found" -ForegroundColor Red; $failed++ }

# [P1] JWT tokens versioned anywhere in the repo must be expired
Write-Host "[P1] Checking JWT tokens versioned anywhere in the repo are expired..." -ForegroundColor Yellow
& $py scripts/check-jwt-expiry.py
if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: a live (non-expired) JWT is versioned in the repository" -ForegroundColor Red; $failed++ }

# [P1] ZAP scan really targeted the public deployment
Write-Host "[P1] Checking ZAP scan really targeted the public deployment..." -ForegroundColor Yellow
$zapPublic = Select-String -Path "dataset\zap\zap.html", "dataset\zap\zap-baseline-2026-09-06.html" -Pattern "sgroas-backend.onrender.com" -ErrorAction SilentlyContinue -CaseSensitive
$zapLocal = Select-String -Path "dataset\zap\zap.html", "dataset\zap\zap-baseline-2026-09-06.html" -Pattern "localhost|127\.0\.0\.1" -ErrorAction SilentlyContinue -CaseSensitive
if ($zapPublic -and -not $zapLocal) { Write-Host "  OK - ZAP reports target the public URL, not localhost" -ForegroundColor Green } else { Write-Host "  FAIL: ZAP report does not clearly target the public URL" -ForegroundColor Red; $failed++ }

# [P2] Raw k6 runs (hot x5 + cold x5) + cache contrast (K10-K14)
Write-Host "[P2] Checking raw k6 runs (hot x5 + cold x5) reproducible contrast..." -ForegroundColor Yellow
$hot = (Get-ChildItem "dataset\perf\k0[4-8]-run1.json" -ErrorAction SilentlyContinue).Count
$cold = (Get-ChildItem "dataset\perf\k0[4-8]-cold.json" -ErrorAction SilentlyContinue).Count
$contrast = (Get-ChildItem "dataset\perf\k1[0-4]-cache-contrast.json" -ErrorAction SilentlyContinue).Count
if ($hot -lt 5 -or $cold -lt 5 -or $contrast -lt 5) {
    Write-Host "  FAIL: hot=$hot cold=$cold cache-contrast=$contrast (expected >= 5 each)" -ForegroundColor Red; $failed++
} else {
    Write-Host "  $hot hot runs, $cold cold runs, $contrast cache-contrast runs (K10-K14) found" -ForegroundColor Green
    & $py scripts/perf/recalcular-contraste.py | Out-Null
    if ($LASTEXITCODE -eq 0) { Write-Host "  OK: nonparametric contrast reproducible (nonparametric.py)" -ForegroundColor Green } else { Write-Host "  FAIL: nonparametric contrast script did not run cleanly" -ForegroundColor Red; $failed++ }
    Write-Host "[P2] Checking internal consistency of raw k6 exports (anti-falsification)..." -ForegroundColor Yellow
    & $py scripts/check-k6-authenticity.py
    if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: an internal consistency identity is broken in a k6 export" -ForegroundColor Red; $failed++ }
}

# [P4] Cookie Secure(true)
Write-Host "[P4] Checking cookie Secure(true)..." -ForegroundColor Yellow
$p4bad = Select-String -Path "src\main\java\ec\edu\uteq\sgroas\controller\AuthController.java" -Pattern "\.secure\(cookieSecure\)" -CaseSensitive
$p4lines = Get-Content "src\main\java\ec\edu\uteq\sgroas\controller\AuthController.java" | Where-Object { $_ -notmatch "//" }
$p4good = ($p4lines | Select-String -Pattern "\.secure\(true\)" -CaseSensitive).Count
if ($p4bad) {
    Write-Host "  FAIL: .secure(cookieSecure) found" -ForegroundColor Red; $failed++
} elseif ($p4good -lt 4) {
    Write-Host "  FAIL: only $p4good .secure(true) calls found (expected >= 4)" -ForegroundColor Red; $failed++
} else {
    Write-Host "  OK ($p4good .secure(true) calls)" -ForegroundColor Green
}

# [P5] Spanish fields in entities
Write-Host "[P5] Checking Spanish field names in entities..." -ForegroundColor Yellow
$p5 = Get-ChildItem "src\main\java\ec\edu\uteq\sgroas\entity\*.java" | Select-String "private String (nombre|apellido|estado|direccion|telefono|placa|marca|modelo)\b" -CaseSensitive
if ($p5) { Write-Host "  FAIL: Spanish fields found" -ForegroundColor Red; $failed++ } else { Write-Host "  OK - no Spanish fields in entities" -ForegroundColor Green }

# [P6] Javadoc coverage
Write-Host "[P6] Checking Javadoc coverage on public methods..." -ForegroundColor Yellow
& $py scripts/check-javadoc.py
if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: Javadoc coverage below threshold" -ForegroundColor Red; $failed++ }

# [P7] Spanish captions, prose cross-references, figure pixels
Write-Host "[P7] Checking Spanish captions in informe..." -ForegroundColor Yellow
& $py scripts/check-caption-language.py
if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: Spanish captions found" -ForegroundColor Red; $failed++ }

Write-Host "[P7] Checking Spanish float names in prose cross-references..." -ForegroundColor Yellow
$p7prose = Select-String -Path "docs\informe-final\*.tex", "docs\informe-final\capitulos\*.tex" -Pattern "\b(la|La|las|Las) (Tabla|Figura)\b" -ErrorAction SilentlyContinue -CaseSensitive
if ($p7prose) { Write-Host "  FAIL: prose still says 'la Tabla/Figura N' while the float is labelled Table/Figure N" -ForegroundColor Red; $failed++ } else { Write-Host "  OK - prose refers to Table/Figure" -ForegroundColor Green }

Write-Host "[P7] Checking Spanish text baked into the figure pixels..." -ForegroundColor Yellow
& $py scripts/check-figure-text.py
if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: Spanish text inside a figure of the report" -ForegroundColor Red; $failed++ }

# [P10] MANIFEST verification
Write-Host "[P10] Verifying MANIFEST.sha256..." -ForegroundColor Yellow
$manifest = Get-Content "dataset\MANIFEST.sha256" | Where-Object { $_ -match '\S' }
$allOk = $true
foreach ($line in $manifest) {
    $parts = $line -split '\s{2,}'
    if ($parts.Count -ge 2) {
        $hash = $parts[0]
        $file = $parts[1]
        if (Test-Path $file) {
            $actual = (Get-FileHash -Algorithm SHA256 $file).Hash.ToLower()
            if ($hash -ne $actual) { Write-Host "  MISMATCH: $file" -ForegroundColor Red; $allOk = $false; $failed++ }
        } else {
            Write-Host "  MISSING: $file" -ForegroundColor Red; $allOk = $false; $failed++
        }
    }
}
if ($allOk) { Write-Host "  OK ($($manifest.Count) files verified)" -ForegroundColor Green }

# [P3] Lighthouse runs
Write-Host "[P3] Checking Lighthouse runs..." -ForegroundColor Yellow
$lh = (Get-ChildItem "dataset\lighthouse\lh-*.json" -ErrorAction SilentlyContinue).Count
if ($lh -lt 9) { Write-Host "  FAIL: only $lh lighthouse runs found (expected >= 9)" -ForegroundColor Red; $failed++ } else { Write-Host "  $lh lighthouse runs found" -ForegroundColor Green }
Write-Host "[P3] Checking the 9 cited Lighthouse runs targeted the public URL..." -ForegroundColor Yellow
& $py scripts/check-lighthouse-url.py
if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: a cited Lighthouse run does not target the public URL" -ForegroundColor Red; $failed++ }

# [P8] SUS demographics
Write-Host "[P8] Checking SUS demographics script..." -ForegroundColor Yellow
& $py scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv | Out-Null
if ($LASTEXITCODE -eq 0) { Write-Host "  OK: SUS demographics script runs" -ForegroundColor Green } else { Write-Host "  FAIL: SUS demographics script did not run cleanly" -ForegroundColor Red; $failed++ }
$bashCandidates = @("bash", "C:\Program Files\Git\bin\bash.exe", "C:\Program Files (x86)\Git\bin\bash.exe")
$bashExe = $null
foreach ($candidate in $bashCandidates) {
    $cmd = Get-Command $candidate -ErrorAction SilentlyContinue
    if ($cmd) {
        # "bash" on Windows can resolve to the WSL relay, which fails at
        # runtime (not import time) if no WSL distro is installed -- probe
        # it, don't just trust Get-Command finding *a* bash.exe.
        & $cmd.Source --version *> $null
        if ($LASTEXITCODE -eq 0) { $bashExe = $cmd.Source; break }
    }
}
if ($bashExe) {
    & $bashExe scripts/validate-sus-demografia.sh
    if ($LASTEXITCODE -ne 0) { Write-Host "  FAIL: SUS demographics table does not match dataset/sus/sus-raw.csv" -ForegroundColor Red; $failed++ }
} else {
    Write-Host "  SKIP: no working bash found (tried PATH and Git for Windows) to run scripts/validate-sus-demografia.sh" -ForegroundColor Yellow
}

# [P11] SUS instrument and consent
Write-Host "[P11] Checking SUS instrument and consent..." -ForegroundColor Yellow
$p11files = @("dataset\sus\SUS-INSTRUMENT.md", "dataset\sus\CONSENT-FORM.md", "dataset\sus\CONSENT-REGISTRY.md")
foreach ($f in $p11files) {
    if (Test-Path $f) { Write-Host "  $f exists" -ForegroundColor Green } else { Write-Host "  MISSING: $f" -ForegroundColor Red; $failed++ }
}

# [P9] Postman collection
Write-Host "[P9] Checking Postman collection..." -ForegroundColor Yellow
$p9 = (Select-String -Path "docs\postman\coleccion.json" -Pattern "asignaciones" -CaseSensitive).Count
Write-Host "  $p9 assignment endpoints found" -ForegroundColor Green

Write-Host ""
if ($failed -eq 0) {
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host "ALL CHECKS PASSED" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host "==========================================" -ForegroundColor Red
    Write-Host "FAILED: $failed check(s) failed" -ForegroundColor Red
    Write-Host "==========================================" -ForegroundColor Red
    exit 1
}
