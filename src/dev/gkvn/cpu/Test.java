package dev.gkvn.cpu;

import dev.gkvn.cpu.fl32r.FL32REmulator;
import static dev.gkvn.cpu.fl32r.FL32RConstants.*;

import java.util.Arrays;

public class Test {
	public static void main(String[] args) {
		FL32REmulator emu = new FL32REmulator(1024);
		emu.setFrequencyHz(-1);
		
		int[] text = new int[] {
			// LUI    R5, 0xABCD ; 16-bit upper bits
			(LUI << 24) | (1 << 19) | (0xff << 3), // I-type 2 [opcode 0...7][reg 8...12][imm (16) 13...28]
			(MOV << 24) | (1 << 19) | (REG_ZERO << 14),
			(ORI << 24) | (1 << 19) | 5,
			// ADDI   R1, 10
			(ADDI << 24) | ((1 << 19) | 10), // I-type [opcode 0..7][reg 8...12][imm (19) 13...31]
			// ADDI   R1, 20
 			(ADDI << 24) | ((1 << 19) | 20),
 			// MOV    R0, R1
 			(MOV << 24) | ((0 << 19) | (1 << 14)), // R-type [opcode 0..7][reg0 8...12][reg1 13...17][reg3 18...22] 
 			// ADD    R2, R0, R1
 			(ADD << 24) | ((2 << 19) | (0 << 14) | (1 << 9)), 
 			// PUSH   R2
			(PUSH << 24) | (2 << 19), 
 			// POP   R3
			(POP << 24) | (3 << 19), 
			(PUSH << 24) | (3 << 19), 
			(JMP << 24) | 4, // J-type [opcode 0..7][immediate 8...31]
			(KILL << 24), 
			(ADDI << 24) | (8 << 19) | 36,
			(KILL << 24),
		};
		
		byte[] boot = new byte[text.length * 4];
		for (int i = 0; i < text.length; i++) {
			boot[i * 4 + 0] = (byte) ((text[i] >> 24) & 0xFF);
			boot[i * 4 + 1] = (byte) ((text[i] >> 16) & 0xFF);
			boot[i * 4 + 2] = (byte) ((text[i] >> 8) & 0xFF);
			boot[i * 4 + 3] = (byte) (text[i] & 0xFF);
		}
		emu.loadBootProgram(boot);
		emu.start();
		int reg[] = emu.dumpRegisters();
		for (int i = 0; i < reg.length; i++) {
			System.out.println("R" + i + ": " + Integer.toUnsignedLong(reg[i]));
		}
	}
}
