package dev.gkvn.cpu.fl32r.mmio.devs;

import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class DebugConsoleMMIO extends AbstractMMIODevice {
	// register offsets
	private static final int REG_TX_CHAR = 0x00;
	private static final int REG_TX_WORD = 0x04;
	private static final int REG_STATUS = 0x08;

	public DebugConsoleMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 16);
	}

	@Override
	public byte readByte(int address) {
		if (offset(address) == REG_STATUS) {
			return 1; // always ready
		}
		return 0;
	}

	@Override
	public int readWord(int address) {
		if (offset(address) == REG_STATUS) {
			return 1;
		}
		return 0;
	}

	@Override
	public void writeByte(int address, byte value) {
		if (offset(address) == REG_TX_CHAR) {
			char c = (char) (value & 0xFF);
			System.out.print(c);
			return;
		}
	}
	
	@Override
	public void writeWord(int address, int value) {
		if (offset(address) == REG_TX_WORD) {
			System.out.printf("0x%08X%n", value);
			return;
		}
		// allow word write to char reg (lower byte only)
		writeByte(address, (byte) (value & 0xFF));
	}
}