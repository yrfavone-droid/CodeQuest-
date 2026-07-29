# CodeQuest Academy Desktop — Production Repair Validation

Validation date: 2026-07-17 (Africa/Cairo)

## Outcome

The desktop source now compiles and the repository regression suite passes. The curriculum loads as 5 tracks, 10 paths, 50 levels, and 1,650 timeline nodes. Production placeholder phrases, TODO markers, empty click callbacks, and the previous hardcoded dashboard data were removed from `shared` and `desktopApp` source.

The portable Windows application image was created successfully. MSI creation reached `jpackage` but failed when WiX `light.exe` exited with Windows code 216; the portable distribution is the verified package artifact.

## Root cause: Tracks loading

The installed curriculum was being rebuilt on every launch from approximately 33.4 MB of JSON before profile restoration. Catalog/resource exceptions were swallowed by `JvmCurriculumFileReader.listPaths()`, the database seed omitted Track rows, UI metadata used hyphenated IDs while the canonical assets use underscore IDs, and the Kotlin `Level` model ignored `timeline_nodes`, `adaptive_review`, and the lesson-nested practice/challenge objects. This left the UI dependent on a slow or invalid database index while displaying only a centered spinner.

The repair now:

- validates exactly 10 catalog assets;
- logs full technical exceptions and publishes Loading, Loaded, Empty, Error, or Not Found states;
- seeds Track, Path, Level, and CurriculumNode rows transactionally;
- verifies 5/10/50/1,650 counts;
- skips the 33 MB parse when the installed curriculum version and counts are current;
- uses canonical underscore IDs everywhere;
- renders skeleton, empty, error, retry, and safe-return states.

## Root cause: placeholder navigation

The typed destination model declared more routes than `App.kt` rendered. Unhandled destinations fell through to a generic placeholder/not-found branch, and Cheat Sheet clicks had an empty callback. Several sidebar destinations were only static text screens.

Every current destination now has an explicit renderer. Invalid legacy IDs receive a designed Content Not Found state with Back and Dashboard actions.

## Implemented screens and flows

- Startup validation and recoverable curriculum error state
- Offline profile onboarding
- Dashboard backed by saved profile, node progress, activity, and project data
- Responsive five-card Tracks browser
- Track Detail for all five tracks
- Path Detail for all ten paths and their five levels
- 33-node Level learning map for every level
- Diagnostic
- Cheat Sheet/document viewer
- Real curriculum Lesson reader
- Practice with grading, hints, retry, and feedback
- Four-activity Challenge
- Mixed Reviews
- Adaptive Review
- 30-question Final Quiz and integrated result state
- Level Project workspace with persisted notes, milestones, rubric, save, and submit
- Project Reflection with persisted responses
- Optional Mastery Challenge/document workspace
- Projects hub
- Review hub
- Progress
- Profile editing and selectable JSON progress export
- Persistent Settings
- Locked Path Capstone and Track Final prerequisite screens
- Loading skeleton, empty, error, locked, completion, and content-not-found states

## Functional interactions

- Full-card Track, Path, Level, and learning-node navigation
- Backstack-based Back actions
- Sidebar collapse/expand
- Exact next-node Dashboard continuation
- Lesson section navigation and completion persistence
- Answer selection/free response, Check Answer, correct/incorrect feedback, three progressive hints, retry, and next question
- Exact 75% Final Quiz boundary
- Project draft autosave action, milestone changes, confirmation, and submission
- Reflection draft persistence and submission
- Profile name persistence
- Reduced-motion and reading-size preference persistence
- Real recent activity and honest weak-skill empty state
- Version-aware, idempotent curriculum seeding

## Curriculum counts

| Item | Count |
| --- | ---: |
| Tracks | 5 |
| Paths | 10 |
| Levels | 50 |
| Timeline nodes | 1,650 |
| Nodes per level | 33 |
| Lessons | 400 |
| Lesson practices | 400 |
| Lesson challenges | 400 |
| Final Quiz questions | 1,500 |

The last four counts come from the validated asset structure: eight lessons/practices/challenges and one 30-question quiz per level.

## Build validation

JDK: portable Eclipse Temurin 17.0.19+10 in `.jdk/jdk-17.0.19+10`.

Command:

```powershell
$env:JAVA_HOME=(Resolve-Path '.jdk\jdk-17.0.19+10').Path
.\gradle-8.7\bin\gradle.bat :desktopApp:compileKotlinJvm --no-daemon --console=plain
```

Result: **BUILD SUCCESSFUL** in 2 minutes. A later up-to-date compile also succeeded as part of packaging.

## Test validation

Command:

```powershell
.\gradle-8.7\bin\gradle.bat :shared:jvmTest --no-daemon --console=plain
```

Result: **BUILD SUCCESSFUL** in 50 seconds.

- Tests: 6
- Passed: 6
- Failures: 0
- Errors: 0
- Skipped: 0

XML results:

- `shared/build/test-results/jvmTest/TEST-com.codequest.academy.shared.data.ProgressRepositoryTest.xml`
- `shared/build/test-results/jvmTest/TEST-com.codequest.academy.shared.models.CurriculumParserTest.xml`

Covered: exact production counts, 33 nodes per level, parse failure, zero-state profile, idempotent seeding, lesson/practice/challenge progression, Final Quiz prerequisite, 22/30 failure, 23/30 pass, Project unlock, and Reflection unlock.

## Package validation

Portable distribution command:

```powershell
.\gradle-8.7\bin\gradle.bat :desktopApp:createDistributable --no-daemon --console=plain
```

Result: **BUILD SUCCESSFUL** in 34 seconds.

Artifacts:

