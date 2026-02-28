.define MMIO_BASE       0xF8000000
.define UART_DEVICE     #MMIO_BASE + 32 
.define DISK_DEVICE     #MMIO_BASE + 64 
.define BALLS 0x7B

; UART
.define UART_TX_READY   #UART_DEVICE + 0x8

; registers
.define DISK_ID         #DISK_DEVICE
.define DISK_STATUS     #DISK_DEVICE + 0x04 
.define DISK_CONTROL    #DISK_DEVICE + 0x08
.define DISK_COMMAND    #DISK_DEVICE + 0x0C

; command params
.define DISK_LBA        #DISK_DEVICE + 0x10
.define DISK_SEC_CNT    #DISK_DEVICE + 0x14
.define DISK_DMA_ADDR   #DISK_DEVICE + 0x18
.define DISK_DMA_LEN    #DISK_DEVICE + 0x1C

.ifdef STUFF
    .define ANOTHER_THING 2
.else
    .define ANOTHER_THING 1
.endif


@text
; i know we could use [Reg + offset] so we dont need so many LDIs
; but eh.
main:
    ; uart console & stack
    LDI     RSP, 0xFFFFFF ; setup the stack (primitive)
    LDI     R8,  #UART_DEVICE
    LDI     R7,  #UART_TX_READY
    ; disk
    LDI     RAX, 0xFF   ; DMA Address
    LDI     RDX, #DISK_DMA_ADDR
    LDI     RBX, #DISK_COMMAND
    LDI     R5 , #DISK_STATUS
    LDI     RCX, 0x02 ; IDENTIFY
    ; dispatch mmio command
    STW     [RDX], RAX
    STW     [RBX], RCX
    LDI     RDX, 0x01 ; DISK_BUSY
    ; you could use the CONTROL bit to make it IRQ, but fuck it
    ; i aint gonna install a fucking irq handler for it
    wait_disk_ready:
        LDW     RBX, [R5]
        CMP     RBX, RDX
        JEQ     wait_disk_ready     
    ; typedef struct {
	;    uint32_t sector_size;
	;    uint32_t sector_count;
    ;    uint32_t disk_bytes_hi;
	;    uint32_t disk_bytes_lo;
	;    char     disk_brand[16];
	;    char     disk_model[32];
	;    char     disk_serial[32];
	;    char     disk_firmware[16];
	; } DISK_IDENTIFY;
    LDI     RAX, 0xFF + (4 * 4) ; dma->disk_brand
    CALL    print
    LDI     RAX, 0xFF + (4 * 4) + 16 ; dma->disk_model
    CALL    print
    LDI     RAX, 0xFF + (4 * 4) + 16 + 32 ; dma->disk_serial
    CALL    print
    LDI     RAX, 0xFF + (4 * 4) + 16 + 32 + 32 ; dma->disk_firmware
    CALL    print
    KILL

print:
    ; RAX = pointer to data
    ; R8  = UART base
    ; R7  = UART_TX_READY
    MOV     RBX, RAX ; RBX = current pointer
    print_loop:
        LDB     RCX, [RBX]
        CMP     RCX, RZERO
        JEQ     done
        STB     [R8], RCX ; write byte to UART
        ADDI    RBX, 1
        wait_dev_ready:
            LDB     R4, [R7]
            CMP     R4, RZERO 
            JEQ     wait_dev_ready
        JMP     print_loop
    done:
    RET