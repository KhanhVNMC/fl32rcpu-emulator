package dev.gkvn.cpu.fl32r.assembler.frontend.operands;

public sealed interface ImmOperand extends Operand 
permits ImmLiteral, ImmLabel, ImmVariable, SizedMemoryOperand {}
