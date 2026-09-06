# Nous AI Academy — Monochrome Website System

## Product and purpose

Nous AI Academy is an offline-first Windows AI learning application. The public website must feel like the same product as the desktop workspace: precise, technical, calm, private by default, and honest about locally stored learning data. Preserve the real Nous AI Academy logo in the header, preview, and footer. Never replace it with text-only branding, initials, emoji, or a generic AI symbol.

## Visual language

- Strict monochrome only: black, white, and grayscale. No orange, brown, beige, blue, green, purple, red, gradients, or glass effects.
- Light canvas: `#FFFFFF`; secondary canvas: `#F7F7F7`; surface: `#FFFFFF`; soft surface: `#F2F2F2`.
- Text: strong `#0A0A0A`; standard `#242424`; secondary `#656565`; muted `#909090`.
- Borders: `#E2E2E2`; strong borders: `#BDBDBD`.
- Dark release section: `#090909`; supporting dark surface: `#161616`; light text: `#FFFFFF`; secondary dark text: `#AEAEAE`; dark border: `#303030`.
- Primary action: black fill `#0A0A0A` with white text. Hover `#242424`; pressed `#000000`.
- Secondary action: white fill, `#CFCFCF` border, black text. Do not use colored accents for success, warnings, or state; use clear labels, icons, and outline/fill treatment instead.

## Typography and geometry

- Interface: Inter or an equivalent system sans-serif. Code/checksum labels: JetBrains Mono or monospace fallback. Do not load fonts at runtime; use local/system font stacks.
- Large headline: bold, 56–76px desktop, tightly tracked but highly legible. Body: 16–18px with generous 1.6 line height.
- Maximum content width: 1240px. Page insets: 32px desktop, 22px compact.
- Cards: 14–18px radius, 1px grayscale border, restrained shadows only where hierarchy needs it.
- Buttons: 10–12px radius, 42px minimum target height, clear keyboard focus outline.

## Layout

- Header: minimal white bar, black logo treatment, quiet text links, black Download action.
- Hero: editorial two-column composition. Left communicates the verified local-first desktop release. Right is a grayscale desktop-workspace preview with a compact sidebar, top bar, status label, and content surface.
- Proof and library regions: flat white or soft-gray cards with dark typography; no decorative color blocks.
- Install region: practical release details, checksum, and accessible download guidance.
- Release-integrity section: near-black surface with white typography and grayscale metric cards.
- Footer: minimal, bordered, and monochrome.

## Interaction and accessibility

- Visible 2–3px black focus outline, logical keyboard order, semantic headings, and descriptive logo alt text.
- Hover 150–180ms; button press 100–140ms; card lift at most 2px; page movement 220–280ms. Disable nonessential motion with `prefers-reduced-motion`.
- The live `api/download?os=windows` route remains the only download path. Release version, filename, size, and checksum continue to be populated from the live release endpoint.
- Never claim a capability, completion state, or security property that is not present in the actual release.
