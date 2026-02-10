package dev.gkvn.cpu.assembler.fl32r.frontend.arch;

import static dev.gkvn.cpu.assembler.fl32r.frontend.arch.OperandKind.*;
import static dev.gkvn.cpu.assembler.fl32r.frontend.utils.FL32RSpecs.*;

public enum FrontendOp {
	ADD   (FL32R_SIZE * 1, REG, REG, REG),
	ADDI  (FL32R_SIZE * 1, REG, IMM19_ABS),
	JMP   (FL32R_SIZE * 1, IMM24_PC_REL),
	LDW   (FL32R_SIZE * 1, REG, MEMORY),
	STW   (FL32R_SIZE * 1, MEMORY, REG),
	// pseudo-op (instructions that expand into other instruction(s))
	LDI   (FL32R_SIZE * 2, REG, IMM32_ABS),
	LD    (FL32R_SIZE * 1, REG, VARIABLE),
	;
	
	// cunt
	private final int size;
	private final OperandKind[] operands;
	
	public static final FrontendOp[] values = FrontendOp.values();
	
	FrontendOp(int size, OperandKind... operands) {
		if (size == 0 || size % FL32R_SIZE != 0) {
			throw new IllegalArgumentException("what are you cooking boy");
		}
		this.size = size;
		this.operands = operands;
	}
	
	public int getSize() {
		return size;
	}
	
	public OperandKind[] getOperandKinds() {
		return operands;
	}
	
	public static FrontendOp get(String str) {
		try {
			return FrontendOp.valueOf(str);
		} catch (Exception e) {
			return null;
		}
	}
	
	public boolean isPseudoOp() {
		return size != FL32R_SIZE;
	}
}
