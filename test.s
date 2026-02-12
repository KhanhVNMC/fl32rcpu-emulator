@data
vramBufferSelect    .word 1073588223
vramFrontBuffer     .word 1073588223 + 1
;vramBackBuffer      .word 1073588223 + 1 + ((640 * 480) * 4)
badapple            .blob "junk/badapple-video.bin"
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
    LDI  R6, 0xCAFEBABE
cpy_frame_loop:
    LDW  R5, [RBX] 
    CMP  R5, R6 ; this is a very stupid way to detect frame end...
    JEQ  next_frame
    STW  [RAX], R5
    ADDI RAX, 4  ; rax += 4
    ADDI RBX, 4
    ADDI RCX, 4
    CMP  RCX, RDX ; if (rax < rdx) loop;
    JLT  cpy_frame_loop
    KILL
next_frame:
    ADDI RBX, 4
    LD   RAX, $vramFrontBuffer
    JMP  cpy_frame_loop