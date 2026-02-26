
.include "disk.s" once
.include "randomshit.s" 

@text
balls:
    LDB     R1, [RPC + #BALLS]
    LDI     rax, 0xFF
    LDI     rbx, 0x0F 

    ; this writes the instruction: "LDB RAX/R1, [RPC + 0x10]"
    LLI     RCX, #BALLS + 1
    STB     [RAX], RCX
    LLI     RCX, 0x06
    STB     [RAX + 1], RCX
    LLI     RCX, 0x80
    STB     [RAX + 2], RCX
    LLI     RCX, 0x10
    STB     [RAX + 3], RCX

    ; setup the region
    MWST     rax, rbx
    LDI      RCX, 0xFF
    HLR      RCX ; de-escalate & jump to that region (will crash immediately)