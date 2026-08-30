# Nous AI Academy cleanup report

Rollback checkpoint: `bfeb761` (`Checkpoint before Nous curriculum cleanup`).

## Removed from the repository

- The archived transformation package source tree and both unpacked legacy curriculum trees.
- The legacy curriculum ZIP stored in the repository.
- Five bundled book PDFs, twenty bundled intensive-file PDFs, and their twenty-five cover images.
- The old document handler, offline PDF reader, library screens, curriculum loading screen, coding/editor screens, track/path/lesson/practice/review/project screens, and their curriculum/PDF tests.
- The direct local code runner and its execution implementation.
- Legacy CodeQuest icon files from the public website and desktop resource locations.
- Historical CodeQuest documentation, build manifests, development reports, and all locally generated CodeQuest installers and portable archives.
- Unreachable curriculum models, track UI components, and curriculum-specific view models.

## Active-data migration

At first startup, `LocalAcademyStore.prepareEmptyLibrary()` records existing catalogue metadata in `LegacyLibraryArchive`, removes active book/file/search entries, and leaves profiles, settings, active sessions, bookmarks, notes, highlights, and reader metadata untouched. No current or previous curriculum package is imported.

## Deliberate compatibility exceptions

- The local database migration reads the historical `.codequest-academy/codequest_progress.db` path exactly once only to copy existing learner data into `.nous-ai-academy/nous_ai_academy.db` when the new file does not exist.
- The old installer path remains an endpoint alias only; it never exposes a file and returns the same safe unpublished-installer response until a verified Nous installer is released.
- Kotlin package namespaces and the existing SQLDelight database namespace remain unchanged in this cleanup commit to preserve binary/database compatibility. They are implementation identifiers, not visible product branding.

## Verification status

- The website server test suite passes: 7 tests, 0 failures.
- Desktop compilation, desktop tests, and installer packaging require a JDK. This workspace currently has no `java` executable and no configured `JAVA_HOME`, so those checks were not represented as passed and no installer was generated.
- Vercel configuration was not changed because the official CLI login flow timed out before authorization. No project rename, custom domain, redirect, or deployment was claimed.
