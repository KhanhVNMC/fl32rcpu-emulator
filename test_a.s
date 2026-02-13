@data
vramOffset  .size(4) 534413312 ; .word is Ok too
bytes       .size(1) 0xFF
strings     .asciz "Hi"
string2     .ascii "World"
offset      .byte  97 - 'a' ; what
;blobOfBin   .blob ""
@text
; this is a long block of comment
; it has no problems because its ignored

nigga:
    JMP nigga

pseudoop_expanding:
    LD   RAX, $strings[0]
    LD   RBX, $strings[1]
    ; self modifying string
    LDI  RCX, 'o'
    ST   $strings[1], RCX
    ; runtime array action
    LDI  RAX, $strings ; obtain the pointer
    ADDI RAX, 1 ; now it is -> at 'o'
    LDB  R7, [RAX] ; load byte
    JMP  ok

test4:
    LDB  RAX, [RBX + (16 >> 1) + (4 * 5) / 42]
    STB  [RAX + (10 + 20 * 50)], RBX
    JMP  ok

test2:
    LD   RAX, $bytes[2]   ; smart $address[mode], size aware 
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