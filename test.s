@data
vramBufferSelect    .word 534413311
vramFrontBuffer     .word 534413311 + 1
vramBackBuffer      .word 534413311 + 1 + ((640 * 480) * 4)
badapple            .blob "apple.bin"
end                 .size(0)

@text
JMP main
;JMP debug

debug:
    LD     RBX, $vramFrontBuffer
    LD     RAX, $badapple
    STW    [RBX], RAX
    KILL
main:
    LDI  RAX, $end
    LDI  RBX, $badapple
    SUB  RDX, RAX, RBX  ; RDX = length
    LD   RAX, $vramFrontBuffer ; RAX = vramPointer
    LDI  RBX, $badapple        ; RBX = imagePointer
    MOV  RCX, R0 ; counter
loop:
    LDW  R5, [RBX]
    STW  [RAX], R5
    ADDI RAX, 4  ; rax++
    ADDI RBX, 4
    ADDI RCX, 4
    CMP  RCX, RDX ; if (rax < rdx) loop;
    JLT  loop
    KILL