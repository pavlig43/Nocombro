# PowerShell pre-commit hook for Windows

$ErrorActionPreference = "Stop"

Write-Host "Running pre-commit hooks..." -ForegroundColor Cyan

# Get list of staged Kotlin/Gradle files
$stagedFiles = git diff --cached --name-only --diff-filter=ACM | Select-String -Pattern '\.(kt|kts|gradle)$' | Out-String
$stagedFiles = $stagedFiles.Trim()

if ([string]::IsNullOrEmpty($stagedFiles)) {
    Write-Host "No Kotlin/Gradle files staged. Skipping checks." -ForegroundColor Green
    exit 0
}

Write-Host "Staged files:" -ForegroundColor Yellow
Write-Host $stagedFiles
Write-Host ""

# Run Detekt (autoCorrect is enabled in config)
Write-Host "Running Detekt (formatting + check)..." -ForegroundColor Yellow
& "./gradlew.bat" "detektAll", "--continue"
$detektExitCode = $LASTEXITCODE

# Check if any files were modified by detekt
$modifiedFiles = git diff --name-only | Out-String
$modifiedFiles = $modifiedFiles.Trim()

if (-not [string]::IsNullOrEmpty($modifiedFiles)) {
    Write-Host ""
    Write-Host "Detekt made changes to your code!" -ForegroundColor Red
    Write-Host "Modified files:" -ForegroundColor Yellow
    Write-Host $modifiedFiles
    Write-Host ""
    Write-Host "Please review the changes and stage them again:" -ForegroundColor Cyan
    Write-Host "  git add " + $modifiedFiles
    Write-Host ""
    Write-Host "Then run git commit again." -ForegroundColor Cyan
    exit 1
}

if ($detektExitCode -ne 0) {
    Write-Host ""
    Write-Host "Detekt found issues in your code!" -ForegroundColor Red
    Write-Host "Please fix the issues and try again." -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "Pre-commit checks passed!" -ForegroundColor Green
exit 0
