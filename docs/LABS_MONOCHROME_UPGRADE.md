# Offline labs and monochrome desktop upgrade — 2.2.0

Release status: **not release-ready** until final desktop/installer and interactive verification pass.

## Rollback and data

Rollback tag: `rollback-before-monochrome-labs-2.1.0`, pointing to `0e09e7a2bd5f40801fc0701288ac19c205993ba2`.
The existing curriculum archives, IDs, account tables, notes, bookmarks and reading data are not replaced.
`LabStore` creates two additive, idempotent learner tables (`NousLabProject`, `NousLabAttempt`) and one index. Downgrading ignores these tables; do not drop them. Queries bind user IDs and values. New lab records are separate from read-only curriculum.

## Python architecture

The desktop bundles Windows x64 CPython 3.11.9, NumPy 1.26.4, Pandas 2.2.3 and their pinned dependencies. Build inputs are SHA-256 verified. The generated `python-lab/runtime.zip` and its digest are resources in the desktop JAR. The app extracts and verifies them locally; no Python installation or runtime network download is required.

`PythonRunner` starts only the trusted `NousPythonSandbox.exe` broker, without a shell or learner-supplied command-line executable. The broker creates a fresh capability-free AppContainer and a Windows Job Object before resuming Python. Runtime dependencies have read/execute permission only for package processes; each disposable run directory grants modify permission only to its unique container SID. No account/database/curriculum path is granted to learner code.

Limits: one process, 512 MiB process memory, 10 seconds process CPU time, 30 seconds elapsed execution time, 64 KiB captured Python output, and a monitored 16 MiB / 10,000-entry workspace limit. Network capabilities and desktop/clipboard access are absent. Workspace reparse points are rejected. Stop terminates the job. The desktop removes the run workspace after completion. The 16 MiB filesystem limit is monitored, not an instantaneous filesystem quota; short overshoots between checks are possible.

RSyntaxTextArea provides real Python syntax highlighting in grayscale, line numbers, auto-indent, bracket matching, undo/redo and standard clipboard actions. Projects and autosave live in SQLite; `.py` import/export requires an explicit file selection and confirms replacement. Thirteen exercises have executable public/hidden assertions and canonical solutions. They link to existing S02/S03/S04/S05/S06/S12 lessons. Lab mastery is distinct from curriculum quiz completion.

Tests provide local learning feedback, not tamper-proof examination certification. The machine/profile owner controls their own local files. No certification or security claim depends on a learner-owned score database.

## Mathematics architecture

Twenty topics generate deterministic problems from a local seed at three numerical difficulty ranges. Validation supports integers, decimals, fractions, expanded polynomials, MCQs, vectors and matrices. Numeric tolerance is `1e-6 × max(1, abs(expected))`. Polynomial checking is deliberately bounded to expanded terms through degree eight, not a general computer algebra system. Unknown variables are rejected in numeric answers. Reasoning responses reveal their rubric only after submission; self-assessments are stored separately from automatically verified mastery.

Graphs use a bounded local arithmetic parser, never eval or a cloud service, with text samples for nonvisual access. Matrix multiplication, vector dot products and norms, population statistics and binomial probability are calculated locally. Attempts include response, correctness, hints and elapsed time. Mastery uses the latest evidence per distinct problem, with hint penalties and an evidence requirement; repeatedly submitting the same math answer cannot manufacture independent mastery.

## Monochrome interface

The centralized light/dark palette uses grayscale only. Sidebar and editor use monochrome rendering; existing data-driven learning/PDF routes remain active. Buttons/navigation have explicit keyboard focus outlines. The sidebar scrolls at small sizes. The Windows client-area animation preference is read through `SystemParametersInfoW(SPI_GETCLIENTAREAANIMATION)` and combined with the saved Reduce Motion preference.

## Evidence collected so far

- Final broker security suite: 14/14 passed, including Python/NumPy/Pandas, syntax/runtime errors, output cap, timeout, Stop, memory exhaustion, workspace writes, denied network/private-file/runtime writes/child process, and passing/failing assertions.
- Evidence directory: `C:\Users\AL-FAGR\AppData\Local\Temp\NousLabTests-0552bf42bd724d2383d1232b91c70273`.
- On September 5, shared JVM tests passed 16/16 and desktop JVM tests passed 7/7. Math tests exercise 3,000 deterministic topic/difficulty/seed combinations. The real Python test ran all thirteen canonical solutions and thirteen deliberately incorrect submissions, plus syntax-error handling and run-directory cleanup. The website suite passed 8/8.
- `:shared:jvmTest :desktopApp:jvmTest :desktopApp:createDistributable` completed successfully with Temurin 17.0.19+10 and Gradle 8.7. The final rerun after the navigation-time autosave fix passed (BUILD SUCCESSFUL, 2m 23s; 16 shared and 7 desktop tests, zero failures). An intermediate packaging attempt was blocked by the open QA process locking build files; closing only that QA process resolved it.
- The packaged 2.2.0 EXE launched from a new explicitly labelled QA profile using its bundled JVM, with `JAVA_HOME` empty, only Windows directories on PATH, and online update checks disabled. No real learner profile was used for this test.
- Windows Computer Use recovered after the plugin update. The packaged editor was exercised interactively: entering `total = quantity * price` and printing the result produced `Offline Python: 10.0`; both real exercise assertions passed. Evidence: `build/verification/screenshots/python-light.png`.
- The first 2.2.0 candidate installer built successfully (160,771,584 bytes; SHA-256 `4CEB65FA18C6F5268EF71829F7D60A0879D1D2D99BF75D867B4CD74AE278E5D4`). This candidate predates the navigation-time autosave fix and is not the final validated installer. Its payload contains the x64 launcher, `runtime/bin/javaw.exe`, `runtime/bin/server/jvm.dll`, application JARs and RSyntax dependencies.
- The final-source installer was rebuilt locally at `D:\coding academy\releases\Nous-AI-Academy-Setup-2.2.0.exe`: 160,771,584 bytes; SHA-256 `63941A20BE97005942269E09D329E37B2E944F979401C14AADCACFF943F99706`. Packaging passed; this artifact still awaits the interactive installer gates below and is not publicly published.
- The user approved installation, but Windows Computer Use reported `Computer Use app approval timed out` before launching setup. A user-supplied screenshot shows 2.2.0 running; that does not independently prove installation, shortcut launch or uninstall behavior.
- A local backup of the existing user database was saved under ignored `build/verification/` before installer testing. No learner database, credential, runtime archive or installer is included in the source commit.

## Remaining release gates

Complete interactive autosave/restart verification and test the final installer through normal installation, shortcuts, interactive Math, dark layout, keyboard navigation, small windows, offline restart and uninstall. Collapsed-navigation tooltips and the full requested transition polish have not been completed. Math currently has twenty deterministic topic templates, not an exhaustive symbolic algebra engine or advanced mathematics course. Do not update public download manifests or publish an installer until release gates pass.

The source push is separately authorized by the owner. Pushing source is not publication or release-readiness approval; existing public update/download manifests remain unchanged.
