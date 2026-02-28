package dev.gkvn.cpu.fl32r.assembler.frontend.operands;

public sealed interface Operand permits RegisterOperand, ImmOperand, MemoryOperand {}