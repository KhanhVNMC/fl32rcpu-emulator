package dev.gkvn.cpu;

import dev.gkvn.cpu.fl32r.FL32REmulator;
import dev.gkvn.cpu.fl32r.Utils;

import static dev.gkvn.cpu.fl32r.FL32RConstants.*;
import static dev.gkvn.cpu.fl32r.FL32RHelper.*;

import java.util.Arrays;

public class Test implements Cloneable {
	public static void main(String[] args) {
		FL32REmulator emu = new FL32REmulator(1024);
		emu.setFrequencyHz(-1);
		
		int[] text = new int[] {
			U(LUI, 0, 0xCAFE),
			I(ORI, 0, 0xBABE),
			M(STW, 0, 1, Utils.convertIntToU14(16)),
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
		ReadOnlyByteMemory mem = emu.dumpMemory();
		int reg[] = emu.dumpRegisters();
		for (int i = 0; i < reg.length; i++) {
			System.out.println("R" + i + ": 0x" + Integer.toHexString(reg[i]) + " | " + Integer.toUnsignedLong(reg[i]));
		}
		for (long i = 0; i < 1024; i++) {
			System.out.print(String.format("%02x ", mem.get(i)).toUpperCase());
			if ((i + 1) % 16 == 0) {
				System.out.println();
			}
		}
	}
}
