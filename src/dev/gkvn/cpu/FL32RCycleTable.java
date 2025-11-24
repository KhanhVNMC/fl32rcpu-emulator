package dev.gkvn.cpu;

import static dev.gkvn.cpu.FL32RConstants.*;

public class FL32RCycleTable {
	// approximate cycles per instruction for FL32RCPU
	public static final int[] COST_TABLE = new int[256];
	
	static {
		// DEFAULT: 1 cycle for unspecified instructions
		for (int i = 0; i < COST_TABLE.length; i++) {
			COST_TABLE[i] = 1;
		}
		
		// DATA CONTROLS
		COST_TABLE[MOV] = 1;
		COST_TABLE[LUI] = 1;
		COST_TABLE[LDW] = 4; // memory read
		COST_TABLE[LDB] = 2; // byte read faster
		COST_TABLE[STW] = 4; // memory write
		COST_TABLE[STB] = 2; // byte write
		
		// ARITHMETIC
		COST_TABLE[ADD] = 1;
		COST_TABLE[SUB] = 1;
		COST_TABLE[MUL] = 3;
		COST_TABLE[DIV] = 8;
		COST_TABLE[MOD] = 8;
		COST_TABLE[AND] = 1; // bitwise is cheep
		COST_TABLE[OR] = 1;
		COST_TABLE[XOR] = 1;
		COST_TABLE[SHR] = 1;
		COST_TABLE[SHL] = 1;
		COST_TABLE[NOT] = 1;
		
		// immediate arithmetic
		COST_TABLE[ADDI] = 1;
		COST_TABLE[ORI] = 1;
		COST_TABLE[ANDI] = 1;
		
		// STACK
		COST_TABLE[PUSH] = 3; // stack write
		COST_TABLE[POP] = 3; // stack read
		
		// FLOW CONTROLS
		COST_TABLE[JMP] = 2;
		COST_TABLE[JR] = 2;
		COST_TABLE[CMP] = 1;
		COST_TABLE[JEQ] = 2;
		COST_TABLE[JNE] = 2;
		COST_TABLE[JLT] = 2;
		COST_TABLE[JGT] = 2;
		COST_TABLE[JLE] = 2;
		COST_TABLE[JGE] = 2;
		COST_TABLE[CALL] = 5; // stack + jump
		COST_TABLE[RET] = 5; // pop + jump

		// VMEM privileged
		COST_TABLE[VMO] = 1;
		COST_TABLE[VMB] = 1;

		// NOP
		COST_TABLE[NOP] = 1;
	}
}
