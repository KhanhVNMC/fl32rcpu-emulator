package dev.gkvn.cpu.assembler.fl32r.parser;

public record MemOpAddress(long address, int width) implements ImmOperand {
	@Override
	public final String toString() {
		return "MemOpAddress(" + address + ", size=" + width + ")";
	}
}
