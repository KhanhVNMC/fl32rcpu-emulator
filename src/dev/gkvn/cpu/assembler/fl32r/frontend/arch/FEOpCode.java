package dev.gkvn.cpu.assembler.fl32r.frontend.arch;

import static dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOperandType.*;
import static dev.gkvn.cpu.assembler.fl32r.frontend.utils.FL32RSpecs.*;

public enum FEOpCode {
	// actual ops
	MOV   (1, REG, REG),
	LUI   (1, REG, IMM16_ABS),
	LLI   (1, REG, IMM16_ABS),
	LDW   (1, REG, MEMORY),
	LDB   (1, REG, MEMORY),
	STW   (1, MEMORY, REG),
	STB   (1, MEMORY, REG),
	
	ADD   (1, REG, REG, REG),
	SUB   (1, REG, REG, REG),
	MUL   (1, REG, REG, REG),
	UMUL  (1, REG, REG, REG),
	DIV   (1, REG, REG, REG),
	UDIV  (1, REG, REG, REG),
	MOD   (1, REG, REG, REG),
	UMOD  (1, REG, REG, REG),
	
	AND   (1, REG, REG, REG),
	OR    (1, REG, REG, REG),
	XOR   (1, REG, REG, REG),
	SHL   (1, REG, REG, REG),
	SHR   (1, REG, REG, REG),
	SRA   (1, REG, REG, REG),
	NOT   (1, REG, REG),
	
	ADDI  (1, REG, IMM19_ABS),
	ANDI  (1, REG, IMM19_ABS),
	ORI   (1, REG, IMM19_ABS),
	XORI  (1, REG, IMM19_ABS),
	SHRI  (1, REG, IMM19_ABS),
	SRAI  (1, REG, IMM19_ABS),
	SHLI  (1, REG, IMM19_ABS),
	
	PUSH  (1, REG),
	POP   (1, REG),
	
	CMP   (1, REG, REG),
	
	JMP   (1, IMM24_PC_REL),
	JEQ   (1, IMM24_PC_REL),
	JNE   (1, IMM24_PC_REL),
	JLT   (1, IMM24_PC_REL),
	JGT   (1, IMM24_PC_REL),
	JGE   (1, IMM24_PC_REL),
	JLE   (1, IMM24_PC_REL),
	JOF   (1, IMM24_PC_REL),
	JNO   (1, IMM24_PC_REL),
	CALL  (1, IMM24_PC_REL),
	
	CLR   (1, REG),
	JR    (1, REG),
	
	RET	  (1),
	INT	  (1, IMM24_ABS),
	
	VMO   (1, REG),
	VMB   (1, REG),
	HLR   (1, REG),
	STI   (1),
	CLI   (1),
	GTFS  (1, REG),
	GTPC  (1, REG),
	STFS  (1, REG),
	
	NOP   (1),
	HLT   (1),
	KILL  (1),
	
	// pseudo-op (instructions that expand into other instruction(s))
	LDI   (2, REG, IMM32_ABS),
	LD    (1, REG, VARIABLE),
	ST    (1, VARIABLE, REG),
	LEA   (2, REG, VARIABLE_OR_LABEL),
	;
	
	// cunt
	private final int size;
	private final FEOperandType[] operands;
	
	public static final FEOpCode[] values = FEOpCode.values();
	
	FEOpCode(int expandInto, FEOperandType... operands) {
		this.size = expandInto * FL32R_SIZE;
		this.operands = operands;
	}
	
	public int getSize() {
		return size;
	}
	
	public FEOperandType[] getOperandKinds() {
		return operands;
	}
	
	public static FEOpCode get(String str) {
		try {
			return FEOpCode.valueOf(str);
		} catch (Exception e) {
			return null;
		}
	}
	
	public boolean isPseudoOp() {
		return size != FL32R_SIZE;
	}
}
