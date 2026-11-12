
# CF-Evolution

## Introduction

CF-Evolution is a Java-based open source editor for Geoff Crammond's Formula One Grand Prix / World Circuit. It is built on the efforts undertaken in [Chequered Flag](https://github.com/CyberArchitect777/chequeredflag) which last saw an update in 2008. The original starting code for this successor project is the 0.1.1 tagged codebase from Chequered Flag.

## Notes

* File > Open Game Folder lists all editable elements in the game including the game program and the 16 circuits, each opening in their own tab. A packed GP.EXE is offered for unpacking, keeping the original as GPPACKED.EXE.
* The first time a game folder is opened, a copy of every file the editor can change is kept in CFE-BACKUP\original inside it.

## Features

CF-Evolution edits a whole F1GP/WC game folder: the circuits, the game program and the game's other files. Game datasets are also supported, CF-Evolution's own CFE format supports all appropriate edits for the actual game and a more limited import/export for F1ED and ArgEditor based on the capabilities of those tools.

### Track files

* Full editing capability is provided up to our knowledge and understanding of the game data. It is possible to create playable tracks with full computer car lines.
* Computer car lines can be generated automatically for any track by simulating laps and keeping the fastest line. An incomplete line can also be completed, leaving hand-edited sectors untouched. After editing a sector by hand, the rest of the line can be repaired so the lap keeps its shape.
* Complete random circuits can be generated, including elevation, kerbs, pit lane, starting grid, countdown boards and scenery taken from the base track.
* Tools > Check Track gives one verdict on whether a track is ready to race, with buttons to fix what it finds.
* Each circuit's car and computer car settings, race strategy (laps, first pit stop) and horizon picture can be edited.
* Track shaping tools are provided: reducing a track to a short straight canvas to build on, squaring up a track whose start and end headings do not match, and moving a track away from the edge of the game's map.
* Direct clicking on the track map is supported for element selection.
* The track map can also be exported as a PNG image.
* Computer cars at every circuit: how hard the computer cars corner (their aggression, or late braking) at all 16 circuits on one page, with buttons to set or raise them everywhere at once.

### Game executable

* Drivers (names, skill), teams (names, engines, power, line-ups), with names loaded from or saved to a name file, points, and the colours of every car, helmet and pit crew, shown as the game draws them.
* Game settings, including player and computer car power and grip, pit stops, the shortest race, cars out in practice and qualifying, best results that count, rain, damage and timers.
* The pit lane order, the human drivers' setups (each circuit's, and each car's for a first quick race), lap records, car physics and opposition levels.

### The game's other files

* Game options (F1PREFS.DAT), including the frame rate, saved setups, name files (from any folder) and championship saves, saved with the game's checksums.
* The game's pictures (export and import as PNG) and its sound effects.

## Getting Started

### Requirements

* Java 8 or later
* A copy of Formula One Grand Prix or World Circuit patched to version 1.05 (a packed executable is unpacked for you when the game folder is opened)

### Before you start

Opening a game folder keeps a copy of its files as they were the first time (see Notes). Keep your own copies of any track files you edit outside a game folder, and of work in progress you may want to go back to.

### Running

The editor is the file `cf-evolution.jar` in the `dist` folder. On most systems it can be started by double-clicking it, or from a command line with:

```
java -jar dist/cf-evolution.jar
```

The included scripts do the same on each system.

#### Windows

```bat
run.bat
```

#### Linux

```bash
./run.sh
```

To edit the game program and the circuits, use **File > Open Game Folder** and choose your F1GP/WC folder.

To build the editor from source code, see [DEVELOPMENT.md](https://github.com/CyberArchitect777/cf-evolution/blob/main/DEVELOPMENT.md).

## Repository

The CF-Evolution project is hosted on GitHub at this location: [CF-Evolution](https://github.com/CyberArchitect777/cf-evolution)

## Credits

As noted earlier, this project is based on the research and development undertaken for Chequered Flag by the following people:

* Klaus Six
* Barrie Millar
* Rene Smit

Thanks also to the following people: 

* Fredrik Meyer for his excellent work on the ArgTools project which is aiding further development here.
* Hrvoje Stimac for his excellent testing and suggestions as well as his F1GP community leadership.

## License

CF-Evolution is licensed under the GNU General Public License version 2, the same as with Chequered Flag. A full copy of the terms and conditions of this license is available in the LICENSE file included with the program. All project legal disclaimers and restrictions can be found there.
