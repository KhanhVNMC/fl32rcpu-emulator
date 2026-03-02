# FL32R Virtual System On a Crisp
Yes, that was not a typo.

## What is this?
FL32R is shorthand for **Fixed-Length 32-bit RISC** ISA, intended as the spiritual successor to the much more inferior  
[FL516 ISA & Emulator Project](https://github.com/khanhVNMC/cpu-emulator).

## This project contains:
- The definition of the FL32R Instruction Set Architecture
- The **first-party** assembler for the **FL32R ISA**
- The FL32R Virtual Machine (a.k.a. the emulator), with basic external devices support:
  - SoC Control
  - Hardware Timer
  - Debug UART (Java console)
  - Mass Storage Device(s)
  - VGA Adapter (Java Swing as the screen)
- A few random snippets of assembly (not tested as of right now)
- The BIOS/Firmware (in a separate repository), available [here](https://github.com/KhanhVNMC/firmware-bios-fl32rsoc)
- To be included at a later date: a **softcore** implementation of an FL32R-compliant processor (Verilog)

---
(C) 2026 GiaKhanhVN, go fucking wild.