@data
vramBufferSelect    .word 1073588223
vramFrontBuffer     .word 1073588223 + 1
vramBackBuffer      .word 1073588223 + 1 + ((640 * 480) * 4)
;badapple            .blob "junk/badapple-video.bin"
badapple            .size(36)
end                 .size(0)
number     .word 36
@text

.define hello world

;JMP debug
setuptable:
    LDI  RAX, 0x2C ; interrupt table
    LEA  RBX, table
    STW  [RAX], RBX
    LEA  RBX, table2
    STW  [RAX + 4], RBX
    JMP  ok
debug:
    LEA  RAX, ok
    JR   RAX
ok:
    INT  0x0 ; here
    STI
    INT  0x1 
    LDI  RCX, 255
    KILL
table:
    LDI  RDX, 36
    ; (IRET-ish)
    GTPC R10
    STI
    JR   R10
table2:
    LDI  R8, 18
    ; (IRET-ish)
    GTPC R10
    STI
    JR   R10
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