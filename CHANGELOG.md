
# Changelog

0.3.0

- Random playable tracks can now be generated with changeable parameters. An AI drivable best line is also generated
- Best lines can also be generated on existing tracks, either creating a completely new line or adding segments onto the existing line.
- A minimal track mode has been created, cutting a circuit back to the bare basics for further user development.
- A circuit can be automatically joined up to ensure that the first and last track segments face the same way.
- A track map can now be exported to PNG.
- Clicking the track map now selects the matching best line sector, pit lane segment or track segment in the tree.
- The window title now warns when the best line is shorter than the track, which would leave the end of the lap without an AI line.
- Project website added under `web/`, published automatically to GitHub Pages.

0.2.0

- Initial version, built on the latest available code from Chequered Flag.
- Project restructured into a modern layout with basic documentation, build and run scripts added.
- Track map can now be panned using a mouse drag and the scroll-wheel can be used for zooming.
- Pit lane rendering has been improved (more work probably needed).
- Safe radius range provided for each CC line segment to guide users towards choosing game appropriate radius values.
- All Chequered Flag references renamed to CF Evolution.
- CC Line world position calculation now uses game-accurate formula based on past research to improve accuracy.
- Fixed a bug where wide-radius CC line segments calculated an incorrect radius due to an operator precedence error.
- Fixed a bug where extra side values on track segments were never stored due to a self-assignment error, affecting track edge rendering.
- Fixed an off-by-one error causing the CC line to misalign by one TLU when wrapping around the end of the track.
- Fixed an overflow that produced incorrect track positions for CC line sectors with very small radii.
