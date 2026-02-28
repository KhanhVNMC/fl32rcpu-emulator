package dev.gkvn.cpu.fl32r.assembler.frontend;

import java.util.Map;

import dev.gkvn.cpu.fl32r.assembler.frontend.core.DataSymbol;

public record IRDataSection(Map<String, DataSymbol> symbolTable, byte[] dataBytes) {}
