param([string]$Runtime = (Join-Path (Split-Path $PSScriptRoot -Parent) 'desktopApp/build/python-lab'), [switch]$PreparedRuntime)
$ErrorActionPreference = 'Stop'
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('NousLabTests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$isolatedRuntime = Join-Path $testRoot 'runtime'
if ($PreparedRuntime) { $isolatedRuntime = [IO.Path]::GetFullPath($Runtime) } else { Copy-Item -LiteralPath $Runtime -Destination $isolatedRuntime -Recurse }
$sentinel = Join-Path $testRoot 'private.txt'
[IO.File]::WriteAllText($sentinel,'private test data')
$cases = @(
 @{name='python-numpy-pandas'; source='import numpy as np, pandas as pd; print(np.array([1,2,3]).sum()); print(pd.Series([2,4]).mean())'; status='success'; output="6`n3.0"},
 @{name='syntax';source='if :';status='syntax_error'},
 @{name='runtime';source='1/0';status='runtime_error'},
 @{name='stdout-limit';source='print("x" * 70000)';status='runtime_error'},
 @{name='timeout';source='import time; time.sleep(60)';broker='Timeout'},
 @{name='stop';source='import time; time.sleep(60)';broker='Stopped';stop=$true},
 @{name='memory';source='a = bytearray(800 * 1024 * 1024)';status='runtime_error'},
 @{name='workspace';source='from pathlib import Path; Path("hello.txt").write_text("local"); print(Path("hello.txt").read_text())';status='success';output='local'},
 @{name='network';source="import socket`ntry:`n socket.create_connection(('1.1.1.1',443),timeout=1)`nexcept OSError:`n print('DENIED')`nelse:`n raise AssertionError('network allowed')";status='success';output='DENIED'},
 @{name='private-file';source="from pathlib import Path`ntry:`n Path(r'$sentinel').read_text()`nexcept PermissionError:`n print('DENIED')`nelse:`n raise AssertionError('private file readable')";status='success';output='DENIED'},
 @{name='runtime-write';source="from pathlib import Path`ntry:`n Path(r'$isolatedRuntime/forbidden.txt').write_text('bad')`nexcept PermissionError:`n print('DENIED')`nelse:`n raise AssertionError('runtime writable')";status='success';output='DENIED'},
 @{name='child-process';source="import subprocess,sys`ntry:`n subprocess.run([sys.executable,'-c','print(1)'],check=True)`nexcept (OSError,subprocess.SubprocessError):`n print('DENIED')`nelse:`n raise AssertionError('child process allowed')";status='success';output='DENIED'},
 @{name='test-pass';source='answer=42';status='success';tests=@(@{name='answer';code='assert answer == 42';hidden=$false})},
 @{name='test-fail';source='answer=0';status='test_failure';tests=@(@{name='answer';code='assert answer == 42';hidden=$false})}
)
$results = @()
foreach($case in $cases) {
 $work = Join-Path $testRoot $case.name
 New-Item -ItemType Directory -Path $work | Out-Null
 Copy-Item -LiteralPath (Join-Path $isolatedRuntime 'bootstrap.py') -Destination $work
 $request=@{source=$case.source;stdin='';tests=@()}
 if($case.tests) {$request.tests=$case.tests}
 [IO.File]::WriteAllText((Join-Path $work 'request.json'),($request | ConvertTo-Json -Depth 8))
 if ($case.stop) {
  $runner = Start-Process -FilePath (Join-Path $isolatedRuntime 'NousPythonSandbox.exe') -ArgumentList ('"'+$isolatedRuntime+'" "'+$work+'"') -WindowStyle Hidden -PassThru
  [IO.File]::WriteAllText((Join-Path $work 'cancel'),'stop')
  if (!$runner.WaitForExit(15000)) { $runner.Kill(); throw 'Stop did not terminate the isolated run' }
  $code=$runner.ExitCode
 } else {
  & (Join-Path $isolatedRuntime 'NousPythonSandbox.exe') $isolatedRuntime $work
  $code=$LASTEXITCODE
 }
 if($case.broker) {
  $actual=Get-Content -LiteralPath (Join-Path $work 'broker-error.txt') -Raw
  if($actual -notmatch $case.broker) {throw "$($case.name): unexpected broker result $actual"}
 } else {
  if($code -ne 0) {throw "$($case.name): broker exit $code"}
  $actual=Get-Content -LiteralPath (Join-Path $work 'result.json') -Raw | ConvertFrom-Json
  if($actual.status -ne $case.status) {throw "$($case.name): $($actual | ConvertTo-Json -Depth 8 -Compress)"}
  if($case.output -and $actual.output.Trim() -ne $case.output) {throw "$($case.name): wrong output"}
 }
 $results += [pscustomobject]@{test=$case.name;passed=$true}
 Write-Host "PASS $($case.name)"
}
$results | ConvertTo-Json | Out-File (Join-Path $testRoot 'results.json') -Encoding utf8
Write-Host "Evidence: $testRoot"
