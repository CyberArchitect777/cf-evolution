
# CF-Evolution

## Introduction

CF-Evolution is a Java-based open source editor for Geoff Crammond's Formula One Grand Prix / World Circuit. It is built on the efforts undertaken in [Chequered Flag](https://github.com/CyberArchitect777/chequeredflag) which last saw an update in 2008. The original starting code for this successor project is the 0.1.1 tagged codebase from Chequered Flag.

## Features

CF-Evolution supports editing two types of F1GP/WC data:

- Track files
    - Full editing capability is provided up to our knowledge and understanding of the game data. It is possible to create playable tracks with full computer car lines.
    - Computer car lines can be generated automatically for any track by simulating laps and keeping the fastest line. An incomplete line can also be completed, leaving hand-edited sectors untouched.
    - Complete random circuits can be generated, including elevation, kerbs, pit lane, starting grid, countdown boards and scenery taken from the base track.
    - Track shaping tools are provided: reducing a track to a short straight canvas to build on, and squaring up a track whose start and end headings do not match.
    - Direct clicking on the track map is supported for element selection.
    - The track map can also be exported as a PNG image.
- Game executable
    - A limited editing capability is provided to date. There are existing game editors available that offer more features at the moment.
    - The settings that can be edited include the following
        - Driver names, qualifying grip, and race grip values
        - Team names, BHP settings, and driver assignments
        - Points system configuration
        - Game settings (e.g. unlimited qualifying tyres)

## Getting Started / Installation

### Requirements

- Java 8 or later
- A copy of Formula One Grand Prix or World Circuit patched to version 1.05 (unpacked executable required for executable editing)

### Building from source

#### Windows

```bat
build.bat
```

#### Linux

```bash
./build.sh
```

Both scripts compile all sources and produce `dist/cf-evolution.jar`.

### Running

After building, the following command will run the editor on each system.

#### Windows

```bat
run.bat
```

#### Linux

```bash
./run.sh
```

Before opening the executable editor for the first time, go to **File > Options** and set the path to your F1GP/WC executable.

## Repository

The CF-Evolution codebase is hosted on Github at this location: [CF-Evolution](https://github.com/CyberArchitect777/cf-evolution)

## Credits

As noted earlier, this project is based on the research and development undertaken for Chequered Flag by the following people:

* Klaus Six
* Barrie Millar
* Rene Smit

Thanks also to Fredrik Meyer for his excellent work on the ArgTools project which is aiding further development here.

## License

CF-Evolution is licensed under the GNU General Public License version 2, the same as with Chequered Flag. A full copy of the terms and conditions of this license is available in the LICENSE file included with the program. All project legal disclaimers and restrictions can be found there.
