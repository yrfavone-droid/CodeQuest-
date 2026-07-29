# CodeQuest Academy — Complete Standalone Curriculum

This package contains the generated, concrete curriculum for all five tracks, ten paths, and fifty levels specified for CodeQuest Academy. It is designed for integration into an existing Android application, but it does not assume an unseen Kotlin data model.

## Exact curriculum size

- 5 tracks
- 10 paths
- 50 levels
- 50 diagnostics with 600 diagnostic questions
- 50 large cheat sheets
- 400 lessons
- 400 practice sets with 4,000 concrete questions
- 400 lesson challenges with 1,600 activities
- 100 mixed-review sessions with 1,200 questions
- 50 adaptive-review units with 400 remedial activities
- 1,500 final-quiz questions
- 50 projects with 100-point rubrics
- 50 project reflections
- 50 optional mastery challenges
- 10 path capstones
- 5 track final projects
- 400 skills and 1,200 misconception mappings

## Package map

```text
assets/
  curriculum_catalog.json        Track/path registry and loading policy
  paths/*.json                    Ten complete path assets
  skill_graph.json                Skills, prerequisites, and misconceptions
  path_capstones.json             Ten path capstones
  track_final_projects.json       Five track final projects
schema/
  curriculum.schema.json          Standalone JSON schema
tools/
  curriculum_blueprint.py         400 distinct lesson principles
  build_curriculum.py             Deterministic asset builder
  validate_curriculum.py          Strict referential/content validator
tests/
  test_curriculum.py              Reproducible validation tests
integration/
  ANDROID_INTEGRATION_GUIDE.md    Mapping and safe rollout guidance
curriculum_build_manifest.json    Per-path completion manifest
CURRICULUM_COVERAGE_REPORT.md     Exact level-by-level coverage
CURRICULUM_VALIDATION_REPORT.md   Validation evidence and boundary
```

## Curriculum workflow

Every level provides this ordered timeline:

```text
DIAGNOSTIC → CHEAT SHEET
→ (LESSON → PRACTICE → CHALLENGE) × 8
→ MIXED REVIEW 1 → MIXED REVIEW 2 → ADAPTIVE REVIEW
→ FINAL QUIZ → PROJECT → PROJECT REFLECTION
→ OPTIONAL MASTERY CHALLENGE
```

The final quiz passes at 75%. The project unlocks after a passing quiz. Submitting the project unlocks the next level; the reflection is required for the completion badge, while the mastery challenge is optional.

## Rebuild and validate

From this directory:

```bash
python3 tools/build_curriculum.py
python3 tools/validate_curriculum.py
python3 -m unittest discover -s tests -v
```

The builder is deterministic. IDs are append-only and the manifest declares zero initial progress plus preservation of existing user progress.

## Important integration boundary

The Android project was not available in the generation workspace. Therefore, the package validates against its documented standalone schema but has not been built through the app's `CurriculumLoader`, Room database, `MainViewModel`, `CodeRunner`, or UI. Follow the integration guide and map fields to the project's canonical models without renaming existing IDs.

