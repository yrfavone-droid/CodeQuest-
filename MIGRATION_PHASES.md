# Archived cloud-first migration sequence — do not implement in the local-first release

The active sequence uses additive SQLite migrations and local content packs only. See `LOCAL_FIRST_ARCHITECTURE.md`.

# CodeQuest Academy — Migration Phases

**Guardrail:** Phases are sequential approval gates. All data changes are additive, reversible, backed up, and measured. No production data is deleted or overwritten.

| Phase | Objective | Deliverables / evidence | Exit gate and rollback |
| --- | --- | --- | --- |
| 00 — Audit | Establish factual baseline | `CURRENT_STATE_AUDIT.md`, legacy inventory, migration map, risk register; database and test baseline | Completed. No code/data change. |
| 01 — Architecture | Decide how systems connect | ADRs, route map, data flow, phased plan, POC plan, cost options | **Current phase.** Owner approves documents and POC scope before any cloud resource/data-model work. |
| 02 — Additive data model | Create dev/staging Supabase model and local migration design | Reviewed SQL migrations, RLS policy tests, role tests, data dictionary, migration journal design | Roll back by disabling feature flags and applying approved down/compensating migration; no production migration yet. |
| 03 — Content foundations | Build controlled content import/validation pipeline | Schemas, content studio skeleton, first draft batch (100–250), automated checks, reviewer evidence | Publish nothing without review; content remains draft/feature-flagged. |
| 04 — Identity and linking POC | Prove opt-in account linking safely | PKCE flow, local-to-cloud preview, encrypted token storage, idempotent test migration, privacy copy | Delete staging data; disable `cloud_auth` and `cloud_sync`; local accounts unaffected. |
| 05 — Offline sync | Prove two-way resilient progress synchronization | SQLite outbox, cursor protocol, conflict tests, reconnect/retry tests, reconciliation report | Disable sync and retain local outbox; no learner evidence lost. |
| 06 — Safe labs | Replace unsafe execution path progressively | Browser sandbox POC, isolated runner POC, quotas, security review, signed receipts | Default-disable runners; retain non-executing editor and lessons. `legacy_local_runner` stays off in production. |
| 07 — Academy experience | Deliver responsive learner and studio workflows | Orange/cream design implementation, keyboard/screen-reader checks, mobile/desktop tests, feature-flag cohorts | Route users to legacy screens/content; data contracts remain compatible. |
| 08 — Content production | Fill validated academy curriculum in batches | 100–250 item batches, schema/uniqueness/answer/test/difficulty/prerequisite/provenance reports, human reviews | Unpublish a version/pointer rollback; never represent planned slots as finished. |
| 09 — Books and library | Deliver reading and knowledge systems | Accessible reader, bookmarks/highlights/notes, PDF export/download authorization, progress sync | Disable feature flag; preserve source and learner annotations. |
| 10 — Quality and operations | Prove security, accessibility, analytics, and observability | WCAG 2.2 AA audit, penetration/security review, monitoring dashboards, backup restore drill, incident runbooks | Block release if any required gate fails. |
| 11 — Staged production migration | Roll out without forcing users | Opt-in cohorts, migration reconciliation, support tooling, rollback release plan, production monitoring | Pause cohort / disable flag / restore prior published version; source data remains intact. |
| 12 — Acceptance | Validate the complete transformed product | `FINAL_ACCEPTANCE_CHECKLIST.md` evidence including sign-in, migrated progress, safe labs, publishing, downloads, responsive behavior, monitoring | Owner signs off or defects return to the responsible phase. |

## Existing-data migration sequence

1. Inventory the SQLite schema/version and create a device-local migration snapshot/checksum. Do not modify the legacy tables.
2. Add new local mapping/outbox/cache tables in a new SQLDelight migration. Keep old read paths working.
3. Add Supabase tables, RLS policies, and migration journal in development, then staging. Test roles and rollback with synthetic data.
4. Link one test account with explicit consent. Migrate a small, deterministic fixture set and verify counts, checksums, content IDs, attempt timestamps, and derived mastery.
5. Reconcile server receipts against the local journal. Only then enable a voluntary internal pilot cohort.
6. Expand by measured cohorts. Maintain per-device migration status and human support recovery tooling.
7. Retain legacy mappings and raw local source records through the approved retention and rollback window. Archive only after owner approval and verified exports.

## Required quality gates for every implementation phase

- Show changed files and an implementation/review summary.
- Run applicable lint, typecheck, unit, integration, accessibility, and production build checks; record command and outcome. A documentation-only phase may reuse a same-revision code baseline, but must clearly say no source was changed.
- Use staging and synthetic data for all new cloud, migration, and execution tests until production approval.
- Stop and request a decision when a secret, service account, data residency decision, legal requirement, or cost commitment is needed.
- Keep a named rollback owner, recovery steps, and success/reconciliation metric for every data-affecting release.

## Legacy content disposition

Existing strong problem-solving, algorithm, mathematics, and AI/ML foundations are candidates for mapping into the new Python, mathematics, and algorithm foundations only after lesson-by-lesson review. Web development, app development, and cybersecurity material is archived behind `legacy_content`; it is not deleted. Current claims of a 10,000-problem bank remain disallowed until all 10,000 manifest slots contain validated published content.
