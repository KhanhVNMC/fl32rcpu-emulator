.define VGA_BASE        0xf8000000 + 0x7c00000

.define VIDEO_CONTROL   #VGA_BASE + 0x00
.define VIDEO_MODE      #VGA_BASE + 0x04
.define CURSOR_X        #VGA_BASE + 0x18
.define CURSOR_Y        #VGA_BASE + 0x1C
.define CURSOR_CONTROL  #VGA_BASE + 0x20
.define TEXT_VRAM_BASE  #VGA_BASE + 0xF0 + 0x258000

.define SOC_CTRL_BASE   0xf8000000 ; mmio base

@data
hello    .asciz "Hello"
@text

_vga_entry:
    ; tell the screen to wake the fuck up
    LDI     RAX, #VIDEO_CONTROL
    LDI     RBX, 0b01 ; VBLANKIRQ|ENABLE
    STW     [RAX], RBX

    LDI     RAX, #VIDEO_MODE
    LDI     RBX, 0x00 ; TEXT
    STW     [RAX], RBX

    LDI     RAX, #CURSOR_CONTROL
    LDI     RBX, 0b011 ; FULLBLOCK|BLINK|ENABLE
    STW     [RAX], RBX

    LDI     RBX, (0 << 25) | (0 << 24) | ((15 % 16) << 8)
    ; VERY crude program, for testing only
    LDI     RCX, #SOC_CTRL_BASE + 0xF0 ; SOC_BRAND_STRING
    LDI     RAX, #TEXT_VRAM_BASE
    LDI     R10, 16 + 1 + 48
    wrt:
        LDB     R6, [RCX]
        OR      RDX, RBX, R6
        STW     [RAX], RDX
        ADDI    RCX, 1
        ADDI    RAX, 4
        ADDI    R9, 1
        CMP     R9, R10
        JNE     wrt

    LDI     RAX, #CURSOR_X ; move the cursor to x=3 (with x0=0)
    LDI     RBX, 3
    STW     [RAX], R10

    LDI     RCX, #SOC_CTRL_BASE
    LDI     RAX, 1 << 0 ; power off the platform
    STB     [RCX], RAX

    SPIN:
    JMP SPIN ; prevents it from shutting down
