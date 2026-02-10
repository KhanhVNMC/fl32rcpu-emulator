package dev.gkvn.cpu.assembler.fl32r.parser;

public sealed interface ImmOperand extends Operand 
permits ImmLiteral, ImmLabel, ImmVariable, MemOpAddress {}
