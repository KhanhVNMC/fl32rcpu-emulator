package dev.gkvn.cpu.fl32r.assembler.frontend.operands;

import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;

public record ImmVariable(String varname, int offset, int expectedBitWidth, boolean pcRelative, Token owner) implements ImmOperand {
	@Override
	public String toString() {
		return String.format(
			"ImmVariable%d(" + varname + "[" + offset + "], pcRel=" + pcRelative + ")", 
			expectedBitWidth
		);
	}
}
