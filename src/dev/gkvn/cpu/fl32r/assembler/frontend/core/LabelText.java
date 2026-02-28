package dev.gkvn.cpu.fl32r.assembler.frontend.core;

import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;

public record LabelText(String name, int address, Token owner) {}
