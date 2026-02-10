package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;

public record ImmVariable(String varname, int offset, int expectedBitWidth, boolean pcRelative, Token owner) implements ImmOperand {
	@Override
	public String toString() {
		return String.format(
			"ImmVariable%d(" + varname + "[" + offset + "], pcRel=" + pcRelative + ")", 
			expectedBitWidth
		);
	}
}
