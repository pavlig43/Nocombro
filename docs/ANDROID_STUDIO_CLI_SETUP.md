# Android Studio CLI setup after Windows reinstall

## Goal

Set up Android CLI and Android Studio semantic tools so this command shows `READY` for this repo:

```powershell
android studio check
```

Expected result:

```text
pid: ...
version: Quail 1 | 2026.1.1 Patch 2
Projects:
    READY     Nocombro  C:/Users/user/AndroidStudioProjects/Nocombro
```

## What was done on the old system

1. Installed Android CLI from the official installer.
2. Added Android CLI to PATH.
3. Updated Android Studio from Panda 4 `2025.3.4 Patch 1` to Quail 1 `2026.1.1 Patch 2`.
4. Opened `Nocombro` in Android Studio and waited for project analysis to finish.
5. Verified Android Studio semantic tools:

```powershell
android studio check
android studio analyze-file --project=Nocombro rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/RootNocombroComponent.kt
```

`analyze-file` returned:

```text
No issues found!
```

## Install Android CLI

Run in PowerShell:

```powershell
$installer = Join-Path $env:TEMP "android-cli-install.cmd"
curl.exe -fsSL "https://dl.google.com/android/cli/latest/windows_x86_64/install.cmd" -o $installer
& $installer
```

The installer should put `android.exe` here:

```text
C:\Users\user\AppData\AndroidCLI\android.exe
```

It should also add this folder to the user PATH:

```text
C:\Users\user\AppData\AndroidCLI
```

If the current terminal still does not see `android`, restart PowerShell or run:

```powershell
$androidCli = Join-Path (Split-Path $env:APPDATA -Parent) "AndroidCLI"
$env:Path = "$androidCli;$env:Path"
```

Check:

```powershell
android --version
android update
```

## Install or update Android Studio

Install Android Studio Quail 1 or newer.

If Android Studio is already installed and shows an update like this:

```text
Panda 4 2025.3.4 Patch 1 -> Quail 1 2026.1.1 Patch 2
```

use the built-in `Update` button. Do not install Canary unless you explicitly want a preview build.

After update:

1. Restart Android Studio.
2. Open `C:\Users\user\AndroidStudioProjects\Nocombro`.
3. Wait until project analysis/indexing finishes.
4. Run:

```powershell
android studio check
```

## If `android studio check` is not READY

First check Android Studio version:

```powershell
Get-Content -Raw "$env:LOCALAPPDATA\Programs\Android Studio\product-info.json" |
    ConvertFrom-Json |
    Select-Object name,version,buildNumber,dataDirectoryName
```

If it is Panda `2025.3.x`, update to Quail.

If Android Studio is Quail but `check` still fails:

1. Make sure `Nocombro` is open in Android Studio.
2. Wait for indexing/project analysis to finish.
3. Restart Android Studio.
4. Run again:

```powershell
android studio check
```

## Useful commands

Use `--project=Nocombro` if Android CLI fails to auto-pick the project by path.

```powershell
android studio check
android studio find-declaration --short --project=Nocombro SYMBOL
android studio find-usages --short --project=Nocombro SYMBOL
android studio analyze-file --project=Nocombro PATH_TO_FILE.kt
android studio open-file --project=Nocombro PATH_TO_FILE.kt
android studio version-lookup --project=Nocombro agp kotlin gradle compose
```

For Compose Preview:

```powershell
android studio render-compose-preview --project=Nocombro --output-image-file=preview.png --print-semantics PATH_TO_FILE.kt PreviewFunctionName
```

## Repo rule

When `android studio check` shows `READY`, use Android Studio semantic tools first for Kotlin/Android symbols.

Use `rg` only if:

- `android studio check` is not `READY`;
- Android Studio is closed;
- searching plain text, TODO, resource names, or comments;
- Android Studio semantic command did not find a result.

Gradle remains the main build check:

```powershell
.\gradlew build
```
