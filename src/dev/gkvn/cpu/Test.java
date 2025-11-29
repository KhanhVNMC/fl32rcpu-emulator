package dev.gkvn.cpu;

import dev.gkvn.cpu.fl32r.FL32REmulator;
import static dev.gkvn.cpu.fl32r.FL32RConstants.*;
import static dev.gkvn.cpu.fl32r.FL32RHelper.*;

import java.util.Arrays;

public class Test implements Cloneable {
	public static void main(String[] args) {
		FL32REmulator emu = new FL32REmulator(1024);
		emu.setFrequencyHz(-1);
		
		int[] text = new int[] {
			// LUI   R5, 0xABCD ; 16-bit upper bits
			U(LUI, 1, 0xFF),
			// MOV   R1, ZERO
			R(MOV, 1, REG_ZERO, 0),
			// ORI   R1, 5
			I(ORI, 1, 5),
			// ADDI  R1, 10
			I(ADDI, 1, 10),
			// ADDI  R1, 20
			I(ADDI, 1, 20),
			// MOV   R0, R1
			R(MOV, 0, 1, 0),
			// ADD   R2, R0, R1
			R(ADD, 2, 0, 1),
			// PUSH  R2
			R(PUSH, 2, 0, 0),
			// POP   R3
			R(POP, 3, 0, 0),
			// PUSH  R3
			R(PUSH, 3, 0, 0),
			// JMP   +4
			J(JMP, +4),
			// KILL
			J(KILL, 0),
			// MOV  R8, RPC
			R(MOV, 8, REG_PROGRAM_COUNTER, 0),
			// KILL
			J(KILL, 0), 
		};
		
		byte[] boot = new byte[text.length * 4];
		for (int i = 0; i < text.length; i++) {
			boot[i * 4 + 0] = (byte) ((text[i] >> 24) & 0xFF);
			boot[i * 4 + 1] = (byte) ((text[i] >> 16) & 0xFF);
			boot[i * 4 + 2] = (byte) ((text[i] >> 8) & 0xFF);
			boot[i * 4 + 3] = (byte) (text[i] & 0xFF);
		}
		emu.loadBootProgram(boot);
		emu.start(false);
		emu.dumpMemory();
		int reg[] = emu.dumpRegisters();
		for (int i = 0; i < reg.length; i++) {
			System.out.println("R" + i + ": 0x" + Integer.toHexString(reg[i]) + " | " + Integer.toUnsignedLong(reg[i]));
		}
	}
}
