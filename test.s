@data
vramOffset  .size(4) 534413312 ; .word is Ok too
bytes       .size(1) 0xFF
@text
; this is a long block of comment
; it has no problems because its ignored

test2:
    LD   RAX, $bytes[2]
    JMP  ok

main:
    LDI  RAX, $vramOffset
    LDW  RBX, [RAX]
    LD   RCX, $vramOffset[0]
    CMP  RBX, RCX
    JEQ  ok
    JMP  notok
ok:
    LDI  R8, 1
    KILL
notok:
    LDI  R8, 0
    KILL

start:
    ;hi
    LD    R2, $vramOffset   ; comment supported
loop:
    ADDI  R1, 0xFA
    ADDI  R2, 4
    CMP   R2, RSP
    JLE   loop
    KILL
skip:
    MOV   R8, RPC
    KILL

switchBuffer:
