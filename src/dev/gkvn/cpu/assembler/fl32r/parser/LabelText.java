package dev.gkvn.cpu.assembler.fl32r.parser;

import dev.gkvn.cpu.assembler.fl32r.lexer.Token;

public record LabelText(String name, int address, Token owner) {}
