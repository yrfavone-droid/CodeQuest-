# Archived cloud-first data-flow design — do not implement in the local-first release

The active application keeps learner data, content packs, analytics, search, and backups on-device. See `LOCAL_FIRST_ARCHITECTURE.md`.

# CodeQuest Academy — Data Flow and Sync Design

**Phase:** 01 — target design; no remote backend is connected in this phase.

## Component relationship

```text
Public website (Vercel) ── download/sign-in links ──> Desktop Compose app
                                                      │
                                              SQLite offline cache
                                           ┌──────────┴──────────┐
                                           │ local curriculum     │
                                           │ local attempts/drafts│
                                           │ sync outbox/journal  │
                                           └──────────┬──────────┘
                                                      │ HTTPS + PKCE session
                                                      v
                                          Supabase Auth + Postgres + Storage
                                           │      │          │
                                RLS-protected sync   │      └─ signed content assets
                                           │          v
                                           │     content studio / audit trail
                                           v
                                    durable analytics and mastery derivation

Browser sandbox <── small deterministic exercise bundle ── desktop/web client
Isolated execution service <── signed advanced job ── queue/API ── execution receipt ──> Supabase
```

## 1. Authentication and device/account linking

1. A user may continue using the desktop application with an existing local `UserProfile` and no network identity.
2. When the user chooses **Sign in** or **Link this device**, the app opens the system browser to Supabase Auth using PKCE. The password reset, OAuth, and verification pages remain in the browser, not in the desktop process.
3. The callback is validated against the PKCE verifier and expected state. The app records the cloud user UUID, device installation ID, and link time in a new local mapping table; it never uploads the local PBKDF2 password hash.
4. A consent page previews local records to migrate. Only after confirmation does the sync worker create append-only cloud records and migration mappings.
5. The user can sign out/unlink the cloud account without deleting local progress. Remote deletion/export requests follow the selected privacy policy and require a separate, auditable workflow.

## 2. Content publication to learner cache

1. An author creates a draft version in the studio.
2. Automated validators check schema, references, accessibility text, uniqueness, answer correctness, deterministic code tests, difficulty/prerequisites, and provenance. A failed gate cannot publish.
3. Reviewer and editorial approvals advance the version according to its state machine. A publisher changes the logical item’s published-version pointer and records the action.
4. Supabase exposes only published versions through RLS-safe APIs. The desktop client requests a signed manifest and only downloads missing/changed versioned assets.
5. SQLite stores the manifest, payload, content hash, and cache status. Existing open content remains available offline; later work treats the content version in use as part of every evidence record.

## 3. Learning evidence and mastery

1. The learner opens a cached lesson, completes a validated activity, or submits a project.
2. The application stores an immutable local event and associated attempt in one SQLite transaction, then appends its event UUID to `sync_outbox`.
3. The UI may show provisional progress calculated locally, labelled as pending while unsynchronized.
4. On reconnect, the sync worker batches events, signs in as the linked user, and calls a server endpoint that verifies actor, content version, idempotency UUID, and payload schema.
5. The server stores accepted evidence, writes a receipt, recomputes mastery and review schedules from evidence, and returns the authoritative delta/cursor.
6. The client applies the delta transactionally, writes the new cursor, and acknowledges only accepted outbox records. Rejected records remain visible in a recovery queue with a reason; they are never silently discarded.

## 4. Conflict handling and recovery

- **Network loss:** local transactions continue; outbox uses exponential backoff with jitter and a visible sync state.
- **Duplicate retry:** a unique event UUID yields the original receipt, not a second attempt or XP award.
- **Two devices:** append-only evidence is retained from both; server mastery derivation resolves outcomes consistently.
- **Content changed during an offline lesson:** server evaluates evidence against the submitted content version. If the version is withdrawn for safety/correctness, the server records a remediation status rather than rewriting history.
- **Draft collision:** project text drafts track a base revision. Non-overlapping changes may merge; otherwise both revisions are preserved for learner choice.
- **Schema incompatibility:** the client stops the affected queue lane, keeps data local, and requests an app update/migration. It does not coerce unknown records.

## 5. Execution data flow

### Browser sandbox

1. The client downloads a signed, immutable challenge bundle containing public examples, hidden-test policy, limits, and allowed runtime version.
2. The browser/WebAssembly runtime runs only code and assets in its sandbox, without network or host access.
3. A result object contains output, status, execution duration, resource-limit outcome, challenge version, and a local checksum. It becomes evidence only when the verified test harness accepts it.

### Isolated advanced service

1. The client sends source code and challenge ID to an authenticated submission API; the API fetches the fixed challenge version server-side.
2. The API creates a signed job, applies per-user quota, and queues the job. The execution service has a narrowly scoped service credential—not a learner session or database superuser key.
3. An ephemeral hardened sandbox runs the allow-listed image with CPU, RAM, time, PID, file size, package, and network controls. No untrusted code runs in Compose Desktop, the Node website server, an Edge Function, or a database function.
4. The service emits a signed receipt with sanitized stdout/stderr and limit status. The API validates it, records the attempt, then discards the workspace.
5. Only a bounded, redacted result is returned to the learner and analytics stream. Security events and excessive quota use alert operations.

## 6. Analytics flow

The old Node process-memory counters are replaced progressively, not copied as fact. Product analytics receives verified event records from successful server-side actions and explicitly consented imported local history. Metrics such as active learners, completion, mastery, execution success, and publication quality are derived from versioned event data with query definitions, not manually incremented totals. Operational logs are separate from learner analytics and use retention/redaction rules.

## 7. Backup, restore, and failure recovery

- Before a migration or import, create a signed SQLite export/checksum on the device where technically feasible and take the managed database backup defined for the target environment.
- Test restore procedures in staging before production rollout. A backup is not considered valid until a documented restore succeeds.
- A failed sync rollout turns off the relevant feature flag, pauses the server worker, retains the outbox, and restores the previous cursor-compatible service version.
- A bad publication rolls back the published pointer. A faulty client release uses the existing installer/update channel to return to the last supported app release; local data schema changes must remain backward readable during the supported rollback window.
