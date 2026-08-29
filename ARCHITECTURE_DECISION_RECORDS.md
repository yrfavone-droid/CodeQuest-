# Archived cloud-first option — do not implement in the local-first release

The owner superseded this cloud-first proposal on 2026-08-30. Supabase, Cloud Run, Vercel Analytics, managed hosting, cloud accounts, remote databases, billing, and secret keys are excluded from the current version. Retain this file only as a future reference; the active decision record is `LOCAL_FIRST_ARCHITECTURE.md`.

# CodeQuest Academy — Architecture Decision Records

**Phase:** 01 — Migration Architecture
**Status:** Proposed for owner approval; the two decisions below are owner-approved.
**Scope:** Documentation only. No schema, interface, deployment, or production-data change is authorized by this phase.

## System context

CodeQuest Academy currently ships as a Kotlin Multiplatform / Compose Desktop application, with SQLDelight backed by a local SQLite database. Its public website is a Vercel-hosted Node/static site. The current desktop database has local profiles, passwords, curriculum, attempts, progress, drafts, and activity tables, but no remote identity, row-level authorization, durable shared analytics, or synchronization service.

The target architecture retains the desktop app and its SQLite database as an offline cache. Supabase becomes the managed cloud system of record only after a learner explicitly creates or links a cloud account. The public website stays separate from the authenticated learning application and continues to provide marketing, downloads, legal pages, and entry points.

## ADR-001 — Managed backend and authoritative data store

**Decision: accepted by owner**

Use Supabase (Postgres, Auth, Storage, and database APIs) as the managed backend for authenticated accounts, curriculum content, role assignments, cloud progress, persistent analytics, content administration, and sync metadata. Keep SQLite as the per-device offline cache.

### Rationale

- The present app is local-only and cannot safely synchronize learners, authors, or administrators.
- Supabase provides managed Postgres and Auth without replacing the working desktop stack.
- A shared database enables audited publication, durable evidence, backups, and row-level authorization.

### Constraints

- All migrations are additive and reversible. Existing SQLite tables and rows remain untouched.
- No local profile is silently converted to a cloud account.
- The server is authoritative for published content, role memberships, cloud submissions, and audit records. A device is authoritative only for its unsynchronized local events and drafts.
- Service-role credentials never ship in the desktop application or public website.

### Data ownership and RLS

`auth.users` is the authentication root. An application `profiles` record uses the same UUID. `user_roles` assigns only `learner`, `author`, `reviewer`, or `administrator`; a user may hold multiple roles where approved.

| Actor | Permitted data access |
| --- | --- |
| Learner | Own profile, own attempts/mastery/review queue/drafts/submissions; published content only |
| Author | Learner access plus drafts they own; cannot publish their own work unless also a reviewer and policy permits it |
| Reviewer | Assigned draft/review artifacts, QA records, approval/rejection actions; no learner PII by default |
| Administrator | Role-managed operational access, publication rollback, audit review; sensitive support access must be separately logged |
| Execution service | Narrow service identity: read immutable challenge/version/test bundle and write an execution receipt only |

Every table introduced in Phase 02 must enable RLS before exposure. Policies must use `auth.uid()` and role checks in security-definer helper functions with a fixed search path. Direct table access is denied by default. Privileged administrative actions go through a server-side API or Supabase Edge Function that writes an immutable audit record.

## ADR-002 — Offline-first sync and conflict resolution

**Decision: accepted as the operating model**

SQLite remains available with no network. The desktop client writes user activity first to local tables and a new append-only `sync_outbox`; it never blocks learning on cloud availability. A background sync worker uploads idempotent events once a linked account is authenticated and online. A server cursor returns only changed data since the last acknowledged pull.

### Conflict rules

| Data type | Authority / resolution |
| --- | --- |
| Published track, lesson, problem, book, knowledge content | Server version is authoritative; old cached version remains readable only long enough to display an update notice or complete an already-open offline activity |
| Attempts, answers, review evidence, XP evidence, analytics events | Append-only and idempotent by event UUID; never overwrite a prior attempt |
| Mastery and review schedule | Recomputed by the server from validated evidence; client values are provisional until acknowledged |
| Settings and device-local preferences | Per-key last-write-wins with UTC timestamp and device ID; no learning evidence is resolved this way |
| Project drafts / notes | Revision number plus base revision; auto-merge only non-overlapping text changes, otherwise preserve both copies and require learner choice |
| Profile display fields | Explicit last-write-wins with audit timestamp; identity/security fields are server-managed |

Each event carries a UUID, local sequence, device installation ID, account ID when linked, occurred-at UTC timestamp, schema version, content version, and payload checksum. The server stores the UUID in a uniqueness constraint, making retries safe. The client marks an outbox item acknowledged only after receiving its server receipt.

