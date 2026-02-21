package dev.gkvn.cpu.fl32r.mmio;

public abstract class AbstractMMIODevice implements MMIODevice {
	protected final int base;
	protected final int size;
	protected final FL32RMMIO mmio;

	protected AbstractMMIODevice(FL32RMMIO mmio, int base, int size) {
		this.base = base;
		this.size = size;
		this.mmio = mmio;
	}
	
	@Override
	public long getBaseAddress() {
		return base;
	}
	
	@Override
	public int getSize() {
		return size;
	}
	
	protected void interrupt(int code) {
		mmio.emulator.hardwareIRQ(code);
	}
}