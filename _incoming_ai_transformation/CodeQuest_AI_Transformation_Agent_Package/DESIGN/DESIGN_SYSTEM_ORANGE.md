# CodeQuest AI Academy - Warm Orange Design System

## Brand principle

Warm, thoughtful, technical, and human. The interface should feel like a serious learning studio with the clarity and encouragement of a modern language-learning product. It may use a warm editorial AI aesthetic, but it must remain recognizably CodeQuest and must not copy another company's interface or assets.

## Color tokens

- primary-600: #E56A3A - principal action and active path
- primary-700: #C9542A - hover and pressed action
- primary-100: #FBE4D7 - selected and supportive surfaces
- canvas: #FBF5EE - warm application background
- surface: #FFFDFC - cards, editor shells, and reading surfaces
- ink-950: #271F1B - primary text
- ink-700: #574A43 - secondary text
- ink-500: #7B6D65 - tertiary text
- border: #E8D8CE - standard dividers
- success: #2F7D5B
- warning: #B87516
- danger: #B5443C
- info: #3D6F91

Do not use low-contrast orange text on cream. Primary buttons use white text on primary-700 or darker. All focus rings must be visible.

## Typography

- UI sans: Inter, Geist, or the project's existing highly legible sans-serif.
- Reading serif (optional only inside books): Source Serif 4 or an existing licensed serif.
- Code: JetBrains Mono, IBM Plex Mono, or the existing monospace.
- Minimum body size: 16px desktop and mobile. Use 1.5-1.7 line height for reading.

## Geometry

- 4px base spacing; common gaps 8, 12, 16, 24, 32, 48.
- Card radius 16px; controls 10-12px; pills fully rounded only for tags and compact statuses.
- Shadows are soft and rare. Borders and surface contrast carry most hierarchy.

## Motion

- 120-220ms for state transitions.
- Use motion for progress, path unlocking, feedback, and panel transitions.
- No decorative constant motion. Respect reduced-motion settings.

## Component rules

- One clear primary action per region.
- Use real icon components from one consistent icon set; no emoji as interface icons.
- Learning path nodes show state: locked, available, active, practiced, mastered.
- Feedback colors never stand alone; pair with icons and text.
- Editors use a calm dark or light code surface separated from the warm navigation shell.
- Charts include labels, accessible descriptions, and non-color indicators.

## Responsive behavior

- Mobile: bottom navigation for Home, Learn, Practice, Labs, and More.
- Tablet: collapsible rail.
- Desktop: left navigation plus main canvas and optional right contextual panel.
- Reading and lesson content stays within a comfortable 65-80 character line length.
