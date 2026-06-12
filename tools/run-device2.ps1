param(
    [switch]$Reset
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$deviceRoot = Join-Path $env:USERPROFILE "NocombroDevice2"
$deviceAppData = Join-Path $deviceRoot "Nocombro"
$primaryAppData = Join-Path $env:APPDATA "Nocombro"

if ($Reset -and (Test-Path -LiteralPath $deviceRoot)) {
    $resolvedDeviceRoot = (Resolve-Path -LiteralPath $deviceRoot).Path
    $expectedDeviceRoot = [System.IO.Path]::GetFullPath(
        (Join-Path $env:USERPROFILE "NocombroDevice2")
    )
    if ($resolvedDeviceRoot -ne $expectedDeviceRoot) {
        throw "Refusing to reset unexpected path: $resolvedDeviceRoot"
    }
    Remove-Item -LiteralPath $resolvedDeviceRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $deviceAppData -Force | Out-Null

foreach ($configName in @("s3.properties", "ydb-sa-key.json")) {
    $source = Join-Path $primaryAppData $configName
    $target = Join-Path $deviceAppData $configName
    if (!(Test-Path -LiteralPath $target)) {
        if (!(Test-Path -LiteralPath $source)) {
            throw "Missing primary configuration file: $source"
        }
        Copy-Item -LiteralPath $source -Destination $target
    }
}

$env:APPDATA = $deviceRoot
$env:NOCOMBRO_YDB_JDBC_URL =
    "jdbc:ydb:grpcs://ydb.serverless.yandexcloud.net:2135/?database=/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73"

Write-Host "Starting Nocombro Device2"
Write-Host "App data: $deviceAppData"

& (Join-Path $projectRoot "gradlew.bat") :app:desktopApp:run
