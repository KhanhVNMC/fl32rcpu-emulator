package dev.gkvn.cpu.assembler.fl32r.frontend.arch;

public enum FEOperandType {
    REG, // classic register: R0, R1, R2, ...
    MEMORY, // [baseRegister + offset] or [baseRegister]
    IMM14_ABS(14, false),
    IMM16_ABS(16, false),
    IMM19_ABS(19, false),
    IMM24_ABS(24, false),
    IMM24_PC_REL(24, true),
    IMM32_ABS(32, false),
    // SPECIAL type of MEMORY operand
    VARIABLE, // $variable (expands into a MEMORY operand, context dependent)
    VARIABLE_OR_LABEL // accept only a label or a variable (LEA)
    ;
	
	public final int bitWidth;
    public final boolean pcRelative;
    
    FEOperandType(int bitWidth, boolean pcRelative) {
        this.bitWidth = bitWidth;
        this.pcRelative = pcRelative;
    }
    
    FEOperandType() {
        this(0, false);
    }
}
