# Desktop build and release

Use the bundled JDK 17 at `.jdk/jdk-17.0.19+10` and Gradle 8.7:

```powershell
$env:JAVA_HOME=(Resolve-Path '.jdk\\jdk-17.0.19+10').Path
$env:PATH="$env:JAVA_HOME\\bin;$env:PATH"
.\\gradle-8.7\\bin\\gradle.bat :shared:jvmTest :desktopApp:jvmTest :desktopApp:createDistributable --no-daemon --console=plain
```

The distributable is written to `desktopApp/build/compose/binaries/main/app/CodeQuestAcademy`.
`scripts/build-release.ps1` repeats validation, creates the portable Windows zip in `downloads/`,
and updates `downloads.json` with measured size and SHA-256. The current verified artifact is
`downloads/CodeQuest-Academy-1.0.0-portable.zip` (75,070,519 bytes; SHA-256
`32F29C391B11F29A17F51A7122E0578269984D4527940012365C189A50C4D8B6`).

The WiX MSI/EXE task is attempted by Compose packaging but currently fails with `light.exe` exit
code 216 (tool architecture mismatch). The portable image launches independently; an installer
install/launch success is not claimed until a matching WiX toolchain is available.
