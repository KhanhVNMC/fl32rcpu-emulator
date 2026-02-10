package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

import dev.gkvn.cpu.assembler.fl32r.frontend.utils.FL32RSpecs;
import dev.gkvn.cpu.fl32r.FL32RConstants;

public record RegisterOperand(int register) implements Operand {
	
	public RegisterOperand {
		if (register < 0 || register >= FL32RSpecs.FL32R_REGISTERS_COUNT) {
			throw new IllegalArgumentException("Invalid register index: " + register);
		}
	}
	
	@Override
	public String toString() {
		return "HR(" + register + ")";
	}
	
	public static final RegisterOperand PC_REG = new RegisterOperand(FL32RConstants.REG_PROGRAM_COUNTER);
}
