package dev.gkvn.cpu.assembler.fl32r.frontend.core;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;

public record LabelText(String name, int address, Token owner) {}
