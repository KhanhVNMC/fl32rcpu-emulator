package dev.gkvn.cpu.assembler.fl32r.frontend.arch;

public enum OperandKind {
    REG,
    MEMORY,
    VARIABLE(0, true),
    IMM14_ABS(14, false),
    IMM16_ABS(16, false),
    IMM19_ABS(19, false),
    IMM24_ABS(24, false),
    IMM24_PC_REL(24, true),
    IMM32_ABS(32, false);
	
	public final int bitWidth;
    public final boolean pcRelative;
    
    OperandKind(int bitWidth, boolean pcRelative) {
        this.bitWidth = bitWidth;
        this.pcRelative = pcRelative;
    }
    
    OperandKind() {
        this(0, false);
    }
}
