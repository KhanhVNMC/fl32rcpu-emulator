.define MMIO_BASE       0xF8000000
.define TIMER_DEVICE    #MMIO_BASE + 0
.define UART_DEVICE     #MMIO_BASE + 16

; UART
.define UART_TX_READY   #UART_DEVICE + 0x8

; TIMER
.define TIMER_COUNT_LO  #TIMER_DEVICE + 0x0
.define TIMER_COUNT_HI  #TIMER_DEVICE + 0x4
.define TIMER_CNT_CTRL  #TIMER_DEVICE + 0x0C

.define TIMER_ENABLE    1 << 0

@data
string .ascii "Hello World Ball Sack. "
str2   .asciz "This string would be printed too." 
@text

__entry__:
    LDI     RSP, 0xFFFFFF ; setup the stack (primitive)
    ; turn on timer
    LDI     R8, #TIMER_ENABLE
    LDI     R7, #TIMER_CNT_CTRL
    STW     [R7], R8
    ; sleep
    LDI     r8, 5000000
    CALL    sleep
    KILL

get_time:
    LDI     RAX, #TIMER_COUNT_LO
    LDI     RBX, #TIMER_COUNT_HI
    LDW     R3, [RAX]
    LDW     R4, [RBX]
    RET
    
sleep:
    ; R8 = sleep time
    CALL    get_time
    ADD     R8, R3, R8
    roll:
        CALL    get_time
        CMP     R3, R8
        JLE     roll
    RET

main:
    LDI     R8,  #UART_DEVICE
    LDI     R7,  #UART_TX_READY
    LEA     RAX, $string ; pointer passed in RAX
    CALL    print
    LDI     R10, 255
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