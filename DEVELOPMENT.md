# CF-Evolution — Development

This guide is for working on the editor's source code. Instructions for using the editor are in [README.md](README.md).

## Building from source

### Requirements

- A Java Development Kit (JDK), version 8 or later. A plain Java runtime is not enough, because building needs the `javac` compiler. Newer JDKs still build for Java 8.

### On Windows

```bat
build.bat
```

### On Linux

```bash
./build.sh
```

Both scripts compile all sources and produce `dist/cf-evolution.jar`, which can then be started with `run.bat` or `./run.sh`.

The scripts compile against the Java 8 class library, so code that uses anything newer fails to build rather than failing on a user's Java 8 installation.
