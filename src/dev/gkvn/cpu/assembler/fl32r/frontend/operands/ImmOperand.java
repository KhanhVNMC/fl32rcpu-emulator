package dev.gkvn.cpu.assembler.fl32r.frontend.operands;

public sealed interface ImmOperand extends Operand 
permits ImmLiteral, ImmLabel, ImmVariable, MemOpAddress {}
