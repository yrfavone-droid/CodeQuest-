# Development and Upgrade Roadmap

## Healthy baseline now

- The distribution server exposes only the landing-page allowlist and public assets; source code, local databases, and arbitrary repository files return `404`.
- Release data is read from `downloads.json`; only the Windows installer is advertised until another signed platform artifact exists.
- The central application release version is `codequest.version` in `gradle.properties`. Desktop packaging receives it as a JVM property and package version.
- Release generation calculates real SHA-256 and SHA-512 values. The broken automatic installer replacement path has been removed; updates are manual installer downloads until a signed updater exists.
- Prototype-schema compatibility migration checks the actual SQLite columns and fails visibly if it cannot finish, rather than swallowing a migration error.
- The code runner is disabled by default and drains both process streams with an output cap when explicitly enabled.

## Next development increments

1. **Signed update system:** select a Windows signing certificate and an updater framework/helper that supports signed artifacts, staged installation, rollback, and clean-VM end-to-end tests. Do not re-enable automatic installation before this is complete.
2. **Database migrations:** replace the remaining prototype compatibility migration with numbered SQLDelight migration files. Add preserved database fixtures for every released schema and enable migration verification.
3. **Execution sandbox:** run learner code in an OS/container sandbox with CPU, memory, filesystem, and network limits. Keep `codequest.enableCodeRunner=false` in production until this exists.
4. **Configuration:** inject production API and WebSocket URLs during packaging (`codequest.apiBaseUrl`, `codequest.wsUrl`); enable WebSocket push only with authenticated clients and reconnect/backoff tests.
5. **CI/release gates:** run Kotlin tests, desktop compilation, server tests, curriculum validation, manifest generation, checksum verification, and installer smoke tests for each release.
6. **Dependency upgrades:** after the gates are in place, update Gradle/Kotlin/Compose/SQLDelight one compatibility layer per pull request; rebuild the installer and execute the migration/update test matrix after each layer.

## Release checklist

- Bump only `codequest.version` in `gradle.properties`.
- Build the Windows installer, then run `npm run release` to generate the manifest.
- Verify the published installer checksum, Authenticode signature, version API, range download, and a clean-machine install.
- Keep macOS and Linux disabled until their installers, signing, manifests, and clean-machine tests are available.
