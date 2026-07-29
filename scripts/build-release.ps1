[CmdletBinding()]
param([string]$Version = "1.2.0")

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot ".."))
$gradle = Join-Path $root "gradle-8.7\bin\gradle.bat"
$jdk = Join-Path $root ".jdk\jdk-17.0.19+10"
if (!(Test-Path $gradle)) { throw "Gradle 8.7 was not found at $gradle" }
if (!(Test-Path (Join-Path $jdk "bin\java.exe"))) { throw "JDK 17 was not found at $jdk" }

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
Push-Location $root
try {
    Write-Host "Building CodeQuest Academy Desktop Distributables (v$Version)..."
    & $gradle :shared:jvmTest :desktopApp:jvmTest :desktopApp:createDistributable --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Validation or packaging failed with exit code $LASTEXITCODE" }

    $app = Join-Path $root "desktopApp\build\compose\binaries\main\app\CodeQuestAcademy"
    if (!(Test-Path (Join-Path $app "CodeQuestAcademy.exe"))) { throw "The packaged Windows application executable was not produced." }

    Write-Host "Building Single EXE Installer (codequest-academy-setup.exe)..."
    $installerScript = Join-Path $root "scripts\create-single-exe-installer.ps1"
    & powershell -ExecutionPolicy Bypass -File "$installerScript" -Version "$Version"
    if ($LASTEXITCODE -ne 0) { throw "Single EXE installer creation failed with exit code $LASTEXITCODE" }

    $exeInstallerPath = Join-Path $root "public\installers\codequest-academy-setup.exe"
    if (!(Test-Path $exeInstallerPath)) { throw "The Single EXE installer was not found at $exeInstallerPath" }

    $file = Get-Item $exeInstallerPath
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $exeInstallerPath).Hash

    $manifest = [ordered]@{
        latestVersion = $Version
        releaseDate = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        windows = [ordered]@{
            enabled = $true
            label = "Download Single EXE Installer for Windows"
            url = "api/download?os=windows"
            fileName = "codequest-academy-setup.exe"
            minimumVersion = "Windows 10"
            architecture = "x64"
            sizeBytes = $file.Length
            sha256 = $hash
            distribution = "installer"
        }
        macos = [ordered]@{
            enabled = $true
            label = "Download for macOS"
            url = "api/download?os=macos"
            fileName = "CodeQuest-Academy-$Version.dmg"
            minimumVersion = "macOS 11+"
            architecture = "universal"
            sizeBytes = $file.Length
            sha256 = $hash
        }
        linux = [ordered]@{
            enabled = $true
            label = "Download for Linux"
            url = "api/download?os=linux"
            fileName = "CodeQuest-Academy-$Version.AppImage"
            minimumVersion = "Ubuntu 20.04+"
            architecture = "x64"
            sizeBytes = $file.Length
            sha256 = $hash
        }
    }

    $manifestPath = Join-Path $root "downloads.json"
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

    $releasesDir = Join-Path $root "releases"
    New-Item -ItemType Directory -Force -Path $releasesDir | Out-Null

    # Create latest.yml for auto-update client
    $latestYmlContent = @"
version: $Version
releaseDate: '$((Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ"))'
releaseName: 'Version $Version - Single EXE Installer'
path: codequest-academy-setup.exe
sha512: $hash
files:
  - url: codequest-academy-setup.exe
    sha512: $hash
    size: $($file.Length)
"@
    Set-Content -LiteralPath (Join-Path $releasesDir "latest.yml") -Value $latestYmlContent -Encoding UTF8
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $releasesDir "releases.json") -Encoding UTF8

    Write-Host "==================================================="
    Write-Host "Single EXE Installer Build Complete: $($file.FullName)"
    Write-Host "Version: $Version"
    Write-Host "Size: $($file.Length) bytes"
    Write-Host "SHA-256 Checksum: $hash"
    Write-Host "Releases directory updated with latest.yml and releases.json"
    Write-Host "==================================================="
} finally {
    Pop-Location
}
