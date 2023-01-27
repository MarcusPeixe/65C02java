# 65C02 Java Emulator

65C02 Emulator in Java with graphical interface.

![Screenshot of the emulator screen](imgs/screen.png)

# Features

## Assembler

This emulator comes with a 65C02 assembler written completely from scratch

## Files

You can drag and drop files into the code editor; you can load and save your
code; and you can even edit it somewhere else, and have the emulator reload it
every time you click "Assemble".

## Monitor

There is a monitor screen that shows 256 bytes of RAM at a time. This monitor
updates in real time and can be used to inspect the memory of the program
during execution. You also have the option to show a disassembled vision of
the code, which also updates in real time (in case you have self modifying
code).

## Interrupts

The emulator emulates interrupt functionality on the chip, allowing you to
send an interrupt every time you press a key (in addition to getting the key
code). You can also set periodic interrupts, to have an interrupt sent every
couple of ticks (and possibly synchronise some code).

## Debugger

There is a debugger available, through which you may run, stop, and step by
each instruction at a time. In addition to that, you can control the execution
speed, and send various interrupt signals to the CPU. While the program is
running, you can inspect the values of the registers and see them changing
through the console.

## Screen

There is a memory-mapped screen, which you can draw to. This allows you to
write more interesting programs, and maybe even little games.

## Modding?

The 65C02 CPU Emulator is a separate java class in the code. Memory mappers
and additional circuitry can be implemented through the provided class
interface, to interact with the processor. Please see Mapper.java, the
interface whose methods must be overridden for the emulator to work. You can
implement your own graphical interface or other program to interact with the
chip.

## How to build:

You must have the java JDK and JRE installed. You can verify by running the
following commands:

```bash
$ java --version
openjdk version "19.0.1" 2022-10-18
$ javac --version
javac 19.0.1
```

To download and run this project, you must type in the following commands (the
JavaFX dependencies come bundled with the source code):

```bash
git clone https://github.com/MarcusPeixe/65C02java.git
cd 65C02java
javac -d bin/ --module-path ./lib --add-modules javafx.controls,javafx.fxml src/*.java
java -cp bin/ --module-path ./lib --add-modules javafx.controls,javafx.fxml Main
```
