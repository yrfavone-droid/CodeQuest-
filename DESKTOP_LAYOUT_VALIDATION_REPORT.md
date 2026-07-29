# Desktop layout validation

The Compose shell targets a 1440×900 default window with a 960×640 minimum, scrollable content,
responsive track grids, navigation collapse, visible loading/error/empty/locked states, and
bottom padding for final learning-map actions. Authentication has a centered, keyboard-friendly
card with explicit loading, cancel, retry, timeout, network, and missing-configuration messages.

Automated desktop process smoke testing confirmed the packaged app starts independently. AWT
window screenshots were not captured in this headless validation session, so no visual screenshot
claim is made.
