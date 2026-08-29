# Archived cloud-first route map — do not implement in the local-first release

The active desktop route map is in `LOCAL_FIRST_ARCHITECTURE.md`. Existing public installer and update compatibility paths remain untouched, but this version has no required online route.

# CodeQuest Academy — Route Map

**Phase:** 01 — proposed routes only. Existing public paths remain stable; this document does not implement navigation.

## Routing principles

- Preserve existing public website URLs and installer/update API paths.
- Keep desktop navigation usable offline. Route names are logical destinations, not a promise that every desktop screen will be URL-addressable on day one.
- Content IDs are stable logical IDs; versions are selected by publication state, not embedded in public learner URLs.
- Every protected route must have a server authorization check in addition to hiding navigation items.

## Public website

| Route | Audience | Purpose | Compatibility |
| --- | --- | --- | --- |
| `/` | Public | CodeQuest Academy landing page, English-only academy positioning, “Learn. Build. Improve.” | Preserve existing root URL |
| `/download` | Public | Download chooser and desktop requirements | Preserve existing route/links where present |
| `/installers/:asset` | Public | Release installer redirect/download | Preserve legacy installer route |
| `/privacy`, `/terms`, `/accessibility` | Public | Required policies and accessibility statement | New additive routes |
| `/signin` | Public | Browser sign-in/start account-link flow | New additive route |
| `/app` | Authenticated | Web entry/deep link to learner workspace where provided | New additive route |

The established release paths remain unchanged: `/api/app/latest-version`, `/api/app/check-updates`, `/api/download`, `/api/telemetry/*`, and any deployed installer compatibility URL. Their future implementations may gain durable storage but must preserve request/response compatibility until deprecation is explicitly announced.

## Learner workspace

| Logical route | Destination | Access | Offline behavior |
| --- | --- | --- | --- |
| `/app/home` | Home dashboard: continue learning, goal, streak, mastery, weak skills, review, projects | Learner | Cached dashboard with “not synced” status |
| `/app/learn` | Skill-path map, units, prerequisites, checkpoints | Learner | Cached published path only |
| `/app/learn/:trackId/:moduleId/:lessonId` | Lesson version selected by server publication pointer | Learner | Open cached version; defer evidence sync |
| `/app/practice` | Adaptive review, timed practice, interview/math drills, mistake notebook | Learner | Local/cached practice only; server recomputes final mastery |
| `/app/labs/python` | Python Lab | Learner | Browser sandbox where bundle is cached; advanced runs unavailable offline |
| `/app/labs/math` | Math Lab | Learner | Cached formulas/tools permitted; results queued |
| `/app/labs/algorithms` | Algorithm Visualizer | Learner | Fully local when visualizer assets are cached |
| `/app/labs/ml` | ML Lab / guided notebook | Learner | Read-only/cached guidance; advanced execution requires network |
| `/app/projects` | Project list, rubric, submissions, portfolio export | Learner | Drafts local; submissions queue |
| `/app/books` | Five academy books, reading, bookmarks, notes, download entitlement | Learner | Cached/downloaded chapters and notes |
| `/app/library` | Optional knowledge deep dives | Learner | Cached published files |
| `/app/progress` | Mastery, attempts, review, readiness | Learner | Provisional cached calculations clearly labelled |
| `/app/profile` | Account, linked devices, privacy/export preferences | Learner | Local settings; account changes need network |

## Content studio and operations

| Route | Required role | Purpose |
| --- | --- | --- |
| `/studio` | Author, reviewer, administrator | Content workspace home |
| `/studio/content` | Author+ | Draft tracks/modules/objectives/lessons/problems/books/knowledge files |
| `/studio/content/:type/:id/versions/:versionId` | Author/reviewer by assignment | Version editor, preview, provenance and validation evidence |
| `/studio/review` | Reviewer+ | Review queue, QA gates, approval/rejection |
| `/studio/publish` | Administrator or delegated publisher | Publish, unpublish, rollback; every action audited |
| `/studio/imports` | Administrator | Controlled batch import and legacy mapping review |
| `/ops/sync` | Administrator/support with explicit grant | Sync-health and migration reconciliation, no unrestricted learner data browse |
| `/ops/execution` | Administrator/security operator | Sanitized execution queue, quotas, incidents |

## Legacy and transition routes

Legacy content retains stable identifiers internally. New mappings live in `legacy_content_mappings` and redirect only after validation. Curriculum that is not part of the AI academy is hidden behind `legacy_content` rather than deleted. Existing desktop navigation can continue routing to its current screen until the equivalent new destination is released under a feature flag.

## Deep-link safety

Deep links carry logical IDs only. The client resolves the current published version after authorization. A learner cannot add `?version=draft` or substitute another account ID to see private content. Unknown or archived items return a clear “no longer available” state with an accessible back link; they never fall through to a blank page.
