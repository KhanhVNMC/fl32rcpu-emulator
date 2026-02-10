package dev.gkvn.cpu.assembler.fl32r.parser;

public record MemoryOperand(RegisterOperand base, ImmOperand offset) implements Operand {
	@Override
	public String toString() {
		if (offset == null) {
			return "Mem[" + base + "]";
		}
		return "Mem[" + base + " + " + offset + "]";
	}
}
