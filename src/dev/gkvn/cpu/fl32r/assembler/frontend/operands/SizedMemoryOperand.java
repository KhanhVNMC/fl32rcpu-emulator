package dev.gkvn.cpu.fl32r.assembler.frontend.operands;

public record SizedMemoryOperand(MemoryOperand memop, int sizeByte) implements ImmOperand {
	@Override
	public final String toString() {
		return "Sized(" + memop.toString() + ", sizeByte=" + sizeByte + ")";
	}
}
