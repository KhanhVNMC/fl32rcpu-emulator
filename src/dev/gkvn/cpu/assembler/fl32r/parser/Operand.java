package dev.gkvn.cpu.assembler.fl32r.parser;

public sealed interface Operand permits RegisterOperand, ImmOperand, MemoryOperand {}