package dev.gkvn.cpu.fl32r.mmio.devs;

import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class VGAGraphicsMMIO extends AbstractMMIODevice {
	public VGAGraphicsMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 32);
	}

	@Override
	public byte readByte(int address) {
		return 0;
	}

	@Override
	public int readWord(int address) {
		return 0;
	}
	
	@Override
	public void writeByte(int address, byte value) {
		
	}
	
	@Override
	public void writeWord(int address, int value) {
		
	}
}