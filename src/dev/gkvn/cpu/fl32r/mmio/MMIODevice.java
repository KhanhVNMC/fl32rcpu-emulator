package dev.gkvn.cpu.fl32r.mmio;

public interface MMIODevice {
	int getBaseAddress();
	int getSize();
	
	byte readByte(int address);
	int  readWord(int address);
	
	void writeWord(int address, int value);
	void writeByte(int address, byte value);
}