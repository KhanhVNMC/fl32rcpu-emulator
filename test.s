@data
vramOffset  .size(4) 534413312 ; .word is Ok too
@text
; this is a long block of comment
; it has no problems because its ignored
start:
    ;hi
    LD    R2, $vramOffset   ; comment supported
loop:
    ADDI  R1, 0xFF
    STW   [R2], R1
    ADDI  R2, 4
    CMP   R2, RSP
    JGT   loop
    KILL
skip:
    MOV   R8, RPC
    KILL
