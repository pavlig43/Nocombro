param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$versionFile = Join-Path $projectRoot "gradle\libs.versions.toml"
$msiDir = Join-Path $projectRoot "app\desktopApp\build\compose\binaries\main-release\msi"
$outputDir = $PSScriptRoot

function Test-JpackageJdkHome {
    param([string]$JdkHome)

    if ([string]::IsNullOrWhiteSpace($JdkHome)) {
        return $false
    }

    return (Test-Path -LiteralPath (Join-Path $JdkHome "bin\java.exe") -PathType Leaf) -and
        (Test-Path -LiteralPath (Join-Path $JdkHome "bin\jpackage.exe") -PathType Leaf)
}

function Get-ProjectJavaMajor {
    $javaVersionMatch = Select-String -Path $versionFile -Pattern '^\s*java\s*=\s*"([^"]+)"' |
        Select-Object -First 1
    if ($null -ne $javaVersionMatch -and $javaVersionMatch.Matches.Count -gt 0) {
        return $javaVersionMatch.Matches[0].Groups[1].Value
    }
    return $null
}

function Resolve-JpackageJdkHome {
    $candidates = @($env:JAVA_HOME, $env:JDK_HOME)
    $jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($null -ne $jpackageCommand) {
        $candidates += Split-Path -Parent (Split-Path -Parent $jpackageCommand.Source)
    }

    $searchRoots = @(
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:USERPROFILE ".jdks")
    )
    foreach ($root in $searchRoots) {
        if (Test-Path -LiteralPath $root) {
            $candidates += Get-ChildItem -LiteralPath $root -Recurse -Filter "jpackage.exe" -ErrorAction SilentlyContinue |
                ForEach-Object { Split-Path -Parent (Split-Path -Parent $_.FullName) }
        }
    }

    $validCandidates = $candidates |
        Where-Object { Test-JpackageJdkHome $_ } |
        ForEach-Object { [System.IO.Path]::GetFullPath($_) } |
        Select-Object -Unique
    if ($null -eq $validCandidates) {
        return $null
    }

    $javaMajor = Get-ProjectJavaMajor
    if (![string]::IsNullOrWhiteSpace($javaMajor)) {
        $preferred = $validCandidates |
            Where-Object { (Split-Path -Leaf $_) -match "(^|[^0-9])$([regex]::Escape($javaMajor))([^0-9]|$)" } |
            Select-Object -First 1
        if ($null -ne $preferred) {
            return $preferred
        }
    }

    return $validCandidates | Select-Object -First 1
}

if (!(Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}

$jpackageJdkHome = Resolve-JpackageJdkHome
if ($null -eq $jpackageJdkHome) {
    throw "JDK with jpackage.exe not found. Install JDK 21 and rerun this script."
}

$env:JAVA_HOME = $jpackageJdkHome
$env:Path = "$(Join-Path $jpackageJdkHome "bin");$env:Path"
Write-Host "Using JDK: $jpackageJdkHome"

& $gradleWrapper "-Dorg.gradle.java.home=$jpackageJdkHome" :app:desktopApp:packageReleaseMsi
if ($LASTEXITCODE -ne 0) {
    throw "packageReleaseMsi failed with code $LASTEXITCODE"
}

$sourceMsi = Get-ChildItem -LiteralPath $msiDir -Filter "*.msi" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1
if ($null -eq $sourceMsi) {
    throw "MSI not found: $msiDir"
}

$versionMatch = Select-String -Path $versionFile -Pattern '^\s*versionName\s*=\s*"([^"]+)"' |
    Select-Object -First 1
$version = if ($null -ne $versionMatch -and $versionMatch.Matches.Count -gt 0) {
    $versionMatch.Matches[0].Groups[1].Value
} else {
    [System.IO.Path]::GetFileNameWithoutExtension($sourceMsi.Name)
}

New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

$targetPath = Join-Path $outputDir "Nocombro-$version.msi"
Copy-Item -LiteralPath $sourceMsi.FullName -Destination $targetPath -Force

$resolvedTargetPath = (Resolve-Path -LiteralPath $targetPath).Path
Write-Host "MSI: $resolvedTargetPath"
