package dev.gkvn.cpu.assembler.fl32r.parser;

import dev.gkvn.cpu.assembler.fl32r.lexer.Token;

public record DataSymbol(String name, int address, int elementSize, int elementCount, Token owner) {
	public int totalSize() {
		return elementSize * elementCount;
	}
}
