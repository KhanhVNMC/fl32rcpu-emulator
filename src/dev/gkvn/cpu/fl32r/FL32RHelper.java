package dev.gkvn.cpu.fl32r;

public class FL32RHelper {
	/**
	 * NO-TYPE (no-operands) instruction encoder
	 * 
	 * Format: [ opcode:8 | unused:24 ]
	 * 
	 * Straight fucking forward.
	 * 
	 * @param opcode      8-bit opcode value.
	 * 
	 * @return Encoded 32-bit instruction.
	 */
	public static int NO(int opcode) {
		opcode &= 0xFF;
		return (opcode << 24);
	}
	
	/**
	 * U-TYPE instruction encoder.
	 *
	 * Format: [ opcode:8 | rd:5 | imm16:16 | unused:3 ]
	 *
	 * Used by LUI (load upper immediate) and LLI (load lower imm).
	 *
	 * @param opcode      8-bit opcode value.
	 * @param register    5-bit destination register (rd).
	 * @param upper16Bits 16-bit immediate placed into bits 18..3.
	 *
	 * @return Encoded 32-bit instruction.
	 */
	public static int U(int opcode, int register, int upper16Bits) {
		opcode &= 0xFF;
		register &= 0b11111;
		upper16Bits &= 0xFFFF;
		return (opcode << 24) | (register << 19) | (upper16Bits << 3);
	}

	/**
	 * R-TYPE instruction encoder (register<->register).
	 *
	 * Format: [ opcode:8 | rd:5 | rs1:5 | rs2:5 | unused:9 ]
	 *
	 * Used for MOV, arithmetic ops, bitwise ops, CMP, LD/ST register fields, etc.
	 *
	 * @param opcode 8-bit opcode.
	 * @param reg0   destination register (rd).
	 * @param reg1   source register 1 (rs1).
	 * @param reg2   source register 2 (rs2).
	 *
	 * @return Encoded 32-bit instruction.
	 */
	public static int R(int opcode, int reg0, int reg1, int reg2) {
		opcode &= 0xFF;
		reg0 &= 0b11111;
		reg1 &= 0b11111;
		reg2 &= 0b11111;
		return (opcode << 24) | (reg0 << 19) | (reg1 << 14) | (reg2 << 9);
	}

	/**
	 * J-TYPE instruction encoder (24-bit immediate jump/call).
	 *
	 * Format: [ opcode:8 | imm24:24 ]
	 *
	 * Used for JMP, JEQ/JNE/etc., CALL, JR, CLR. The immediate may be signed or
	 * unsigned depending on instruction.
	 *
	 * @param opcode         8-bit opcode.
	 * @param signedImm24bit 24-bit immediate (signed, 2's complement).
	 *
	 * @return Encoded 32-bit instruction.
	 */
	public static int J(int opcode, int signedImm24bit) {
		opcode &= 0xFF;
		signedImm24bit &= 0xFFFFFF;
		return (opcode << 24) | signedImm24bit;
	}

	/**
	 * I-TYPE instruction encoder (register + 19-bit immediate).
	 *
	 * Format: [ opcode:8 | rd:5 | imm19:19 ]
	 *
	 * Used for immediate arithmetic and bitwise instructions: ADDI, ANDI, ORI, XORI
	 *
	 * @param opcode   8-bit opcode.
	 * @param register 5-bit destination register (rd).
	 * @param imm19Bit 19-bit immediate (signed).
	 *
	 * @return Encoded 32-bit instruction word.
	 */
	public static int I(int opcode, int register, int imm19Bit) {
		opcode &= 0xFF;
		register &= 0b11111;
		imm19Bit &= 0x7FFFF;
		return (opcode << 24) | (register << 19) | imm19Bit;
	}
	
	/**
	 * M-TYPE instruction encoder (register + register + 14-bit immediate).
	 *
	 * Format: [ opcode:8 | rd_or_rs:5 | rs1:5 | imm14:14 ]
	 *
	 * This format is used by memory-access instructions such as LDW, LDB, STW, STB,
	 * and any other operations requiring two registers plus a 14-bit offset.
	 *
	 * The meaning of the first two register fields depends on the instruction: 
	 * - For LOAD: rd = destination register, rs1 = base register. 
	 * - For STORE: rd = source register, rs1 = base register.
	 *
	 * The low 14 bits encode the unsigned immediate offset. If the instruction
	 * requires a signed displacement, it is sign-extended by the execution unit.
	 *
	 * @param opcode    8-bit opcode.
	 * @param register0 First register field (rd or rs depending on instruction).
	 * @param register1 Second register field (base register, rs1).
	 * @param imm14Bit  14-bit immediate (signed, 2's complement).
	 *
	 * @return Encoded 32-bit instruction.
	 */
	public static int M(int opcode, int register0, int register1, int imm14Bit) {
		opcode &= 0xFF;
		register0 &= 0b11111;
		register1 &= 0b11111;
		imm14Bit &= 0x3FFF;
		return (opcode << 24) | (register0 << 19) | (register1 << 14) | imm14Bit;
	}
	
}
