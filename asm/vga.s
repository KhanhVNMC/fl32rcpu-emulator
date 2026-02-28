.define MMIO_BASE       0xF8000000
.define VGA_BASE        #MMIO_BASE + 96

.define VIDEO_CONTROL   #VGA_BASE + 0x00
.define VIDEO_MODE      #VGA_BASE + 0x04
.define CURSOR_X        #VGA_BASE + 0x18
.define CURSOR_Y        #VGA_BASE + 0x1C
.define CURSOR_CONTROL  #VGA_BASE + 0x20
.define TEXT_VRAM_BASE  #VGA_BASE + 0xF0 + 0x258000


@text
BALLS
__entry__:
    LDI     RAX, #VIDEO_CONTROL
    LDI     RBX, 0b01 ; VBLANKIRQ|ENABLE
    STW     [RAX], RBX

    LDI     RAX, #VIDEO_MODE
    LDI     RBX, 0x00 ; TEXT
    STW     [RAX], RBX

    LDI     RAX, #CURSOR_CONTROL
    LDI     RBX, 0b011 ; FULLBLOCK|BLINK|ENABLE
    STW     [RAX], RBX

    ; VERY crude program, for testing only
    LDI     RAX, #TEXT_VRAM_BASE
    LDI     RBX, (0 << 25) | (0 << 24) | ((15 % 16) << 8) | ('H' % 256)
    STW     [RAX], RBX
    
    LDI     RAX, #TEXT_VRAM_BASE + 4
    LDI     RBX, (0 << 25) | (0 << 24) | ((11 % 16) << 8) | ('i' % 256)
    STW     [RAX], RBX

    LDI     RAX, #TEXT_VRAM_BASE + 8
    LDI     RBX, (0 << 25) | (0 << 24) | ((15 % 16) << 8) | ('!' % 256)
    STW     [RAX], RBX

    LDI     RAX, #CURSOR_X ; move the cursor to x=3 (with x0=0)
    LDI     RBX, 3
    STW     [RAX], RBX

    KILL

    SPIN:
    JMP SPIN ; prevents it from shutting down

print:
