$ErrorActionPreference = 'Stop'
function RuntimeHash([string]$Path) {
 $algorithm = [Security.Cryptography.SHA256]::Create()
 $stream = [IO.File]::OpenRead($Path)
 try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','') }
 finally { $stream.Dispose(); $algorithm.Dispose() }
}
$root = Split-Path $PSScriptRoot -Parent
$cache = Join-Path $root 'build/python-downloads'
$dest = Join-Path $root 'desktopApp/build/python-lab'
$expected = @{
 'python-3.11.9-embed-amd64.zip'='009D6BF7E3B2DDCA3D784FA09F90FE54336D5B60F0E0F305C37F400BF83CFD3B'
 'numpy-1.26.4-cp311-cp311-win_amd64.whl'='CD25BCECC4974D09257FFCD1F098EE778F7834C3AD767FE5DB785BE9A4AA9CB2'
 'pandas-2.2.3-cp311-cp311-win_amd64.whl'='3FC6873A41186404DAD67245896A6E440BAACC92F5B716CCD1BC9ED2995AB2C5'
 'python_dateutil-2.9.0.post0-py2.py3-none-any.whl'='A8B2BC7BFFAE282281C8140A97D3AA9C14DA0B136DFE83F850EEA9A5F7470427'
 'pytz-2026.3.post1-py2.py3-none-any.whl'='DD95840DD199BAEA12D9CC096A1D452CAA6596A1C1E4B5F3DBD1541855D5E815'
 'six-1.17.0-py2.py3-none-any.whl'='4721F391ED90541FDDACAB5ACF947AA0D3DC7D27B2E1E8EDA2BE8970586C3274'
 'tzdata-2026.3-py2.py3-none-any.whl'='DC096730C87AF6CAB1B171C9D532BE840741FF5D459015E7F6947BD7D7E54931'
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Validate all inputs before replacing the disposable build output.
New-Item -ItemType Directory -Force -Path $cache | Out-Null
foreach ($name in $expected.Keys) {
 $source = Join-Path $cache $name
 if (!(Test-Path -LiteralPath $source)) {
  # Build-time only. The installed application never fetches runtimes/packages.
  [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
  if ($name -eq 'python-3.11.9-embed-amd64.zip') {
   $url = 'https://www.python.org/ftp/python/3.11.9/python-3.11.9-embed-amd64.zip'
  } else {
   $parts = $name.Split('-')
   $metadata = Invoke-RestMethod -Uri ("https://pypi.org/pypi/{0}/{1}/json" -f $parts[0],$parts[1])
   $artifact = @($metadata.urls | Where-Object { $_.filename -eq $name })
   if ($artifact.Count -ne 1 -or $artifact[0].digests.sha256 -ne $expected[$name]) { throw "Pinned package metadata mismatch: $name" }
   $url = $artifact[0].url
   if (([Uri]$url).Scheme -ne 'https' -or ([Uri]$url).Host -ne 'files.pythonhosted.org') { throw 'Untrusted package host' }
  }
  Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $source
 }
 if (!(Test-Path $source) -or (RuntimeHash $source) -ne $expected[$name]) { throw "Missing or unverified runtime input: $name" }
}
$resolvedDest = [IO.Path]::GetFullPath($dest)
$expectedDest = [IO.Path]::GetFullPath((Join-Path $root 'desktopApp\build\python-lab'))
if ($resolvedDest -ne $expectedDest -or !$resolvedDest.StartsWith([IO.Path]::GetFullPath($root) + '\')) { throw 'Unsafe runtime build directory' }
if (Test-Path -LiteralPath $dest) {
 if ((Get-Item -LiteralPath $dest).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Runtime output cannot be a link' }
 Remove-Item -LiteralPath $resolvedDest -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dest | Out-Null
foreach ($name in $expected.Keys) {
 $source = Join-Path $cache $name
 if (!(Test-Path $source) -or (RuntimeHash $source) -ne $expected[$name]) { throw "Missing or unverified runtime input: $name" }
 $zip = [IO.Compression.ZipFile]::OpenRead($source)
 try { foreach ($entry in $zip.Entries) {
   $target = [IO.Path]::GetFullPath((Join-Path $dest $entry.FullName))
   if (!$target.StartsWith([IO.Path]::GetFullPath($dest) + [IO.Path]::DirectorySeparatorChar)) { throw 'Unsafe runtime path' }
   if ($entry.Name) { New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null; [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$true) }
 } } finally { $zip.Dispose() }
}
$brokerSource = Join-Path $root 'tools\python-lab\SandboxHost.cs'
$brokerOutput = Join-Path $dest 'NousPythonSandbox.exe'
& 'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe' /nologo /target:exe /platform:x64 "/out:$brokerOutput" $brokerSource
if ($LASTEXITCODE -ne 0) { throw 'Sandbox broker compilation failed' }
Copy-Item -LiteralPath "$root/tools/python-lab/bootstrap.py" -Destination "$dest/bootstrap.py" -Force
$index = Get-ChildItem -LiteralPath $dest -Recurse -File | Where-Object { $_.Name -ne 'files.sha256' } | ForEach-Object {
 $relative = $_.FullName.Substring($dest.Length + 1).Replace('\','/')
 (RuntimeHash $_.FullName).ToLowerInvariant() + '  ' + $relative
}
[IO.File]::WriteAllLines((Join-Path $dest 'files.sha256'),$index)
$resources = Join-Path $root 'desktopApp/build/generated/python-resources/python-lab'
New-Item -ItemType Directory -Force -Path $resources | Out-Null
$zipPath = Join-Path $resources 'runtime.zip'
if(Test-Path $zipPath) { Remove-Item -LiteralPath $zipPath }
[IO.Compression.ZipFile]::CreateFromDirectory($dest,$zipPath,[IO.Compression.CompressionLevel]::Optimal,$false)
[IO.File]::WriteAllText((Join-Path $resources 'runtime.sha256'),(RuntimeHash $zipPath).ToLowerInvariant())
Write-Host "Prepared verified Python runtime at $dest"
