package dev.gkvn.cpu.assembler.fl32r.parser;

public record ImmLiteral(long value) implements ImmOperand {
	@Override
	public String toString() {
		return "ImmLiteral(" + value + ")";
	}
}
