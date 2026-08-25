# Upgrade Readiness Review

Reviewed: 2026-08-24  
Scope: Kotlin/Compose desktop app, SQLDelight schema/repository, Node distribution server, landing page JavaScript, and release/install scripts.

## Current baseline

- Kotlin `1.9.23`, Compose Multiplatform `1.6.1`, SQLDelight `2.0.2`, Gradle `8.7`, JDK `17.0.19`.
- The checked Kotlin suite passes: 11 tests, 0 failures.
- `:desktopApp:compileKotlinJvm` completes successfully.
- The app, site, and server declare release `1.2.0`, but the desktop updater reports `1.0.0`.

## Remediation status (2026-08-25)

All immediate blockers identified in this review have been addressed in the codebase. The historical details below are retained as the rationale for the implementation and as regression targets. Automatic installer replacement remains intentionally disabled until a signed, rollback-capable updater is implemented.

## Remediated blockers

1. **Do not deploy the current static-file server as-is.**
   `server/server.js` serves any existing file below the repository root. A request such as `/server/server.js` exposes source code; `/codequest_progress.db` can expose local learner data when present. Serve from a dedicated public directory and enforce a resolved-path containment check.

2. **Make the update contract real and single-sourced.**
   `AutoUpdateManager.currentVersion` is hard-coded to `1.0.0`, while the release pipeline and site use `1.2.0`. The server also compares versions using string inequality. Generate the application version, release manifest, web display, and server response from one release input and compare semantic versions on both sides.

3. **Replace the updater install path before enabling automatic updates.**
   The updater downloads the Windows installer/EXE, then copies it to `updated_app.zip` and restarts the JVM classpath; it never installs or atomically replaces the application. Define a platform-specific updater helper with verified artifact type, staging, rollback, exit/wait, install, and restart behavior.

4. **Correct checksum metadata.**
   `latest.yml` calls a SHA-256 value `sha512` in both `server/server.js` and `scripts/build-release.ps1`. This is a false integrity claim. Either generate the actual SHA-512 hash for that field or use an explicitly named SHA-256 field in a documented custom manifest.

5. **Publish only artifacts actually built and signed.**
   The release scripts advertise macOS `.dmg` and Linux `.AppImage` files, but the project packaging config targets `.dmg`, `.exe`, and `.deb`; the release script packages only Windows. The server contains placeholder hashes for macOS/Linux. Disable unsupported OS downloads until their actual artifacts, checksums, signing, and installation flows exist.

6. **Turn database migrations into versioned, verified migrations.**
   SQLDelight migration verification is disabled and `ProgressRepository` applies DDL at runtime while swallowing failures. Move schema changes to ordered migration files, enable verification, back up the user database before a migration, and add upgrade tests from supported historical schemas.

7. **Secure or remove demonstration server endpoints.**
   `/api/admin/broadcast-update` has no authentication, `/api/analytics/dashboard` leaks operational data, CORS permits any origin, and JSON bodies have no size limit. Gate administration with server-side authentication/authorization, apply request-size limits and rate limits, and restrict CORS to known origins.

## High-priority hardening

- Drain `CodeRunner` stdout/stderr concurrently and cap output sizes. It waits for process completion before reading either stream, so a child that fills an OS pipe can block until timeout. Add resource limits or isolate execution before running untrusted learner code in production.
- Encode telemetry with the Kotlin JSON serializer rather than interpolating strings into JSON in `ApiClient.logUpdateStatus`; quotes/newlines in an error message can make invalid JSON.
- Use HTTPS/WSS production endpoints supplied by configuration, not `localhost` defaults compiled into `ApiClient`, `WsNotificationClient`, and logging settings.
- Replace global coroutine scopes with application-owned scopes and cancel them at shutdown; add WebSocket reconnect/backoff and proper close handling.
- Add an artifact-signing policy. A hash fetched from the same unauthenticated update channel does not provide strong origin authenticity.

## Upgrade sequence

1. Freeze a release contract: app version, supported OSes, manifest schema, hash algorithm, signing, download URL rules, and rollback behavior.
2. Repair the server boundary and admin endpoints; add HTTP integration tests for allowed static files, traversal/source/database denial, ranges, manifests, and authorization.
3. Introduce versioned database migrations plus fixtures for pre-upgrade databases; enable SQLDelight migration verification.
4. Rebuild the updater around signed platform artifacts and test update/rollback in a clean VM for every supported OS.
5. Centralize dependency versions (for example, a Gradle version catalog), upgrade one compatibility layer at a time, then run the full desktop, release, and installer smoke tests.
6. Add CI gates for Kotlin tests, desktop compilation, curriculum validation, JavaScript syntax checks, dependency audit, manifest/artifact consistency, and installer checksum verification.

## Review notes

- Local password storage is a positive baseline: PBKDF2-HMAC-SHA256 with a random salt and constant-time comparison is implemented and covered by tests.
- The existing test suite covers curriculum parsing, progress persistence, and local accounts; it does not cover updater behavior, API/server authorization, installer behavior, upgrade migrations, or code-runner resource limits.
