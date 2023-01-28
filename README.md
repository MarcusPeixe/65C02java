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

# How to use

Every time a key is pressed, its ASCII code is stored in `$FF`, and if the
"IRQ on key press" checkbox is checked, an Interrupt Request Signal is
triggered.

If the "Periodic IRQs" slider is set to a non-zero value, every N ticks an
IRQ is triggered.

To assign functions as interrupt handlers, put their labels/addresses at the
corresponding memory locations:

```
$FFFA-$FFFB : Non Maskable Interrupt (NMI)
$FFFC-$FFFD : Reset (RES)
$FFFE-$FFFF : Interrupt Request (IRQ)
```

Example:

```
.org $FFFA
.word NMI, RES, IRQ
```

Memory locations `$0400` to `$07FF` map to the screen pixels. Different values 
will draw different colour/character pixels. The colours are:

```
$00: Black
$01: Dark Red
$02: Dark Green
$03: Dark Yellow
$04: Dark Blue
$05: Dark Purple
$06: Dark Cyan
$07: Light Grey

$08: Dark Grey
$09: Light Red
$0A: Light Green
$0B: Light Yellow
$0C: Light Blue
$0D: Light Purple
$0E: Light Cyan
$0F: White

$10 - $1F: 2x2 Cell with black and white pixels. The format is %0001_ABCD,
where A, B, C, and D are the bits referring to each subpixel (0 is black
and 1 is white).

$20 - $7F: ASCII character

$80 - $BF: Colours (each RGB channel has a 2 bit depth, as in %10_BBGGRR)
$C0 - $FF: Greyscale 
```

## Assembler syntax

Single line comments begin at a `;` and go until the end of the line.
Block comments are enclosed between `;*` and `*;`.

Number literal notations are:

- `123` for decimal
- `$1234` for hex
- `%10101010` for binary

Strings must be enclosed in single or double quotes. The delimiter used
must be escaped with a backslash if it appears within the string. The
following escape sequences are recognised: `\b`, `\n`, `\r`, `\t`, `\e`,
`\'`, `\"`, `\\`, `\0xNN`, `\0NNN`, and `\NNN` (N are digits in the
appropriate base).

The assembler directives are:
- `.org ADDRESS` - Generates subsequent machine code at ADDRESS.
- `.byte BYTE1, [...]` - Generates constant byte(s).
- `.word WORD1, [...]` - Generates constant word(s). Each value is cast to a
  word, and endianness is taken care of.
- `.text TEXT` - Generates a constant string in memory.
- `.string TEXT` - Generates a null-terminated constant string.
- `.data SIZE` - Advances SIZE bytes, reserving space for data.

You can define labels for branching by writing their name and ending it with
a `:`. Other constants are defined by writing `NAME = VALUE`.

Regular labels are referred to as global labels, and they define a "scope"
until the next global label. If a label or constant begins with a dot `.`,
then they are considered local to the scope they are defined in.

Local symbols can be referred to in the scope they are defined by simply
their name and the prefix dot (as in `.NAME`). They can also be referred to
in other scopes by writing their full name `SCOPE.NAME` (where SCOPE is the
name of the global label). The scope before the first global label is called
`_root`.

Everywhere a value is expected (as in instruction operands, directive
arguments, and even definitions of constants), you can instead write an
expression. They can evaluate to a byte, to a word, or even to a string
(but they must respect the expected size in bytes for the value).

Unary operations defined:
- `~` - Complement
- `-` - Negate
- `<` - Low byte of a word
- `>` - High byte of a word
- `#` - Length of a string

binary operations defined:
- `+` - Add (or concatenate strings)
- `-` - Subtract
- `*` - Multiply
- `/` - Divide
- `&` - Bitwise AND
- `|` - Bitwise OR
- `^` - Bitwise XOR
- `%` - Modulo

Some constants are already defined in the format: _OP_ADDR (where OP is
the mnemonic, and ADDR is the three-letter name for the addressing mode).
They evaluate to the opcode of that instruction, if it exists with that 
particular addressing mode.

Branch instructions that are out of reach are automatically converted to
a JMP instruction in machine code, as in:

```
  beq label
```

becomes effectively

```
  bne else
  jmp label
else:
```

## Examples

Some examples are available in the `tests` folder included in this repo.
But you can also try the following code:

```
screen  =  $0400
code    =  $0800
msg     =  "Hello, world!"
size    =  #msg

; This code writes hello
; world to the screen.
  .org code
main:
  ldx  #0

.loop:
  lda  txt, x
  beq  .end
  sta  screen, x
  inx
  bra  .loop

.end:
  stp

txt:
; Null terminated string
  .string msg

; Ignore interrupts
irq: nmi:
  rti

  .org $fffa
  .word nmi, main, irq
```

# How to build:

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
