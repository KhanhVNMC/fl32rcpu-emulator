package dev.gkvn.cpu.fl32r.assembler.frontend.core;

import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;

public record DataSymbol(
	String name, int addressOffset, 
	int elementSize, int elementCount, 
	Token owner
) {
	public int totalSize() {
		return elementSize * elementCount;
	}
}
