# CodeQuest AI Academy

**Learn. Build. Improve.**

CodeQuest AI Academy is a local-first Compose Desktop learning application for Python, mathematics, algorithms, data skills, and AI foundations. Learner profiles, progress, attempts, mastery, review scheduling, notes, and analytics are stored locally in SQLite.

## Version 1.4.0

- Bundled offline AI Academy source pack with 12 curriculum tracks.
- The supplied 10,000-problem manifest is imported as 10,000 production slots; only three reviewed foundation problems are currently published.
- Three authored beginner lessons: AI limits and accountability, problem decomposition, and Python values/types.
- Five 100-page book plans and twenty optional knowledge deep dives are bundled locally.
- SQLite FTS indexes published lessons, book metadata, and knowledge metadata locally.
- Home, Learn, Practice, Labs, Books, and Knowledge navigation is available in the desktop workspace.
- The legacy host-process code runner is disabled. The product does not fabricate execution output, automated tests, or grades.
- Online update checks are opt-in. No cloud account, paid API, remote database, or external analytics service is required to use the Academy.

## Development

```powershell
npm test
$env:JAVA_HOME = 'D:\coding academy\.jdk\jdk-17.0.19+10'
.\gradlew.bat :shared:jvmTest :desktopApp:jvmTest --no-daemon --console=plain
```

Build the Windows installer:

```powershell
npm run build:win
```

The installer and release manifest are produced locally. Publishing a release asset is a separate, explicit GitHub release step.

## Truthful content status

The 10,000-slot manifest is a production plan, not a claim that 10,000 validated problems already exist. Content is published only when its lesson/problem content, answer, explanations, and tests have been authored and reviewed.
