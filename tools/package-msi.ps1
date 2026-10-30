param(
    [switch]$NoPause
)

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

$exitCode = 0
$stage = "GitHub"
$githubStatus = "проверка не выполнена"
$msiStatus = "сборка не выполнялась"

try {
    Write-Host "[1/2] GitHub: проверка и получение изменений..."
    $gitCommand = Get-Command git -CommandType Application -ErrorAction Stop | Select-Object -First 1
    $headBefore = & $gitCommand.Source -C $projectRoot rev-parse HEAD
    if ($LASTEXITCODE -ne 0) {
        throw "git rev-parse HEAD failed with code $LASTEXITCODE"
    }

    & $gitCommand.Source -C $projectRoot pull --ff-only --no-rebase --no-autostash
    if ($LASTEXITCODE -ne 0) {
        throw "git pull failed with code $LASTEXITCODE"
    }

    $headAfter = & $gitCommand.Source -C $projectRoot rev-parse HEAD
    if ($LASTEXITCODE -ne 0) {
        throw "git rev-parse HEAD failed with code $LASTEXITCODE"
    }

    if ($headBefore -eq $headAfter) {
        $githubStatus = "OK, проект уже обновлён"
    } else {
        $githubStatus = "OK, изменения подтянуты"
    }
    Write-Host "GitHub: $githubStatus"

    $stage = "MSI"
    Write-Host "[2/2] MSI: сборка установщика..."

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

    & $gradleWrapper "-Dorg.gradle.java.home=$jpackageJdkHome" --project-dir $projectRoot :app:desktopApp:packageReleaseMsi
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
    $msiStatus = "OK, установщик собран: $resolvedTargetPath"
} catch {
    $exitCode = 1
    if ($stage -eq "GitHub") {
        $githubStatus = "ОШИБКА получения изменений"
    } else {
        $msiStatus = "ОШИБКА сборки установщика"
    }
    Write-Host "Ошибка: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Write-Host ""
    Write-Host "GitHub: $githubStatus"
    Write-Host "MSI: $msiStatus"
    if ($exitCode -eq 0) {
        Write-Host "Всё успешно: GitHub проверен, MSI собран." -ForegroundColor Green
    } else {
        Write-Host "Выполнение остановлено. Причина ошибки указана выше." -ForegroundColor Red
    }
    if (!$NoPause) {
        Read-Host "Нажмите Enter, чтобы закрыть окно" | Out-Null
    }
}

exit $exitCode
