# Nous AI Academy

Nous AI Academy is a private, offline-first Windows reading workspace for serious AI study.

## Current clean-release state

The application deliberately contains no bundled curriculum, sample book, intensive file, placeholder lesson, synthetic progress count, or fake analytics. It is ready for the owner-provided official curriculum package.

Existing local accounts, settings, bookmarks, notes, highlights, and reader metadata are retained in SQLite. The clean-library startup migration archives old catalogue metadata before removing it from active storage; it does not delete learner-owned records.

## Build

Set `JAVA_HOME` to the bundled supported JDK, then run:

```powershell
.\gradle-8.7\bin\gradle.bat :shared:jvmTest :desktopApp:jvmTest :desktopApp:createDistributable --no-daemon --console=plain
```

The requested release installer name is `Nous-AI-Academy-Setup-{version}.exe`. A release manifest is enabled only after a real installer is built and checksum-verified.
