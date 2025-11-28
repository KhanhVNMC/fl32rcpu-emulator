package dev.gkvn.cpu.fl32r;

public class FL32RHelper {
	/**
	 * U-TYPE instruction encoder.
	 *
	 * Format: [ opcode:8 | rd:5 | imm16:16 | unused:3 ]
	 *
	 * Used by LUI (load upper immediate).
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
		return (opcode << 24) | (register << 19) | (upper16Bits << 3);
	}

	/**
	 * R-TYPE instruction encoder (register–register).
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
	 * @param signedImm24bit 24-bit immediate (masked to 0xFFFFFF).
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
	 * @param imm19Bit 19-bit immediate.
	 *
	 * @return Encoded 32-bit instruction word.
	 */
	public static int I(int opcode, int register, int imm19Bit) {
		opcode &= 0xFF;
		register &= 0b11111;
		imm19Bit &= 0x7FFFF;
		return (opcode << 24) | (register << 19) | imm19Bit;
	}
}
