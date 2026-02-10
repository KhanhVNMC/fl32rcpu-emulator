package dev.gkvn.cpu.fl32r;

public class FL32RConstants {
	// a word is 32-bit
	public static final byte // INSTRUCTIONS
		// DATA controls
		MOV  = 0x01, // reg-to-reg move; rDest = rSrc
		LUI  = 0x02, // load upper immediate (16-bit MSB); clears rDest before loading
		LLI  = 0x03, // load lower immediate (16-bit MSB); clears rDest...
		LDW  = 0x04, // load word from mem into reg; rDest = Memory[rAddr]...Memory[rAddr + 3]
		LDB  = 0x05, // load byte from mem into reg; rDest = Memory[rAddr]
		STW  = 0x06, // store word from reg into mem; Memory[rAddr]..Memory[rAddr + 3] = rSrc
		STB  = 0x07, // store byte from reg into mem; Memory[rAddr] = rSrc
		
		// ARITHMETIC CONTROLS
		ADD  = 0x10, // add; rDest = rOp1 + rOp2
		SUB  = 0x11, // same thing...
		MUL  = 0x12,
		UMUL = 0x13,
		DIV  = 0x14,
		UDIV = 0x15,
		MOD  = 0x16,
		UMOD = 0x17,
		AND  = 0x18,
		OR   = 0x19,
		XOR  = 0x1A,
		SHR  = 0x1B, // shift right logic
		SRA  = 0x1C, // shift right arithmetic
		SHL  = 0x1D, // shift left
		NOT  = 0x1E,
		// immediate
		ADDI = 0x20,
		ORI  = 0x21,
		XORI = 0x22,
		ANDI = 0x23,
		SHLI = 0x24, // shift left
		SRAI = 0x25, // shift right arithmetic
		SHRI = 0x26, // shift right logic
		
		// STACK CONTROLS
		PUSH = 0x40,
		POP  = 0x41,
		
		// FLOW CONTROLS
		JMP  = 0x30, // [RJUMP = Relative Jump] jump to an offset relative to PC (set RPC)
		JR   = 0x31, // JUMP to an address in rAddr
		CMP  = 0x32, // COMPARE rOp1,rOp2; sets flags: ZFL (zero) and NFL (less-than)
		JEQ  = 0x33, // IF EQUAL then RJUMP (ZFL = TRUE)
		JNE  = 0x34, // IF NOT EQUAL then RJUMP (ZFL = FALSE)
		JLT  = 0x35, // IF LESS THAN then RJUMP (NFL = TRUE)
		JGT  = 0x36, // IF GREATER THAN then RJUMP (NFL = FALSE)
		JLE  = 0x37, // IF LESS OR EQ then RJUMP (NFL = TRUE, ZFL = TRUE)
		JGE  = 0x38, // IF GREATER OR EQ then RJUMP (NFL = FALSE, ZFL = TRUE)
		JOF  = 0x39, // IF SIGNED ARITHMETIC OVERFLOW then RJUMP (OFL = TRUE)
		JNO  = 0x3A, // IF SIGNED ARITHMETIC NOT OVERFLOW then RJUMP (OFL = FALSE)
		CALL = 0x3B, // CALL a function (RJUMP) (return address PUSH into stack)
		CLR  = 0x3C, // CALL a function from an address in register (rAddr, like JR)
		RET  = 0x3D, // RETURN by popping the stack and JUMP ABSOLUTE there
		
		// MEMORY MANAGEMENT/CPU STATE (privileged instructions starts with 0x5)
		/*
		 * Virtual Memory (VMEM) - Brief Documentation
		 *
		 * These instructions are only available to HLP (Host-Level Privilege) entities.
		 * When VMEM is enabled, all subsequent memory accesses (load, store, stack,
		 * jumps that reference memory, etc.) are translated relative to a protected
		 * address range. User programs therefore see their memory starting at 0, while
		 * the host enforces the real mem bounds
		 *
		 * - VMO sets the base offset. All addresses become: physical = VMO + address 
		 * - VMB sets the maximum accessible range. Access outside the range raise a FAULT 
		 * - Setting either value to 0 disables VMEM protection and grants full access
		*/
		VMO  = 0x50, // set Virtual Memory offset (0 for FULL ACCESS)
		VMB  = 0x51, // set Virtual Memory max bound (set to 0 for FULL ACCESS)
		
		// SPECIALS
		HLR  = 0x52, // de-escalation, clear HLR and return to the address on the register
		NOP  = 0x00, // no-op
		HLT  = 0x7A, // halt the cpu
		KILL = 0x7B // kill the cpu and print debug (EMU ONLY), like HLT on FL516, on real HW, this is NO-OP
	;
	
	public static final int // SPECIAL REGISTERS INDEX
		REG_ZERO = 24, // zero reg, just 0
		// usermode-accessible
		REG_STACK_POINTER = 25,
		REG_PROGRAM_COUNTER = 26,
		// HLP registers (>= VMEM_OFFSET == HLP-only)
		REG_VMEM_OFFSET = 27,
		REG_VMEM_MAX_BOUND = 28,
		REG_HLP_APR1 = 29, // all-purpose-register
		REG_HLP_APR2 = 30,
		REG_HLP_APR3 = 31
	;
}
