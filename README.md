# paktools

###### Level-5 PS2 Package Tools

`paktools` is a piece of software designed to manage Level-5 PS2 packages, including `.chr`, `.pac`, `.pak`, `.snd`
and others.

## Requirements

- A compatible 64-bit operating system
    - Windows
    - Linux

## Getting started

- Download the latest release [here](https://github.com/piorrro33/paktools/releases/latest)
    - You have to pick the version corresponding to your operating system
- Extract the archive and open the folder in a terminal
- Type `paktools --help` for a list of commands

You can also drag and drop onto the executable to easily extract and rebuild packages.

## Building

You will need JDK 17 on your computer.

- Clone the repository and open it in a terminal
- Execute `gradlew build`

You can also create GraalVM native images by executing `gradlew native-image`.

