# Android Integration Guide

## 1. Treat existing project IDs as authoritative

The package preserves all level codes supplied in the curriculum specification. Before importing, compare these standalone identifiers with the application's current `track_id`, `path_id`, `level_id`, `lesson_id`, and `skill_id` values.

If an existing identifier differs, create an explicit mapping table. Do not rename a production identifier after users may have progress attached to it. In particular, preserve every established FE and BE identifier already present in the app.

## 2. Map the standalone schema to Kotlin models

Each file under `assets/paths/` has this shape:

```text
PathAsset
├── schema_version
├── track_id
├── path
└── levels[5]
    ├── metadata and weekly_plan
    ├── diagnostic
    ├── cheat_sheet
    ├── lessons[8]
    │   ├── lesson content
    │   ├── practice_set.questions[10]
    │   └── challenge.activities[4]
    ├── mixed_reviews[2]
    ├── adaptive_review
    ├── final_quiz.questions[30]
    ├── project
    ├── project_reflection
    ├── optional_mastery_challenge
    └── timeline_nodes[33]
```

Prefer adapter data classes rather than changing stable production models immediately. Parse the standalone asset into adapter models, validate it, then convert it to the app's canonical `CurriculumModels.kt` types.

Fields that may require backward-compatible model extensions are:

- diagnostic assessment
- mixed-review sessions
- adaptive-review activity bands
- project reflection
- optional mastery challenge
- rich cheat-sheet diagrams and trace tables
- challenge activity arrays
- timeline node types beyond lesson, quiz, and project

When a node type is not yet renderable, do not silently discard it. Keep the content in storage, mark the renderer unavailable in development diagnostics, and release only after the node has a tested UI mapping.

## 3. Register real assets

Add all ten path assets to the loader registry:

| Path ID | Asset |
| --- | --- |
| `frontend_web_development` | `paths/frontend_web_development.json` |
| `backend_web_development` | `paths/backend_web_development.json` |
| `flutter_development` | `paths/flutter_development.json` |
| `react_native_development` | `paths/react_native_development.json` |
| `security_fundamentals` | `paths/security_fundamentals.json` |
| `ethical_hacking` | `paths/ethical_hacking.json` |
| `data_structures` | `paths/data_structures.json` |
| `algorithms_coding_interviews` | `paths/algorithms_coding_interviews.json` |
| `machine_learning_fundamentals` | `paths/machine_learning_fundamentals.json` |
| `nlp_ai` | `paths/nlp_ai.json` |

Treat missing or invalid real assets as a visible loading error. Synthesized content should remain an emergency error fallback and must not mark a path complete.

## 4. Add version-aware Room seeding

Use a curriculum version table or existing seed-version mechanism with these rules:

1. Seed by stable primary ID using insert-ignore or upsert that does not overwrite completion fields.
2. Store curriculum content separately from user progress whenever possible.
3. On version change, insert new content and update mutable educational fields while preserving user-owned progress, attempts, mastery history, submissions, and timestamps.
4. Run the import in a transaction.
5. Enforce unique indices on stable IDs to make repeated startup idempotent.
6. Record the successfully imported schema/content version only after the transaction commits.
7. Never reset progress because the asset's declared `initial_progress` is zero; that value applies only when creating a new user record.

## 5. Timeline and unlocking

Render `timeline_nodes` in ascending `order`. Keep the current visual identity and map each type to the nearest existing card renderer.

Required ordering and gates:

- Diagnostic unlocks with the level.
- Cheat sheet unlocks after diagnostic completion.
- Each lesson unlocks after the preceding challenge, except Lesson 1, which follows the cheat sheet.
- Practice unlocks after its lesson.
- Challenge unlocks after its practice.
- Mixed Review 1 unlocks after Challenge 8.
- Mixed Review 2 unlocks after Mixed Review 1.
- Adaptive Review unlocks after Mixed Review 2.
- Final quiz unlocks after adaptive review and passes at 75%.
- Project unlocks at a quiz score of at least 75.
- Project reflection unlocks after submission.
- Optional mastery challenge unlocks after submission and never blocks the next level.
- Next level unlocks after project submission.
- Level 5 project submission unlocks the path capstone.
- Both path capstones unlock the track final project.

Add enough bottom content padding for the final timeline node to clear the system navigation bar and application bottom navigation on a Pixel 6 Pro-size viewport.

## 6. Question renderer mapping

The package uses:

- `multiple_choice`
- `match_pairs`
- `drag_order`
- `trace_state`
- `predict_output`
- `identify_bug`
- `choose_structure`
- `scenario`
- `adaptive_review`
- `flowchart_completion`
- `trace_table`
- `debugging`
- `complexity_comparison`
- `block_builder`
- `compare_solutions`

Every final-quiz question is single-choice even when its pedagogical type is structural, so it can use the existing option renderer as a compatibility fallback. Practice matching and ordering items require their native interaction renderers. Challenge structured-object items require a block/form adapter or a deterministic selection renderer.

No `coding_editor` exercise is emitted for Flutter, cybersecurity, ML, or NLP. The standalone package also avoids editor tasks for other paths because the actual `CodeRunner` contract was unavailable. Add editor variants only after checking supported language, syntax, timeouts, expected-output comparison, and sandbox behavior in the real project.

## 7. Validation before release

Run the supplied validator before copying assets, then add project-native tests for:

- JSON deserialization of every path asset
- exactly 5 tracks, 10 paths, and 50 levels
- Room first seed and repeated startup
- no duplicate inserts
- a new user's zero progress
- preservation of an existing user's progress during a curriculum update
- every timeline node renderer
- every question interaction renderer and grader
- lesson → practice → challenge gates
- final-quiz lock and exact 75% boundary
- failure recommendations and retry
- project and next-level gates
- Level 5 → path capstone → track final gates
- a complete level in each of the ten paths
- scroll clearance and restoration on Pixel 6 Pro dimensions

Do not mark Android integration complete until the app build, unit tests, database tests, and UI workflow tests produce actual passing results.

## 8. Content review strategy

The package passes structural, count, reference, answer-shape, and safety validation. Before a public production release, conduct subject-matter editorial review in batches, beginning with the ten Level 1 assets. Record approved content version and reviewer decisions without changing established IDs.

