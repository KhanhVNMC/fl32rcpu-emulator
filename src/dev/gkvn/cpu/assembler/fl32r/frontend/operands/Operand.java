package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

public sealed interface Operand permits RegisterOperand, ImmOperand, MemoryOperand {}