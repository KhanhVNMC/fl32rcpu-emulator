package dev.gkvn.cpu.fl32r.mmio;

public interface MMIODevice {
	int getBaseAddress();
	int getSize();
	
	int readByte(int address);
	void writeByte(int address, int value);
}