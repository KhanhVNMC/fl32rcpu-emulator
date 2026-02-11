package dev.gkvn.cpu.assembler.fl32r.frontend;

import java.util.Map;

import dev.gkvn.cpu.assembler.fl32r.frontend.core.DataSymbol;

public record IRDataSection(Map<String, DataSymbol> symbolTable, byte[] dataBytes) {}
