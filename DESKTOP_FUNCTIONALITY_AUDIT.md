# CodeQuest Desktop Functionality Audit (Historical Baseline — 2026-07-16)

## General Observations
This file records the initial audit that preceded the production repair. It is retained as evidence
of the root causes; see `DESKTOP_FUNCTIONALITY_REPORT.md` for the validated current implementation.

## Screens and Controls

### 1. App Navigation Rail (`App.kt`)
- **Home**: Working route to `DashboardScreen`.
- **Tracks**: Working route to `TrackBrowserScreen`.
- **Code**: `onClick = { }`, decorative.
- **Rankings**: `onClick = { }`, decorative.
- **Profile**: `onClick = { }`, decorative.

### 2. Dashboard (`DashboardScreen.kt`)
- **Greeting & Progress**: Hardcoded text "Hello, hh!" and `0.0f` circular progress.
- **Stats Row**: Hardcoded to `0` for Lessons, Challenges, Quizzes, Projects.
- **Continue Learning Card**: Hardcoded to "WEB DEVELOPMENT", "FE-101: HTML Basics", and `0% Complete` with static progress bar.
- **Continue Learning Button**: Navigates to `Screen.LevelOverview("FE-101")` statically.
- **Learning Track Cards**: 
  - All 4 cards have static `progress = 0f`.
  - All cards statically navigate to `Screen.TrackBrowser`.

### 3. Track Browser (`TrackBrowserScreen.kt`)
- Contains static track placeholders.
- Buttons statically navigate to `FE-101`, `BE-101`, or back to `Dashboard`.

### 4. Level Overview (`LevelOverviewScreen.kt`)
- Displays level title and static content.
- Button to take diagnostic statically navigates to `Screen.Diagnostic("FE-101-DIAG")`.
- Back button navigates to `Screen.TrackBrowser`.

### 5. Diagnostic (`DiagnosticScreen.kt`)
- Shows a hardcoded diagnostic question ("Which of the following is true about web requests?").
- **Check Answer Button**: Contains empty lambda: `onClick = { /* Check answer */ }`.
- **Finish Diagnostic Button**: Statically navigates back to `Screen.LevelOverview("FE-101")`.

### 6. Lesson (`LessonScreen.kt`)
- Shows static lesson placeholder.
- **Next Button**: Statically navigates to `Screen.LevelOverview("FE-101")`.

## Data and Persistence
- **Database**: `AppDatabase` via SQLDelight is configured in `ProgressRepository.kt`, but it's not connected to the UI state flow.
- **Curriculum Loader**: `CurriculumLoader.kt` has logic to parse `PathAsset` JSON, but it is not utilized in the UI or app startup.
