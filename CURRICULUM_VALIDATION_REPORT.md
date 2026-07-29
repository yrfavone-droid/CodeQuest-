# Curriculum validation report

Validated from the packaged curriculum assets and the shared JVM test suite:

| Entity | Count |
| --- | ---: |
| Tracks | 5 |
| Paths | 10 |
| Levels | 50 |
| Timeline nodes | 1,650 |
| Lessons / practices / challenges | 400 / 400 / 400 |
| Practice questions | 4,000 |
| Challenge activities | 1,600 |
| Mixed reviews / adaptive reviews | 100 / 50 |
| Final quiz questions | 1,500 |
| Projects / reflections / mastery challenges | 50 / 50 / 50 |

`ProgressRepositoryTest.curriculumHasExactProductionCounts` checks the catalog shape and node
count; parser and progression tests cover nested content, idempotent seeding, unlock order, and
the 75% quiz boundary. The source manifest is `curriculum_build_manifest.json`.
