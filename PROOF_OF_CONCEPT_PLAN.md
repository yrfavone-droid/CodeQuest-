# Archived cloud-first proof-of-concept plan — do not implement in the local-first release

The active proofs are local migration, backup, search, and no-network operation. See `LOCAL_FIRST_ARCHITECTURE.md`.

# CodeQuest Academy — Proof-of-Concept Plan

**Purpose:** Reduce the highest data, security, and learner-trust risks before Phase 02 implementation. These are real, isolated staging proofs—not demo-only controls or production deployments.

## POC-A — Supabase authorization boundary

**Question:** Can the chosen schema expose only the correct data to learner, author, reviewer, administrator, and execution-service identities?

**Scope:** Development Supabase project only; synthetic users and synthetic content. Create a minimal `profiles`, `user_roles`, `lessons`, `lesson_versions`, `attempts`, `migration_journal`, and audit schema with RLS enabled.

**Tests:**

- Learner A can read published content and only Learner A’s attempt.
- Learner A cannot read Learner B’s profile, attempt, draft, or audit record.
- Author can edit owned draft but cannot expose it as published.
- Reviewer sees assigned review material but cannot alter learner progress.
- Administrator publication action creates an immutable audit row.
- Direct REST queries without a matching policy fail closed.

**Pass condition:** Automated negative and positive policy tests pass; no table is exposed with RLS disabled. **Stop condition:** Any cross-account read/write or a need to place a service-role secret in a client.

## POC-B — Opt-in SQLite-to-cloud migration and offline sync

**Question:** Can one existing local profile migrate safely without changing its source records or duplicating evidence?

**Scope:** A copied, synthetic SQLite fixture modeled on the current database—not a learner’s production file. Add only new local mapping/outbox/journal tables.

**Tests:**

- Preview accurate counts by source table before consent.
- Migrate profile, progress, attempts, project drafts, and activity events using stable legacy IDs and event UUIDs.
- Interrupt during upload, restart, and confirm idempotent completion.
- Make conflicting edits on two simulated devices and confirm append-only evidence, server-derived mastery, and visible draft conflict handling.
- Compare source/target row counts, checksums, timestamps, and migration receipts; source tables remain byte-for-byte unchanged.

**Pass condition:** Reconciliation report is complete, retried uploads create no duplicates, and unlink leaves local learning intact. **Stop condition:** source mutation, data loss, ambiguous account mapping, or non-recoverable conflict.

## POC-C — Publication, validation, and rollback

**Question:** Can a lesson/problem become learner-visible only after checks and be restored safely after a bad release?

**Scope:** Staging content with one real sample lesson and one real coded/math problem; no placeholder represented as complete.

**Tests:**

- Validator rejects malformed schema, duplicate item IDs, wrong answer/derivation, missing accessibility text, missing provenance, or failing hidden code test.
- Author drafts; reviewer approves/rejects; publisher releases a version.
- Learner sees only the published version.
- Rollback changes the published pointer and leaves the bad version/audit record intact.

**Pass condition:** Full state/audit trail and deterministic rollback are demonstrated. **Stop condition:** draft leakage, untracked publishing, or loss of version history.

## POC-D — Safe execution boundary

**Question:** Can small work run locally in a browser sandbox and advanced work run remotely without exposing the desktop host or service host?

**Scope:** Isolated security test environment only. No production endpoint. Use intentionally harmless fixtures and security probes authorized for the test environment.

**Browser sandbox checks:** known Python/math exercises execute deterministically; network fetch fails; host APIs are unavailable; timeout/output limits work; execution does not require the Compose process to spawn an interpreter.

**Advanced service checks:** allow-listed image only; no outbound network; no host mounts; package installation denied; time/memory/PID/file/output limits enforce; workspace is destroyed; malicious escape probes are rejected; signed receipt cannot be forged or replayed.

**Pass condition:** An independent security review accepts the isolation evidence and a threat model. **Stop condition:** any host/process escape, uncontrolled egress, missing quota, or reliance on `ProcessBuilder` for learner code.

## POC-E — Observability and recovery

**Question:** Can operators detect and recover from sync, execution, content, and database failures without losing learning evidence?

**Tests:** disconnect/reconnect simulation, dead-letter/recovery queue, invalid server response, RLS denial alert, failed publication alert, backup restore in staging, and feature-flag rollback.

**Pass condition:** Each exercise has an alert, owner, runbook, and observed recovery time. **Stop condition:** only a dashboard screenshot exists without a tested recovery path.

## Order, approvals, and deliverables

1. Approve this plan and select the initial cost envelope/regions.
2. Run POC-A first, then POC-B and POC-C. POC-D starts only after the execution threat model and provider design are reviewed.
3. Deliver source changes, test output, screenshots/log extracts that avoid PII, architecture deltas, threat model, cost measurement, and rollback procedure after each POC.
4. Owner approves or rejects each POC independently. Passing a POC authorizes only the next planned phase, not production deployment.
