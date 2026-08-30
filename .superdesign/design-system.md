# Nous AI Academy clean-library direction

## Product and boundary

Nous AI Academy is an offline-first Windows desktop reading environment for serious AI study. This design represents the clean transition state before the owner supplies the final curriculum package: the product may have accounts, settings, reader history, bookmarks, and search infrastructure, but it must not present any books, intensive files, counts, lessons, completion claims, sample documents, or catalogue cards as if content were installed.

The public site must accurately say that the desktop application is being prepared for the official Nous curriculum release. The download experience remains visible only when a real, verified installer manifest exists; it must never promise an unavailable file.

## Brand

- Product name: **Nous AI Academy**.
- Use the supplied orange book-and-circuit logo in every logo position. Never substitute initials, emoji, generic AI glyphs, invented SVGs, or text alone.
- Wordmark pairing: logo followed by **Nous AI Academy**; use **Read deeply. Build locally.** as the supporting line where appropriate.
- Typography: `Outfit` for focused display headlines, `Inter` for UI/body, and `JetBrains Mono` only for small technical metadata.

## Visual system

- Ink: `#211812`; espresso: `#34241B`; warm orange: `#E95C24`; deep orange: `#B83A16`; amber: `#F5A524`.
- Canvas: `#FFF8F2`; paper: `#FFFFFF`; line: `#E9D8CC`; muted: `#76685F`; success: `#23835B`.
- Use white reading surfaces, dark text, restrained orange actions, a 12-column desktop grid, 12–20 px rounded corners, and generous 24–96 px vertical rhythm.
- Do not use purple, cyan, neon effects, glassmorphism, stock robot artwork, fake charts, generic AI gradients, or decorative equations.
- Motion is purposeful and short; honour `prefers-reduced-motion`. Every focus state is visible and contrast meets WCAG AA.

## Experience

- Tone: quiet, serious, academic, reading-first, trustworthy. It should feel like carefully engineered library software, not a gamified learning product.
- Primary desktop areas: Home, Learning Library, Books, Intensive Files, Reading Progress, Bookmarks, Search, Settings, and About.
- Empty library states explain that the final official curriculum package is not installed yet and state exactly what will become available after verification. Do not show fake content.
- Preserve user-created reading history and bookmarks in a clearly labelled legacy/archive-safe state without implying that removed documents remain readable.

## Public landing page

1. Header with the supplied real logo, simple navigation, keyboard-accessible focus behavior, and a verified download action.
2. Hero with a clear transition message: a private, offline-first AI reading workspace that is being prepared for the official curriculum package. Avoid unverified numerical claims.
3. Capability panel with accurate platform proof: local reader, downloaded PDFs, progress and bookmarks kept on device, and package verification before publishing.
4. A clear "Curriculum release" status area—not publication cards—describing the forthcoming package without dates, counts, or invented titles.
5. Compact privacy, installation, FAQ, and footer sections.

## Implementation constraints

- Use only real brand assets included in the repository.
- Keep existing accounts, settings, valid user-created files, reading progress, and bookmarks intact.
- Do not include old CodeQuest labels in user-facing copy.
- Do not fabricate download URLs, installer sizes, release notes, content metadata, analytics, or activity.
