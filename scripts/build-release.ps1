[CmdletBinding()]
param([string]$Version)

$ErrorActionPreference = "Stop"

function Get-FileDigest {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Algorithm
    )

    $hasher = [System.Security.Cryptography.HashAlgorithm]::Create($Algorithm)
    if ($null -eq $hasher) { throw "Unsupported hash algorithm: $Algorithm" }
    try {
        return ([System.BitConverter]::ToString($hasher.ComputeHash([System.IO.File]::ReadAllBytes($Path)))).Replace("-", "")
    } finally {
        $hasher.Dispose()
    }
}

$root = (Resolve-Path (Join-Path $PSScriptRoot ".."))
$versionProperty = Get-Content (Join-Path $root "gradle.properties") | Where-Object { $_ -match '^nous\.version=' } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($Version)) {
    if ($null -eq $versionProperty) { throw "nous.version is missing from gradle.properties" }
    $Version = ($versionProperty -split '=', 2)[1].Trim()
}
$gradle = Join-Path $root "gradle-8.7\bin\gradle.bat"
$jdk = Join-Path $root ".jdk\jdk-17.0.19+10"
if (!(Test-Path $gradle)) { throw "Gradle 8.7 was not found at $gradle" }
if (!(Test-Path (Join-Path $jdk "bin\java.exe"))) { throw "JDK 17 was not found at $jdk" }

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
Push-Location $root
try {
    Write-Host "Building Nous AI Academy Desktop Distributables (v$Version)..."
    & $gradle :shared:jvmTest :desktopApp:jvmTest :desktopApp:createDistributable --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Validation or packaging failed with exit code $LASTEXITCODE" }

    $app = Join-Path $root "desktopApp\build\compose\binaries\main\app\Nous-AI-Academy"
    if (!(Test-Path (Join-Path $app "Nous-AI-Academy.exe"))) { throw "The packaged Windows application executable was not produced." }

    Write-Host "Building Single EXE Installer (Nous-AI-Academy-Setup-$Version.exe)..."
    $installerScript = Join-Path $root "scripts\create-single-exe-installer.ps1"
    & powershell -ExecutionPolicy Bypass -File "$installerScript" -Version "$Version" -Publish
    if ($LASTEXITCODE -ne 0) { throw "Single EXE installer creation failed with exit code $LASTEXITCODE" }

    $exeInstallerPath = Join-Path $root "public\installers\Nous-AI-Academy-Setup-$Version.exe"
    if (!(Test-Path $exeInstallerPath)) { throw "The Single EXE installer was not found at $exeInstallerPath" }

    $file = Get-Item $exeInstallerPath
    $hash = Get-FileDigest -Path $exeInstallerPath -Algorithm "SHA256"
    $sha512 = Get-FileDigest -Path $exeInstallerPath -Algorithm "SHA512"

    $manifest = [ordered]@{
        latestVersion = $Version
        releaseDate = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        releaseName = "Nous AI Academy $Version"
        releaseNotes = "Nous AI Academy 1.6.1: verified offline library, secure local PDF reading, bookmarks, progress, search, and a per-user Windows installer."
        minimumVersion = "1.5.0"
        windows = [ordered]@{
            enabled = $true
            label = "Download Single EXE Installer for Windows"
            url = "api/download?os=windows"
            downloadUrl = "https://codequest-sage.vercel.app/installers/Nous-AI-Academy-Setup-$Version.exe"
            fileName = "Nous-AI-Academy-Setup-$Version.exe"
            minimumVersion = "Windows 10"
            architecture = "x64"
            sizeBytes = $file.Length
            sha256 = $hash
            sha512 = $sha512
            distribution = "installer"
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
path: Nous-AI-Academy-Setup-$Version.exe
sha512: $sha512
files:
  - url: Nous-AI-Academy-Setup-$Version.exe
    sha512: $sha512
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
