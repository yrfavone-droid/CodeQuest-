# Desktop Interaction Matrix (Historical Baseline — superseded)

| Screen | Element | Current Behavior | Expected Behavior | Intent/Route | Persistence | Test Status | Final Status |
|--------|---------|------------------|-------------------|--------------|-------------|-------------|--------------|
| App | Home Rail Item | Opens Dashboard | Opens Dashboard | `Screen.Dashboard` | None | Missing | Incomplete |
| App | Tracks Rail Item | Opens Track Browser | Opens Track Browser | `Screen.TrackBrowser` | None | Missing | Incomplete |
| App | Code Rail Item | `onClick = {}` | Remove or disable with reason | N/A | None | Missing | Incomplete |
| App | Rankings Rail Item | `onClick = {}` | Remove or disable with reason | N/A | None | Missing | Incomplete |
| App | Profile Rail Item | `onClick = {}` | Open Profile Screen | `Screen.Profile` | None | Missing | Incomplete |
| Dashboard | Continue Learning | Opens `FE-101` statically | Opens next available node for active track | `Screen.Node(id)` | None | Missing | Incomplete |
| Dashboard | Track Cards | Opens Track Browser | Open selected track details | `Screen.Track(id)` | None | Missing | Incomplete |
| Diagnostic | Check Answer | `onClick = { /* Check answer */ }` | Grade answer, show feedback, save state | Action | Update Node Progress | Missing | Incomplete |
| Diagnostic | Finish Diagnostic | Opens `FE-101` | Unlock next node, go back to timeline | Action | Mark Diagnostic Complete | Missing | Incomplete |
| LevelOverview | Take Diagnostic | Opens `FE-101-DIAG` statically | Opens Diagnostic for current level | `Screen.Diagnostic(id)` | None | Missing | Incomplete |
| Lesson | Next / Complete | Opens `FE-101` | Mark complete, unlock practice, advance | Action | Update Node Progress | Missing | Incomplete |
