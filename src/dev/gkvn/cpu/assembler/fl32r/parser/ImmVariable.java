package dev.gkvn.cpu.assembler.fl32r.parser;

import dev.gkvn.cpu.assembler.fl32r.lexer.Token;

public record ImmVariable(String varname, int expectedBitWidth, int offset, Token owner) implements ImmOperand {
	@Override
	public String toString() {
		return String.format("ImmVariable%dPCRel(" + varname + "[" + offset + "])", expectedBitWidth);
	}
}
