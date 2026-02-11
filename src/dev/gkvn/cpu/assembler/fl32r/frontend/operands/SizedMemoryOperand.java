package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

public record SizedMemoryOperand(MemoryOperand memop, int sizeByte) implements ImmOperand {
	@Override
	public final String toString() {
		return "Sized(" + memop.toString() + ", sizeByte=" + sizeByte + ")";
	}
}
