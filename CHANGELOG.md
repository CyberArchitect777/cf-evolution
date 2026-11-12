# Changelog

0.4.0

- A new interface design, with a tab for each open file, a start page, a toolbar and menus that follow the open file.
- File > Open Game Folder replaces Open Executable. It lists the game program and the 16 circuits, and now offers to unpack a packed GP.EXE.
- The executable editor now covers team line-ups, the colours of every car, helmet and pit crew (shown as the game draws them, with copy and paste), many more game settings, the pit lane order, setups, lap records, car physics and opposition levels.
- Some of the other files can now be edited: game options, saved setups, name files, championship saves, pictures and sound effects.
- Game datasets can be exported from one game folder and imported into another, and F1ED and ArgEditor carsets can be imported and exported.
- The first time a game folder is opened, its game files are copied to CFE-BACKUP\original, so they can always be put back.
- Tracks gain Car and AI Settings, Race Strategy and Horizon editing as well as a check track function.
- Editing the computer cars' cornering (aggression) for all 16 circuits is now possible on one page.
- Game options now support frame rate editing, up to 50 fps, with a warning above the game's own 25 fps.
- Setup values are limited to what the game's own setup screen allows.
- Driver, team and engine names can be loaded into GP.EXE from a name file, or saved from it into one.
- Generated tracks: fixed a best line running off the road, pit lane entrances hidden by walls and pits on the wrong side; fewer suggested-gear changes; no more scenery standing on the road; smoother best lines where the road is crossed quickly.
- Fixed the AI line at the start of tracks that cross the edge of the game's map.
- Saving now warns when a track is longer than the game allows.

0.3.1

- Fixed generation failure issues, particularly with the Germany and Belgium tracks.
- Fixed sizing issues in the Generate Random Track, Generate Best Line and Export Map Image dialogs.
- Fixed tracks that cross the edge of the game's map area being drawn in pieces, and added Tools > Move Track Away From Map Edge, since the game gives the AI the wrong racing line on such tracks.
- The mouse wheel now zooms the track map in when scrolled up, and trackpads zoom smoothly in the right direction.

0.3.0

- Random playable tracks can now be generated with changeable parameters. An AI drivable best line is also generated.
- Best lines can also be generated on existing tracks, either creating a completely new line, adding segments onto the existing line or refitting the rest of the line after an edit.
- The older best line generation methods are now hidden by default. They can be shown again by enabling legacy options under File -> Options.
- A minimal track mode has been created, cutting a circuit back to the bare basics for further user development.
- A circuit can be automatically joined up to ensure that the first and last track segments face the same way.
- A track map can now be exported to PNG.
- Clicking the track map now selects the matching best line sector, pit lane segment or track segment in the tree.
- The window title now warns when the best line is shorter than the track, which would leave the end of the lap without an AI line.
- Project website added under `web/`, published automatically to GitHub Pages.
- Several track editing process fixes, including resolving a best line length truncation error.
- Saving now warns when a track would freeze the game on loading, either because the best line has too many corners for the game's coaching table or because the file is too large.
- Generated tracks keep more of their best line's corners and drop scenery objects if needed to stay under the game's file size limit.
- Saving now warns if the pit lane is longer than 220 TLU, which freezes the game.

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