- `desktopApp/build/compose/binaries/main/app/CodeQuestAcademy/CodeQuestAcademy.exe`
- `desktopApp/build/compose/binaries/main/CodeQuestAcademy-1.0.0-portable.zip` (74,810,259 bytes)

MSI command:

```powershell
.\gradle-8.7\bin\gradle.bat :desktopApp:packageMsi --no-daemon --console=plain
```

Result: **FAILED** after compilation and runtime-image creation because WiX `light.exe` exited with code 216.

Logs:

- `desktopApp/build/compose/logs/packageMsi/jpackage-2026-07-17-11-50-51-out.txt`
- `desktopApp/build/compose/logs/packageMsi/jpackage-2026-07-17-11-50-51-err.txt`

## Responsive layout validation

The source implements these measured breakpoints:

| Window/content condition | Result |
| --- | --- |
| Compact window / content below 900 dp | 76 dp navigation rail; stacked content/workspaces |
| Tracks content below 1,000 dp | One card per row |
| Tracks content 1,000–1,319 dp | Two cards per row |
| Tracks content 1,320 dp and above | Three cards per row |
| Project workspace below 1,200 dp | Stacked workspace, milestones, and rubric |
| Project workspace 1,200 dp and above | 270 dp milestones, flexible workspace, 310 dp rubric |
| Assessment workspace below 1,100 dp | Inline question and support layout |
| Assessment workspace 1,100 dp and above | Question workspace plus 300 dp support panel |

Window defaults to 1,440 × 900 with a 960 × 640 minimum. Main content is capped at 1,440 dp and all long pages use independent scrolling and bottom padding.

## Screenshot status

No acceptance screenshots are claimed. Both Gradle and the packaged executable launched processes and initialized the database, but this automation session exposed no interactive AWT window handle. App-specific window enumeration returned no CodeQuest window, and whole-desktop capture was correctly rejected because it could collect unrelated private screen content. The invalid blank capture was deleted.

The required 960 × 640, 1,280 × 720, 1,440 × 900, and 1,920 × 1,080 visual acceptance set therefore remains unverified and must be captured in an interactive Windows desktop session.

## Source files changed

### Desktop entry and build behavior

- `gradle.properties`
- `desktopApp/src/jvmMain/kotlin/com/codequest/academy/desktop/Main.kt`
- `desktopApp/src/jvmMain/kotlin/com/codequest/academy/desktop/JvmCurriculumFileReader.kt`

### Data, models, navigation, and shell

- `shared/src/commonMain/sqldelight/com/codequest/academy/database/AppDatabase.sq`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/App.kt`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/data/ProgressRepository.kt`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/models/CurriculumModels.kt`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/models/TrackIdentity.kt`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/ui/navigation/Navigation.kt`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/ui/components/AppShell.kt`
- `shared/src/commonMain/kotlin/com/codequest/academy/shared/ui/components/TrackCard.kt`

### Screens

- `AssessmentScreen.kt`
- `CapstoneScreen.kt`
- `ChallengeScreen.kt`
- `CommonStates.kt`
- `CurriculumLoadingScreen.kt`
- `DashboardScreen.kt`
- `DiagnosticScreen.kt`
- `DocumentNodeScreen.kt`
- `FinalQuizScreen.kt`
- `LessonScreen.kt`
- `LevelOverviewScreen.kt`
- `MixedReviewScreen.kt`
- `PathDetailsScreen.kt`
- `PracticeScreen.kt`
- `ProfileScreen.kt`
- `LocalAccountScreens.kt` (create account, sign-in, legacy credential setup, and password change)
- `ProgressScreen.kt`
- `ProjectScreen.kt`
- `ProjectsScreen.kt`
- `ReviewScreen.kt`
- `SettingsScreen.kt`
- `TrackBrowserScreen.kt`
- `TrackDetailsScreen.kt`

All screen paths are under `shared/src/commonMain/kotlin/com/codequest/academy/shared/ui/screens/`.

### View models

- `AdaptiveReviewViewModel.kt`
- `AssessmentViewModel.kt`
- `ChallengeViewModel.kt`
- `DashboardViewModel.kt`
- `DiagnosticViewModel.kt`
- `DocumentNodeViewModel.kt`
- `FinalQuizViewModel.kt`
- `LessonViewModel.kt`
- `LevelOverviewViewModel.kt`
- `MixedReviewViewModel.kt`
- `PathDetailsViewModel.kt`
- `PracticeViewModel.kt`
- `ProjectViewModel.kt`
- `TrackBrowserViewModel.kt`
- `TrackDetailsViewModel.kt`

All view-model paths are under `shared/src/commonMain/kotlin/com/codequest/academy/shared/ui/viewmodels/`.

### Tests

- `shared/src/jvmTest/kotlin/com/codequest/academy/shared/data/ProgressRepositoryTest.kt`

## Remaining limitations

1. Visual screenshots could not be captured in this non-interactive automation desktop session.
2. The MSI installer is not produced; use the portable ZIP. WiX code 216 remains an environment/toolchain packaging issue.
3. Match-pairs, drag-order, trace-table, flowchart, and block-builder curriculum items use the common selection/free-response assessment interaction rather than specialized drag canvases. They are gradable but do not yet provide every requested manipulation mode.
4. Path Capstone and Track Final screens currently provide accurate locked prerequisite states; their full unlocked multi-deliverable workspaces are not implemented.
5. Weak-skill recommendations currently appear only from persisted failed assessment events; per-skill mastery aggregation and misconception scoring are not yet stored.
6. The reading-size setting is persisted and presented, but it is not yet applied globally to the Compose typography scale.
