# Data Migration Map - Legacy CodeQuest to AI Academy

## Migration rules

1. Use additive tables and versioned migrations only.
2. Never delete or overwrite `UserProfile`, `ActiveSession`, `NodeProgress`, `AssessmentAttempt`, `ProjectDraft`, `ActivityEvent`, or existing curriculum assets.
3. Every mapping is idempotent, recorded with a migration version, and reversible by disabling the AI Academy feature flag. Rollback never deletes learner evidence.
4. Do not expose a legacy completion as AI mastery. Imported evidence is labelled `legacy_mapped` until a new AI Academy mastery check supplies fresh evidence.

## Entity mappings

| Existing entity | Preserve as | Additive AI Academy entity | Migration behavior |
| --- | --- | --- | --- |
| `UserProfile.user_id` | Canonical local learner identifier | Future `ai_learner_identity`/cloud-link record | Copy reference only; no new remote account without user consent. |
| `ActiveSession` | Existing local session | Feature-flag lookup only | Keep intact; AI screens must honor the existing session guard. |
| `Track`, `Path`, `Level` | Legacy catalog | `ai_tracks`, `ai_modules`, `ai_objectives`, `ai_lessons` | Insert AI records separately; write mapping rows for approved reused content. |
| `Level.json_data` | Original lesson source | `ai_lesson_versions.content` | Copy only reviewed, schema-valid lessons into a new versioned record. |
| `CurriculumNode` | Existing prerequisite graph | `ai_objectives.prerequisite_objective_ids` | Preserve graph; map only approved nodes. |
| `NodeProgress` | Existing completion state | `ai_legacy_mappings`, optional mastery evidence | Do not change state. Add non-unlocking evidence with provenance. |
| `AssessmentAttempt` | Immutable legacy attempt history | `ai_attempts` only when a reviewed equivalent exists | Never fabricate correctness, hints, or execution fields. |
| `SkillMastery` | Existing coarse mastery | `ai_mastery` | Do not convert scale without a documented objective mapping and recalculation policy. |
| `ProjectDraft` | Existing learner draft | `ai_project_submissions`/draft link | Preserve original text and timestamps; do not mark completed without rubric evidence. |
| `ActivityEvent` | Existing activity log | Analytics import event | Keep as legacy events, never count as verified AI XP by default. |
| `AppSetting` | Device preferences | AI Academy local preferences | Keep keys; introduce namespaced new keys. |

## Proposed reversible migration sequence

1. Backup the SQLite file and verify integrity before migration.
2. Create new `ai_*` SQLDelight tables with numbered migration files; do not modify existing columns.
3. Seed T01/T02 metadata in `draft` state only.
4. Import only approved legacy mappings into `ai_legacy_mappings`.
5. Enable a per-device `ai_academy_v1` feature flag for preview users.
6. Validate account/session continuity, progress immutability, migration idempotence, and rollback (flag off) against fixtures.

## Explicit non-mappings

- `planned` rows from `problem_manifest_10000.csv` do not become `published` problems.
- Old completion percentages do not become mastery crowns, streaks, XP, or readiness.
- Existing in-process code execution results do not become trusted exercise evidence.
- No legacy content is deleted, renamed, or hidden globally without owner approval.
