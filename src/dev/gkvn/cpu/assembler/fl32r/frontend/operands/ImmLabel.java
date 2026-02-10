package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;

public record ImmLabel(String label, int expectedBitWidth, boolean pcRelative, Token owner) implements ImmOperand {
	@Override
	public String toString() {
		return String.format(pcRelative ? "ImmLabel%d(pcrel=" + label + ")" : "ImmLabel%d(" + label + ")", expectedBitWidth);
	}
}
