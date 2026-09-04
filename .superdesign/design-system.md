# Nous AI Academy — Premium Desktop Design System

## Product character

Nous AI Academy is an offline-first AI learning workspace: editorial enough for sustained reading, precise enough for scientific work, warm enough for daily study, and rewarding without becoming a game. The interface must never fabricate learner activity or outcomes. All progress, recommendations, scores, and achievements come from local evidence.

The real orange book-and-circuit Nous logo is required in every logo position. Never replace it with initials, emoji, a generic AI mark, an invented SVG, or text alone.

## Color tokens

### Light

| Token | Value | Use |
| --- | --- | --- |
| App background | `#F7F3EC` | Main canvas |
| Elevated background | `#FBF8F3` | Top bars and raised regions |
| Main surface | `#FFFCF8` | Cards and reading paper |
| Secondary surface | `#F2EAE2` | Secondary panels |
| Primary orange | `#E9693A` | Brand and primary actions |
| Primary hover | `#D95A2F` | Hover |
| Primary pressed | `#BE4724` | Pressed |
| Soft orange | `#F9DED0` | Selected navigation and callouts |
| Deep terracotta | `#A94226` | Strong orange text |
| Celebration amber | `#F2B544` | Earned accomplishments only |
| Soft amber | `#FFF0C7` | Achievement surfaces |
| Success green | `#408A68` | Correct, completed, success |
| Soft success | `#DCEDE4` | Success surface |
| Information blue | `#5278A3` | Informational state |
| Soft information | `#E2EBF4` | Information surface |
| Error red | `#C74E46` | Errors and destructive actions |
| Soft error | `#F6DFDC` | Error surface |
| Main text | `#241D19` | Primary text |
| Secondary text | `#6A5E57` | Supporting text |
| Muted text | `#8D8179` | Metadata |
| Border | `#DED2C7` | Standard outline |
| Strong border | `#CBB9AC` | Emphasized outline |
| Disabled surface | `#ECE6E0` | Disabled control |
| Disabled text | `#A79D96` | Disabled label |

### Dark

| Token | Value |
| --- | --- |
| App background | `#171310` |
| Elevated background | `#1D1815` |
| Main surface | `#241E1A` |
| Secondary surface | `#2C241F` |
| Primary orange | `#F07948` |
| Primary hover | `#F28A5F` |
| Primary pressed | `#D95F34` |
| Soft orange | `#4A291D` |
| Celebration amber | `#F1BE55` |
| Success green | `#67AA87` |
| Information blue | `#78A0CC` |
| Error red | `#E07169` |
| Main text | `#F7EFE8` |
| Secondary text | `#C8BAB0` |
| Muted text | `#998C83` |
| Border | `#453930` |
| Strong border | `#5A493E` |

Color reinforces meaning but never carries it alone. Amber is reserved for real accomplishments, green for verified success, and red for errors or destructive actions.

## Typography

- Interface: locally bundled **Manrope**; headings, navigation, controls, metrics, labels.
- Reading: locally bundled **Source Serif 4**; article paragraphs and editorial quotations.
- Technical: locally bundled **JetBrains Mono**; code, IDs, output, formulas represented as code.
- Display: 40 px bold / 48 px line height.
- Page title: 32 px bold / 40 px.
- Section title: 26 px bold / 34 px.
- Card heading: 20 px semibold / 28 px.
- Lesson heading: 18 px semibold / 26 px.
- Body: 16 px / 25 px.
- Article: 17 px / 29 px, maximum readable width 800 px.
- Supporting: 14 px / 21 px.
- Caption: 12 px / 17 px.
- Code: 14 px / 21 px.

## Spacing and geometry

- Base spacing unit: 4 px.
- Page inset: 32 px standard, 24 px compact.
- Major regions: 24 px.
- Related controls: 8–12 px.
- Navigation: 248 px expanded, 76 px collapsed.
- Top bar: 68 px.
- Card radius: 16 px; feature card: 22 px; button: 12 px; small control: 9 px.
- Border: 1 px.
- Shadows: warm and subtle, never oversized.
- Content canvas: centered and bounded; article width 760–820 px.

## Shell

The persistent left rail contains the exact Nous logo and grouped destinations: Home, Learning Hub, Practice, Python Lab, Math Lab, Books, Intensive Files, Search, Progress, Updates, Settings. The current item has a 3 px orange indicator and soft-orange surface. Collapsed items retain accessible labels/tooltips and the state persists locally.

The compact top bar shows contextual page title, breadcrumbs inside curriculum, global offline search, offline status, curriculum version, profile, and an optional contextual action. It is calm and does not repeat navigation.

## Components

- Buttons: primary, secondary, tertiary, destructive, and icon-only with tooltip; minimum 42 px height.
- Cards: one clear purpose per surface; avoid nested borders and overcrowding.
- Status: icon plus label, never color alone.
- Section cards: section ID, title, level, description, five lessons, genuine progress, open action, PDF actions, restrained geometric subject motif.
- Lesson rows: explicit Not started, In progress, Article complete, Practice complete, Quiz passed, or Mastered state.
- Progress: animated only between real values and includes textual equivalent.
- Callouts: information, worked example, misconception, success, and error use semantic icon/title/body structure.
- Quiz choices: four accessible rows; answers remain hidden until submission; submitted state includes icon and label.
- Empty/error/update states always explain what happened, data safety, and the next action.

## Motion

- Micro interaction: 100–140 ms.
- Hover: 150–180 ms.
- Component: 200–240 ms.
- Page: 260–320 ms.
- Celebration: 700–900 ms maximum and only for earned milestones.
- Use ease-out entering, quicker exits, and subtle spring only for real progress.
- Cards rise 2–4 px, buttons compress slightly, navigation indicator glides, disclosures expand smoothly.
- Reduce Motion disables optional movement and honors the OS preference when available.

## Screen direction

- Home: real continue-learning command center, weekly evidence, current mastery, recent lessons/bookmarks, next curriculum recommendation, offline and update state.
- Learning Hub: searchable/filterable 20-section path with real progress and no fake locks.
- Lesson: persistent Article / Detailed Summary / Quiz sidebar, bounded reading column, TOC, practice access, notes and bookmarks.
- Practice: one focused problem with filters, local attempts, explanations, retry, and review queues.
- Python Lab: professional editor shell; Run disabled with an honest security explanation unless an approved isolated runner is available.
- Math Lab: structured problem, notation, steps, hints, answer and visualization region only when supported.
- Books and Intensive Files: cover-led library with verified local file actions and genuine reader continuity.
- Search: instant offline categories and direct unit navigation.
- Progress: accessible real-data summaries with text equivalents.
- Updates: app and curriculum versions, verified local package install, progress, rollback, and failure recovery.
- Settings: Appearance, Reading, Accessibility, Learning, Storage, Updates, Privacy, About.

## Accessibility and performance

- WCAG AA contrast, visible 2 px focus outline, logical keyboard order, labels/tooltips, screen-reader descriptions, minimum comfortable targets, text scaling without clipping.
- Do not communicate state by color alone.
- Respect reduced motion and provide a high-contrast option.
- Avoid loading the 10,000-problem bank into UI memory; load only the active lesson/practice slice and keep search indexing local.
- Avoid uncontrolled coroutines and animations.
