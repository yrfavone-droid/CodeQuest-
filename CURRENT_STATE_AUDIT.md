# CodeQuest AI Academy - Current State Audit

Audited: 2026-08-29

## Scope and evidence

This is a read-only audit of the existing repository, the development SQLite database shape, the public Vercel site, and the supplied AI Academy package. No production data, application behavior, or legacy curriculum was changed.

## Current architecture

| Area | Observed implementation | Migration implication |
| --- | --- | --- |
| Desktop product | Kotlin Multiplatform + Compose JVM; Windows is the shipped target. `desktopApp` is a thin shell around `shared`. | Preserve the desktop shell; add new bounded domains in `shared` rather than replacing it. |
| Web product | Static landing page plus a small Node HTTP server deployed through Vercel. | It is a distribution/marketing site, not an authenticated learning backend. |
| Data | SQLDelight over local SQLite. Production desktop databases live under `%USERPROFILE%/.codequest-academy/`. | Existing learner data is device-local. Cloud accounts, cross-device sync, admin publication, and durable analytics do not exist yet. |
| Authentication | Local email/password profiles; PBKDF2-HMAC-SHA256 password records; local `ActiveSession`. | Preserve every existing `UserProfile` and session. A future cloud identity migration must be opt-in and map local IDs without deleting them. |
| Curriculum | Packaged JSON assets seed 5 tracks, 10 paths, 50 levels, and 1,650 timeline nodes. | Preserve the existing asset IDs and progress rows. Map/reuse content additively. |
| Releases | GitHub Release asset is served through Vercel update/download routes. | Keep `/api/download`, `/api/app/latest-version`, and `/api/app/check-updates` stable. |

## Existing learner and content model

The development database contains catalog data only: 5 tracks, 10 paths, 50 levels, and 1,650 curriculum nodes; it has no profiles, sessions, attempts, project drafts, or progress rows. This is evidence about the checked-in development database only, not a count of production learners.

The current catalog is:

| Legacy track | Paths | Disposition |
| --- | --- | --- |
| Web Development | Frontend Web Development; Backend Web Development | Archive behind a feature flag; selectively reuse Python/web-demo transferable lessons only after review. |
| App Development | Flutter Development; React Native Development | Archive behind a feature flag; do not delete. |
| Cybersecurity | Security Fundamentals; Ethical Hacking | Archive behind a feature flag; retain for later optional knowledge content only with owner approval. |
| Problem Solving | Data Structures; Algorithms and Coding Interviews | Candidate for direct reuse in T01/T03 after objective-level review. |
| AI and Machine Learning | Machine Learning Fundamentals; Natural Language Processing and AI | Candidate for controlled migration into T06/T09 after content QA. |

The current package reports 400 lessons, 4,000 practice questions, 1,600 challenge activities, 1,500 final quiz questions, 50 projects, and 50 mastery challenges. Those counts describe the current packaged assets; they are not evidence that the supplied 10,000-slot AI problem manifest is complete.

## Supplied AI Academy package

The supplied manifest has exactly 10,000 unique slots across T01-T12, but every row has status `planned`. It is therefore a planning inventory, not a production problem bank. The package contains the required 12-track map, mastery model, JSON schemas, five book blueprints, 20 deep-dive prompts, design requirements, and a logical PostgreSQL schema. The logical schema must be adapted additively to SQLite/current SQLDelight or to an approved future service; it must not replace the existing database.

## Authentication, analytics, and privacy

- Authentication is offline-only. There is no OAuth, remote account service, recovery flow, role model, row-level authorization, or cross-device synchronization.
- Password hashing is a positive baseline, but the SQLite database itself is not encrypted. Documentation currently claims local data is encrypted; that statement is not supported by the implementation and must be corrected before a public AI Academy release.
- The Node server stores download counters, update logs, error logs, and WebSocket clients in process memory. On serverless/Vercel instances this is not durable analytics and must not be presented as product analytics or learner evidence.
- The repository and supplied files contain no configured production secret/service for an AI tutor, centralized content studio, sandbox provider, email, cloud database, or monitoring platform.

## Security and reliability findings

| Severity | Finding | Evidence | Required resolution before feature release |
| --- | --- | --- | --- |
| Critical | Learner code can run via JVM `ProcessBuilder` in the desktop application process context. | `shared/.../runner/CodeRunner.kt` | Replace with a sandboxed browser runtime for small exercises or an isolated runner service with CPU, memory, filesystem, network, timeout, and audit controls. Disable production execution until then. |
| High | No centralized authorization/roles exist for authoring, publishing, review, or rollback. | Local account schema and Node server | Owner must choose an identity/backend model before Content Studio implementation. |
| High | Existing runtime schema updates are imperative DDL in `ProgressRepository`; SQLDelight migration verification is disabled. | `ensureSupplementalSchema()` and build configuration | Introduce numbered, tested, backup-aware additive migrations before adding AI Academy tables. |
| High | Analytics are ephemeral and cannot substantiate the 7,000 learner claim or completion metrics. | `server/server.js` in-memory arrays/counters | Choose a privacy-conscious durable telemetry/monitoring service or keep claims uninstrumented. |
| Medium | Public landing page and README still describe a dark coding-through-math product, stale 10,000+/5,000 claims, and broad language support. | Public page/README audit | Replace only after the new product has real matching capability and approved copy. |
| Medium | Accessibility evidence is partial. The website has reduced-motion and ARIA hooks; desktop Compose has a reduced-motion setting but little semantic/keyboard/screen-reader coverage. | Source scan | Establish WCAG 2.2 AA automated/manual gates before UI migration. |

## Deployment and test baseline

- Vercel serves the landing page and update API. The audited live update endpoint publishes desktop version `1.3.0`.
- `npm test` passed: 7 tests, 0 failures.
- The Kotlin/JVM test and compile command was started as required by the audit; its result must be recorded from the running process before a migration phase begins.
- No CI workflow, production monitoring, durable analytics, or server-side migration environment was found in the repository audit.

## Conclusions

The safe path is an incremental AI Academy foundation, not a visual rewrite. Existing accounts/progress/content can be preserved locally, but the requested multi-device accounts, content workflow, durable analytics, and isolated execution require owner-approved service decisions. Legacy content must be flagged and mapped, not deleted. The 10,000 manifest must remain visibly planned until batch QA and review evidence exists.
