
# Changelog

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
