# Local authentication validation

Validation covers the local account path: field validation, normalized email uniqueness, PBKDF2
hashing with unique salts, generic invalid-credential messaging, active-session persistence,
sign-out preservation, profile isolation, password changes, and legacy credential attachment.

The JVM test suite is run with:

```powershell
.\gradle-8.7\bin\gradle.bat :shared:jvmTest --no-daemon --console=plain
.\gradle-8.7\bin\gradle.bat :desktopApp:jvmTest --no-daemon --console=plain
```

No external provider, browser callback, token, or client identifier is involved in this flow.
