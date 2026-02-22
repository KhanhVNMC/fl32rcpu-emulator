package dev.gkvn.cpu.fl32r.mmio;

import dev.gkvn.cpu.fl32r.FL32REmulator;

public abstract class AbstractMMIODevice implements MMIODevice {
	protected final int base;
	protected final int size;
	protected final FL32RMMIO mmio;

	protected AbstractMMIODevice(FL32RMMIO mmio, int base, int size) {
		this.base = base;
		this.size = size;
		this.mmio = mmio;
	}
	
	// evil bit hack type shit
	// byte is 2nd class citizen (unless defined otherwise)
	@Override
	public byte readByte(int address) {
		int shift = (address & 3) * 8;
		return (byte) ((readWord(address & ~3) >>> shift) & 0xFF);
	}

	@Override
	public void writeByte(int address, byte value) {
		int shift = (address & 3) * 8; // just like (addr % 4) * 8
		int mask = 0xFF << shift;
		// floor(addr / 4) * 4 === addr & ~3
		int word = readWord(address & ~3); // find the word from the byte address
		word = (word & ~mask) | ((value & 0xFF) << shift);
		writeWord(address & ~3, word);
	}
	
	@Override
	public int getBaseAddress() {
		return base;
	}
	
	@Override
	public int getSize() {
		return size;
	}
	
	public int offset(int address) {
		return address - base;
	}
	
	public FL32REmulator getEmulator() {
		return mmio.emulator;
	}
	
	public void interrupt(int code) {
		mmio.emulator.hardwareIRQ(code);
	}
}