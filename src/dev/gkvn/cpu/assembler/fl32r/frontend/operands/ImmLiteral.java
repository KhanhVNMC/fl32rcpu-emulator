package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

public record ImmLiteral(long value) implements ImmOperand {
	@Override
	public String toString() {
		return "ImmLiteral(" + value + ")";
	}
}
