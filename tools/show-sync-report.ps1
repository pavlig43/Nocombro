param(
    [string]$DatabasePath = (Join-Path $env:APPDATA "Nocombro\nocombro.db"),
    [string]$ReportDirectory = (Join-Path $env:APPDATA "Nocombro\sync-reports"),
    [switch]$NoOpen
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$sourceDatabase = (Resolve-Path -LiteralPath $DatabasePath).Path
$temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$workDirectory = Join-Path $temporaryRoot ("nocombro-sync-report-" + [Guid]::NewGuid().ToString("N"))
# До создания: обычный пользователь владеет TEMP; проверяем доступность его ACL.
Get-Acl -LiteralPath $temporaryRoot -ErrorAction Stop | Out-Null
$expectedWorkDirectory = [IO.Path]::GetFullPath($workDirectory)
if (![IO.Path]::GetDirectoryName($expectedWorkDirectory).Equals($temporaryRoot.TrimEnd('\'), [StringComparison]::OrdinalIgnoreCase)) {
    throw "Unexpected temporary directory"
}
New-Item -ItemType Directory -Path $workDirectory -ErrorAction Stop | Out-Null
try {
    $backupPath = Join-Path $workDirectory "snapshot.db"
    $backupScript = @'
import pathlib, sqlite3, sys
source = pathlib.Path(sys.argv[1]).resolve().as_uri() + '?mode=ro'
with sqlite3.connect(source, uri=True) as original, sqlite3.connect(sys.argv[2]) as backup:
    original.backup(backup)
'@
    & python -c $backupScript $sourceDatabase $backupPath
    if ($LASTEXITCODE -ne 0) { throw "SQLite backup failed" }
    $projectDirectory = Split-Path -Parent $PSScriptRoot
    & (Join-Path $projectDirectory "gradlew.bat") -p $projectDirectory :database:syncAnalysisReport `
        "-PsyncReportDb=$backupPath" "-PsyncReportDirectory=$ReportDirectory" --console=plain --no-configuration-cache
    if ($LASTEXITCODE -ne 0) { throw "Sync report generation failed" }
    $html = Join-Path $ReportDirectory "latest-sync-analysis.html"
    if (!$NoOpen) { Start-Process -FilePath ([Uri]([IO.Path]::GetFullPath($html))).AbsoluteUri }
    Write-Output $html
} finally {
    if (Test-Path -LiteralPath $workDirectory) {
        $resolvedWorkDirectory = (Resolve-Path -LiteralPath $workDirectory).Path
        if ($resolvedWorkDirectory -ne $expectedWorkDirectory) { throw "Refusing to delete unexpected directory" }
        $unexpected = Get-ChildItem -LiteralPath $resolvedWorkDirectory -Recurse | Where-Object {
            $_.PSIsContainer -or $_.Name -notin @("snapshot.db", "snapshot.db-wal", "snapshot.db-shm", "snapshot.db-journal", "snapshot.db.lck")
        }
        if ($unexpected) { throw "Unexpected temporary files; refusing recursive deletion" }
        Remove-Item -LiteralPath $resolvedWorkDirectory -Recurse -ErrorAction Stop
    }
}
