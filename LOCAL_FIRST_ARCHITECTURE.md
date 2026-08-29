# CodeQuest Academy — Active Local-First Architecture

**Effective:** 2026-08-30
**Status:** Owner-directed implementation in progress
**Scope:** Compose Desktop only. No cloud account, Supabase, Cloud Run, paid API, remote database, secret, online analytics, or mandatory network service is part of this version.

## Source of truth

SQLite on the learner's device is the source of truth for profiles, passwords, progress, attempts, mastery, review queue, projects, bookmarks, notes, local analytics, backup history, content-pack metadata, legacy mappings, and downloaded content. The installed app bundles the complete supplied source package under desktop resources, including the 10,000-slot manifest, book plans/PDF blueprints, deep-dive source files, schemas, curriculum map, and design specifications.

The application works with networking disabled after installation. The public installer/update website is not required for a learner to open content or persist progress. The prior cloud documents are retained as archived future options only.

## Additive storage and migration

The existing `UserProfile`, `Track`, `Path`, `Level`, `NodeProgress`, `ActivityEvent`, `ProjectDraft`, `AssessmentAttempt`, and `SkillMastery` records remain unchanged. New `Ai*`, `LocalAnalyticsEvent`, `LocalBackupRecord`, `LegacyContentMapping`, and `AcademyContentPack` tables are additive and created with `IF NOT EXISTS`.

`AcademySearch` is a local SQLite FTS5 index over published lesson content, books, and knowledge metadata. It never sends a learner query outside the device. The content importer reads the bundled CSV manifest, verifies exactly 10,000 unique slots, creates them as `planned`, and upgrades only authored/validated slots to `published`. The initial pack publishes three real foundation lessons and three matching verified problems; it does not make a 10,000-problem completion claim.

## Offline flows

```text
Bundled package / local content pack
                 |
                 v
        Local Academy importer ----> SQLite content/version/search tables
                 |                              |
                 v                              v
      Compose Desktop screens <---- local learner profile, evidence, drafts
                 |
                 v
       Portable local backup export/import (next implementation batch)
```

Local event analytics are derived only from persisted activity, attempt, mastery, project, backup, and import events. Opening a screen does not earn XP, mastery, completion, or a fabricated count. Mastery is 0-100 per objective, with normal progression targeting 70 and checkpoints/projects requiring 80 plus the relevant mastery check. Reviews are scheduled locally at expanding intervals and shortened after errors or heavy hint use.

## Local content and publication policy

Content packs are versioned files supplied with the installer or selected by the learner from a local file. Each pack records ID, version, source hash, install time, and source paths. A future local content studio will author draft/review/published/archived versions in SQLite; until then, the bundled pack is the publisher. Legacy coding material remains in its existing tables and is not deleted. It can be hidden behind a local feature flag only after a mapping is recorded.

The five books and twenty knowledge deep dives remain locally packaged resources. Their entries, page plans, and source paths are persisted in SQLite. Viewing/search/bookmark/note import-export work is being delivered in staged local batches; no download is needed after installation.

## Execution safety

The existing `ProcessBuilder` runner is an unsafe host-process runner for untrusted learner code and is disabled by default in this release. The editor is honest about that limitation: source can be saved locally, but the product does not display a fabricated terminal, test result, or grade. A future executable Python feature must ship an offline no-network runtime with verified OS/process isolation, strict time/memory/PID/filesystem limits, package allow-lists, and a security review. It must not execute arbitrary learner code in the Compose application process.

## Backup and recovery

Before any content/schema migration, retain the original database and add migration journal/backup records. The next batch will add a portable, checksummed local backup format with export, import preview, restore to a copy, and explicit conflict handling. Import never overwrites a profile or progress record silently. A failed Academy content import leaves the prior committed SQLite state in place because installation runs as a transaction.

## Verification gates

- Database migration and manifest import tests: exactly 10,000 unique rows; published count reflects actual authored material only.
- Local persistence tests: profile, attempt, mastery, review queue, mistake notebook, and local analytics survive a database reopen.
- Backup export/import and legacy progress mapping tests before exposing the backup UI.
- SQLite FTS search tests across lesson/book/knowledge entries.
- Existing website and JVM tests, plus new navigation tests.
- A network-disabled manual test before a desktop release. No source may require a cloud URL, key, billing account, remote database, or external runtime.

## Current implementation batch

1. Complete package copied into desktop resources.
2. Additive local Academy schema and FTS index added.
3. Bundled importer installs the 10,000-slot manifest, 12 track definitions derived from it, five books, twenty deep dives, and the first three authored foundation lessons/problems.
4. Home, Learn, Practice, Labs, Books, and Knowledge navigation uses local records.
5. Host-process execution is feature-gated off.

The remaining work is intentionally staged: portable backups, full local readers/search surfaces, content-studio validation, more reviewed content batches, and a security-reviewed local execution runtime.
