param(
    [string]$OutputDirectory = (Join-Path $env:APPDATA "Nocombro")
)

$ErrorActionPreference = "Stop"

$keystorePath = Join-Path $OutputDirectory "nocombro-mobile-release.jks"
$propertiesPath = Join-Path $OutputDirectory "mobile-signing.properties"
$keyAlias = "nocombro-mobile"

if (Test-Path -LiteralPath $keystorePath) {
    throw "Signing key already exists: $keystorePath"
}
if (Test-Path -LiteralPath $propertiesPath) {
    throw "Signing properties already exist: $propertiesPath"
}

$keytool = Get-Command keytool -ErrorAction Stop
$passwordBytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$random.GetBytes($passwordBytes)
$random.Dispose()
$password = ([BitConverter]::ToString($passwordBytes)).Replace("-", "").ToLowerInvariant()

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

try {
    $env:NOCOMBRO_MOBILE_KEYSTORE_PASSWORD = $password
    & $keytool.Source `
        -genkeypair `
        -keystore $keystorePath `
        -storetype PKCS12 `
        -storepass:env NOCOMBRO_MOBILE_KEYSTORE_PASSWORD `
        -keypass:env NOCOMBRO_MOBILE_KEYSTORE_PASSWORD `
        -alias $keyAlias `
        -keyalg RSA `
        -keysize 3072 `
        -validity 10000 `
        -dname "CN=Nocombro, OU=Mobile, O=Pavlig43, L=Moscow, C=RU"

    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed with exit code $LASTEXITCODE"
    }

    $escapedStorePath = $keystorePath.Replace("\", "/")
    $properties = @(
        "storeFile=$escapedStorePath"
        "storePassword=$password"
        "keyAlias=$keyAlias"
        "keyPassword=$password"
    ) -join [Environment]::NewLine
    [System.IO.File]::WriteAllText($propertiesPath, "$properties$([Environment]::NewLine)")
}
catch {
    if (Test-Path -LiteralPath $keystorePath) {
        Remove-Item -LiteralPath $keystorePath -Force
    }
    if (Test-Path -LiteralPath $propertiesPath) {
        Remove-Item -LiteralPath $propertiesPath -Force
    }
    throw
}
finally {
    Remove-Item Env:NOCOMBRO_MOBILE_KEYSTORE_PASSWORD -ErrorAction SilentlyContinue
    [Array]::Clear($passwordBytes, 0, $passwordBytes.Length)
    $password = $null
}

Write-Output "Created signing key: $keystorePath"
Write-Output "Created signing properties: $propertiesPath"
Write-Output "Back up both files. Future APK updates require this exact key."
