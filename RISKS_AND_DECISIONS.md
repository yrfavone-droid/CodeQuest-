# Risks and Owner Decisions - AI Academy Migration

## Decisions required before Phase 01 architecture approval

| Decision | Why it is material | Recommended default |
| --- | --- | --- |
| Identity and synchronization service | The current product has only device-local accounts. Multi-device learners, roles, Content Studio, and row-level authorization cannot be delivered from SQLite alone. | Adopt an approved managed backend with per-user authorization; retain local accounts through opt-in linking. |
| Execution isolation | User instruction forbids running untrusted code in the main application process. | Use a sandboxed browser Python runtime for small exercises plus an isolated service runner for packages; keep the current desktop `ProcessBuilder` runner off in production. |
| Analytics/monitoring | Existing counters are memory-only and cannot prove learner metrics or operational health. | Select privacy-aware durable events, error monitoring, retention policy, and consent language. |
| Content publication authority | Draft/review/publish/rollback requires roles and accountable reviewers. | Define author, technical reviewer, editorial reviewer, admin, and audit log policies before Content Studio implementation. |
| Legacy content policy | Relevance varies by lesson; deleting it risks learner progress. | Archive via feature flag; migrate only individually reviewed content. |
| 7,000 learner claim | The repository has no durable source for the claim. | Treat as owner-provided marketing context only; do not display a new numeric claim without a source of record. |

## Mandatory release gates

- Versioned SQLDelight migrations with backup, fixture upgrades, idempotence, and rollback tests.
- No `planned` content is labeled complete; content batches stay 100-250 items and must pass schema, uniqueness, correctness, code/math/ML, and review gates.
- WCAG 2.2 AA automated and manual checks for mobile/desktop designs.
- Isolated code execution with resource/network/filesystem limits, audit logs, and abuse controls.
- Authenticated, permission-checked content publication with immutable version history and rollback.
- Production monitoring and durable analytics before claiming product metrics.

## Known documentation corrections

- README claims 10,000+ learners and 5,000 total problems; neither is supported by the current auditable data model.
- README claims 18+ language execution; current runtime discovery/execution support is not equivalent to that claim.
- README says local database data is encrypted. The inspected implementation stores an SQLite file and password hashes; database encryption was not found.

## Rollback strategy

Each AI Academy phase must ship behind a feature flag. Disable the flag to return users to the existing navigation/content without deleting new tables, mappings, drafts, attempts, or legacy data. Migrations must be additive; content rollbacks change publication pointers/statuses rather than deleting versions.
