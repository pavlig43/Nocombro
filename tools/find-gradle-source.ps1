param(
    [Parameter(Mandatory = $true)]
    [string]$ClassName,

    [string]$GradleCacheRoot = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1",

    [string]$Group,

    [string]$Artifact,

    [switch]$ShowContent
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $GradleCacheRoot)) {
    throw "Gradle cache not found: $GradleCacheRoot"
}

$classPath = ($ClassName -replace '\.', '/')
$candidateEntries = @(
    "$classPath.kt",
    "$classPath.java"
)

$searchRoots = if ($Group -and $Artifact) {
    $groupPath = Join-Path $GradleCacheRoot $Group
    $artifactPath = Join-Path $groupPath $Artifact
    if (-not (Test-Path -LiteralPath $artifactPath)) {
        throw "Artifact path not found: $artifactPath"
    }
    @($artifactPath)
} elseif ($Group) {
    $groupPath = Join-Path $GradleCacheRoot $Group
    if (-not (Test-Path -LiteralPath $groupPath)) {
        throw "Group path not found: $groupPath"
    }
    @($groupPath)
} else {
    @($GradleCacheRoot)
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$versionPattern = [regex]'[\\/][^\\/]+[\\/](?<version>\d+(?:\.\d+)+(?:[-A-Za-z0-9.]*)?)[\\/]'

$archives =
    Get-ChildItem -Path $searchRoots -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Extension -eq ".jar" -and
        (
            $_.Name -like "*-sources.jar" -or
            $_.Name -notlike "*-javadoc.jar"
        )
    } |
    Select-Object *, @{
        Name = "VersionText"
        Expression = {
            $match = $versionPattern.Match($_.FullName)
            if ($match.Success) { $match.Groups["version"].Value } else { "" }
        }
    }, @{
        Name = "VersionSortKey"
        Expression = {
            $match = $versionPattern.Match($_.FullName)
            if (-not $match.Success) {
                return [version]"0.0.0"
            }

            $versionText = $match.Groups["version"].Value
            $numericPrefix = ([regex]::Match($versionText, '^\d+(?:\.\d+)+')).Value
            if ([string]::IsNullOrWhiteSpace($numericPrefix)) {
                return [version]"0.0.0"
            }

            try {
                return [version]$numericPrefix
            }
            catch {
                return [version]"0.0.0"
            }
        }
    } |
    Sort-Object @{
        Expression = { if ($_.Name -like "*-sources.jar") { 0 } else { 1 } }
    }, @{
        Expression = { $_.VersionSortKey }
        Descending = $true
    }, @{
        Expression = { $_.FullName }
    }

foreach ($archive in $archives) {
    $zip = $null
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($archive.FullName)
        $entry = $zip.Entries | Where-Object {
            $fullName = $_.FullName
            foreach ($candidate in $candidateEntries) {
                if ($fullName -like "*/$candidate" -or $fullName -eq $candidate) {
                    return $true
                }
            }
            return $false
        } | Select-Object -First 1

        if ($null -ne $entry) {
            Write-Output "Archive: $($archive.FullName)"
            Write-Output "Entry: $($entry.FullName)"
            if ($ShowContent) {
                Write-Output ""
                $reader = New-Object System.IO.StreamReader($entry.Open())
                try {
                    Write-Output ($reader.ReadToEnd())
                }
                finally {
                    $reader.Dispose()
                }
            }
            return
        }
    }
    finally {
        if ($null -ne $zip) {
            $zip.Dispose()
        }
    }
}

throw "Source not found for class: $ClassName"
