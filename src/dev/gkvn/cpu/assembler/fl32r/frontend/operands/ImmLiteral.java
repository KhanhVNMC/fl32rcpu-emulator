package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

public record ImmLiteral(int value) implements ImmOperand {
	@Override
	public String toString() {
		return "ImmLiteral(" + Integer.toHexString(value).toUpperCase() + ")";
	}
}
