package dev.gkvn.cpu.assembler.fl32r.frontend.core;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;

public record DataSymbol(
	String name, int addressOffset, 
	int elementSize, int elementCount, 
	Token owner
) {
	public int totalSize() {
		return elementSize * elementCount;
	}
}