## ADR-003 — Local-account preservation and account linking

**Decision: accepted**

Existing local profiles remain valid offline. On upgrade, the app offers three explicit paths: continue offline, sign in to an existing CodeQuest cloud account, or create a new cloud account and link this device. The desktop app uses Supabase Auth’s system-browser PKCE flow; it receives a short-lived session through an app callback/deep link and stores refresh material only in OS-protected credential storage. It must not upload local password hashes.

After user consent, a migration preview groups local records by the existing local `UserProfile.id`, shows counts and the earliest/latest activity dates, and creates stable legacy mapping records. The upload is resumable, idempotent, and recorded in a migration journal. The local source data is retained until the owner-approved retention period and a successful reconciliation pass are complete.

## ADR-004 — Content versioning and publication

**Decision: accepted**

Content is immutable at the version level. Logical items (tracks, modules, objectives, lessons, problems, books, book sections, and knowledge files) point to a current published version; each version carries author, reviewer, timestamps, provenance, validation results, and a content hash.

Publication states are `planned`, `draft`, `automated_checked`, `technical_review`, `editorial_review`, `pilot`, `published`, and `archived`. Only published versions are learner-visible. A rollback is a pointer change to a previously published version plus an audit event; it does not delete the withdrawn version. A `legacy_content` feature flag keeps currently irrelevant curriculum available only to approved testers/administrators until an owner approves retirement.

`problem_manifest_10000.csv` is a capacity plan, not proof of completion: a `planned` slot is never shown as a finished problem or counted as production content.

## ADR-005 — Hybrid code and mathematics execution

**Decision: accepted by owner**

Small deterministic Python and mathematics exercises run in a browser/WebAssembly sandbox (for example, a vetted Pyodide-based runtime) with no network access and a tightly scoped API. Advanced Python, packages, machine learning, and deep-learning workloads run only in a separately deployed isolated execution service.

The isolated service receives a signed challenge bundle and learner submission, puts work into a queue, and starts an ephemeral sandbox in a hardened microVM/container runtime. It enforces:

- allow-listed interpreters and package images only; no arbitrary installation;
- default-deny egress and no cloud metadata access;
- read-only challenge files and a small disposable writable workspace;
- no host mounts, no Docker socket, no privileged mode, no shell escape, no nested virtualization;
- one process tree, PID cap, CPU quota, memory limit, output cap, execution timeout, and queued-job quota;
- per-user/rate quotas, signed execution receipts, immutable logs with PII redaction, and automatic workspace destruction.

The present JVM `ProcessBuilder` runner is **not** a production-safe execution architecture. It must be placed behind `legacy_local_runner` (default `false` in production) and retired/gated before the new execution feature is exposed. Neither the desktop process nor the website may execute untrusted learner code directly.

## ADR-006 — Evidence-based analytics

**Decision: accepted**

Analytics are event records, not counters incremented on screen opening. A verified activity, submission, execution receipt, content publication action, sync event, or error is recorded with a versioned event schema and a server receipt. Derived dashboards are rebuilt from these facts. Events include actor pseudonym/role, event ID, occurred-at, content version, device/app version, outcome, and consent category; raw code, answers, and unnecessary identifiers are excluded or redacted.

The current Node server’s in-memory analytics arrays are not a durable analytics system and are not a migration source of truth. Existing local `ActivityEvent` data can be imported only after the learner links an account and consents to the migration.

## ADR-007 — Environments, secrets, observability, and recovery

**Decision: proposed for Phase 02 implementation**

Use separate Supabase projects and execution-service environments for `development`, `staging`, and `production`. Development uses synthetic test data only. Staging uses production-like policies and scrubbed/seeded content, never copied learner PII without approved controls. Production changes require reviewed migrations, backups verified through restore drills, and a rollback plan.

Feature flags are server-evaluated, scoped by environment/role/cohort, and default closed for new remote features. Required initial flags include `cloud_auth`, `cloud_sync`, `ai_curriculum`, `content_studio`, `browser_runner`, `advanced_runner`, and `legacy_local_runner`.

Monitoring must combine service health, sync backlog age, failed RLS checks, migration errors, execution denials/timeouts, content publication failures, client crash reporting, and backup/restore evidence. Alerts must name an owner and runbook. Rollback disables the feature flag, restores the prior published content pointer or app release, and preserves all append-only evidence for investigation.

## Open implementation choices for owner approval

1. Select the initial isolated-runtime provider after the execution POC proves the chosen isolation boundary; Cloud Run is a cost reference, not a security certification for arbitrary learner code.
2. Select Supabase region based on learner residency, latency, and legal requirements before creating a production project.
3. Decide whether anonymous/offline learners may use browser-only exercises before account linking; the default recommendation is yes, without cloud analytics or sync.
