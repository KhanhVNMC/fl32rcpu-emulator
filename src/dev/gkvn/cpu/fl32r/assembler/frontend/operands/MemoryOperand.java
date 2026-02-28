package dev.gkvn.cpu.fl32r.assembler.frontend.operands;

public record MemoryOperand(RegisterOperand base, ImmOperand offset) implements Operand {
	@Override
	public String toString() {
		if (offset == null) {
			return "Mem[" + base + "]";
		}
		return "Mem[" + base + " + " + offset + "]";
	}
}
