package dev.gkvn.cpu.fl32r.assembler.frontend.operands;

public record ImmLiteral(int value) implements ImmOperand {
	@Override
	public String toString() {
		return "ImmLiteral(0x" + Integer.toHexString(value).toUpperCase() + ")";
	}
}
